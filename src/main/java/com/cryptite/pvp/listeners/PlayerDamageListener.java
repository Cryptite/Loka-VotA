package com.cryptite.pvp.listeners;

import com.cryptite.pvp.CustomEffect;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.talents.Talent;
import com.cryptite.pvp.utils.Combat;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEnderPearl;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftThrownPotion;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.logging.Logger;

import static com.cryptite.pvp.utils.LocationUtils.inWGRegion;
import static com.cryptite.pvp.utils.LocationUtils.playCustomSound;

public class PlayerDamageListener implements Listener {
    private final Logger log = Logger.getLogger("LokaPvP-PlayerDamageListener");
    private final LokaVotA plugin;
    private final Combat combat;
    private final Random r;
    private final float spawnDamageBuff = .4f;

    public PlayerDamageListener(LokaVotA plugin) {
        this.plugin = plugin;
        r = new Random();
        combat = new Combat(plugin);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        if (!e.isCancelled()) {
            if (!(e.getEntity() instanceof Player)) {
                // Victim is not a player
                if (e.getEntity() instanceof Horse && e.getDamager() instanceof Player) {
                    Horse horse = (Horse) e.getEntity();
                    if (!horse.isEmpty()) {
                        horse.remove();
                    }
                }
                return;
            }

            // Cast victim
            Player victim = (Player) e.getEntity();

            //No durability loss in PvP
            for (ItemStack armor : victim.getInventory().getArmorContents()) {
                armor.setDurability((short) 0);
            }
            victim.updateInventory();

            //Always clear, think it's easier this way.
            Talent cause = null;

            //Cast ArenaPlayers
            PvPPlayer pVictim = plugin.getAccount(victim.getName());
            PvPPlayer pAttacker;

            //Invulnerable when picking talents in the grounds
            if (pVictim.provingGrounds && plugin.talents.viewingPlayers.contains(pVictim.name)) {
                e.setCancelled(true);
                playCustomSound(victim, "LastStandAbsorb");
                return;
            }

            // Create an empty player object to store attacker
            Player attacker;
            if (pVictim.hasTalentActive(Talent.LAST_STAND) && !(e.getDamager() instanceof CraftThrownPotion)) {

                //Add to attacker list for achievement
                //TODO: Watch this one as it could get spammy for tracking performance
                if (e.getDamager() instanceof Player
                        && !pVictim.lastStandAttackers.contains(((Player) e.getDamager()).getName())) {
                    attacker = (Player) e.getDamager();
                    pVictim.lastStandAttackers.add(attacker.getName());
                }

                playCustomSound(victim, "LastStandAbsorb");

                e.setCancelled(true);
                return;
            }

            if (pVictim.hasTalent(Talent.IRON_FORM)
                    && !pVictim.hasTalentActive(Talent.IRON_FORM)
                    && pVictim.talentAbilities.get(Talent.IRON_FORM).available) {
                if (r.nextInt(7) == 3) {
                    pVictim.activateTalent(Talent.IRON_FORM);
                }
            }

            if (e.getDamager() instanceof Player && e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
                // Attacker is a player (melee damage)
                attacker = (Player) e.getDamager();
                pAttacker = plugin.getAccount(attacker.getName());

                attacker.getItemInHand().setDurability((short) 0);

                //Cancel if same team
                if (combat.sameTeam(pAttacker, pVictim)) {
                    e.setCancelled(true);
                    return;
                }

                //Cancel event, reapply damage to prevent knockback
                if (pVictim.hasTalentActive(Talent.ENDER_HOOK)) {
                    e.setCancelled(true);
                    cause = Talent.SWORD_DAMAGE;
                    plugin.registerDamage(pVictim.name, pAttacker.name, Talent.SWORD_DAMAGE);
                    victim.damage(e.getDamage());
                    return;
                }

                //Dismount if on horseback
                if (attacker.isInsideVehicle() && attacker.getVehicle() instanceof Horse) {
                    attacker.getVehicle().remove();
                }

                if (victim.isInsideVehicle() && victim.getVehicle() instanceof Horse) {
                    victim.getVehicle().remove();
                }

                if (pAttacker.hasTalent(Talent.SWORD_DAMAGE)) {
                    e.setDamage(e.getDamage() + (e.getDamage() * .19));
                }

                //Player has bloodlust, redo the damage
                if (pAttacker.getPlayer().hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
                    double newDamage = e.getDamage() - (e.getDamage() * .35);
                    e.setDamage(newDamage);
                }

                if (pAttacker.hasTalentActive(Talent.RALLYING_CRY)) {
                    pAttacker.setPlayerArmor(true);
                    pAttacker.talentAbilities.get(Talent.RALLYING_CRY).active = false;
                }

                if (pAttacker.hasTalentActive(Talent.LUNGE)) {
                    cause = Talent.LUNGE;
                    plugin.registerDamage(pVictim.name, pAttacker.name, Talent.LUNGE);

                    e.setDamage(e.getDamage() * 2);

                    //Blood Splatter
                    plugin.blockEffect(pVictim.getLocation().add(0, 1.4f, 0), CustomEffect.BLOCKDUST, Material.REDSTONE_BLOCK.getId(), 0, 13, .15f, 5);

                    int splatterID = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                            () -> plugin.blockEffect(pVictim.getLocation().add(0, 1.4f, 0), CustomEffect.BLOCKDUST, Material.REDSTONE_BLOCK.getId(), 0, 4, .04f, 7), 5, 5);

                    plugin.scheduler.runTaskLater(plugin,
                            () -> plugin.scheduler.cancelTask(splatterID), 20 * 5);

                    playCustomSound(victim, "LungeHit");
                    pAttacker.talentAbilities.get(Talent.LUNGE).active = false;

                    log.info("[Lunge] " + pVictim.name + " hit by " + pAttacker.name + " with lunge strike");

                    //This will kill the player
                    if (victim.getHealth() - e.getDamage() <= 0 && !pAttacker.provingGrounds) {
                        plugin.bungee.sendMessage(pAttacker.name + ".pvp.killwithlunge", "Achievement");
                    }
                }

                if (pAttacker.hasTalent(Talent.FLAMING_SWORD)) {
                    if (r.nextInt(8) == 3) {
                        if (pVictim.talents.contains(Talent.FIRE_PROTECTION)) {
                            victim.setFireTicks(20 * 3);
                        } else {
                            victim.setFireTicks(20 * 5);
                        }
                    }
                }

                if (pAttacker.hasTalent(Talent.SLOW)) {
                    if (r.nextInt(8) == 3) {
                        playCustomSound(victim, "Slow");
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 1));
                    }
                }

