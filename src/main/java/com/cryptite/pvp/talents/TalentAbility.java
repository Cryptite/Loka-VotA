package com.cryptite.pvp.talents;

import com.cryptite.pvp.CustomEffect;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.listeners.SpeedRampHandler;
import com.cryptite.pvp.utils.Combat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.cryptite.pvp.utils.LocationUtils.playCustomSound;
import static java.util.Arrays.asList;

public class TalentAbility {
    private final LokaVotA plugin;

    //Numericals
    private final int cooldown;
    private final int length;
    private int abilitySecondsLeft = 0;
    private int cooldownSecondsLeft = 0;

    //Talent identifiers
    public final String name;
    public final Talent talent;
    public final Material material;
    public ItemStack item;

    //Talent states
    public boolean available = true;
    public boolean active = false;
    private boolean playerDead = false;

    //Talent tasks
    private int taskId = 0;
    private int countdownBarTask;
    private int cooldownTask;

    //Trap stuff
    private Material machineBlockMaterial;

    //Misc
    private final Combat combat;
    private byte machineBlockData;
    private final List<Material> invalidTrapMaterials = new ArrayList<>(asList(Material.AIR, Material.LEAVES, Material.LEAVES_2));
    private SpeedRampHandler speedRampHandler;

    public TalentAbility(LokaVotA plugin, Talent talent, String name, Material material, int length, int cooldown) {
        this.plugin = plugin;
        this.talent = talent;
        this.name = name;
        this.material = material;
        this.length = length;
        this.cooldown = cooldown;

        combat = new Combat(plugin);
    }

    public void activate(final PvPPlayer p, ItemStack talentItem) {

        if (!talentEffects(p.getPlayer())) {
            //This only happens with Trap atm, but if it fails, don't eat the cooldown.
            return;
        }

        available = false;
        cooldownSecondsLeft = cooldown;
        item = talentItem;

        //If this is a duration ability.
        if (length > 0) {
            active = true;

            abilitySecondsLeft = length;

            //Ability duration task
            plugin.scheduler.runTaskLater(plugin,
                    () -> deactivate(p), 20 * length
            );
        }

        //Cooldown duration task
        plugin.scheduler.runTaskLater(plugin,
                () -> {
                    setItemAvailable(p);
                    available = true;
                }, 20 * cooldown
        );

        //XP Bar cooldown
        countdownBarTask = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                () -> {
                    if (abilitySecondsLeft > 0) {
                        updateXPBar(p.getPlayer(), abilitySecondsLeft);
                    }
                    abilitySecondsLeft -= 1;
                }, 0, 20
        );

//        startCooldownIndicator(p);

