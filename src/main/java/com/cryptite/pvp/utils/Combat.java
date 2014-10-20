package com.cryptite.pvp.utils;

import com.cryptite.pvp.CustomEffect;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.arena.Arena2v2;
import com.cryptite.pvp.talents.Talent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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
                if (!victim.hasTalentActive(Talent.ENDER_HOOK) && victim.hasTalent(Talent.BLAST_PROTECTION)) {
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
                if (!victim.hasTalentActive(Talent.ENDER_HOOK)) {
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

    public int aoeDebuffEffect(PvPPlayer p, Location source, int radius, PotionEffectType effect, int multiplier, int seconds, String sound) {
        int enemiesAffected = 0;

        for (Entity e : source.getWorld().getEntities()) {
            if (!(e instanceof Player) || e.isDead()) continue;
            Player player = (Player) e;
            PvPPlayer victim = plugin.getAccount(player.getName());

            //If spectator
            if (victim.spectator) continue;

            //If same team
            if (p != null && sameTeam(player.getName(), p.name)
                    || p == null
                    || p.name.equals(player.getName())) continue;

            double distance = player.getLocation().distance(source);
            if (distance <= radius) {
                //If spectator or they have Last Stand, stop here
                if (victim.hasTalentActive(Talent.LAST_STAND)) continue;

                //Add potion effect
                player.addPotionEffect(new PotionEffect(effect, 20 * seconds, multiplier));

                if (sound != null) {
                    playCustomSound(player, sound);
                }

                if (!player.getName().equals(p.name)) {
                    enemiesAffected += 1;
                }
            }
        }

        return enemiesAffected;
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
        heal(p.getPlayer(), 6);
        playCustomSound(p.getPlayer(), "Heal", 2f);

        return friendliesAffected;
    }

    public void smallHealPlayer(Player p, double amount) {
        plugin.smallEffect(p.getLocation(), CustomEffect.HEART, 1, .1f, 1);
        heal(p, amount);
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

        if (defender.arena != null && defender.arena instanceof Arena2v2) {
            if (defender.isArenaPartner(attacker.name)) return true;
        }

        return false;
    }

    public void hookPlayer(PvPPlayer p, Location loc) {
        if (p.getPlayer() == null || p.hook == null) return;

        if (!p.hook.canPull()) {
            System.out.println("Pull result is: " + p.hook.getResult());
            return;
        }

        pullTo(p.getPlayer(), loc);

        //Get rid of the hook rope after 3 seconds.
        plugin.scheduler.runTask(plugin, () -> {
            p.hook.despawn();
            p.hook = null;
        });
    }

    public void hookPlayer(PvPPlayer p, PvPPlayer hooker) {
        if (p.getPlayer() == null || hooker.getPlayer() == null || hooker.hook == null) return;

        if (!hooker.hook.canPull()) {
            System.out.println("Pull result is: " + p.hook.getResult());
            return;
        }

        pullTo(p.getPlayer(), hooker.getPlayer().getLocation());

        //Get rid of the hook rope after 3 seconds.
        plugin.scheduler.runTask(plugin, () -> {
            hooker.hook.setEnd(p.getPlayer().getLocation());
            hooker.hook = null;
        });
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
}
