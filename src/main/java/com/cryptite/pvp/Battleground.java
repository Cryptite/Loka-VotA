package com.cryptite.pvp;

import com.cryptite.pvp.bungee.BattlegroundStatus;
import com.cryptite.pvp.utils.Countdown;
import com.cryptite.pvp.utils.Prowess;
import me.confuser.barapi.BarAPI;
import net.minecraft.util.com.google.gson.Gson;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.kitteh.vanish.staticaccess.VanishNoPacket;
import org.kitteh.vanish.staticaccess.VanishNotLoadedException;
import org.mcsg.double0negative.tabapi.TabAPI;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.cryptite.pvp.utils.LocationUtils.inWGRegion;
import static com.cryptite.pvp.utils.TimeUtil.secondsUntil;
import static org.bukkit.ChatColor.*;

public class Battleground {
    private final Logger log = Logger.getLogger("Artifact-Battleground");

    private final LokaVotA plugin;
    protected String name = null;
    public final Map<String, PvPPlayer> blueTeam = new HashMap<>();
    public final Map<String, PvPPlayer> redTeam = new HashMap<>();
    protected final Map<String, PvPPlayer> spectators = new HashMap<>();
    protected final Map<String, PvPPlayer> players = new HashMap<>();

    //Team numerics
    protected int blueTeamReadyCount = 0;
    protected int redTeamReadyCount = 0;
    public int blueScore = 0;
    public int redScore = 0;
    protected int redTeamMMR = 1500;
    protected int blueTeamMMR = 1500;

    //Tasks
    protected int matchExpireTimer;
    protected int matchWarn;
    protected int matchStart;
    protected int imbalanceCheck;
    protected BukkitTask imbalanceExpire;
    protected int startExpire;
    protected int doorsTask;

    //Match state booleans
    protected Boolean imbalanced = false;
    protected Boolean active = false;
    protected Boolean matchBegun = false;
    public Boolean matchStarted = false;
    private Boolean doorsOpen = false;

    //Location stuff
    public Location blueSpawn;
    public Location redSpawn;
    protected Location spectatorPoint;
    protected final List<Block> doorBlocks = new ArrayList<>();
    public World world;

    //Scoreboard stuff
    private final ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
    protected final Scoreboard scoreboard = scoreboardManager.getNewScoreboard();
    protected TeamManager teamManager;
    protected Score blueScoreBoard;
    protected Score redScoreBoard;
    protected Objective scoresObjective;
    protected Objective playerScoresObjective;
    protected final Map<String, Integer> playerScores = new HashMap<>();

    //Timers
    protected static final int InviteTimeout = 60;
    protected static final int MatchTooLongWarning = 60 * 19;
    protected static final int MatchExpiration = 60 * 20;
    protected static final int WinnerTeleport = 10;
    protected static final int ImbalanceCheckPeriod = 15;
    protected static final int ImbalanceMatchEndTimer = 120;
    protected static final int MatchStart = 60;
    protected static final int minPlayersPerTeam = 2;
    protected static final int DoorsOpenLength = 15;
    protected long timeDoorsClosed = 0;

    //Coutndown
    protected Countdown countdown;

    //Length
    protected long matchStartTime = 0;

    protected Battleground(LokaVotA plugin, World world) {
        this.plugin = plugin;
        this.world = world;
    }

    protected void updateTalentSigns() {
        for (Block b : plugin.talentSigns) {
            if (b != null && b.getType() == Material.WALL_SIGN) {
                Sign sign = (Sign) b.getState();
                if (matchStarted) {
                    sign.setLine(0, ChatColor.GOLD + "Talents");
                    sign.setLine(1, ChatColor.BLUE + "cannot be");
                    sign.setLine(2, ChatColor.BLUE + "changed now");
                    sign.setLine(3, ChatColor.GOLD + "");
                } else {
                    sign.setLine(0, ChatColor.RED + "Right-Click");
                    sign.setLine(1, ChatColor.BLUE + "to view or");
                    sign.setLine(2, ChatColor.BLUE + "change your");
                    sign.setLine(3, ChatColor.GOLD + "Talents");
                }
                sign.update();
            }
        }
    }

    public void sendMatchUpdate(Boolean matchActive) {
        BattlegroundStatus status = new BattlegroundStatus(name);
        status.active = matchActive;
        if (matchActive) {
            status.redSize = redTeam.size();
            status.blueSize = blueTeam.size();
        } else {
            status.redSize = 0;
            status.blueSize = 0;
        }
        plugin.bungee.sendMessage(new Gson().toJson(status), "BGUpdate");
    }

