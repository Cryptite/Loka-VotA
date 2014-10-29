package com.cryptite.pvp.listeners;

import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.talents.Talent;
import com.cryptite.pvp.utils.Combat;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftThrownPotion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.logging.Logger;

public class PotionListener implements Listener {
    private final Logger log = Logger.getLogger("Artifact-PvPPlayer");
    private final LokaVotA plugin;
    private final Combat combat;

    public PotionListener(LokaVotA plugin) {
        this.plugin = plugin;
        this.combat = new Combat(plugin);
    }

    @EventHandler
    public void splashEvent(PotionSplashEvent e) {
        if (!(e.getPotion() instanceof CraftThrownPotion) || !(e.getPotion().getShooter() instanceof Player)) return;

        // Cast attacker
        Player attacker = (Player) e.getPotion().getShooter();
        PvPPlayer pThrower = plugin.getAccount(attacker.getName());

        Boolean heal = false;

        for (PotionEffect eff : e.getPotion().getEffects()) {
            if (eff.getType().equals(PotionEffectType.HEAL)) {
                heal = true;
                if (pThrower.talents.contains(Talent.HOLY_GRENADE)) {
                    e.setCancelled(true);
                    int amount = new Random().nextInt(2) + 1;
                    int friendliesAffected = combat.healNearbyFriendlies(e.getEntity().getLocation(), pThrower, amount);

                    //If 3 or more, achievement!
                    if (friendliesAffected >= 3) {
                        plugin.bungee.sendMessage("loka", pThrower.name + ".pvp.holygrenademultiple", "Achievement");
                    }
                }
            }
        }

        //Check each entity that was hit by this potion
        Player victim;
        for (LivingEntity entity : e.getAffectedEntities()) {
            if (!(entity instanceof Player)) continue;
            victim = (Player) entity;

            //You can easily hit yourself with a splash potion.
            if (victim == attacker) {
                // eah, this is the same player. Let him burn! Next!
                continue;
            }

            // Check teams
            if (combat.sameTeam(victim.getName(), attacker.getName()) && !heal) {
                //Reduce the effect of this potion to zero (victim only)
                e.setIntensity(victim, 0);
            }
        }
    }
}