        if (!plugin.talents.itemlessTalents.contains(talent)) {
            if (talent.equals(Talent.EXPLOSIVE_ARROW)) {
                if (p.getPlayer() == null) return;

                ItemStack item = getAbilityItem(p.getPlayer());
                if (item != null) item.setType(Material.COAL);
            } else if (talent.equals(Talent.HEAL_ARROW)) {
                if (p.getPlayer() == null) return;

                ItemStack item = getAbilityItem(p.getPlayer());
                if (item != null) item.setType(Material.COAL);
            } else {
                item.setType(Material.COAL);
            }
        }
    }

    public void startCooldownIndicator(final PvPPlayer p) {
        //Inventory ItemStack amount cooldown
        cooldownTask = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                () -> {
                    if (p.getPlayer().isDead()) playerDead = true;

                    item = getCoalItem(p.getPlayer());

                    if (!playerDead
                            && item != null
                            && cooldownSecondsLeft > 0
                            && !plugin.talents.itemlessTalents.contains(talent)) {
                        item.setAmount(cooldownSecondsLeft);
                    }
                    cooldownSecondsLeft -= 2;

//                        //If player WAS dead and no longer is, get that item and carry on.
                    if ((playerDead && !p.getPlayer().isDead())) {
                        playerDead = false;
                    }
                }, 0, 40
        );
    }

    public void deactivate(PvPPlayer p) {
        plugin.scheduler.cancelTask(taskId);
        plugin.scheduler.cancelTask(countdownBarTask);

        if (!active) return;
        active = false;

        if (p.getPlayer() == null) return;

        p.getPlayer().setExp(0);
        offEffect(p);
    }

    void setItemAvailable(PvPPlayer p) {
        //Player might've DC'd or something, so let's be sure.
        if (p.getPlayer() == null) return;

        plugin.scheduler.cancelTask(cooldownTask);

        //Set ability available
        if (!plugin.talents.itemlessTalents.contains(talent)) {

            //If they went offline
            if (p.getPlayer() == null) return;

            item = getCoalItem(p.getPlayer());
            if (item == null) return;

            item.setType(material);
            item.setAmount(1);
        }

        playCustomSound(p.getPlayer(), "AbilityAvailable");
    }

    private ItemStack getCoalItem(Player p) {
        for (ItemStack i : p.getInventory()) {
            if (i != null
                    && i.getType().equals(Material.COAL)
                    && i.getItemMeta().getDisplayName() != null
                    && ChatColor.stripColor(i.getItemMeta().getDisplayName()).equalsIgnoreCase(name)) {
                return i;
            }
        }

        return null;
    }

    private ItemStack getAbilityItem(Player p) {
        for (ItemStack i : p.getInventory()) {
            if (i != null
                    && !i.getType().equals(Material.COAL)
                    && i.getItemMeta().getDisplayName() != null
                    && ChatColor.stripColor(i.getItemMeta().getDisplayName()).equalsIgnoreCase(name)) {
                return i;
            }
        }

        return null;
    }

    void updateXPBar(Player p, int secondsLeft) {
        if (p == null) return;
        float increment = (float) secondsLeft / (float) length;
        p.setExp(increment);
    }

    Boolean talentEffects(final Player p) {
        switch (talent) {
            case BANDAGE:
                playCustomSound(p, "Heal");

                new Bandage(p, length);
                break;
            case LAST_STAND:
                playCustomSound(p, "LastStand");

                plugin.effect(p.getLocation().add(0, 1, 0), CustomEffect.HAPPY_VILLAGER, 1, .1f, 2f);

                taskId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                        () -> {
                            if (p.isDead()) return;
                            if (new Random().nextInt(2) == 0) {
                                plugin.smallEffect(p.getLocation().clone().add(.4f, 1, -.4f), CustomEffect.HAPPY_VILLAGER, 2, .3f, 5);
                            } else {
                                plugin.smallEffect(p.getLocation().clone().add(-.4f, 1, .4f), CustomEffect.HAPPY_VILLAGER, 2, .3f, 5);
                            }
                        }, 5, 2
                );
                break;

            case QUAKE:
                plugin.blockEffect(p.getLocation().clone().add(0, .3f, 0), CustomEffect.BLOCKDUST, Material.DIRT.getId(), 0, 25, .15f, 5);
                playCustomSound(p, "Absorb_Cast");

                PvPPlayer quakeOwner = plugin.getAccount(p.getName());
                final List<String> playersQuaked = combat.aoeKnockback(p, quakeOwner, p.getLocation(), 10, 6.1, 0, 1.2f, null, Talent.QUAKE);

                //Add players to rooted list after 2 seconds (about how long the airtime should last.
                plugin.scheduler.runTaskLater(plugin, () -> {
                    plugin.playersRooted.addAll(playersQuaked);
                    plugin.lengthyEffect(playersQuaked, 5, CustomEffect.BLOCKDUST, Material.DIRT.getId(), (byte) 0, 4, .04f, 6);
                }, 20 * 2);


                //After 5 seconds, remove the players from the rooted list.
                plugin.scheduler.runTaskLater(plugin, () -> plugin.playersRooted.removeAll(playersQuaked), 20 * 5);
                break;

            case LUNGE:
                playCustomSound(p, "Lunge");

                //Cloud poof
                plugin.effect(p.getLocation(), CustomEffect.CLOUD, 10, .1f);

                //Lunge
                p.setVelocity(p.getLocation().getDirection().multiply(2).add(new Vector(0, 1.15, 0)));

                //Lunge effect
                int lungeID = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                        () -> plugin.smallEffect(p.getLocation().add(0, .2f, 0), CustomEffect.CLOUD, 1, .1f, 5), 0, 1);
                plugin.scheduler.runTaskLater(plugin,
                        () -> plugin.scheduler.cancelTask(lungeID), 30);
                break;

            case ENDER_HOOK:
                playCustomSound(p, "Resolve");

