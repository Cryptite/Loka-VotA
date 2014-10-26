package com.cryptite.pvp.talents;

import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.utils.Combat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

public class Bandage implements Runnable {
    private LokaVotA plugin;
    private PvPPlayer p;
    private Location startLocation;
    private Combat combat;
    public Boolean active = false;
    private int taskID;

    public Bandage(PvPPlayer p) {
        p.bandage = this;
        this.p = p;

        plugin = (LokaVotA) Bukkit.getPluginManager().getPlugin("LokaVotA");
        combat = new Combat(plugin);

        start(p);
    }

    public void start(PvPPlayer p) {
        active = true;
        startLocation = p.getLocation();

        taskID = plugin.scheduler.scheduleSyncRepeatingTask(plugin, this, 10, 20);
        int length = 7;
        if (p.hasTalent(Talent.IMPROVED_BANDAGE)) {
            length = 9;
        }
        plugin.scheduler.runTaskLater(plugin, () -> cancel(false), 20 * length);
    }

    @Override
    public void run() {
        if (!active) return;

        if (p.getPlayer() == null || startLocation.distance(p.getLocation()) > .5f) {
            cancel(true);
            return;
        }

        combat.smallHealPlayer(p.getPlayer(), .8d);
    }

    public void cancel(Boolean cancelled) {
        Bukkit.getScheduler().cancelTask(taskID);

        if (p.getPlayer() != null && cancelled) {
            p.getPlayer().sendMessage(ChatColor.GRAY + "Bandaging interrupted.");
        }

        active = false;
    }
}