    public List<PvPPlayer> getAllPlayers() {
        List<PvPPlayer> players = new ArrayList<>();
        players.addAll(blueTeam.values());
        players.addAll(redTeam.values());
        players.addAll(spectators.values());
        return players;
    }

    public void messageAllPlayers(String message, Boolean warning) {
        String sound;
        if (warning != null && warning) {
            sound = "PvPWarn";
        } else {
            sound = "PvPNotify";
        }
        for (PvPPlayer player : getAllPlayers()) {
            player.sendMessage(message);
            if (warning != null && player.getPlayer() != null) {
                player.getPlayer().playSound(player.getPlayer().getLocation(), sound, 1, 1);
            }
        }
        log.info("[" + name + "] Message: " + stripColor(message));
    }

    protected void playSoundAllPlayers(String sound) {
        for (PvPPlayer player : getAllPlayers()) {
            if (player.getPlayer() == null) continue;
            player.getPlayer().playSound(player.getPlayer().getLocation(), sound, 1, 1);
        }
    }

    public void removePlayer(PvPPlayer p) {
        messageAllPlayers(getTeamColor(p) + p.name + GRAY + " has left the game.", false);

        if (redTeam.containsKey(p.name)) {
            redTeam.remove(p.name);
            teamManager.removePlayer(p.name, "red");
            if (redTeam.size() > 0) redTeamMMR = getTeamMMR("red");
        } else if (blueTeam.containsKey(p.name)) {
            blueTeam.remove(p.name);
            teamManager.removePlayer(p.name, "blue");
            if (blueTeam.size() > 0) redTeamMMR = getTeamMMR("blue");
        }
        updateTeamDisplay();
        updateTab();
        log.info("[" + p.bg + "] " + p.name + " was removed from the match.");
    }

    protected void updateTeamDisplay() {
        scoresObjective.setDisplayName(GRAY + name + ": " +
                RED + teamManager.getTeam("red").getPlayers().size() +
                GRAY + "v" +
                AQUA + teamManager.getTeam("blue").getPlayers().size());
    }

    protected ChatColor getTeamColor(PvPPlayer p) {
        if (plugin.getBG(p.bg).redTeam.containsKey(p.name)) {
            return RED;
        } else {
            return AQUA;
        }
    }

    public void joinSpectator(PvPPlayer p) {
        Player player = p.getPlayer();
        log.info("[" + name + "] " + player.getName() + " now spectating.");

        if (spectatorPoint != null) {
            player.teleport(spectatorPoint);
        }

        player.setNoDamageTicks(50000);
        p.cleanPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 4500, 1));

        try {
            VanishNoPacket.toggleVanishSilent(player);
        } catch (VanishNotLoadedException e) {
            e.printStackTrace();
        }

        player.setAllowFlight(true);
        player.setScoreboard(teamManager.board);
        updateTab();
    }

    public void processNewPlayers(List<String> players) {
        for (String player : players) {
            PvPPlayer p = plugin.getAccount(player);
            p.bg = name;
            setTeam(p);
        }
        sendMatchUpdate(true);
    }

