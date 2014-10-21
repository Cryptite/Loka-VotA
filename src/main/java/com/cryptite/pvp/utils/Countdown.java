package com.cryptite.pvp.utils;

import com.cryptite.pvp.Battleground;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import me.confuser.barapi.BarAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.YELLOW;

public class Countdown implements Runnable {

    private final int length;
    private final int id;
    private int interval;
    private Battleground battleground;
    private PvPPlayer player;
    private final long timerStarted;
    private final String message;
    private Player countdownPlayer = null;

    public Countdown(String battleground, String message, int length) {
        LokaVotA plugin = (LokaVotA) Bukkit.getPluginManager().getPlugin("LokaPvP");
        if (battleground.equalsIgnoreCase("vota")) {
            this.battleground = plugin.vota;
        }
        this.timerStarted = System.currentTimeMillis();
        this.message = message;
        this.length = length;
        this.interval = length;
        this.id = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 20, 20);
    }


    public Countdown(String message, int length, Player p) {
        LokaVotA plugin = (LokaVotA) Bukkit.getPluginManager().getPlugin("LokaPvP");
        this.timerStarted = System.currentTimeMillis();
        this.message = message;
        this.length = length;
        this.interval = length;
        this.countdownPlayer = p;
        this.id = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 20, 20);
    }

    @Override
    public void run() {
        interval--;
        updateBars();
        if (interval <= 0) {
            Bukkit.getScheduler().cancelTask(id);
            removeBars();
        }
    }

    void updateBars() {
        if (countdownPlayer == null) {
            for (PvPPlayer player : getPlayers()) {
                Player p = player.getPlayer();
                if (p == null) continue;

                BarAPI.setMessage(p, message + ": " + YELLOW + interval + GRAY + " seconds", getPercent());
            }
        } else {
            if (countdownPlayer.isDead()) {
                removeBars();
                return;
            }

            BarAPI.setMessage(countdownPlayer, message + ": " + YELLOW + interval + GRAY + " seconds", getPercent());
        }
    }

    void removeBars() {
        if (countdownPlayer == null) {
            for (PvPPlayer player : getPlayers()) {
                Player p = player.getPlayer();
                if (p == null) continue;

                BarAPI.removeBar(p);
            }
        } else {
            BarAPI.removeBar(countdownPlayer);
        }

        Bukkit.getScheduler().cancelTask(id);
    }

    List<PvPPlayer> getPlayers() {
        if (battleground != null) {
            return battleground.getAllPlayers();
        } else {
            return new ArrayList<>(Arrays.asList(player));
        }
    }

    float getPercent() {
        return (int) (((float) interval / (float) length) * 100);
    }
}
