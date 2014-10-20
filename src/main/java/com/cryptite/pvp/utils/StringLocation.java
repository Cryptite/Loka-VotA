package com.cryptite.pvp.utils;

import com.sk89q.worldedit.Vector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

class StringLocation {
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final float yaw;
    private final float pitch;

    public StringLocation(Location l) {
        this.world = l.getWorld().getName();
        this.x = (int) l.getX();
        this.y = (int) l.getY();
        this.z = (int) l.getZ();
        this.yaw = l.getYaw();
        this.pitch = l.getPitch();
    }

    public StringLocation(String string) {
        String[] elems = string.split(",");
        world = elems[0];
        x = Integer.parseInt(elems[1]);
        y = Integer.parseInt(elems[2]);
        z = Integer.parseInt(elems[3]);
        yaw = elems.length > 4 ? Float.parseFloat(elems[4]) : 0f;
        pitch = elems.length > 5 ? Float.parseFloat(elems[5]) : 0f;
    }

    public Boolean hasMoved(Location l) {
        return x != l.getBlockX() || y != l.getBlockY() || z != l.getBlockZ();
    }

    Location getLocation() {
        return getBlock().getLocation();
    }

    Block getBlock() {
        return Bukkit.getWorld(world).getBlockAt(x, y, z);
    }

    Material getType() {
        return getBlock().getType();
    }

    void setType(Material m) {
        getBlock().setType(m);
    }

    Integer getBlockX() {
        return x;
    }

    Integer getBlockY() {
        return y;
    }

    Integer getBlockZ() {
        return z;
    }

    public Float getPitch() {
        return pitch;
    }

    public Float getYaw() {
        return yaw;
    }

    public double distance(Location location) {
        return getLocation().distance(location);
    }

    public org.bukkit.World getWorld() {
        return Bukkit.getWorld(world);
    }

    public Vector toVector() {
        return new Vector(getBlockX(), getBlockY(), getBlockZ());
    }
}
