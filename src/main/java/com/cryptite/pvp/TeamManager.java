package com.cryptite.pvp;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class TeamManager {
    private final Logger log = Logger.getLogger("Artifact-ScoreboardManager");

    private final LokaVotA plugin;
    public final Scoreboard board;
    private final Map<String, Team> teams = new HashMap<>();

    public TeamManager(LokaVotA plugin, Scoreboard board) {
        this.plugin = plugin;
        this.board = board;
    }

//    public void updateScoreboards() {
//        for (Player p : plugin.getServer().getOnlinePlayers()) {
//            if (!plugin.isQueued(p.getName())
//                    && !plugin.is2v2Queued(p.getName())
//                    && !plugin.isBGQueued(p.getName())
//                    && plugin.getArenaParty(p.getName()) == null
//                    && plugin.getAccount(p.getName()).bg == null
//                    && plugin.getAccount(p.getName()).arena == null) {
//                p.setScoreboard(this.board);
//            }
//        }
//    }

    public Team registerTeam(String name) {
        Team team;
        team = board.registerNewTeam(name);
        team.setDisplayName(name);
        teams.put(name, team);
        return team;
    }

    public Team getTeam(String name) {
        Team team;
        if (!teams.containsKey(name)) {
            team = registerTeam(name);
            teams.put(name, team);
        } else {
            team = teams.get(name);
        }
        return team;
    }

    public void addPlayer(String player, String teamName) {
        Team team = getTeam(teamName);
        if (team != null) {
            team.addPlayer(plugin.getServer().getPlayerExact(player));
        } else {
            log.info("NO TEAM: " + teamName);
        }
    }

    public void setPlayerTitle(String player, String teamName, ChatColor color) {
        if (teamName.contains("Stone") && color != null) {
            teamName = "Gladiator";
        }
        Team team = getTeam(teamName);
        if (team != null) {
            addPlayer(player, team.getName());
            if (color != null) {
                team.setPrefix(color + teamName + " ");
            } else {
                team.setPrefix(teamName + " ");
            }
        }
    }

    public void removePlayer(String player, String teamName) {
        Team team = getTeam(teamName);
        OfflinePlayer p = plugin.server.getPlayerExact(player);
        if (team != null && p != null) {
            team.removePlayer(p);
        } else {
            log.info("NO TEAM: " + teamName);
        }
    }

    public void resetTeam(String teamName) {
        Team team = getTeam(teamName);
        Set<OfflinePlayer> removalSet = team.getPlayers();
        for (OfflinePlayer p : removalSet) {
            team.removePlayer(p);
        }
    }
}
