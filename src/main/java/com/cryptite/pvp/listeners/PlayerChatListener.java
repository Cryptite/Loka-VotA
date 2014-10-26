package com.cryptite.pvp.listeners;

import com.cryptite.pvp.Battleground;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {
    private final LokaVotA plugin;

    public PlayerChatListener(LokaVotA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        //Remove arena player from pool so they can be properly recreated next time they join.
        Player player = e.getPlayer();
        PvPPlayer p = plugin.getAccount(player.getName());

        e.setCancelled(true);

        if (p.bg != null) {
            Battleground bg = plugin.getBG(p.bg);

            String message;
            if (bg != null && bg.redTeam.containsKey(p.name)) {
                message = (ChatColor.GRAY + "[" +
                        ChatColor.RED + player.getName() + ChatColor.GRAY + "] " +
                        ChatColor.WHITE + ": " + e.getMessage());
                bg.messageRedTeam(message);
            } else if (bg != null && bg.blueTeam.containsKey(p.name)) {
                message = (ChatColor.GRAY + "[" +
                        ChatColor.DARK_AQUA + player.getName() + ChatColor.GRAY + "] " +
                        ChatColor.WHITE + ": " + e.getMessage());
                bg.messageBlueTeam(message);
            }
        } else {
            plugin.chat.sendMessage(player.getName(), "public", e.getMessage(), true);
        }
    }
}
