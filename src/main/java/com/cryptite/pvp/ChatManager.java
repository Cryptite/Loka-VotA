package com.cryptite.pvp;

import com.cryptite.pvp.bungee.Bungee;
import com.cryptite.pvp.data.Chat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.entity.Player;

import static org.bukkit.ChatColor.*;

public class ChatManager implements CommandExecutor {
    private LokaVotA plugin;
    private Bungee bungee;

    public ChatManager(LokaVotA plugin, Bungee bungee) {
        this.plugin = plugin;
        this.bungee = bungee;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String commandLabel, String[] args) {
        if (!(commandSender instanceof Player)) return false;
        Player player = (Player) commandSender;

        if (commandLabel.equalsIgnoreCase("p")) {
            if (args.length < 1) {
                player.sendMessage(GRAY + "Talk in public chat instead of team chat.");
                player.sendMessage(AQUA + "Usage: " +
                        YELLOW + "/p <message>" + AQUA + ".");
                return true;
            }

            sendMessage(player.getName(), "public", args, true);
            return true;
        } else if (commandLabel.equalsIgnoreCase("t")) {
            if (args.length < 1) {
                player.sendMessage(GRAY + "Talk in town chat.");
                player.sendMessage(AQUA + "Usage: " +
                        YELLOW + "/t <message>" + AQUA + ".");
                return true;
            } else if (plugin.getAccount(player.getName()).town == null) {
                player.sendMessage(GRAY + "You must be in a town to do this");
                return true;
            }

            sendMessage(player.getName(), "town", args, true);
            return true;
        } else if (commandLabel.equalsIgnoreCase("a")) {
            if (args.length < 1) {
                player.sendMessage(GRAY + "Talk in alliance chat.");
                player.sendMessage(AQUA + "Usage: " +
                        YELLOW + "/a <message>" + AQUA + ".");
                return true;
            } else if (plugin.getAccount(player.getName()).town == null) {
                player.sendMessage(GRAY + "You must be in a town to do this");
                return true;
            } else if (plugin.getAccount(player.getName()).town.alliance == null) {
                player.sendMessage(GRAY + "You must be in an alliance to do this");
                return true;
            }

            sendMessage(player.getName(), "alliance", args, true);

            return true;
        } else if (commandLabel.equalsIgnoreCase("o")) {
            if (args.length < 1) {
                player.sendMessage(GRAY + "Talk in admin chat.");
                player.sendMessage(AQUA + "Usage: " +
                        YELLOW + "/o <message>" + AQUA + ".");
                return true;
            } else if (!isAdmin(plugin.getAccount(player.getName()))) return true;

            sendMessage(player.getName(), "admin", args, true);

            return true;
        }
        return true;
    }

    public void sendMessage(Chat chat, Boolean outgoing) {
        sendMessage(chat.name, chat.channel, chat.message, outgoing);
    }

    public void sendMessage(String player, String channel, String[] args, Boolean outgoing) {
        sendMessage(player, channel, assembleMessage(args), outgoing);
    }

    public void sendMessage(String player, String channel, String message, Boolean outgoing) {
        PvPPlayer p = plugin.getAccount(player);
        switch (channel) {
            case "public":
                globalChatMessage(p, message);
                break;
            case "town":
                townChatMessage(p, message);
                break;
            case "alliance":
                allianceChatMessage(p, message);
                break;
            case "admin":
                adminChatMessage(p, message);
        }

        if (outgoing) {
            //Send to network
            Chat chat = new Chat(player, channel, message);
            bungee.sendMessage("ALL", new Gson().toJson(chat), "Chat");
        }
    }

    private String assembleMessage(String[] args) {
        StringBuilder b = new StringBuilder();
        for (String arg : args) {
            b.append(arg).append(" ");
        }
        return b.toString();
    }

    public void globalChatMessage(PvPPlayer p, String message) {
        StringBuilder chatMessage = new StringBuilder();

        if (p.isTownOwner()) {
            chatMessage.append(ChatColor.AQUA).append("[");
        } else {
            chatMessage.append(ChatColor.GRAY).append("[");
        }
        if (p.rank != null && p.rank.equals("Old One")) {
            chatMessage.append(ChatColor.RED).append("Old One");
        } else {
            chatMessage.append(ChatColor.GOLD).append(p.rank);
        }
        if (p.isTownOwner()) {
            chatMessage.append(ChatColor.AQUA).append("]");
        } else {
            chatMessage.append(ChatColor.GRAY).append("]");
        }
        String playerColor = ChatColor.WHITE + p.name;

        chatMessage.append(ChatColor.WHITE).append(" ").append(playerColor).append(ChatColor.WHITE);
        chatMessage.append(": ").append(message);

        for (Player player : plugin.server.getOnlinePlayers()) {
            if (player == null) continue;
            player.sendMessage(chatMessage.toString());
        }
    }

    void townChatMessage(PvPPlayer p, String message) {
        String playerColor;
        if (p.isTownOwner()) {
            playerColor = AQUA + p.name;
        } else {
            playerColor = WHITE + p.name;
        }

        for (Player player : plugin.server.getOnlinePlayers()) {
            if (player == null) continue;

            PvPPlayer pPvPPlayer = plugin.getAccount(player.getName());
            if (pPvPPlayer.town != null && pPvPPlayer.town.equals(p.town)) {
                p.sendMessage(GRAY + "[" +
                        AQUA + p.town.tag + GRAY + "] " + p.town.getMemberLevelString(pPvPPlayer.name) +
                        " " + p.town.getRank(pPvPPlayer.name) + WHITE + " " + playerColor +
                        WHITE + ": " + message);
            }
        }
    }

    void allianceChatMessage(PvPPlayer p, String message) {
        String msg = GRAY + "[" + YELLOW + p.town.alliance + GRAY + "] ";
        msg += GRAY + "[" + AQUA + p.town.tag + GRAY + "] " + WHITE + p.name + ": " + message;

        for (Player player : plugin.server.getOnlinePlayers()) {
            if (player == null) continue;

            PvPPlayer pPvPPlayer = plugin.getAccount(player.getName());
            if (pPvPPlayer.town == null || pPvPPlayer.town.alliance == null) continue;

            if (pPvPPlayer.town.alliance.equals(p.town.alliance)) {
                player.sendMessage(msg);
            }
        }
    }

    void adminChatMessage(PvPPlayer player, String message) {
        String msg = GRAY + "[" + RED + "Admin" + GRAY + "] ";
        msg += RED + player.name + WHITE + ": " + message;
        for (Player p : plugin.server.getOnlinePlayers()) {
            if (p == null) continue;

            if (isAdmin(plugin.getAccount(p.getName()))) {
                p.sendMessage(msg);
            }
        }

        System.out.println(ChatColor.stripColor(msg));
    }

    private boolean isAdmin(PvPPlayer p) {
        return (p.rank.equals("Guardian") || p.rank.equals("Elder") || p.rank.equals("OldOne"));
    }
}
