package com.cryptite.pvp.talents;

import com.cryptite.pvp.CustomEffect;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.utils.Combat;
import com.cryptite.pvp.utils.TimeUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
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
    public int length;
    private int abilitySecondsLeft = 0;
    private long abilityUsed = 0;

    //Talent identifiers
    public final String name;
    public final Talent talent;
    public final Material material;
    public ItemStack item;

    //Talent states
    public boolean available = true;
    public boolean active = false;

    //Talent tasks
    private int taskId = 0;
    private int countdownBarTask;
    private BukkitTask cooldownTask;

    //Misc
    private final Combat combat;
    private final List<Material> invalidTrapMaterials = new ArrayList<>(asList(Material.AIR, Material.LEAVES, Material.LEAVES_2));

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
        item = talentItem;
        abilityUsed = System.currentTimeMillis();

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
        cooldownTask = plugin.scheduler.runTaskLater(plugin,
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

        if (!plugin.talents.itemlessTalents.contains(talent)) {
            if (talent.equals(Talent.EXPLOSIVE_ARROW)
                    || talent.equals(Talent.HEAL_ARROW)
                    || talent.equals(Talent.HOOK)) {
                if (p.getPlayer() == null) return;

                ItemStack item = getAbilityItem(p.getPlayer());
                if (item != null) item.setType(Material.COAL);
            } else {
                item.setType(Material.COAL);
            }
        }
    }

    public void silence(PvPPlayer p, int length) {
        if (talent.equals(Talent.EXPLOSIVE_ARROW)
                || talent.equals(Talent.HOOK)
                || talent.equals(Talent.HEAL_ARROW)) {
            //You can silence somebody's active loaded arrow
            deactivate(p);
        }

        //If available, turn to coal
        if (available) {
            ItemStack item = getAbilityItem(p.getPlayer());
            if (item == null) return;
            item.setType(Material.COAL);
            available = false;

            //Do 6s countdown here
            plugin.scheduler.runTaskLater(plugin, () -> setItemAvailable(p), 20 * length);

        } else {
            //Not available, so it was already on cooldown, redo the cooldown task
            int secondsUntilActive = TimeUtil.secondsUntil(abilityUsed, cooldown);
            if (secondsUntilActive < 6) {
                cooldownTask.cancel();
                plugin.scheduler.runTaskLater(plugin, () -> setItemAvailable(p), 20 * length);
            }
        }
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

        abilityUsed = 0;

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
                playCustomSound(p, "Bandage");
                PvPPlayer bandager = plugin.getAccount(p);
                if (bandager.bandage == null) {
                    new Bandage(plugin.getAccount(p.getName()));
                } else {
                    bandager.bandage.start(bandager);
                }
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
                plugin.blockEffect(p.getLocation().clone().add(0, .4f, 0), CustomEffect.BLOCKDUST, Material.DIRT.getId(), 0, 25, .15f, 5);
                playCustomSound(p, "Quake");

                PvPPlayer quakeOwner = plugin.getAccount(p.getName());
                combat.aoeTalent(quakeOwner, quakeOwner.getLocation(), 10, Talent.QUAKE, 3);
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

            case HOOK:
                playCustomSound(p, "HookLoad");

                //Apply explosive arrow loaded particles.
                taskId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                        () -> plugin.effect(p.getLocation().add(0, .5f, 0), CustomEffect.FIREWORKS_SPARK, 1, .1f, 2f), 5, 10
                );
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
                combat.aoeTalent(owner, p.getLocation(), 15, Talent.RALLYING_CRY, length);

                plugin.speedRamps.createRamp(p, owner.team);
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
                playCustomSound(p, "SilenceCast");
                plugin.effect(p.getLocation().add(0, 1, 0), CustomEffect.PORTAL);

                PvPPlayer silencer = plugin.getAccount(p.getName());
                combat.aoeTalent(silencer, p.getLocation(), 10, Talent.SILENCE, 6);
                break;

            case LIFE_SHIELD:
                playCustomSound(p, "TeamGripHeal");

                PvPPlayer shieldOwner = plugin.getAccount(p.getName());
                combat.aoeTalent(shieldOwner, p.getLocation(), 15, Talent.LIFE_SHIELD, length);
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

    void offEffect(PvPPlayer p) {
        switch (talent) {
            case LAST_STAND:
                if (p.lastStandAttackers.size() >= 3) {
                    plugin.bungee.sendMessage("loka", p.name + ".pvp.laststandmultiple", "Achievement");
                }
                p.lastStandAttackers.clear();

                if (p.getPlayer() == null || p.getPlayer().isDead()) return;
                plugin.blockEffect(p.getPlayer().getLocation().add(0, 1.4f, 0), CustomEffect.BLOCKDUST, Material.EMERALD_BLOCK.getId(), 0, 8, .15f, 5);
                break;
        }
    }
}
