package com.cryptite.pvp.utils;

import com.cryptite.pvp.LokaVotA;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

public class AFK implements Runnable, Listener {
    private final LokaVotA plugin;
    private Map<String, StringLocation> playerLocations = new HashMap<>();
    private Map<String, Integer> violations = new HashMap<>();

    public AFK(LokaVotA plugin) {
        this.plugin = plugin;
        plugin.scheduler.scheduleSyncRepeatingTask(plugin, this, 120, 20 * 30);
    }

    @Override
    public void run() {
        for (Player p : plugin.server.getOnlinePlayers()) {
            String name = p.getName();
            if (!playerLocations.containsKey(name)) {
                playerLocations.put(name, new StringLocation(p.getLocation()));
            } else {
                if (!playerLocations.get(name).hasMoved(p.getLocation())) {
                    if (violations.containsKey(name)) {
                        violations.put(name, violations.get(name) + 1);
                    } else {
                        violations.put(name, 1);
                    }
                    System.out.println("[AFK] " + name + " is AFK " + violations.get(name) + " times.");
                    checkViolations(name);
                } else {
                    violations.remove(name);
                    playerLocations.put(name, new StringLocation(p.getLocation()));
                }
            }
        }
    }

    private void checkViolations(String player) {
        Player p = plugin.server.getPlayerExact(player);
        if (violations.containsKey(player)
                && violations.get(player) >= 4
                && p != null) {
            plugin.bungee.sendPlayer(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void playerQuit(PlayerQuitEvent e) {
        playerLocations.remove(e.getPlayer().getName());
        violations.remove(e.getPlayer().getName());
    }
}
