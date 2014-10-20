package com.cryptite.pvp.listeners;

import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.talents.Talent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.logging.Logger;

public class PlayerRespawnListener implements Listener {
    private final Logger log = Logger.getLogger("Artifact-PvPPlayer");
    private final LokaVotA plugin;

    public PlayerRespawnListener(LokaVotA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final String player = event.getPlayer().getName();
        final PvPPlayer p = plugin.getAccount(player);

        if (p.bg != null) {
            if (plugin.vota.blueTeam.containsKey(p.name)) {
                event.setRespawnLocation(plugin.vota.blueSpawn);
            } else {
                event.setRespawnLocation(plugin.vota.redSpawn);
            }
            plugin.scheduler.runTaskLater(plugin, () -> plugin.vota.showDoorsTimer(p), 20);

            plugin.scheduler.runTaskLater(plugin,
                    () -> p.setPlayerGear(true), 20
            );

            //They get 15 seconds of extra damage to prevent spawn camping
            p.talents.add(Talent.SPAWN);

            //10 seconds of invincibility.
            plugin.scheduler.runTaskLater(plugin,
                    () -> {
                        if (plugin.players.containsKey(p.name)) {
                            p.getPlayer().setNoDamageTicks(20 * 10);
                        }
                    }, 20
            );


            plugin.scheduler.runTaskLater(plugin,
                    () -> {
                        if (plugin.players.containsKey(p.name)) {
                            //Remove spawn protection buff.
                            p.talents.remove(Talent.SPAWN);
                        }
                    }, 20 * 10
            );
        }

        if (plugin.playersToReturn.contains(player)) {
            plugin.scheduler.runTaskLater(plugin,
                    () -> {
                        plugin.bungee.sendPlayer(event.getPlayer());
                        plugin.playersToReturn.remove(player);
                    }, 20
            );

        }
    }
}
