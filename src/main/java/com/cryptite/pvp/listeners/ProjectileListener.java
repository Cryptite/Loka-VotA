package com.cryptite.pvp.listeners;

import com.cryptite.pvp.CustomEffect;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.talents.Talent;
import com.cryptite.pvp.utils.Combat;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;
import java.util.logging.Logger;

import static com.cryptite.pvp.utils.LocationUtils.playCustomSound;
import static com.cryptite.pvp.utils.LocationUtils.playWorldCustomSound;

public class ProjectileListener implements Listener {
    private final Logger log = Logger.getLogger("LokaVotA-Bungee");
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
            p.arrowsFired++;

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

            if (p.hasTalentActive(Talent.HOOK)) {
                e.getEntity().setMetadata("hook", new FixedMetadataValue(plugin, p.name));
            }

            //Easier to assume here that they missed. If they didn't, it'll +2 over in the DamageListener
            if (p.hasTalent(Talent.GROOVE) && p.grooveStacks > 0) {
                p.grooveStacks--;
            }

        } else if (projectile instanceof ThrownPotion) {
            if (p.hasTalent(Talent.POTION_SLINGER)) {
                e.getEntity().setVelocity(e.getEntity().getVelocity().multiply(1.7));
            }
        }
    }

    @EventHandler
    public void onArrowHit(final ProjectileHitEvent e) {
        Projectile projectile = e.getEntity();
        LivingEntity shooter = (LivingEntity) projectile.getShooter();
        if (!(shooter instanceof Player)) return;

        if (projectile instanceof Arrow) {
            final PvPPlayer p = plugin.getAccount(((Player) shooter).getName());

            if (e.getEntity().hasMetadata("explosive")) {
                //Boom!
                projectile.getWorld().createExplosion(projectile.getLocation(), 0, false);
                projectile.remove();

                //Apply knockback to nearby foes
                List<String> enemiesAffected = combat.aoeKnockback(shooter, p, projectile.getLocation(), 5, 13.1, 1.7f, 1.2f, null, Talent.EXPLOSIVE_ARROW);
                combat.aoeTalent(p, projectile.getLocation(), 6, Talent.EXPLOSIVE_ARROW, 6);

                plugin.effect(projectile.getLocation(), CustomEffect.LAVA, 6, .4f);

                //Cancel explosive arrow talent effect since it was just used.
                plugin.scheduler.runTaskLater(plugin,
                        () -> p.talentAbilities.get(Talent.EXPLOSIVE_ARROW).deactivate(p), 10
                );

                //If 3 or more, achievement!
                if (enemiesAffected.size() >= 3) {
                    plugin.bungee.sendMessage("loka", p.name + ".pvp.explosivearrowmultiple", "Achievement");
                }
            } else if (e.getEntity().hasMetadata("healarrow")) {
                //Boom!
                projectile.getWorld().strikeLightningEffect(projectile.getLocation().add(0, 2, 0));
                projectile.remove();

                combat.aoeTalent(p, projectile.getLocation(), 7, Talent.HEAL_ARROW, 6);

                playWorldCustomSound(projectile.getLocation(), "HealArrow", 15);

                //Cancel explosive arrow talent effect since it was just used.
                plugin.scheduler.runTaskLater(plugin,
                        () -> p.talentAbilities.get(Talent.HEAL_ARROW).deactivate(p), 10
                );
            } else if (e.getEntity().hasMetadata("hook")) {
                if (projectile.getLocation().distance(shooter.getLocation()) <= 30) {
                    plugin.scheduler.runTask(plugin, () -> delayHook(e.getEntity(), p));
                } else {
                    playCustomSound((Player) shooter, "HookSnap");
                }
            }
        }
    }

    private void delayHook(Entity projectile, PvPPlayer p) {
        if (projectile.hasMetadata("hooked")) return;

//            p.hook.setEnd(e.getEntity().getLocation());

        System.out.println("Hooking " + p.name + " to location");
        combat.hookPlayer(p, projectile.getLocation());
    }
}