//                taskId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
//                        () -> plugin.effect(p.getLocation(), CustomEffect.FLAME, 3, .05f, 5), 5, 5
//                );
//
//                //If they have a slow, remove it.
//                if (p.hasPotionEffect(PotionEffectType.SLOW)) {
//                    p.removePotionEffect(PotionEffectType.SLOW);
//                }
//
//                //Apply speed buff
//                if (plugin.getAccount(p.getName()).talents.contains(Talent.FLEET_FOOTED)) {
//                    //If they have Fleet Footed, they already have Speed 1 permanently up, so up to 2.
//                    for (PotionEffect potion : p.getActivePotionEffects()) {
//                        if (potion.getType().equals(PotionEffectType.SPEED)) {
//                            p.removePotionEffect(potion.getType());
//                            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * length, 2));
//                            break;
//                        }
//                    }
//
//                    //After ability is over, we need to give them fleet footed back
//                    plugin.scheduler.runTaskLater(plugin,
//                            () -> {
//                                if (!p.isDead()) {
//                                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50000, 0));
//                                }
//                            }, 20 * length + 10
//                    );
//                } else {
//                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * length, 2));
//                }
                break;

            case EXPLOSIVE_ARROW:
                playCustomSound(p, "ExplosiveArrow");

                //Apply explosive arrow loaded particles.
                taskId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                        () -> plugin.effect(p.getLocation().add(0, .5f, 0), CustomEffect.FIREWORKS_SPARK, 1, .1f, 2f), 5, 10
                );
                break;

            case HEAL_ARROW:
                playCustomSound(p, "Heal");

                //Apply explosive arrow loaded particles.
                taskId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                        () -> plugin.effect(p.getLocation().add(0, .5f, 0), CustomEffect.MAGIC_CRIT, 1, .1f, 2f), 5, 10
                );
                break;

            case RALLYING_CRY:
                playCustomSound(p, "SiegeMachineBuff");

                //Grant friendlies Strength
                PvPPlayer owner = plugin.getAccount(p.getName());
                final List<Player> friendliesAffected = combat.aoeBuffEffect(owner, p.getLocation(), 15, PotionEffectType.INCREASE_DAMAGE, 0, 6, "SiegeMachineBuff");
                friendliesAffected.add(p);

                taskId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                        () -> {
                            for (Player friendly : friendliesAffected) {
                                if (friendly == null || friendly.isDead()) continue;
                                plugin.effect(friendly.getLocation().add(0, .5f, 0), CustomEffect.RED_DUST, 5, .1f, 5);
                            }
                        }, 5, 5
                );

                //Give the friendlies a potion and speed
                for (final Player friendly : friendliesAffected) {
                    if (friendly == null || friendly.isDead()) continue;
                    addPotion(friendly);

                    //Apply speed buff
                    if (plugin.getAccount(friendly.getName()).talents.contains(Talent.FLEET_FOOTED)) {
                        //If they have Fleet Footed, they already have Speed 1 permanently up, so up to 2.
                        for (PotionEffect potion : friendly.getActivePotionEffects()) {
                            if (potion.getType().equals(PotionEffectType.SPEED)) {
                                friendly.removePotionEffect(potion.getType());
                                friendly.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * length, 2));
                                break;
                            }
                        }

                        //After ability is over, we need to give them fleet footed back
                        plugin.scheduler.runTaskLater(plugin,
                                () -> {
                                    if (!friendly.isDead()) {
                                        friendly.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50000, 0));
                                    }
                                }, 20 * length + 10
                        );
                    } else {
                        friendly.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * length, 2));
                    }
                }

                //Activate the ground speed ramp
                plugin.speedRamps.ramps.put(p.getName(), p.getLocation());

                //Play trap FX for owner
                taskId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                        () -> plugin.effect(plugin.speedRamps.ramps.get(p.getName()).clone().add(.5, 0, .5), CustomEffect.FIREWORKS_SPARK, 1, .1f, 2f), 5, 10
                );

                plugin.speedRamps.rampTasks.put(p.getName(), taskId);

                //Cancel the ramp
                plugin.scheduler.runTaskLater(plugin, () -> {
                    plugin.speedRamps.ramps.remove(p.getName());
                    plugin.speedRamps.rampTasks.remove(p.getName());
                }, 20 * length);
                break;

            case LIFE_STEAL:
                playCustomSound(p, "LifeSteal");

                //Give Regen
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 5, 0));
                break;

            case IRON_FORM:
                playCustomSound(p, "IronForm");

                p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * length, 1));
                break;

            case FREEZING_TRAP:
                Block onBlock = p.getLocation().getBlock();

                if (invalidTrapMaterials.contains(onBlock.getType())
                        && invalidTrapMaterials.contains(onBlock.getRelative(BlockFace.DOWN).getType())) {
                    p.sendMessage(ChatColor.GRAY + "Cannot place a Freezing Trap here.");
                    return false;
                }

                playCustomSound(p, "FreezingTrapPlace", .7f);

                final Block trapBlock = onBlock;
                //Play trap snowbits
                plugin.effect(onBlock.getLocation(), CustomEffect.SNOWBALL_POOF, 10, 1);

                //Activate the trap
                plugin.scheduler.runTaskLater(plugin,
                        () -> plugin.traps.traps.put(p.getName(), trapBlock.getLocation()), 10
                );

                //Revert block back to what it was.
                plugin.scheduler.runTaskLater(plugin,
                        () -> playCustomSound(p, "FreezingTrapHidden"), 20
                );

                //If another trap exists, cancel it's FX first
                if (plugin.traps.trapTasks.containsKey(p.getName())) {
                    plugin.scheduler.cancelTask(plugin.traps.trapTasks.get(p.getName()));
                }

                //Play trap FX for owner
                taskId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                        () -> plugin.effect(p, trapBlock.getLocation().add(.5, 0, .5), CustomEffect.MOB_SPELL, 1, .1f, 2f), 5, 10
                );

                plugin.traps.trapTasks.put(p.getName(), taskId);
                break;

            case SILENCE:
