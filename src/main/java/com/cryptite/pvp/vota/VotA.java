package com.cryptite.pvp.vota;

import com.cryptite.pvp.Battleground;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.TeamManager;
import com.cryptite.pvp.bungee.BattlegroundResults;
import com.cryptite.pvp.utils.Countdown;
import net.minecraft.util.com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Score;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.cryptite.pvp.utils.Rating.getRatingChange;
import static org.bukkit.ChatColor.*;

public class VotA extends Battleground {
    private final Logger log = Logger.getLogger("Artifact-Arena");

    private final LokaVotA plugin;
    private int artifactWatcher;
    private int scoreTask;
    private Boolean tripleCapNotification = false;
    private Boolean gameAlmostOverNotification = false;

    private final List<VotAArtifact> artifacts = new ArrayList<>();

    //Achievement progress
    private final Map<String, List<String>> artifactCaps = new HashMap<>();

    public VotA(LokaVotA plugin, World world) {
        super(plugin, world);
        this.plugin = plugin;
        this.world = world;
        name = "VotA";

        blueSpawn = new Location(world, -72, 82, 71, -90, 0);
        redSpawn = new Location(world, 37, 82, -65, -90, 0);

        plugin.talentSigns.add(world.getBlockAt(-62, 83, 72));
        plugin.talentSigns.add(world.getBlockAt(43, 83, -66));

        setupScoreboards();

        //Hidden Artifact
        VotAArtifact hiddenControl = new VotAArtifact(this, "hidden");
        hiddenControl.region = "valleya1";
        hiddenControl.world = world;
        hiddenControl.woolMinX = -52;
        hiddenControl.woolMaxX = -50;
        hiddenControl.woolMinZ = -82;
        hiddenControl.woolMaxZ = -80;
        hiddenControl.woolY = 70;
        //Obsidian Blocks
        hiddenControl.artifactBlocks.add(world.getBlockAt(-49, 75, -82));
        hiddenControl.artifactBlocks.add(world.getBlockAt(-49, 75, -81));
        hiddenControl.artifactBlocks.add(world.getBlockAt(-50, 75, -79));
        hiddenControl.artifactBlocks.add(world.getBlockAt(-51, 75, -79));
        hiddenControl.artifactBlocks.add(world.getBlockAt(-53, 75, -80));
        hiddenControl.artifactBlocks.add(world.getBlockAt(-53, 75, -81));
        hiddenControl.artifactBlocks.add(world.getBlockAt(-52, 75, -83));
        hiddenControl.artifactBlocks.add(world.getBlockAt(-51, 75, -83));
        hiddenControl.fireworkSource = new Location(world, -51, 76, -81);

        //Middle Artifact
        VotAArtifact middleControl = new VotAArtifact(this, "middle");
        middleControl.region = "valleya2";
        middleControl.world = world;
        middleControl.woolMinX = -7;
        middleControl.woolMaxX = -5;
        middleControl.woolMinZ = 3;
        middleControl.woolMaxZ = 5;
        middleControl.woolY = 69;
        middleControl.artifactBlocks.add(world.getBlockAt(-4, 74, 3));
        middleControl.artifactBlocks.add(world.getBlockAt(-4, 74, 4));
        middleControl.artifactBlocks.add(world.getBlockAt(-5, 74, 6));
        middleControl.artifactBlocks.add(world.getBlockAt(-6, 74, 6));
        middleControl.artifactBlocks.add(world.getBlockAt(-8, 74, 5));
        middleControl.artifactBlocks.add(world.getBlockAt(-8, 74, 4));
        middleControl.artifactBlocks.add(world.getBlockAt(-7, 74, 2));
        middleControl.artifactBlocks.add(world.getBlockAt(-6, 74, 2));
        middleControl.fireworkSource = new Location(world, -6, 76, 4);

        spectatorPoint = middleControl.fireworkSource.clone().add(0, 5, 0);

        //Lower Artifact
        VotAArtifact lowerControl = new VotAArtifact(this, "lower");
        lowerControl.region = "valleya3";
        lowerControl.world = world;
        lowerControl.woolMinX = 37;
        lowerControl.woolMaxX = 39;
        lowerControl.woolMinZ = 73;
        lowerControl.woolMaxZ = 75;
        lowerControl.woolY = 49;
        lowerControl.artifactBlocks.add(world.getBlockAt(40, 54, 73));
        lowerControl.artifactBlocks.add(world.getBlockAt(40, 54, 74));
        lowerControl.artifactBlocks.add(world.getBlockAt(39, 54, 76));
        lowerControl.artifactBlocks.add(world.getBlockAt(38, 54, 76));
        lowerControl.artifactBlocks.add(world.getBlockAt(36, 54, 75));
        lowerControl.artifactBlocks.add(world.getBlockAt(36, 54, 74));
        lowerControl.artifactBlocks.add(world.getBlockAt(37, 54, 72));
        lowerControl.artifactBlocks.add(world.getBlockAt(38, 54, 72));
        lowerControl.fireworkSource = new Location(world, -38, 55, 74);

        artifacts.add(hiddenControl);
        artifacts.add(middleControl);
        artifacts.add(lowerControl);

        doorBlocks.add(world.getBlockAt(-44, 70, 62));
        doorBlocks.add(world.getBlockAt(-44, 71, 62));
        doorBlocks.add(world.getBlockAt(-51, 70, 62));
        doorBlocks.add(world.getBlockAt(-51, 71, 62));
        doorBlocks.add(world.getBlockAt(-68, 72, 51));
        doorBlocks.add(world.getBlockAt(-68, 73, 51));
        doorBlocks.add(world.getBlockAt(-68, 72, 44));
        doorBlocks.add(world.getBlockAt(-68, 73, 44));

        doorBlocks.add(world.getBlockAt(32, 74, -47));
        doorBlocks.add(world.getBlockAt(32, 75, -47));
        doorBlocks.add(world.getBlockAt(39, 74, -47));
        doorBlocks.add(world.getBlockAt(39, 75, -47));
        doorBlocks.add(world.getBlockAt(52, 74, -48));
        doorBlocks.add(world.getBlockAt(52, 75, -48));
        doorBlocks.add(world.getBlockAt(59, 74, -48));
        doorBlocks.add(world.getBlockAt(59, 75, -48));

        plugin.inventoryPreferenceSigns.add(world.getBlockAt(-58, 83, 63));
        plugin.inventoryPreferenceSigns.add(world.getBlockAt(-59, 83, 62));
        plugin.inventoryPreferenceSigns.add(world.getBlockAt(43, 81, -51));
        plugin.inventoryPreferenceSigns.add(world.getBlockAt(44, 81, -52));
        plugin.inventoryPreferenceSigns.add(world.getBlockAt(45, 81, -51));
    }

