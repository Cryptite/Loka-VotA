package com.cryptite.pvp.talents;

import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.utils.Combat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Bandage implements Runnable {
    private Player p;

    private Location startLocation;

    private Combat combat;

    private int taskID;

    public Bandage(Player p, int length) {
        this.p = p;
        startLocation = p.getLocation();

        LokaVotA plugin = (LokaVotA) Bukkit.getPluginManager().getPlugin("LokaPvP");
        combat = new Combat(plugin);

        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 10, 20);
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.getScheduler().cancelTask(taskID), 20 * length);
    }

    @Override
    public void run() {
        if (startLocation.distance(p.getLocation()) > 1) {
            Bukkit.getScheduler().cancelTask(taskID);
            return;
        }

        combat.smallHealPlayer(p, .5d);
    }
}
