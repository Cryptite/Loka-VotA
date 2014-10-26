package com.cryptite.pvp.listeners;

import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.util.Vector;
import org.mcsg.double0negative.tabapi.TabAPI;

import java.util.logging.Logger;

public class PlayerJoinListener implements Listener {
    private final Logger log = Logger.getLogger("Artifact-Join");
    private final LokaVotA plugin;

    public PlayerJoinListener(LokaVotA plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        PvPPlayer player = plugin.getAccount(p.getName());

        //No movement
        p.setVelocity(new Vector(0, 0, 0));

        //Take no damage for 3 seconds
        p.setNoDamageTicks(20 * 8);

        //Wipe inventory and armor always
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);

        //Wipe Tab list stuff
        TabAPI.setPriority(plugin, event.getPlayer(), -2);

        //Check for chat spam grace period
        checkGracePeriod();

        //We don't announce joins to the pvp server.
        event.setJoinMessage("");

//        if (playerIsProcessed(player)) {
//            System.out.println("Would send " + player + " to match? Server size: " + plugin.server.getOnlinePlayers().size());
//            sendToMatch(player);
//            return;
//        }

        //Send them to the PvP Spawn
        if (plugin.spawn != null) {
            event.getPlayer().teleport(plugin.spawn);
        }

        //Send them on their way.
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> sendToMatch(player), 40
        );
    }

    private void checkGracePeriod() {
        //Grace period is when the the first player joins after the server was empty before bungee will spit out chat
        //from other players. This is to prevent a huge spam of chat buildup when the server's been empty for awhile.
        if (plugin.server.getOnlinePlayers().size() != 1) return;

        //This is the first player on, set a grace period.
        log.info("Activating chat grace period");
        plugin.bungee.muteChatFromLoka = true;

        //After a second, grace period can terminate.
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> {
                    log.info("Deactivating chat grace period");
                    plugin.bungee.muteChatFromLoka = false;
                }, 20
        );
    }

    void sendToMatch(PvPPlayer p) {
        if (p.bg != null) {
            if (p.spectator) {
                plugin.vota.joinSpectator(p);
            } else {
                plugin.vota.playerReady(p);
            }
        }
    }
}