    private void setupScoreboards() {
        teamManager = new TeamManager(plugin, scoreboard);
        scoresObjective = teamManager.board.registerNewObjective("scores", "dummy");
//        playerScoresObjective = teamManager.board.registerNewObjective("pscores", "dummy");

//        playerScoresObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
//        playerScoresObjective.setDisplayName(GRAY + "Player Scores");

        updateArtifactControlObjectives();
    }

    public void updateArtifactControlObjectives() {
        scoresObjective.unregister();
        scoresObjective = teamManager.board.registerNewObjective("scores", "dummy");

        for (VotAArtifact a : artifacts) {
            Score captured = scoresObjective.getScore(Bukkit.getOfflinePlayer(a.teamColor + a.name + DARK_GRAY));
            captured.setScore(0);
        }

        scoresObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        scoresObjective.setDisplayName(GRAY + "Score: " + RED + redScore + GRAY + "-" + AQUA + blueScore);

        for (PvPPlayer p : players.values()) {
            if (p.getPlayer() == null) continue;
            p.getPlayer().setScoreboard(teamManager.board);
        }
    }

    public void updateScore() {
        scoresObjective.setDisplayName(GRAY + "Score: " + RED + redScore + GRAY + "-" + AQUA + blueScore);
    }

    public void playerReady(PvPPlayer p) {
        log.info("[VotA] " + p.name + " joined Valley of the Artifacts");
        p.ready = true;
        p.bg = "VotA";
        if (getTeam(p) == 1) {
            p.team = "red";
            p.playerColor = Color.RED;
            p.preparePlayer(true);
            teamManager.addPlayer(p.name, "red");
            plugin.getServer().getPlayerExact(p.name).teleport(redSpawn);
            redTeamReadyCount++;
        } else if (getTeam(p) == 2) {
            p.team = "blue";
            p.playerColor = Color.BLUE;
            p.preparePlayer(true);
            teamManager.addPlayer(p.name, "blue");
            plugin.getServer().getPlayerExact(p.name).teleport(blueSpawn);
            blueTeamReadyCount++;
        }

        plugin.help.sendHelp(p.name, "vota");
        p.getPlayer().setScoreboard(teamManager.board);
//        registerPlayerHealth(p);
        updateTalentSigns();
        scoresObjective.setDisplayName("" + RED + redScore + GRAY + "-" + AQUA + blueScore);

        if (!matchBegun) {
            setDoorsOpen(false);
        } else {
            p.sendMessage(AQUA + "The match has already begun. Good luck!");
        }

        if (blueTeamReadyCount >= minPlayersPerTeam && redTeamReadyCount >= minPlayersPerTeam && !matchBegun) {
            matchBegun = true;
            plugin.scheduler.cancelTask(startExpire);

            messageAllPlayers(AQUA +
                    "The match will begin in 1 minute.", null);

            plugin.scheduler.scheduleSyncDelayedTask(plugin,
                    () -> messageAllPlayers(AQUA +
                            "The match will begin in " + MatchStart / 2 + " seconds", false), 20 * (MatchStart / 2)
            );

            matchStart = plugin.scheduler.scheduleSyncDelayedTask(plugin,
                    () -> {
                        setDoorsOpen(true);
                        beginMatch();
                    }, 20 * MatchStart
            );

            countdown = new Countdown(name, "Match Start", MatchStart);
        }

        updateTab();
    }

