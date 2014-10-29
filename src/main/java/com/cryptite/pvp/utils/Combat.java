package com.cryptite.pvp.utils;

import com.cryptite.pvp.CustomEffect;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.talents.Talent;
import com.cryptite.pvp.talents.TalentAbility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

import static com.cryptite.pvp.utils.LocationUtils.playCustomSound;

public class Combat {

    private final LokaVotA plugin;

    public Combat(LokaVotA plugin) {
        this.plugin = plugin;
    }

    void heal(Player p, double amount) {
        //You can't (shouldn't ever) modify a dead player's health
        if (p.isDead()) return;

        double health = p.getHealth();

        //You can't set a player's health over 20 so we have to check for that.
        if (health < (20 - amount)) {
            health += amount;
        } else {
            health = 20;
        }
        p.setHealth(health);
    }

    void knockback(Entity e, Location source, float multiplier, float upAmount) {
        e.setVelocity(e.getVelocity().add(e.getLocation().toVector().subtract(source.toVector()).normalize().multiply(multiplier).add(new Vector(0, upAmount, 0))));
    }

    public void knockup(Player p, float amount) {
        p.setVelocity(p.getVelocity().normalize().add(new Vector(0, amount, 0)));
    }

    public List<String> aoeKnockback(Entity entityCaused, PvPPlayer p, Location source, int radius, double damageMultiplier, float multiplier, float upValue, String town, Talent cause) {
        List<String> enemiesHit = new ArrayList<>();

        for (Entity e : source.getWorld().getEntities()) {
            if (!(e instanceof Player) || e.isDead()) continue;
            Player player = (Player) e;
            PvPPlayer victim = plugin.getAccount(player.getName());

            //If spectator
            if (victim.spectator) continue;

            //If same team
            if (p != null && sameTeam(player.getName(), p.name) || p == null) continue;

            //If same town
            if (p.town != null && p.town.equals(town)) continue;

            double distance = player.getLocation().distance(source);
            if (distance <= radius) {
                if (!victim.hasTalentActive(Talent.HOOK) && victim.hasTalent(Talent.BLAST_PROTECTION)) {
                    multiplier = multiplier / 2;
                    upValue = upValue / 2;
                    damageMultiplier = damageMultiplier / 2;
                }

                //Register damage cause before damaging, as this could kill them.
                if (cause != null) {
                    plugin.registerDamage(victim.name, p.name, cause);
                }

                //Do the damage.
                if (!victim.hasTalentActive(Talent.LAST_STAND)) {
                    System.out.println("Damaging " + victim.name + " for " + damageMultiplier + "d from " + entityCaused);
                    player.damage(damageMultiplier);
                }

                //Then do knockback
                if (!victim.hasTalentActive(Talent.HOOK)) {
                    knockback(player, source, multiplier, upValue);
                }

                //Blood splatter for Explosive Arrow
                if (p.hasTalentActive(Talent.EXPLOSIVE_ARROW)) {
                    System.out.println("Blood splatter on " + victim.name + " from " + p.name);
                    plugin.blockEffect(e.getLocation().add(0, 1.4f, 0), CustomEffect.BLOCKDUST, Material.REDSTONE_BLOCK.getId(), 0, 6, .15f, 5);
                }

                //Can safely assume this is fire based, so let's use lava particles
                //Number of lava particles also based on distance
                plugin.effect(player.getLocation(), CustomEffect.LAVA, (int) distance, .5f);

                //Smoulder!
                int smokeID = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                        () -> plugin.smallEffect(victim.getLocation().add(0, .4f, 0), CustomEffect.LARGE_SMOKE, 1, .1f, 5), 0, 2);
                plugin.scheduler.runTaskLater(plugin,
                        () -> plugin.scheduler.cancelTask(smokeID), 40);

                //Add to tracked list of people hit.
                if (!player.getName().equals(p.name)) {
                    enemiesHit.add(player.getName());
                }
            }
        }

