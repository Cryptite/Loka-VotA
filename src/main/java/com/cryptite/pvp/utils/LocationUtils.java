package com.cryptite.pvp.utils;

import com.cryptite.pvp.LokaVotA;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class LocationUtils {
    public static Boolean inWGRegion(String region, String player) {
        Player p = Bukkit.getPlayerExact(player);
        return LocationUtils.inWGRegion(p, region);
    }

    public static Boolean inWGRegion(Player p, String region) {
        return p != null && getWGRegion(p.getLocation()).contains(region);
    }

    private static List<String> getWGRegion(Location loc) {
        ApplicableRegionSet set = WGBukkit.getRegionManager(loc.getWorld()).getApplicableRegions(loc);
        Iterator<ProtectedRegion> prs = set.iterator();
        List<String> regions = new ArrayList<>();
        while (prs.hasNext()) {
            ProtectedRegion pr = prs.next();
            regions.add(pr.getId());
        }
        return regions;
    }

    public static void playCustomSound(Player p, String sound) {
        playCustomSound(p, sound, 1f);
    }

    public static void playCustomSound(Player p, String sound, Float volume) {
        //Get all nearby entities within 15 blocks
        for (Entity near : p.getNearbyEntities(15, 15, 15)) {
            if (!(near instanceof Player)) continue;

            ((Player) near).playSound(p.getLocation(), sound, volume, 1);
        }

        //Also play for the player
        p.playSound(p.getLocation(), sound, 1, 1);
    }

    public static void playWorldCustomSound(Location l, String sound, int radius) {
        for (Entity e : l.getWorld().getEntities()) {
            if (!(e instanceof Player)) continue;
            Player player = (Player) e;

            double distance = player.getLocation().distance(l);
            if (distance <= radius) {
                player.playSound(l, sound, 1, 1);
            }
        }
    }

    public static Location parseCoord(LokaVotA plugin, String string) {
        if (string == null) return null;
        String[] elems = string.split(",");
        World world = plugin.server.getWorld(elems[0]);
        return new Location(world, Double.parseDouble(elems[1]),
                Double.parseDouble(elems[2]), Double.parseDouble(elems[3]),
                elems.length > 4 ? Float.parseFloat(elems[4]) : 0f,
                elems.length > 5 ? Float.parseFloat(elems[5]) : 0f);
    }

    public static String coordsToString(Location point) {
        return point.getWorld().getName() + "," +
                (int) point.getX() + "," +
                (int) point.getY() + "," +
                (int) point.getZ() + "," +
                (int) point.getYaw() + "," +
                (int) point.getPitch();
    }

    public static Location getRandomLocation(Location source, int radius, int minimumRadius, List<Material> excludedTypes) {
        Random r = new Random();
        Location randomLocation = null;
        Material currentBlockType = Material.AIR;
        int spawnTries = 0;

        while (excludedTypes.contains(currentBlockType)) {
            double a = r.nextDouble() * 2 * Math.PI;
            double dist = (r.nextDouble() * radius) + minimumRadius;
            randomLocation = source.clone().add(dist * Math.sin(a), 0, dist * Math.cos(a));
            randomLocation.setY(source.getWorld().getHighestBlockYAt((int) randomLocation.getX(), (int) randomLocation.getZ()));

            currentBlockType = randomLocation.getBlock().getType();
            spawnTries++;

            //To avoid possible infinite looping
            if (spawnTries > 10) break;
        }

        return randomLocation;
    }
}