    public void matchReady() {
        startExpire = plugin.scheduler.scheduleSyncDelayedTask(plugin,
                this::expireMatch, 20 * InviteTimeout
        );

        updateTalentSigns();
    }

    public void beginMatch() {
        matchStarted = true;
        teamManager.getTeam("blue").setPrefix(AQUA + "");
        teamManager.getTeam("red").setPrefix(RED + "");

        log.info("[VotA] Match Begun!");
        messageAllPlayers(AQUA + "Begin!", null);
        playSoundAllPlayers("PvPStart");

        for (VotAArtifact a : artifacts) {
            a.setNeutral();
        }

        for (PvPPlayer p : players.values()) {
            p.saveInventoryOrder();
        }

        updateTalentSigns();
        setDoorsOpen(true);
        resetArtifacts();
        matchStartTime = System.currentTimeMillis();

        artifactWatcher = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                () -> {
                    int blueCount = 0;
                    int redCount = 0;
                    for (VotAArtifact control : artifacts) {
                        checkArtifactControl(control);
                        if (control.controllingTeam == null) continue;
                        if (control.controllingTeam.equals("red")) redCount++;
                        else if (control.controllingTeam.equals("blue")) blueCount++;
                    }

                    if (!tripleCapNotification) {
                        if (redCount == 3) {
                            messageAllPlayers(AQUA + "The " + RED + "Red" +
                                    AQUA + " team has control of all three points!", null);
                            for (PvPPlayer p : redTeam.values()) {
                                plugin.bungee.sendMessage(p.name + ".vota.votatriplecap", "Achievement");
                            }
                            tripleCapNotification = true;
                        } else if (blueCount == 3) {
                            messageAllPlayers(AQUA + "The " + AQUA + "Blue" +
                                    AQUA + " team has control of all three points!", null);
                            for (PvPPlayer p : blueTeam.values()) {
                                plugin.bungee.sendMessage(p.name + ".vota.votatriplecap", "Achievement");
                            }
                            tripleCapNotification = true;
                        } else {
                            tripleCapNotification = false;
                        }
                    }
                }, 0, 20
        );
        //Score default is 200 ticks
        scoreTask = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                this::calculateScore, 0, 200
        );
        matchWarn = plugin.scheduler.scheduleSyncDelayedTask(plugin,
                () -> messageAllPlayers(RED + "Warning!" + AQUA + " This match will end in 1 minute!", true), 20 * MatchTooLongWarning
        );

        imbalanceCheck = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                this::checkTeamBalance, 200, 20 * ImbalanceCheckPeriod
        );

        matchExpireTimer = plugin.scheduler.scheduleSyncDelayedTask(plugin,
                this::expireMatch, 20 * MatchExpiration
        );

        doorsTask = plugin.scheduler.scheduleSyncRepeatingTask(plugin, this::toggleDoors, 20 * DoorsOpenLength, 20 * DoorsOpenLength);
    }

    private void resetArtifacts() {
        for (VotAArtifact a : artifacts) {
            a.setNeutral();
            a.progress = 0;
        }
    }

    void checkTeamBalance() {
        if (Math.abs(redTeam.size() - blueTeam.size()) > 1) {
            log.info("Try team balancing");
        } else if (redTeam.size() + blueTeam.size() == 0) {
            log.info("[VotA] Battleground is empty! Killing self");
            expireMatch();
        } else if (redTeam.size() + blueTeam.size() <= 3) {
            if (!imbalanced) {
                if (this.imbalanceExpire != null) this.imbalanceExpire.cancel();
                messageAllPlayers(AQUA + "There are not enough players to continue this match. It will end in two minutes if there are no other joins.", true);
                imbalanceExpire = plugin.scheduler.runTaskLater(plugin,
                        () -> {
                            messageAllPlayers(AQUA + "There were not enough players to continue this match. It has been ended.", true);
                            expireMatch();
                        }, 20 * ImbalanceMatchEndTimer
                );
                imbalanced = true;
            }
        } else {
            //log.info("Balance is fine");
            imbalanced = false;
            if (this.imbalanceExpire != null) this.imbalanceExpire.cancel();
        }

//        //If any players aren't online, remove them from the match, they may've bugged out somehow.
//        plugin.scheduler.runTaskLater(plugin, new Runnable() {
//            @Override
//            public void run() {
//                removeMissingPlayers();
//            }
//        }, 20*5);

        sendMatchUpdate(true);
    }