        return enemiesHit;
    }

    public List<Player> aoeBuffEffect(PvPPlayer p, Location source, int radius, PotionEffectType effect, int multiplier, int seconds, String sound) {
        List<Player> friendliesAffected = new ArrayList<>();

        for (Entity e : source.getWorld().getEntities()) {
            if (!(e instanceof Player) || e.isDead()) continue;
            Player player = (Player) e;

            //If spectator
            if (plugin.getAccount(player.getName()).spectator) continue;

            //If not same team
            if (!sameTeam(player.getName(), p.name)) continue;

            double distance = player.getLocation().distance(source);
            if (distance <= radius) {
                //If player already has this, remove, so we can reapply properly.
                if (player.hasPotionEffect(effect)) player.removePotionEffect(effect);

                //Add potion effect
                player.addPotionEffect(new PotionEffect(effect, 20 * seconds, multiplier));

                if (sound != null) {
                    playCustomSound(player, sound);
                }

                if (!player.getName().equals(p.name)) {
                    friendliesAffected.add(player);
                }
            }
        }

        return friendliesAffected;
    }

    public void aoeTalent(PvPPlayer p, Location source, int radius, Talent talent, int length) {
        int enemiesAffected = 0, friendliesAffected = 0;

        for (Entity e : source.getWorld().getEntities()) {
            if (!(e instanceof Player) || e.isDead()) continue;
            Player player = (Player) e;

            //Spectators always exempt from everything
            if (plugin.getAccount(player.getName()).spectator) continue;

            double distance = player.getLocation().distance(source);
            if (distance <= radius) {

                //Do same team stuff
                if (sameTeam(player.getName(), p.name)) {
                    if (talent.equals(Talent.HEAL_ARROW)) {
                        //Small instant-heal
                        heal(player, 4);

                        //Apply Regen
                        addPotionEffect(player, PotionEffectType.REGENERATION, 1, length);
                    } else if (talent.equals(Talent.RALLYING_CRY)) {
                        addPotionEffect(player, PotionEffectType.INCREASE_DAMAGE, 0, length);

                        int rallyingCryID = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                                () -> {
                                    if (player.isDead()) return;
                                    plugin.effect(player.getLocation().add(0, .5f, 0), CustomEffect.RED_DUST, 5, .1f, 5);
                                }, 5, 5
                        );
                        plugin.scheduler.runTaskLater(plugin, () -> plugin.scheduler.cancelTask(rallyingCryID), 20 * length);
                        addHarmingPotion(player, Talent.RALLYING_CRY);
                    } else if (talent.equals(Talent.LIFE_SHIELD)) {
                        addPotionEffect(player, PotionEffectType.ABSORPTION, 1, length);

                        int lifeShieldId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                                () -> {
                                    if (player.isDead() || !player.hasPotionEffect(PotionEffectType.ABSORPTION))
                                        return;
                                    plugin.effect(player.getLocation().add(0, .5f, 0), CustomEffect.SLIME, 5, .1f, 2f);
                                }, 5, 10
                        );
                        plugin.scheduler.runTaskLater(plugin, () -> plugin.scheduler.cancelTask(lifeShieldId), 20 * length);
                    }
                } else {
                    //Enemy team stuff
                    if (talent.equals(Talent.HEAL_ARROW)) {
                        addPotionEffect(player, PotionEffectType.WEAKNESS, 1, length);
                        addPotionEffect(player, PotionEffectType.BLINDNESS, 2, 2);
                    } else if (talent.equals(Talent.EXPLOSIVE_ARROW)) {
                        //Apply wither
                        addPotionEffect(player, PotionEffectType.WITHER, 1, length);
                        addPotionEffect(player, PotionEffectType.BLINDNESS, 1, 3);
                    } else if (talent.equals(Talent.FREEZING_TRAP)) {
                        //Add slow and poison
                        addPotionEffect(player, PotionEffectType.SLOW, 4, length);
                        addPotionEffect(player, PotionEffectType.POISON, 1, length);

                        //Add a snowball pop
                        plugin.effect(player.getLocation(), CustomEffect.SNOWBALL_POOF, 20, 1);
                        plugin.blockEffect(player.getLocation().add(0, 1, 0), CustomEffect.BLOCKDUST, Material.SNOW_BLOCK.getId(), 0, 13, .15f, 5);

                        //Snow particles while they're frozed
                        int splatterID = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                                () -> plugin.blockEffect(player.getLocation().add(0, .6f, 0), CustomEffect.BLOCKDUST, Material.SNOW_BLOCK.getId(), 0, 5, .04f, 7), 5, 7);

                        plugin.scheduler.runTaskLater(plugin,
                                () -> plugin.scheduler.cancelTask(splatterID), 20 * length);

                    } else if (talent.equals(Talent.SILENCE)) {
                        PvPPlayer pvpPlayer = plugin.getAccount(player.getName());
                        playCustomSound(player, "Silence", .4f);
                        for (TalentAbility t : pvpPlayer.talentAbilities.values()) {
                            t.silence(pvpPlayer, length);
                        }
                        plugin.effect(player.getLocation().add(0, 1, 0), CustomEffect.MOB_SPELL, 15, 1, 1);
                        if (pvpPlayer.bandage != null && pvpPlayer.bandage.active) pvpPlayer.bandage.cancel(true);
                    } else if (talent.equals(Talent.QUAKE)) {
                        addPotionEffect(player, PotionEffectType.SLOW, 4, 5);
                        addPotionEffect(player, PotionEffectType.BLINDNESS, 2, 4);

                        knockback(player, p.getLocation(), 0, 1.3f);

                        //Dirt Kickup
                        plugin.blockEffect(player.getLocation().add(0, .4f, 0), CustomEffect.BLOCKDUST, Material.DIRT.getId(), 0, 25, .15f, 5);

                        int quakeID = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                                () -> plugin.blockEffect(player.getLocation().add(0, .2f, 0), CustomEffect.BLOCKDUST, Material.DIRT.getId(), 0, 7, .04f, 7), 0, 2);

                        plugin.scheduler.runTaskLater(plugin,
                                () -> plugin.scheduler.cancelTask(quakeID), 20 * length);
                    }
                }
            }
        }

        //3 is the magic number for AoE talent achievements
        if ((friendliesAffected >= 3 || enemiesAffected >= 3)) checkAchievement(talent, p);

    }

    private void checkAchievement(Talent talent, PvPPlayer p) {
        if (talent.equals(Talent.HEAL_ARROW)) {
            plugin.bungee.sendMessage("loka", p.name + ".pvp.healarrowmultiple", "Achievement");
        } else if (talent.equals(Talent.LIFE_SHIELD)) {
            plugin.bungee.sendMessage("loka", p.name + ".pvp.lifeshieldmultiple", "Achievement");
        }
    }

    public void addPotionEffect(Player p, PotionEffectType effect, int multiplier, int seconds) {
        //If player already has this, remove, so we can reapply properly.
        if (p.hasPotionEffect(effect)) p.removePotionEffect(effect);

        //Add potion effect
        p.addPotionEffect(new PotionEffect(effect, 20 * seconds, multiplier));
    }

    public int healNearbyFriendlies(Location l, PvPPlayer p, Integer amount) {
        int friendliesAffected = 0;

        for (Player player : p.getPlayer().getWorld().getPlayers()) {
            //So long as on the same team, not the source player, and within less than 50 distanceSquared
            if (sameTeam(p.name, player.getName())
                    && !player.getName().equals(p.name)
                    && !plugin.getAccount(player.getName()).spectator
                    && l.distanceSquared(player.getLocation()) < 50) {
                heal(player, amount);
                plugin.effect(player.getLocation().add(0, 1, 0), CustomEffect.HEART, 1, .1f, 1);
                playCustomSound(p.getPlayer(), "Heal", 2f);

                friendliesAffected += 1;
            }
        }

        //Heal the player too.
        plugin.effect(p.getLocation().add(0, 1, 0), CustomEffect.HEART, 1, .1f, 1);
        heal(p.getPlayer(), amount);
        playCustomSound(p.getPlayer(), "Heal", 2f);

        return friendliesAffected;
    }

    public void smallHealPlayer(Player p, double amount) {
//        playWorldCustomSound(p.getLocation(), "Heal", 5);
        plugin.smallEffect(p.getLocation().add(0, 1, 0), CustomEffect.HEART, 1, .1f, 1);
        heal(p, amount);
    }

    public Boolean spectator(String player) {
        return plugin.getAccount(player).spectator;
    }

    public boolean sameTeam(String p1, String p2) {
        PvPPlayer defender = plugin.getAccount(p1);
        PvPPlayer attacker = plugin.getAccount(p2);
        return sameTeam(defender, attacker);
    }

    public boolean sameTeam(PvPPlayer defender, PvPPlayer attacker) {
        //Spectators are on everyone's teams!
        if (defender.spectator || attacker.spectator) return true;

        //You're on your own team!
        if (defender.name.equals(attacker.name)) return true;

        if (defender.bg != null && attacker.bg != null && defender.bg.equals(attacker.bg)) {
            if (defender.bg.equalsIgnoreCase("vota")) {
                if ((plugin.vota.redTeam.containsKey(defender.name) && plugin.vota.redTeam.containsKey(attacker.name))
                        || (plugin.vota.blueTeam.containsKey(defender.name) && plugin.vota.blueTeam.containsKey(attacker.name))) {
                    return true;
                }
            } else if (defender.bg.equalsIgnoreCase("overload")) {
                if (defender.offense && attacker.offense || !defender.offense && !attacker.offense) {
                    return true;
                }
            } else if (defender.bg.equalsIgnoreCase("skyvale")) {
                if (defender.town.equals(attacker.town)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void hookPlayer(PvPPlayer p, Location loc) {
        if (p.getPlayer() == null) return;

        playCustomSound(p.getPlayer(), "HookPull");
//        if (!p.hook.canPull()) {
//            System.out.println("Pull result is: " + p.hook.getResult());
//            return;
//        }

        pullTo(p.getPlayer(), loc);

        //Get rid of the hook rope after 3 seconds.
//        plugin.scheduler.runTask(plugin, () -> {
//            p.hook.despawn();
//            p.hook = null;
//        });
    }

    public void hookPlayer(PvPPlayer p, PvPPlayer hooker) {
        if (p.getPlayer() == null || hooker.getPlayer() == null) return;

//        if (!hooker.hook.canPull()) {
//            System.out.println("Pull result is: " + p.hook.getResult());
//            return;
//        }

        playCustomSound(hooker.getPlayer(), "HookPull");
        playCustomSound(p.getPlayer(), "HookYank");
        playCustomSound(p.getPlayer(), "HookPull");
        //Blood Splatter
        plugin.blockEffect(p.getLocation().add(0, 1.4f, 0), CustomEffect.BLOCKDUST, Material.REDSTONE_BLOCK.getId(), 0, 13, .15f, 5);

        pullTo(p.getPlayer(), hooker.getPlayer().getLocation());

        //Get rid of the hook rope after 3 seconds.
//        plugin.scheduler.runTask(plugin, () -> {
//            hooker.hook.setEnd(p.getPlayer().getLocation());
//            hooker.hook = null;
//        });
    }

    private static void pullTo(Entity e, Location loc) {
        // This code written by [USER=90696604]SnowGears[/USER]
        Location l = e.getLocation();

        if (l.distanceSquared(loc) < 9) {
            if (loc.getY() > l.getY()) {
                e.setVelocity(new Vector(0, 0.25, 0));
                return;
            }
            Vector v = loc.toVector().subtract(l.toVector());
            e.setVelocity(v);
            return;
        }

        l.setY(l.getY() + 0.5);
        e.teleport(l);

        double d = loc.distance(l);
        double g = -0.08;
        double x = (1.0 + 0.07 * d) * (loc.getX() - l.getX()) / d;
        double y = (1.0 + 0.03 * d) * (loc.getY() - l.getY()) / d - 0.5 * g * d;
        double z = (1.0 + 0.07 * d) * (loc.getZ() - l.getZ()) / d;

        Vector v = e.getVelocity();
        v.setX(x);
        v.setY(y);
        v.setZ(z);
        e.setVelocity(v);
    }

    public void addHarmingPotion(Player p, Talent cause) {
        PvPPlayer pvpPlayer = plugin.getAccount(p.getName());
        if (!canReceivePotion(pvpPlayer)) return;

        for (ItemStack item : p.getInventory()) {
            //If item is null, not a harming potion, or they have the max potions, disregard
            if (item == null || !isHarmingPotion(item)) continue;

            //Can't have more than the max (except rallying cry, then you can have 1 more)
            if (cause.equals(Talent.RALLYING_CRY)) {
                if (item.getAmount() >= pvpPlayer.maxHarmingPotions + 1) return;
            } else if (item.getAmount() >= pvpPlayer.maxHarmingPotions) return;

            item.setAmount(item.getAmount() + 1);
            p.playSound(p.getLocation(), Sound.ITEM_PICKUP, .5f, 1);
            p.updateInventory();
            return;
        }

        //Otherwise they have no potions in their inventory, so add away.
        ItemStack potion = new ItemStack(Material.POTION, 1, (short) 16428);
        addFreshPotion(p, potion);
        p.playSound(p.getLocation(), Sound.ITEM_PICKUP, .5f, 1);
    }

    public void addHealingPotion(Player p) {
        PvPPlayer pvpPlayer = plugin.getAccount(p.getName());
        if (!canReceivePotion(pvpPlayer)) return;

        for (ItemStack item : p.getInventory()) {
            //If item is null, not a harming potion, or they have the max potions, disregard
            if (item == null || !isHealingPotion(item)) continue;

            //Can't have more than the max
            if (item.getAmount() >= getMaxHealingPotions(pvpPlayer)) return;

            item.setAmount(item.getAmount() + 1);
            p.playSound(p.getLocation(), Sound.ITEM_PICKUP, .5f, 1);
            p.updateInventory();
            return;
        }

        //Otherwise they have no potions in their inventory, so add away.
        ItemStack potion = new ItemStack(Material.POTION, 1, (short) 16389);
        addFreshPotion(p, potion);
        p.playSound(p.getLocation(), Sound.ITEM_PICKUP, .5f, 1);
    }

    private Boolean canReceivePotion(PvPPlayer p) {
        return p.bg != null && plugin.getBG(p.bg) != null && plugin.getBG(p.bg).matchStarted;
    }

    private void addFreshPotion(Player p, ItemStack potion) {
        //Get their saved preferences, that way we're putting potions on their bar where they expect them.
        int slot = plugin.getAccount(p.getName()).getOrder(potion);
        if (slot > -1) {
            p.getInventory().setItem(slot, potion);
        } else {
            p.getInventory().addItem(potion);
        }
        p.updateInventory();
    }

    private Boolean isHarmingPotion(ItemStack i) {
        return i.getType().equals(Material.POTION) && i.getDurability() == 16428;
    }

    private Boolean isHealingPotion(ItemStack i) {
        return i.getType().equals(Material.POTION) && i.getDurability() == 16389;
    }

    private int getMaxHealingPotions(PvPPlayer p) {
        if (p.hasTalent(Talent.POTENCY)) return 3;
        return 2;
    }
}
