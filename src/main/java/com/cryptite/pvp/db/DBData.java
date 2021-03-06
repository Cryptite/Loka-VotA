package com.cryptite.pvp.db;

import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.data.Town;
import com.cryptite.pvp.utils.UUIDFetcher;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.UUID;

public class DBData {
    private LokaVotA plugin;

    public DBObject data;
    public UUID uuid;

    public DBData(PvPPlayer p) {
        plugin = (LokaVotA) Bukkit.getPluginManager().getPlugin("LokaVotA");

        if (p.uuid == null) {
            try {
                uuid = UUIDFetcher.getUUIDOf(p.name);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (uuid == null) {
                System.out.println("[DB] " + p.name + " DOES NOT EXIST?");
                p.sendMessage(ChatColor.RED + "You are invalid for some reason.");
                return;
            }
        } else {
            uuid = p.uuid;
        }

        BasicDBObject query = new BasicDBObject("uuid", uuid.toString());

        DBCursor cursor = plugin.db.getCollection("players").find(query);
        if (cursor.hasNext()) data = cursor.next();
    }

    public DBData(Town t) {
        plugin = (LokaVotA) Bukkit.getPluginManager().getPlugin("LokaVotA");

        BasicDBObject query = new BasicDBObject("name", t.name);

        DBCursor cursor = plugin.db.getCollection("towns").find(query);
        if (cursor.hasNext()) data = cursor.next();
    }

    public int getInt(String key, Integer defaultValue) {
        return Integer.parseInt(get(key, defaultValue));
    }

    public Map getMap(String key) {
        return (Map) data.get(key);
    }

    public String get(String key, Object defaultValue) {
        if (data == null) {
            if (defaultValue == null) {
                //Because I want strings back, I can't return a string called null, so manually doing it.
                return null;
            } else {
                //Return the variable default value as string.
                return defaultValue.toString();
            }
        }

        Object value = data.get(key);
        if (value != null) {
            return value.toString();
        } else {
            if (defaultValue == null) {
                //Because I want strings back, I can't return a string called null, so manually doing it.
                return null;
            } else {
                //Return the variable default value as string.
                return defaultValue.toString();
            }
        }
    }

    public void set(String key, Object value) {
        plugin.scheduler.runTaskAsynchronously(plugin, () -> {
            BasicDBObject saveData = new BasicDBObject(key, value);

            BasicDBObject query = new BasicDBObject().append("uuid", uuid.toString());
            if (uuid == null) return;

            DBCollection coll = plugin.db.getCollection("players");
            BasicDBObject push;
            if (coll.find(query).hasNext()) {
                push = new BasicDBObject().append("$set", saveData);
                coll.update(query, push);
            } else {
                coll.insert(saveData);
            }
        });
    }

    public void update(BasicDBObject data) {
        plugin.scheduler.runTaskAsynchronously(plugin, () -> {
            DBCollection coll = plugin.db.getCollection("players");
            coll.update(new BasicDBObject("uuid", uuid), new BasicDBObject().append("$set", data));
        });
    }

    public void increment(String key, int amount) {
        plugin.scheduler.runTaskAsynchronously(plugin, () -> {
            if (uuid == null) return;

            //Select collection
            DBCollection coll = plugin.db.getCollection("players");

            //Set to increment object key
            BasicDBObject update = new BasicDBObject().append("$inc", new BasicDBObject(key, amount));

            //Update with query being uuid
            coll.update(new BasicDBObject().append("uuid", uuid.toString()), update);
        });
    }
}
