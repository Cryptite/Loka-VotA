package com.cryptite.pvp.utils;

import com.mongodb.DB;

public class Statistics {
    private DB db;

    public Statistics(DB db) {
        this.db = db;
    }

    public void increment(String collection, String key, int value) {

    }

}
