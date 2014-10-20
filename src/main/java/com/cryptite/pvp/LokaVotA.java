package com.cryptite.pvp;

import com.cryptite.pvp.bungee.AllianceChat;
import com.cryptite.pvp.bungee.Bungee;
import com.cryptite.pvp.bungee.Chat;
import com.cryptite.pvp.bungee.SimpleChat;
import com.cryptite.pvp.json.Match;
import com.cryptite.pvp.listeners.*;
import com.cryptite.pvp.talents.Talent;
import com.cryptite.pvp.talents.Talents;
import com.cryptite.pvp.utils.AFK;
import com.cryptite.pvp.vota.VotA;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.kitteh.vanish.staticaccess.VanishNoPacket;
import org.kitteh.vanish.staticaccess.VanishNotLoadedException;

import java.util.*;
import java.util.logging.Logger;

import static com.cryptite.pvp.utils.LocationUtils.playCustomSound;
import static org.bukkit.ChatColor.*;

public class LokaVotA extends JavaPlugin implements CommandExecutor {
    private final Logger log = Logger.getLogger("LokaPvP");

    //Plugin or Server-based variables
    public Server server;
    public BukkitScheduler scheduler;
    private static String serverName; // Example: using the GetServer subchannel

    public PluginManager pm;

    public final Map<String, PvPPlayer> players = new HashMap<>();

    //Battleground variables
    public VotA vota;
//    public Overload overload;

    //Game variables
    public World world;
    public final List<String> playersToReturn = new ArrayList<>();
    public final List<Block> inventoryPreferenceSigns = new ArrayList<>();
    public final List<Block> talentSigns = new ArrayList<>();

    //Trackers
    public Map<String, Talent> lastDamageCause = new HashMap<>();
    public Map<String, String> lastDamager = new HashMap<>();
    public List<String> playersRooted = new ArrayList<>();
    public List<String> bandagingPlayers = new ArrayList<>();

    //Misc
    private Random r;
    public Bungee bungee;
    public Talents talents;
    public Location spawn;
    public TrapHandler traps;
    public SpeedRampHandler speedRamps;
    public PlayerDB db;
    private AFK afk;

    public void onEnable() {
        pm = this.getServer().getPluginManager();
        server = getServer();
        scheduler = server.getScheduler();

        //Bungee proxy stuff
        bungee = new Bungee(this);
        pm.registerEvents(bungee, this);

        serverName = "Arena1";

        world = server.getWorld("world");
        spawn = new Location(world, 415, 44, 657.5);
        r = new Random();

        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
        pm.registerEvents(new PlayerRespawnListener(this), this);
        pm.registerEvents(new PlayerDeathListener(this), this);
        pm.registerEvents(new PlayerDamageListener(this), this);
        pm.registerEvents(new PlayerMoveListener(this), this);
        pm.registerEvents(new PlayerInteractListener(this), this);
        pm.registerEvents(new PlayerChatListener(this), this);
        pm.registerEvents(new ProjectileListener(this), this);
        pm.registerEvents(new PotionListener(this), this);

        //DB
//        db = new PlayerDB(this);

        //Traps
        traps = new TrapHandler(this);
        pm.registerEvents(traps, this);

        //Ender Charges
        speedRamps = new SpeedRampHandler(this);

        talents = new Talents(this);
        pm.registerEvents(talents, this);

        //AFK
        afk = new AFK(this);
        pm.registerEvents(afk, this);

        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
    }