//    void removeMissingPlayers() {
//        for (PvPPlayer p : getAllPlayers()) {
//            if (p.getPlayer() == null || !p.getPlayer().isOnline()) {
//                log.info("Removing " + p.name + " for not being online...");
//                removePlayer(p);
//            }
//        }
//    }

    void checkArtifactAchievement(String name, String artifact) {
        if (artifactCaps.containsKey(name)) {
            if (!artifactCaps.get(name).contains(artifact)) {
                if (artifactCaps.get(name).size() == 2) {
                    //Successfully captured each of the artifacts!
                    plugin.bungee.sendMessage(name + ".vota.captureallartifacts", "Achievement");
                }
                artifactCaps.get(name).add(artifact);
            }
        } else {
            List<String> cap = new ArrayList<>();
            cap.add(artifact);
            artifactCaps.put(name, cap);
        }
    }

    private void checkArtifactControl(VotAArtifact artifact) {
        List<PvPPlayer> blueCappers = artifact.getCapturers(blueTeam.values());
        List<PvPPlayer> redCappers = artifact.getCapturers(redTeam.values());

        if (blueCappers.size() > 0 && redCappers.size() == 0) {
            //Red team owns this point
            if (artifact.isContested("blue")) {
                //Blue team is taking the point from red
//                log.info("Blue is reducing " + artifact.name + " - " + artifact.progress);

                if (artifact.controlled) {
                    artifact.progress -= blueCappers.size();
                } else {
                    //Taking an artifact that is not fully controlled is faster
                    artifact.progress -= blueCappers.size() * 2;
                }

                if (artifact.progress <= 0) {
                    if (artifact.controlled) {
                        teamGoalAlert("blue", AQUA + "The " + RED + "Red" +
                                AQUA + " team has lost control of the " + artifact.name + " artifact", false);
                    }
                    artifact.setNeutral();
                }
            } else if (!artifact.controlled) {
                //The point is neutral

                if (artifact.controllingTeam != null && artifact.capturingTeam.equals("red")) {
                    artifact.progress = 0;
                }

//                log.info("Blue is taking " + artifact.name + " - " + artifact.progress);
                artifact.progress += blueCappers.size();
                artifact.capturingTeam = "blue";
                if (artifact.progress >= 30) {
                    teamGoalAlert("blue", AQUA + "The " + AQUA + "Blue" +
                            AQUA + " team has captured the " + artifact.name + " artifact", true);
                    capturedArtifact(blueCappers, artifact.name);
                    artifact.setControlled("blue");
                    return;
                }
            }
        } else if (redCappers.size() > 0 && blueCappers.size() == 0) {
            if (artifact.isContested("red")) {
//                log.info("Red is reducing " + artifact.name + " - " + artifact.progress);

                if (artifact.controlled) {
                    artifact.progress -= redCappers.size();
                } else {
                    //Taking an artifact that is not fully controlled is faster
                    artifact.progress -= redCappers.size() * 2;
                }

                if (artifact.progress <= 0) {
                    if (artifact.controlled) {
                        teamGoalAlert("red", AQUA + "The " + AQUA + "Blue" +
                                AQUA + " team has lost control of the " + artifact.name + " artifact", false);
                    }
                    artifact.setNeutral();
                }
            } else if (!artifact.controlled) {

                if (artifact.controllingTeam != null && artifact.capturingTeam.equals("blue")) {
                    //Set the point as taken
                    artifact.progress = 0;
                }

//                log.info("Red is taking " + artifact.name + " - " + artifact.progress);
                artifact.progress += redCappers.size();
                artifact.capturingTeam = "red";
                if (artifact.progress >= 30) {
                    teamGoalAlert("red", AQUA + "The " + RED + "Red" +
                            AQUA + " team has captured the " + artifact.name + " artifact", true);
                    capturedArtifact(redCappers, artifact.name);
                    artifact.setControlled("red");
                    return;
                }
            }
        } else {
            if (!artifact.controlled && artifact.progress > 0) {
                //Nobody is around so the artifact should revert back to 0 progress.
                artifact.progress--;
            } else if (!artifact.controlled && artifact.progress == 0) {
                //Artifact is completely neutral now
                artifact.capturingTeam = null;
            } else if (artifact.controlled && artifact.progress < 30) {
                //Nobody is around but a team owns it, so it should revert back to full cap.
                artifact.progress++;
            }

        }

        if ((blueCappers.size() > 0) || (redCappers.size() > 0)) {
            artifact.setVisualProgress();
        }
    }

    void capturedArtifact(List<PvPPlayer> cappers, String artifact) {
        for (PvPPlayer p : cappers) {
            updatePlayerScore(p.name, 3);
            p.valleyCaps++;
            p.save();

            checkArtifactAchievement(p.name, artifact);
            plugin.bungee.sendMessage(p.name + ".vota.captureartifact", "Achievement");
        }
    }

    private void calculateScore() {
        for (VotAArtifact control : artifacts) {
            if (control.controlled) {
                if (control.controllingTeam.equals("red")) {
                    redScore++;
                } else if (control.controllingTeam.equals("blue")) {
                    blueScore++;
                }
            }
        }

        scoresObjective.setDisplayName(GRAY + "Score: " + RED + redScore + GRAY + "-" + AQUA + blueScore);

        if ((redScore > 80 || blueScore > 80) && !gameAlmostOverNotification) {
            playSoundAllPlayers("PvPEndOfGame");
            if (redScore > 80) {
                messageAllPlayers(RED + "Red" + AQUA + " team is close to winning!", null);
            } else if (blueScore > 80) {
                messageAllPlayers(AQUA + "Blue" + AQUA + " team is close to winning!", null);
            }
            gameAlmostOverNotification = true;
        }

        if (redScore >= 100 && redScore > blueScore) {
            messageAllPlayers(RED + "Red " + AQUA + " team wins!", null);

            saveStats("red", (redScore >= 100 && blueScore == 0));
            expireMatch();
            teamWinSound("red");
            log.info("Red team [" + redTeamMMR + "] beat Blue [" + blueTeamMMR + "]");
        } else if (blueScore >= 100 && blueScore > redScore) {
            messageAllPlayers(AQUA + "Blue " + AQUA + " team wins!", null);

            saveStats("blue", (blueScore >= 100 && redScore == 0));
            expireMatch();
            teamWinSound("blue");
            log.info("Blue team [" + blueTeamMMR + "] beat Red [" + redTeamMMR + "]");
        }

        log.info("[VotA] Blue: " + blueScore + " - Red: " + redScore);
    }

    private void saveStats(String team, Boolean shutout) {
        BattlegroundResults results = new BattlegroundResults(name, playerScores);

        if (team.equals("red")) {
            for (PvPPlayer p : redTeam.values()) {
                setWin(p);
                p.updateBGRating(getRatingChange(redTeamMMR, blueTeamMMR), playerScores.get(p.name));

                if (shutout) {
                    plugin.bungee.sendMessage(p.name + ".vota.votashutout", "Achievement");
                }
            }

            for (PvPPlayer p : blueTeam.values()) {
                setLoss(p);
                p.updateBGRating(getRatingChange(redTeamMMR, blueTeamMMR) * -1, playerScores.get(p.name));
            }

            results.winners = redTeam.keySet();
            results.losers = blueTeam.keySet();

        } else if (team.equals("blue")) {
            for (PvPPlayer p : blueTeam.values()) {
                setWin(p);
                p.updateBGRating(getRatingChange(blueTeamMMR, redTeamMMR), playerScores.get(p.name));

                if (shutout) {
                    plugin.bungee.sendMessage(p.name + ".vota.votashutout", "Achievement");
                }
            }

            for (PvPPlayer p : redTeam.values()) {
                setLoss(p);
                p.updateBGRating(getRatingChange(blueTeamMMR, redTeamMMR) * -1, playerScores.get(p.name));
            }

            results.winners = blueTeam.keySet();
            results.losers = redTeam.keySet();
        }

        for (PvPPlayer p : getAllPlayers()) {
            p.save();
            p.postPlayer();
            plugin.bungee.sendMessage(p.name + ".vota.playvota", "Achievement");
        }

        plugin.bungee.sendMessage(new Gson().toJson(results), "BGResults");
    }


    void expireMatch() {
        active = false;
        plugin.scheduler.cancelTask(this.imbalanceCheck);
        if (this.imbalanceExpire != null) this.imbalanceExpire.cancel();
        plugin.scheduler.cancelTask(this.startExpire);
        plugin.scheduler.cancelTask(this.matchExpireTimer);
        plugin.scheduler.cancelTask(this.matchWarn);
        plugin.scheduler.cancelTask(this.matchStart);
        plugin.scheduler.cancelTask(this.artifactWatcher);
        plugin.scheduler.cancelTask(this.scoreTask);
        plugin.scheduler.cancelTask(this.doorsTask);

        resetArtifacts();
        sendMatchUpdate(false);

        log.info("Expiring match: " + matchBegun);

        if (!matchBegun) {
            //Cancel match as a draw, warp players back to their original points.
            messageAllPlayers(AQUA + "Not enough players joined the battleground. You have been returned to Loka.", null);
            setDoorsOpen(false);
            restoreAllPlayers();
            resetMatch();
        } else {
            eggPlayers();
            setDoorsOpen(false);
            messageAllPlayers(GRAY + "You will be returned to Loka in " + WinnerTeleport + " seconds.", null);
            log.info("[ARENA] Match ended successfully.");

            plugin.scheduler.scheduleSyncDelayedTask(plugin, this::restoreAllPlayers, 20 * WinnerTeleport
            );

            plugin.scheduler.runTaskLater(plugin, this::resetMatch, 20 * WinnerTeleport + 5
            );
        }

    }

    void resetMatch() {

        teamManager.resetTeam("blue");
        teamManager.resetTeam("red");

        teamManager.getTeam("blue").setPrefix("");
        teamManager.getTeam("red").setPrefix("");

        playerScores.clear();

        redTeam.clear();
        blueTeam.clear();
        players.clear();
        spectators.clear();
        redTeamReadyCount = 0;
        blueTeamReadyCount = 0;
        redScore = 0;
        blueScore = 0;
        redTeamMMR = 0;
        blueTeamMMR = 0;
        matchBegun = false;
        active = false;
        matchStarted = false;
        imbalanced = false;
        gameAlmostOverNotification = false;

        plugin.scheduler.runTaskLater(plugin, this::unload, 20 * 5);

        log.info("[Vota] Successfully Reset");
    }
}
