package com.cryptite.pvp.listeners;

import com.cryptite.pvp.Battleground;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Logger;

public class PlayerQuitListener implements Listener {
    private final Logger log = Logger.getLogger("LokaVotA-PlayerQuitListener");
    private final LokaVotA plugin;

    public PlayerQuitListener(LokaVotA plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent event) {
        //If the player is not in arenaplayers, their arena game is done and they'll be handled by Respawn most likely..
        if (!plugin.players.containsKey(event.getPlayer().getName())) return;

        PvPPlayer p = plugin.players.get(event.getPlayer().getName());

        //Cancel aura effects
        if (p.aura != null) p.aura.cancel();

        if (p.bg != null) {
            final Battleground bg = plugin.getBG(p.bg);
            if (bg == null) return;
//                if (bg instanceof Overload) {
//                    plugin.overload.removePlayer(p);
//                } else {
            bg.removePlayer(p);
//                }
            //This should happen late so that the quitting player doesn't get used as the bungee communicator
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> bg.sendMatchUpdate(true), 40
            );
        }

        if (plugin.playersToReturn.contains(p.name)) {
            //Player disconnected of own accord and will thus return to Loka. Can safely remove.
            plugin.playersToReturn.remove(p.name);
        }

        //Remove arena player from pool so they can be properly recreated next time they join.
        plugin.players.remove(event.getPlayer().getName());

        //Quits are hidden on this server
        event.setQuitMessage("");
    }
}