                if (pAttacker.hasTalent(Talent.LIFE_STEAL)
                        && !pAttacker.hasTalentActive(Talent.LIFE_STEAL)
                        && pAttacker.talentAbilities.get(Talent.LIFE_STEAL).available) {
                    pAttacker.activateTalent(Talent.LIFE_STEAL);
                }

                if (pAttacker.hasTalent(Talent.SPAWN)) {
                    log.info("Sword Damage (" + e.getDamage() + ") increased to " + (e.getDamage() + (e.getDamage() * spawnDamageBuff)) +
                            " for spawn effect");
                    e.setDamage(e.getDamage() + (e.getDamage() * spawnDamageBuff));
                }

                //Player has bloodlust, redo the damage
                if (pAttacker.getPlayer().hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
                    double newDamage = e.getDamage() - (e.getDamage() * .35);
                    e.setDamage(newDamage);
                }

                if (attacker.getWorld().getName().equalsIgnoreCase("world")) {
                    pAttacker.damage += e.getDamage();
                }

            } else if (e.getDamager() instanceof Arrow) {
                // Attacker is an arrow (projectile damage)
                Arrow arrow = (Arrow) e.getDamager();
                if (!(arrow.getShooter() instanceof Player)) {
                    // Arrow was not fired by a player
                    return;
                }
                // Cast attacker
                attacker = (Player) arrow.getShooter();
                pAttacker = plugin.getAccount(attacker.getName());

                if (pVictim.spectator) {
                    log.info("Arrow recalculate!");
                    victim.teleport(victim.getLocation().add(0, 5, 0));
                    victim.setFlying(true);

                    Arrow newArrow = attacker.launchProjectile(Arrow.class);
                    newArrow.setShooter(attacker);
                    newArrow.setVelocity(arrow.getVelocity());
                    newArrow.setKnockbackStrength(arrow.getKnockbackStrength());
                    newArrow.setCritical(arrow.isCritical());
                    newArrow.setBounce(false);

                    e.setCancelled(true);
                    arrow.remove();
                }

                //Cancel if same team
                if (combat.sameTeam(pAttacker, pVictim)) {
                    e.setCancelled(true);
                    return;
                }

//                //Cancel event, reapply damage to prevent knockback
//                if (pVictim.hasTalentActive(Talent.ENDER_HOOK)) {
//                    e.setCancelled(true);
//                    cause = Talent.HOT_BOW;
//                    plugin.registerDamage(pVictim.name, pAttacker.name, Talent.HOT_BOW);
//                    victim.damage(e.getDamage());
//                    return;
//                }

                if (pAttacker.hasTalent(Talent.HOT_BOW)) {
                    if (r.nextInt(8) == 3) {
                        if (pVictim.talents.contains(Talent.FIRE_PROTECTION)) {
                            victim.setFireTicks(20 * 3);
                        } else {
                            victim.setFireTicks(20 * 5);
                        }
                    }
                }

                if (pAttacker.hasTalent(Talent.GROOVE) && pAttacker.grooveStacks < 4) {
                    if (pAttacker.grooveStacks >= 3) {
                        cause = Talent.GROOVE;
                        plugin.registerDamage(pVictim.name, pAttacker.name, Talent.GROOVE);
                    }

                    e.setDamage(e.getDamage() + (e.getDamage() * (pAttacker.grooveStacks * .1)));

                    attacker.playSound(attacker.getLocation(), "Groove", .75f, (1 + .2f * pAttacker.grooveStacks));

                    pAttacker.grooveStacks += 2;
                    if (pAttacker.grooveStacks >= 3) {
                        pAttacker.grooveStacks = 3;
                    }

                }

                if (pVictim.hasTalentActive(Talent.REFLEXES) && !pAttacker.hasTalentActive(Talent.EXPLOSIVE_ARROW)) {
                    arrow.setFireTicks(0);
                    victim.setFireTicks(0);
                    if (arrow.getFireTicks() > 0) {
                        plugin.effect(victim.getLocation(), CustomEffect.LAVA, 4, .5f);
                    } else {
                        plugin.effect(victim.getLocation(), CustomEffect.SPELL, 5, .5f);
                    }
                    e.setCancelled(true);
                    return;
                }

                if (pAttacker.hasTalentActive(Talent.EXPLOSIVE_ARROW)) {
                    e.setDamage(e.getDamage() * 2.65);
                    cause = Talent.EXPLOSIVE_ARROW;
                    plugin.registerDamage(pVictim.name, pAttacker.name, Talent.EXPLOSIVE_ARROW);
                }

                //Player has bloodlust, redo the damage
                if (pAttacker.getPlayer().hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
                    double newDamage = e.getDamage() + (e.getDamage() * .25);
                    e.setDamage(newDamage);
                }

                if (pAttacker.hasTalent(Talent.SPAWN)) {
                    log.info("Arrow Damage (" + e.getDamage() + ") increased to " + (e.getDamage() + (e.getDamage() * spawnDamageBuff)) +
                            " for spawn effect");
                    e.setDamage(e.getDamage() + (e.getDamage() * spawnDamageBuff));
                }

                if (attacker.getWorld().getName().equalsIgnoreCase("world")) {
                    pAttacker.damage += e.getDamage();
                }

            } else if (e.getDamager() instanceof ThrownPotion) {
                    /* Splash potion of harming triggers this event because it deals direct damage,
                    but we will deal with that kind of stuff in PotionSplashEvent instead */
                ThrownPotion pot = (ThrownPotion) e.getDamager();

                if (pot.getShooter() instanceof Player && pot.getShooter() != victim) {

                    //Let's not count this twice if the player hits himself, though.
                    attacker = (Player) pot.getShooter();
                    pAttacker = plugin.getAccount(attacker.getName());

                    //Cancel if same team
                    if (combat.sameTeam(pAttacker, pVictim)) {
                        e.setCancelled(true);
                        return;
                    }

                    //Cancel event, reapply damage to prevent knockback
                    if (pVictim.hasTalentActive(Talent.ENDER_HOOK)) {
                        e.setCancelled(true);
                        cause = Talent.POTION_SLINGER;
                        plugin.registerDamage(pVictim.name, pAttacker.name, Talent.POTION_SLINGER);
                        victim.damage(e.getDamage());
                        return;
                    }

                    //Player has bloodlust, redo the damage
                    if (pAttacker.getPlayer().hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
                        double newDamage = e.getDamage() + (e.getDamage() * .25);
                        e.setDamage(newDamage);
                    }

                    if (pAttacker.hasTalent(Talent.SPAWN)) {
                        log.info("Potion Damage (" + e.getDamage() + ") increased to " + (e.getDamage() + (e.getDamage() * spawnDamageBuff)) +
                                " for spawn effect");
                        e.setDamage(e.getDamage() + (e.getDamage() * spawnDamageBuff));
                    }

                    if (attacker.getWorld().getName().equalsIgnoreCase("world")) {
                        pAttacker.damage += e.getDamage();
                    }
                }
            } else if (e.getDamager() instanceof CraftEnderPearl) {
                // Attacker is an arrow (projectile damage)
                EnderPearl pearl = (EnderPearl) e.getDamager();
                if (!(pearl.getShooter() instanceof Player)) {
                    // Arrow was not fired by a player
                    return;
                }
                pearl.setMetadata("hooked", new FixedMetadataValue(plugin, true));

                attacker = (Player) pearl.getShooter();
                pAttacker = plugin.getAccount(attacker.getName());

                e.setCancelled(true);
                System.out.println("Hooking " + pVictim.name + " toward " + pAttacker.name);
                combat.hookPlayer(pVictim, pAttacker);
            }

            if (cause == null) plugin.clearDamageCause(victim.getName());
        }
    }

    @EventHandler
    public void onEntityDamageEvent(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            PvPPlayer pVictim = plugin.getAccount(player.getName());

            //No durability loss in PvP
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                armor.setDurability((short) 0);
            }
            player.updateInventory();

            //No fall damage in spawns
            if (e.getCause().equals(EntityDamageEvent.DamageCause.FALL)
                    && (inWGRegion(player, "redspawn") || inWGRegion(player, "bluespawn"))) {
                e.setCancelled(true);
                return;
            }

            //Always clear, think it's easier this way.
//            plugin.clearDamageCause(player.getName());

            if (pVictim.hasTalentActive(Talent.LAST_STAND) && !e.getCause().equals(EntityDamageEvent.DamageCause.MAGIC)) {
                e.setCancelled(true);
            }

            if (pVictim.hasTalentActive(Talent.LUNGE) && e.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
                e.setDamage(e.getDamage() / 2);
            }
        }
    }

    @EventHandler
    public void onPlayerRegen(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player)) return;

        //Regen is slower
        if (r.nextInt(2) == 0) {
            e.setCancelled(true);
        }
    }
}