    public void onDisable() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
                             String commandLabel, String[] args) {
        final Player player;

        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            player = null;
        }

        if (commandLabel.equalsIgnoreCase("pvp")) {
//            if (!isAdmin(player)) return true;

            if (args[0].equalsIgnoreCase("generic")) {
                log.info("Length: " + args.length);
            } else if (args[0].equals("tptarget")) {
                ItemStack i = new ItemStack(Material.BOOK_AND_QUILL);
                ItemMeta meta = i.getItemMeta();
                meta.setDisplayName(args[1]);
                i.setItemMeta(meta);
                player.getInventory().addItem(i);
                player.sendMessage(GRAY + "TPTarget Book for " + args[1] + " added.");
            } else if (args[0].equals("sound")) {
                final String soundArg = args[1];
                if (player != null) {
                    playCustomSound(player, soundArg);
                }
            } else if (args[0].equalsIgnoreCase("join")) {
                PvPPlayer p = getAccount(player.getName());
                if (vota == null) {
                    vota = new VotA(this, server.getWorld("valley"));
                }
                vota.setTeam(p);
                vota.playerReady(p);
            } else if (args[0].equalsIgnoreCase("ready")) {
                vota.matchReady();
            } else if (args[0].equalsIgnoreCase("start")) {
                vota.beginMatch();
            } else if (commandLabel.equalsIgnoreCase("p")) {
                if (args.length < 1) {
                    player.sendMessage(ChatColor.GRAY + "Talk in public chat instead of team chat.");
                    player.sendMessage(ChatColor.AQUA + "Usage: " +
                            ChatColor.YELLOW + "/p <message>" + ChatColor.AQUA + ".");
                    return true;
                }

                PvPPlayer p = getAccount(player.getName());
                StringBuilder b = new StringBuilder();
                for (String arg : args) {
                    b.append(arg).append(" ");
                }

                //Build the chat message with some information from our player class
                Chat chat = new Chat(player.getName(), p.rank, b.toString(), p.townOwner, false, null, null);

                //Send to everyone
                globalChatMessage(parseChatMessage(chat), false);

                //Send to everyone on Loka
                sendChatToNetwork(player.getName(), b.toString());
                return true;
            } else if (commandLabel.equalsIgnoreCase("t")) {
                if (args.length < 1) {
                    player.sendMessage(ChatColor.GRAY + "Talk in town chat.");
                    player.sendMessage(ChatColor.AQUA + "Usage: " +
                            ChatColor.YELLOW + "/t <message>" + ChatColor.AQUA + ".");
                    return true;
                }

                PvPPlayer p = getAccount(player.getName());
                StringBuilder b = new StringBuilder();
                for (String arg : args) {
                    b.append(arg).append(" ");
                }

                //Build the chat message with some information from our player class
//            Chat chat = new Chat(player.getName(), p.townRank, b.toString(), p.townOwner, false, p.town, p.townRank);

                //Send to everyone on Loka
                sendTownChatToNetwork(player.getName(), b.toString());
                return true;
            } else if (commandLabel.equalsIgnoreCase("a")) {
                if (args.length < 1) {
                    player.sendMessage(ChatColor.GRAY + "Talk in alliance chat.");
                    player.sendMessage(ChatColor.AQUA + "Usage: " +
                            ChatColor.YELLOW + "/a <message>" + ChatColor.AQUA + ".");
                    return true;
                }

                PvPPlayer p = getAccount(player.getName());
                StringBuilder b = new StringBuilder();
                for (String arg : args) {
                    b.append(arg).append(" ");
                }

                //Build the chat message with some information from our player class
//            Chat chat = new Chat(player.getName(), p.townRank, b.toString(), p.townOwner, false, p.town, p.townRank);

                //Send to everyone on Loka
                sendAllianceChatToNetwork(player.getName(), b.toString());
                return true;
            } else if (commandLabel.equalsIgnoreCase("leave")) {
                PvPPlayer p = getAccount(player.getName());
                if (p.bg != null) {
                    Battleground bg = getBG(p.bg);
                    bg.removePlayer(p);
                    p.bg = null;
                    bg.sendMatchUpdate(true);
                }
                bungee.sendPlayer(player);
            } else if (commandLabel.equalsIgnoreCase("talents")) {
                talents.showTalentTree(player, null);
            } else if (commandLabel.equalsIgnoreCase("inspect")) {
                Player inspectee = server.getPlayerExact(args[0]);
                if (inspectee == null) {
                    player.sendMessage(GRAY + "No such player " + GOLD + args[0]);
                    return true;
                }

                talents.showTalentTree(inspectee, player);
            } else if (commandLabel.equalsIgnoreCase("shutdown")) {
                if (sender instanceof Player && !isAdmin(player)) return true;

                for (Player pl : server.getOnlinePlayers()) {
                    pl.sendMessage(ChatColor.GRAY + "The PvP server is restarting for maintenance.");
                    bungee.sendPlayer(pl);
                }
            }
        }
        return true;
    }

    private Boolean isAdmin(Player p) {
        return p != null && p.getName().equals("Cryptite");
    }

    public Battleground getBG(String name) {
        if (name.equalsIgnoreCase("vota")) {
            return vota;
//        } else if (name.equalsIgnoreCase("overload")) {
//            return overload;
        }

        return null;
    }

    public void processMatch(Match match) {
        //If vota is null, we need to create it.
        if (vota == null) {
            vota = new VotA(this, server.getWorld("valley"));
        }

        if (!vota.active) {
            //Time to start a new vota.
            vota.matchReady();
            vota.active = true;
        }

        vota.processNewPlayers(match.bgplayers);
        vota.processSpectators(match.spectators);
    }

    public PvPPlayer getAccount(String name) {
        if (players.containsKey(name)) {
            return players.get(name);
        } else {
            PvPPlayer p = new PvPPlayer(this, name);
            p.load();
            players.put(name, p);
            return p;
        }
    }

    public void effect(Location location, CustomEffect effectType) {
        effect(location, effectType, 10, 0.3f, 1);
    }

    public void effect(Location location, CustomEffect effectType, int numParticles, float speed) {
        effect(location, effectType, numParticles, speed, 1);
    }

    public void effect(Location location, CustomEffect effectType, int numParticles, float speed, float spreadDampen) {
        for (int i = 0; i < 10; i++) {
            for (Player p : server.getOnlinePlayers()) {
                effectType.createEffect(p,
                        location,
                        r.nextFloat() / spreadDampen,
                        (.1f + r.nextFloat()) / spreadDampen,
                        r.nextFloat() / spreadDampen,
                        speed,
                        numParticles);
            }
        }
    }

    public void effect(Player p, Location location, CustomEffect effectType, int numParticles, float speed, float spreadDampen) {
        for (int i = 0; i < 10; i++) {
            effectType.createEffect(p,
                    location,
                    r.nextFloat() / spreadDampen,
                    (.1f + r.nextFloat()) / spreadDampen,
                    r.nextFloat() / spreadDampen,
                    speed,
                    numParticles);
        }
    }

    public void smallEffect(Location location, CustomEffect effectType, int numParticles, float speed, float spreadDampen) {
        for (int i = 0; i < numParticles; i++) {
            for (Player p : server.getOnlinePlayers()) {
                effectType.createEffect(p,
                        location,
                        r.nextFloat() / spreadDampen,
                        (.1f + r.nextFloat()) / spreadDampen,
                        r.nextFloat() / spreadDampen,
                        speed,
                        numParticles);
            }
        }
    }

    public void blockEffect(Location location, CustomEffect effectType, int id, int data, int numParticles, float speed, float spreadDampen) {
        for (int i = 0; i < numParticles; i++) {
            for (Player p : server.getOnlinePlayers()) {
                effectType.blockEffect(p,
                        location,
                        id,
                        data,
                        r.nextFloat() / spreadDampen,
                        (.1f + r.nextFloat()) / spreadDampen,
                        r.nextFloat() / spreadDampen,
                        speed,
                        numParticles);
            }
        }
    }

    public void lengthyEffect(List<String> players, int seconds, CustomEffect effectType, int id, int data, int numParticles, float speed, float dampen) {
        int taskID = scheduler.scheduleSyncRepeatingTask(this, () -> {
            for (String player : players) {
                Player p = server.getPlayerExact(player);
                if (p == null || p.isDead()) continue;

                blockEffect(p.getLocation().add(0, .3f, 0), effectType, id, data, numParticles, speed, dampen);
            }
        }, 5, 5);

        scheduler.runTaskLater(this, () -> scheduler.cancelTask(taskID), 20 * seconds);
    }

    public void globalChatMessage(String message, Boolean fromLoka) {
        for (Player p : server.getOnlinePlayers()) {
            if (p != null) {
                p.sendMessage(message);
            }
        }
    }

    public String parseChatMessage(Chat chat) {
        StringBuilder chatMessage = new StringBuilder();

        if (chat.townOwner) {
            chatMessage.append(ChatColor.AQUA).append("[");
        } else {
            chatMessage.append(ChatColor.GRAY).append("[");
        }
        if (chat.op || (chat.rank != null && chat.rank.equals("Old One"))) {
            chatMessage.append(ChatColor.RED).append("Old One");
        } else {
            chatMessage.append(ChatColor.GOLD).append(chat.rank);
        }
        if (chat.townOwner) {
            chatMessage.append(ChatColor.AQUA).append("]");
        } else {
            chatMessage.append(ChatColor.GRAY).append("]");
        }
        String playerColor;
        if (chat.op) {
            playerColor = (ChatColor.RED + chat.name);
        } else {
            playerColor = (ChatColor.WHITE + chat.name);
        }

        chatMessage.append(ChatColor.WHITE).append(" ").append(playerColor).append(ChatColor.WHITE);
        chatMessage.append(": ").append(chat.message);

        return chatMessage.toString();
    }

    public void sendChatToNetwork(String player, String message) {
        SimpleChat chat = new SimpleChat(player, message, false, false);
        bungee.sendMessage(new Gson().toJson(chat), "Chat");
    }

    public void townChatMessage(Chat chat) {
        String playerColor;
        if (chat.townOwner) {
            playerColor = (AQUA + chat.name);
        } else if (chat.op) {
            playerColor = (RED + chat.name);
        } else {
            playerColor = (WHITE + chat.name);
        }

//        log.info("[TOWN] [" + chat.townTag + "] " + chat.rank + chat.name + ": " + chat.message);

        for (Player p : server.getOnlinePlayers()) {
            PvPPlayer pAccount = getAccount(p.getName());
            if (pAccount.town != null && pAccount.town.equals(chat.town)) {
                p.sendMessage(GRAY + "[" +
                        AQUA + chat.townTag + GRAY + "] " +
                        chat.rank + WHITE + playerColor +
                        WHITE + " : " + chat.message);
            }
        }
    }

    public void allianceChatMessage(AllianceChat chat) {
        String msg = GRAY + "[" + YELLOW + chat.alliance + GRAY + "] ";
        msg += GRAY + "[" + AQUA + chat.town + GRAY + "] " + WHITE + chat.name + ": " + chat.message;

//        log.info(ChatColor.stripColor(msg));

        for (Player p : server.getOnlinePlayers()) {
            PvPPlayer pAccount = getAccount(p.getName());
            if (pAccount.alliance != null && pAccount.alliance.equals(chat.alliance)) {
                p.sendMessage(msg);
            }
        }
    }

    void sendTownChatToNetwork(String player, String message) {
        SimpleChat chat = new SimpleChat(player, message, true, false);
        bungee.sendMessage(new Gson().toJson(chat), "Chat");
    }

    void sendAllianceChatToNetwork(String player, String message) {
        SimpleChat chat = new SimpleChat(player, message, false, true);
        bungee.sendMessage(new Gson().toJson(chat), "AllianceChat");
    }

    public void registerDamage(String victim, String attacker, Talent cause) {
        lastDamageCause.put(victim, cause);
        lastDamager.put(victim, attacker);
    }

    public void clearDamageCause(String victim) {
        lastDamageCause.remove(victim);
        lastDamager.remove(victim);
    }

    public boolean vanished(String name) {
        try {
            return VanishNoPacket.isVanished(name);
        } catch (VanishNotLoadedException e) {
            return false;
        }
    }
}
