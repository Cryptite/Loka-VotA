package com.cryptite.pvp.listeners;

import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.talents.Talent;
import com.cryptite.pvp.utils.Combat;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.cryptite.pvp.utils.LocationUtils.playWorldCustomSound;

public class TrapHandler implements Runnable, Listener {
    private final Logger log = Logger.getLogger("Artifact-Join");
    private final LokaVotA plugin;
    private final Combat combat;

    public final Map<String, Location> traps = new HashMap<>();
    public final Map<String, Integer> trapTasks = new HashMap<>();

    public TrapHandler(LokaVotA plugin) {
        this.plugin = plugin;
        this.combat = new Combat(plugin);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 100, 5);
    }

    @Override
    public void run() {
        List<String> playerTrapActivated = new ArrayList<>();

        for (String player : traps.keySet()) {
            Location trapLocation = traps.get(player);
            for (Player victim : trapLocation.getWorld().getPlayers()) {
                Player owner = plugin.server.getPlayerExact(player);

                //If the owner is gone or same team, don't care.
                if (owner == null
                        || combat.sameTeam(player, victim.getName())
                        || victim.getName().equals(player)) continue;

                double distance = victim.getLocation().distance(trapLocation);
                if (distance <= 2) {
                    //Activate the Trap!
                    playWorldCustomSound(trapLocation, "FreezingTrapActivate", 15);

                    combat.aoeTalent(plugin.getAccount(player), trapLocation, 3, Talent.FREEZING_TRAP, 4);

                    //Tell player via sound his trap went off
                    owner.playSound(owner.getLocation(), "FreezingTrapNotify", 2, 1);

                    playerTrapActivated.add(player);

                    //Cancel FX
                    plugin.scheduler.cancelTask(trapTasks.get(player));
                }
            }
        }

        //It's possible two wells go off simultaneously, hence the list. So iterate through those that did and remove
        //from wells list.
        if (playerTrapActivated.size() > 0) {
            for (String player : playerTrapActivated) {
                traps.remove(player);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        String player = e.getEntity().getName();
        if (traps.containsKey(player)) removeTrap(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        String player = e.getPlayer().getName();
        if (traps.containsKey(player)) removeTrap(player);
    }

    private void removeTrap(String player) {
        if (traps.containsKey(player)) {
            traps.remove(player);
        }

        if (trapTasks.containsKey(player)) {
            plugin.scheduler.cancelTask(trapTasks.get(player));
            trapTasks.remove(player);
        }
    }
}
