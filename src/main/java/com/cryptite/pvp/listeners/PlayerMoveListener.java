package com.cryptite.pvp.listeners;

import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.logging.Logger;

public class PlayerMoveListener implements Listener {
    private final Logger log = Logger.getLogger("LokaPvP-PlayerDamageListener");
    private final LokaVotA plugin;

    public PlayerMoveListener(LokaVotA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        PvPPlayer p = plugin.getAccount(e.getPlayer().getName());
        if (p.bg == null && p.provingGrounds == null) return;

        //If these values don't change, this was just a head turn and we don't care.
        if (e.getFrom().getX() == e.getTo().getX()
                && e.getFrom().getY() == e.getTo().getY()
                && e.getFrom().getZ() == e.getTo().getZ()) {
            return;
        }

        if (plugin.playersRooted.contains(p.name)) {
            stopMovement(e);
        }
    }

    private void stopMovement(PlayerMoveEvent e) {
        Location loc = e.getFrom();
        loc.setPitch(e.getTo().getPitch());
        loc.setYaw(e.getTo().getYaw());
        e.getPlayer().teleport(loc);
    }
}