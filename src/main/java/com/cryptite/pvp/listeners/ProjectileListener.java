package com.cryptite.pvp.listeners;

import com.cryptite.pvp.CustomEffect;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.talents.Talent;
import com.cryptite.pvp.utils.Combat;
import com.cryptite.pvp.utils.Rope;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEnderPearl;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.logging.Logger;

import static com.cryptite.pvp.utils.LocationUtils.playWorldCustomSound;

public class ProjectileListener implements Listener {
    private final Logger log = Logger.getLogger("LokaPvP-Bungee");
    private final LokaVotA plugin;
    private final Combat combat;

    public ProjectileListener(LokaVotA plugin) {
        this.plugin = plugin;
        combat = new Combat(plugin);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        Projectile projectile = e.getEntity();
        LivingEntity shooter = (LivingEntity) projectile.getShooter();

        if (!(shooter instanceof Player)) return;
        PvPPlayer p = plugin.getAccount(((Player) shooter).getName());

        if (projectile instanceof Arrow) {
            if (p.hasTalentActive(Talent.EXPLOSIVE_ARROW)) {
                //Maybe this makes the arrow look like it's on fire?
                e.getEntity().setFireTicks(40);
                e.getEntity().setMetadata("explosive", new FixedMetadataValue(plugin, p.name));
                p.talentAbilities.get(Talent.EXPLOSIVE_ARROW).active = false;
            }

            if (p.hasTalentActive(Talent.HEAL_ARROW)) {
                e.getEntity().setMetadata("healarrow", new FixedMetadataValue(plugin, p.name));
                p.talentAbilities.get(Talent.HEAL_ARROW).active = false;
            }

            //Easier to assume here that they missed. If they didn't, it'll +2 over in the DamageListener
            if (p.hasTalent(Talent.GROOVE) && p.grooveStacks > 0) {
                p.grooveStacks--;
            }

        } else if (projectile instanceof ThrownPotion) {
            if (p.hasTalent(Talent.POTION_SLINGER)) {
                e.getEntity().setVelocity(e.getEntity().getVelocity().multiply(1.7));
            }
        } else if (projectile instanceof CraftEnderPearl) {
            plugin.scheduler.runTaskLater(plugin, () -> {
                Rope rope = new Rope(e.getEntity().getLocation(), shooter);
                rope.glueEndTo(e.getEntity());
                p.hook = rope;
            }, 2);

            plugin.scheduler.runTaskLater(plugin, () -> {
                if (p.hook != null) {
                    System.out.println("Removing hook for " + p.name);
                    p.hook.removeAll();
                    p.hook = null;
                }
            }, 20 * 5);
        }
    }

    @EventHandler
    public void onArrowHit(final ProjectileHitEvent e) {
        Projectile projectile = e.getEntity();
        LivingEntity shooter = (LivingEntity) projectile.getShooter();
        if (!(shooter instanceof Player)) return;

        if (projectile instanceof Arrow) {
            final PvPPlayer p = plugin.getAccount(((Player) shooter).getName());

            //Disable arrow hit effects if it's Skyvale before the match starts.
            if (e.getEntity().hasMetadata("explosive")) {
                //Boom!
                projectile.getWorld().createExplosion(projectile.getLocation(), 0, false);
                projectile.remove();

                //Apply knockback to nearby foes
                List<String> enemiesAffected = combat.aoeKnockback(shooter, p, projectile.getLocation(), 5, 13.1, 1.7f, 1.2f, null, Talent.EXPLOSIVE_ARROW);

                //Apply wither
                combat.aoeDebuffEffect(p, projectile.getLocation(), 5, PotionEffectType.WITHER, 1, 5, null);
                //Apply blindness
                combat.aoeDebuffEffect(p, projectile.getLocation(), 5, PotionEffectType.BLINDNESS, 1, 3, null);

                plugin.effect(projectile.getLocation(), CustomEffect.LAVA, 6, .4f);

                //Cancel explosive arrow talent effect since it was just used.
                plugin.scheduler.runTaskLater(plugin,
                        () -> p.talentAbilities.get(Talent.EXPLOSIVE_ARROW).deactivate(p), 10
                );

                //If 3 or more, achievement!
                if (enemiesAffected.size() >= 3 && !p.provingGrounds) {
                    plugin.bungee.sendMessage(p.name + ".pvp.explosivearrowmultiple", "Achievement");
                }
            } else if (e.getEntity().hasMetadata("healarrow")) {
                //Boom!
                projectile.getWorld().strikeLightningEffect(projectile.getLocation().add(0, 2, 0));
                projectile.remove();

                playWorldCustomSound(projectile.getLocation(), "HealArrow", 15);

                //Insta-Heal
                int friendliesAffected = combat.healNearbyFriendlies(projectile.getLocation(), p, 4);
                //Apply Regen
                combat.aoeBuffEffect(p, projectile.getLocation(), 10, PotionEffectType.REGENERATION, 1, 6, "Heal");
                //Apply Weakness
                combat.aoeDebuffEffect(p, projectile.getLocation(), 10, PotionEffectType.WEAKNESS, 1, 6, "Slow");

                plugin.effect(projectile.getLocation(), CustomEffect.HAPPY_VILLAGER, 8, .4f);

                //Cancel explosive arrow talent effect since it was just used.
                plugin.scheduler.runTaskLater(plugin,
                        () -> p.talentAbilities.get(Talent.HEAL_ARROW).deactivate(p), 10
                );

                //If 3 or more, achievement!
                if (friendliesAffected >= 3 && !p.provingGrounds) {
                    plugin.bungee.sendMessage(p.name + ".pvp.healarrowmultiple", "Achievement");
                }
            }
        } else if (projectile instanceof CraftEnderPearl) {
            final PvPPlayer p = plugin.getAccount(((Player) shooter).getName());

            System.out.println("Detected hit on " + p.name);
            if (p.hook == null || projectile.hasMetadata("hooked")) return;
            p.hook.setEnd(e.getEntity().getLocation());

            System.out.println("Hooking " + p.name + " to location");
            combat.hookPlayer(p, e.getEntity().getLocation());
        }
    }
}