//                if (speedRampHandler == null) break;
//
//                //Activate the trap
//                plugin.scheduler.runTaskLater(plugin,
//                        () -> plugin.speedRamps.ramps.put(p.getName(), speedRampHandler.location), 10
//                );
//
//                speedRampHandler = new EnderCharge(p.getName(), l);
//
//                //Play trap FX for owner
//                taskId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
//                        () -> plugin.effect(p, speedRampHandler.location.clone().add(.5, 0, .5), CustomEffect.FIREWORKS_SPARK, 1, .1f, 2f), 5, 10
//                );
//
//                plugin.speedRamps.rampTasks.put(p.getName(), taskId);
                break;

            case LIFE_SHIELD:
                playCustomSound(p, "TeamGripHeal");

                PvPPlayer shieldOwner = plugin.getAccount(p.getName());
                final List<Player> friendliesShielded = combat.aoeBuffEffect(shieldOwner, p.getLocation(), 15, PotionEffectType.ABSORPTION, 1, length, "TeamGripHeal");
                friendliesShielded.add(p);

                taskId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                        () -> {
                            for (Player shieldedFriendly : friendliesShielded) {
                                if (shieldedFriendly == null || shieldedFriendly.isDead() || !shieldedFriendly.hasPotionEffect(PotionEffectType.ABSORPTION))
                                    continue;
                                plugin.effect(shieldedFriendly.getLocation().add(0, .5f, 0), CustomEffect.SLIME, 5, .1f, 2f);
                            }
                        }, 5, 10
                );

                //If 3 or more, achievement!