//    public void registerPlayerHealth(PvPPlayer p) {
//        //Register healthbar
//        p.healthBar = teamManager.board.registerNewObjective("hb-" + p.name, "dummy");
//        p.healthBar.setDisplaySlot(DisplaySlot.BELOW_NAME);
//        p.healthBar.setDisplayName(ChatColor.RED + p.name.substring(0, 1) + " " + "\u2764");
//        p.healthBar.getScore(p.name).setScore(10);
//    }

    public void processSpectators(List<String> players) {
        for (String player : players) {
            PvPPlayer p = plugin.getAccount(player);
            p.bg = name;
            p.spectator = true;
            spectators.put(p.name, p);
            log.info("Added " + p.name + " to spectators.");
        }
    }

    protected void setCountdownBar() {
        for (PvPPlayer player : getAllPlayers()) {
            if (player.getPlayer() == null) continue;
            BarAPI.setMessage(player.getPlayer(), "Match Start", MatchStart - 3);
        }
    }

    public void messageBlueTeam(String message) {
        for (PvPPlayer player : blueTeam.values()) {
            player.sendMessage(message);
        }
        log.info("[" + name + "] Red team: " + stripColor(message));
    }

    public void messageRedTeam(String message) {
        for (PvPPlayer player : redTeam.values()) {
            player.sendMessage(message);
        }
        log.info("[" + name + "] Blue team: " + stripColor(message));
    }

    public void teamGoalAlert(String team, String message, Boolean capture) {
        String good;
        String bad;

        //Capture is the final CAPTURE noise, wheras not capture == false is just TAKEN
        if (capture) {
            good = "PvPPointCaptured";
            bad = "PvPEnemyPointCaptured";
        } else {
            good = "PvPPointTaken";
            bad = "PvPPointLost";
        }

        for (PvPPlayer p : blueTeam.values()) {
            if (p.getPlayer() == null) continue;

            p.sendMessage(message);
            if (team.equals("blue")) {
                p.getPlayer().playSound(p.getLocation(), good, 1, 1);
            } else {
                p.getPlayer().playSound(p.getLocation(), bad, 1, 1);
            }
        }
        for (PvPPlayer p : redTeam.values()) {
            if (p.getPlayer() == null) continue;

            p.sendMessage(message);
            if (team.equals("red")) {
                p.getPlayer().playSound(p.getLocation(), good, 1, 1);
            } else {
                p.getPlayer().playSound(p.getLocation(), bad, 1, 1);
            }
        }
    }

    protected void teamWinSound(String team) {
        for (PvPPlayer p : blueTeam.values()) {
            if (p.getPlayer() == null) continue;

            if (team.equals("blue")) {
                p.getPlayer().playSound(p.getLocation(), "PvPVictory", 1, 1);
            } else {
                p.getPlayer().playSound(p.getLocation(), "PvPLoss", 1, 1);
            }
        }
        for (PvPPlayer p : redTeam.values()) {
            if (p.getPlayer() == null) continue;

            if (team.equals("red")) {
                p.getPlayer().playSound(p.getLocation(), "PvPVictory", 1, 1);
            } else {
                p.getPlayer().playSound(p.getLocation(), "PvPLoss", 1, 1);
            }
        }
    }

    protected void setDoorsOpen(final Boolean open) {
        plugin.scheduler.scheduleSyncDelayedTask(plugin,
                () -> {
                    if (open) {
                        for (Block b : doorBlocks) {
                            b.setType(Material.AIR);
                        }
                        doorsOpen = true;
                    } else {
                        for (Block b : doorBlocks) {
                            b.setType(Material.REDSTONE_TORCH_ON);
                        }
                        doorsOpen = false;
                        timeDoorsClosed = System.currentTimeMillis();
                    }
                }, 20
        );
    }

    protected void toggleDoors() {
        if (doorsOpen) {
            //Close doors
            setDoorsOpen(false);

            //Tell all players in spawn about the new doors closed
            for (PvPPlayer p : getAllPlayers()) {
                if (p.spectator) continue;

                if (inWGRegion(p.getPlayer(), "redspawn") || inWGRegion(p.getPlayer(), "bluespawn")) {
                    new Countdown("Doors Open", DoorsOpenLength, p.getPlayer());
                }
            }
        } else setDoorsOpen(true);
    }

    public void showDoorsTimer(PvPPlayer p) {
        if (p.getPlayer() == null || doorsOpen) return;

        new Countdown("Doors Open", secondsUntil(timeDoorsClosed, DoorsOpenLength) - 1, p.getPlayer());
    }

    public void setTeam(PvPPlayer p) {
        if (redTeam.size() < blueTeam.size()) {
            log.info("Adding " + p.name + " to red because of size");
            redTeam.put(p.name, p);
            redTeamMMR = getTeamMMR("red");
        } else if (blueTeam.size() < redTeam.size()) {
            log.info("Adding " + p.name + " to blue because of size");
            blueTeam.put(p.name, p);
            blueTeamMMR = getTeamMMR("blue");
        } else if (blueTeamMMR < redTeamMMR) {
            log.info("Adding " + p.name + " to blue because of mmr");
            blueTeam.put(p.name, p);
            blueTeamMMR = getTeamMMR("blue");
        } else {
            log.info("Adding " + p.name + " to red  because of mmr");
            redTeam.put(p.name, p);
            redTeamMMR = getTeamMMR("red");
        }
        log.info("Blue rating: " + blueTeamMMR + ", Red rating: " + redTeamMMR);
    }

    protected void setWin(PvPPlayer p) {
        p.valleyWins++;
        plugin.bungee.sendMessage(p.name + ".vota.winvota", "Achievement");

        p.prowess = Prowess.BG_WIN;
    }

    Integer getTeamMMR(String team) {
        int total = 0;
        int average;
        if (team.equalsIgnoreCase("red")) {
            for (PvPPlayer p : redTeam.values()) {
                total += p.bgRating;
            }
            average = total / redTeam.size();
        } else {
            for (PvPPlayer p : blueTeam.values()) {
                total += p.bgRating;
            }
            average = total / blueTeam.size();
        }
        return average;

    }

    protected void setLoss(PvPPlayer p) {
        p.valleyLosses++;
        p.prowess = Prowess.BG_LOSS;
    }

    protected int getTeam(PvPPlayer p) {
        if (redTeam.containsKey(p.name)) {
            return 1;
        } else if (blueTeam.containsKey(p.name)) {
            return 2;
        }
        return 0;
    }

    public void updatePlayerScore(String player, int amount) {
        if (playerScores.containsKey(player)) {
            playerScores.put(player, playerScores.get(player) + amount);
        } else {
            playerScores.put(player, amount);
        }

        updateTab();
    }

    int getPlayerScore(String player) {
        if (playerScores.containsKey(player)) {
            return playerScores.get(player);
        } else {
            return 0;
        }
    }

    protected void updateTab() {
        for (PvPPlayer p : getAllPlayers()) {
            Player player = p.getPlayer();
            if (player == null) continue;
//            TabAPI.clearTab(player);
            TabAPI.setPriority(plugin, player, 2);

            //BG Header, mid column
            TabAPI.setTabString(plugin, player, 0, 1, GOLD + name, 30000);
            TabAPI.setTabString(plugin, player, 1, 1, "--------------", 30000);
            TabAPI.setTabString(plugin, player, 2, 1, "Length: " + GRAY + getMatchLength(), 30000);
            TabAPI.setTabString(plugin, player, 3, 1, "Spectators: " + GRAY + spectators.size(), 30000);

            int row = 2;
            //Blue team score
            TabAPI.setTabString(plugin, player, 0, 0, DARK_AQUA + "Blue Team", 30000);
            TabAPI.setTabString(plugin, player, 1, 0, DARK_AQUA + "--------------", 0);

            List<String> blueTeamSorted = new ArrayList<>(blueTeam.keySet());
            List<String> redTeamSorted = new ArrayList<>(redTeam.keySet());

            //Sort blue team
            Collections.sort(blueTeamSorted, (p1, p2) -> Integer.valueOf(getPlayerScore(p1)).compareTo(getPlayerScore(p2)));

            //Sort red team
            Collections.sort(redTeamSorted, (p1, p2) -> Integer.valueOf(getPlayerScore(p1)).compareTo(getPlayerScore(p2)));

            //Highest scorers first
            Collections.reverse(blueTeamSorted);
            Collections.reverse(redTeamSorted);

            for (String bluePlayer : blueTeamSorted) {
                TabAPI.setTabString(plugin, player, row, 0, YELLOW + "" + getPlayerScore(bluePlayer) + " " + DARK_AQUA + bluePlayer, 30000);
                row++;
            }

            row = 2;
            //Red team score
            TabAPI.setTabString(plugin, player, 0, 2, RED + "Red Team", 30000);
            TabAPI.setTabString(plugin, player, 1, 2, RED + "--------------", 30000);
            for (String redPlayer : redTeamSorted) {
                TabAPI.setTabString(plugin, player, row, 2, YELLOW + "" + getPlayerScore(redPlayer) + " " + RED + redPlayer, 30000);
                row++;
            }

            TabAPI.updatePlayer(player);
        }
    }

    private String getMatchLength() {
        if (matchStartTime == 0) return "0m";

        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - matchStartTime);
        return "" + minutes + "m";
    }

    protected void restoreAllPlayers() {
        for (PvPPlayer player : getAllPlayers()) {
            plugin.playersToReturn.add(player.name);
            player.restorePlayer();

            if (player.spectator) continue;

            updatePlayerScore(player.name, 0);
        }
    }

    protected void eggPlayers() {
        for (PvPPlayer player : getAllPlayers()) {
            player.eggPlayer();
        }
    }

    protected void unload() {
        //Remove all extra entities in the world for cleanup.
        int entitiesRemoved = 0;
        for (Entity current : world.getEntities()) {
            //Remove everything but players.
            if (!(current instanceof Player)) {
                current.remove();
                entitiesRemoved++;
            }
        }

        int chunksUnloaded = 0;
        for (Chunk c : world.getLoadedChunks()) {
            c.unload();
            chunksUnloaded++;
        }

        log.info("[VotA] Removed " + entitiesRemoved + " entities, " + chunksUnloaded + " chunks.");

        //Let's see if this does anything cool.
        plugin.vota = null;
    }
}
