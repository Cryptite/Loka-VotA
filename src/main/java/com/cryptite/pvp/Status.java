package com.cryptite.pvp;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.bukkit.scheduler.BukkitTask;

public class Status {
    private LokaVotA plugin;
    private DB db;
    private BukkitTask updateID;

    public Status(LokaVotA plugin) {
        this.plugin = plugin;
        db = plugin.db;
    }

    public void updatePlayers() {
        if (updateID != null) updateID.cancel();

        updateID = plugin.scheduler.runTaskLater(plugin, () -> {
            DBCollection coll = db.getCollection("servers");
            BasicDBObject query = new BasicDBObject("server", "vota");
            BasicDBObject data = new BasicDBObject("server", "vota").append("players", plugin.server.getOnlinePlayers().size());
            if (coll.find(query).hasNext()) {
                coll.update(query, new BasicDBObject().append("$set", data));
            } else {
                coll.insert(data);
            }
        }, 20 * 3);
    }
}