//                if (nearbyPeople.size() >= 3 && !plugin.getAccount(p.getName()).provingGrounds) {
//                    plugin.bungee.sendMessage(p.getName() + ".pvp.lifeshieldmultiple", "Achievement");
//                }
                break;

            case REFLEXES:
                p.getWorld().playSound(p.getLocation(), Sound.ZOMBIE_METAL, 1, .7f);
                break;

            case SWORD_DAMAGE:
                playCustomSound(p, "ThrilloftheKill");

                //This can get stacked a bit, so let's cancel this if it exists just to be sure
                plugin.scheduler.cancelTask(taskId);

                taskId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                        () -> plugin.effect(p.getLocation(), CustomEffect.CLOUD, 1, .02f, 7), 5, 5
                );

                //Apply speed buff
                if (plugin.getAccount(p.getName()).talents.contains(Talent.FLEET_FOOTED)) {
                    //If they have Fleet Footed, they already have Speed 1 permanently up, so up to 2.
                    if (p.hasPotionEffect(PotionEffectType.SPEED)) {
                        p.removePotionEffect(PotionEffectType.SPEED);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * length, 2));
                    }

                    //After ability is over, we need to give them fleet footed back
                    plugin.scheduler.runTaskLater(plugin,
                            () -> {
                                if (!p.isDead()) {
                                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50000, 0));
                                }
                            }, 20 * length + 5
                    );
                } else {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * length, 2));
                }
                break;
        }

        return true;
    }

    void addPotion(Player p) {
        PvPPlayer pvpPlayer = plugin.getAccount(p.getName());
        if (pvpPlayer.bg != null && !plugin.getBG(pvpPlayer.bg).matchStarted) return;

        for (ItemStack item : p.getInventory()) {
            //So long as it's the right potion and the player isn't maxed on the potions.
            if (item != null
                    && isHarmingPotion(item)) {

                //5 is the max potions you can have.
                if (item.getAmount() == 5) return;

                item.setAmount(item.getAmount() + 1);
                p.updateInventory();
                return;
            }
        }

        //Otherwise they have no potions in their inventory, so add away.
        ItemStack potion = new ItemStack(Material.POTION, 1, (short) 16428);

        //Get their saved preferences, that way we're putting potions on their bar where they expect them.
        int slot = plugin.getAccount(p.getName()).getOrder(potion);
        if (slot > -1) {
            p.getInventory().setItem(slot, potion);
        } else {
            p.getInventory().addItem(potion);
        }
        p.updateInventory();
    }

    Boolean isHarmingPotion(ItemStack i) {
        return i.getType().equals(Material.POTION) && i.getDurability() == 16428;
    }

    void offEffect(PvPPlayer p) {
        switch (talent) {
            case LAST_STAND:
                if (p.lastStandAttackers.size() >= 3 && !p.provingGrounds) {
                    plugin.bungee.sendMessage(p.name + ".pvp.laststandmultiple", "Achievement");
                }
                p.lastStandAttackers.clear();

                if (p.getPlayer() == null || p.getPlayer().isDead()) return;
                plugin.blockEffect(p.getPlayer().getLocation().add(0, 1.4f, 0), CustomEffect.BLOCKDUST, Material.EMERALD_BLOCK.getId(), 0, 8, .15f, 5);
                break;
        }
    }
}
