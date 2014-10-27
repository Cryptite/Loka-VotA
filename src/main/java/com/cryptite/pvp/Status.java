package com.cryptite.pvp;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.stream.Collectors;

public class Status {
    private LokaVotA plugin;
    private DB db;
    private BukkitTask updateID;

    public Status(LokaVotA plugin) {
        this.plugin = plugin;
        db = plugin.db;

        //Always update on init, otherwise could be stuck at a false number
        updatePlayers();
    }

    public void updatePlayers() {
        if (updateID != null) updateID.cancel();

        updateID = plugin.scheduler.runTaskLater(plugin, () -> {
            DBCollection coll = db.getCollection("servers");
            BasicDBObject query = new BasicDBObject("server", "vota");
            BasicDBObject data = new BasicDBObject("server", "vota")
                    .append("players",
                            plugin.server.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            if (coll.find(query).hasNext()) {
                coll.update(query, new BasicDBObject().append("$set", data));
            } else {
                coll.insert(data);
            }
        }, 20 * 3);
    }
}
