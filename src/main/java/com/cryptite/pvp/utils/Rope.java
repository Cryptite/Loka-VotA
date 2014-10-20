package com.cryptite.pvp.utils;

import net.minecraft.server.v1_7_R4.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Credits to [USER=90856537]Desle[/USER] for the original resource. (Uses entities, not packets)
 *
 * @author Goblom
 * @author Jordan
 */
public class Rope {

    private static final List<Rope> ropes = new ArrayList<>();

    private Location endLocation;
    private EntityBat batLocation;
    private Entity holder, hook;
    private RopeResult result;

    public static enum RopeResult {
        MOB, ITEM, GROUND, AIR;
    }

    /**
     * Make a rope with a specified endpoint and holder.
     *
     * @param end    The endpoint of the rope.
     * @param holder The entity that should hold the rope.
     */
    public Rope(Location end, Entity holder) {
        this.endLocation = end;
        this.holder = holder;
        for (Rope r : new ArrayList<>(ropes)) {
            if (r.holder != null && r.holder.equals(holder)) {
                r.holder = null;
                r.despawn();
            }
        }
        ropes.add(this);

        spawn();
    }

    /**
     * Set the end location of the rope. Will cause issues if the end of the
     * rope is attached to a mobile creature.
     *
     * @param end The end location.
     */
    public void setEnd(Location end) {
        this.endLocation = end;
        spawn();
    }

    /**
     * Get the end location of this rope.
     *
     * @return The end location of the rope.
     */
    public Location getEnd() {
        return endLocation;
    }

    /**
     * Make the entities and connections necessary for this rope.
     */
    private void makeEnt() {
        WorldServer world = ((CraftWorld) endLocation.getWorld()).getHandle();

        if (batLocation == null) {
            this.batLocation = new EntityBat(world);
            batLocation.setInvisible(true);
        }

        this.batLocation.setLocation(endLocation.getX(), endLocation.getY(), endLocation.getZ(), 0, 0);

        batLocation.setLeashHolder(((CraftEntity) holder).getHandle(), true);
    }

    /**
     * Make this Rope appear in the world. Despawns if there is no rope holder.
     */
    public void spawn() {
        if (holder == null) {
            despawn();
            return;
        }
        makeEnt();
        PacketPlayOutSpawnEntityLiving bat_end = new PacketPlayOutSpawnEntityLiving(batLocation);
        PacketPlayOutAttachEntity attach = new PacketPlayOutAttachEntity(1, batLocation, ((CraftPlayer) holder).getHandle());

        for (Player player : Bukkit.getOnlinePlayers()) {
            CraftPlayer cP = (CraftPlayer) player;
            cP.getHandle().playerConnection.sendPacket(bat_end);
            cP.getHandle().playerConnection.sendPacket(attach);
        }
    }

    /**
     * Remove all the ropes/packets that are glued.
     *
     * @see despawn()
     */
    public static void removeAll() {
        for (Rope r : new ArrayList<>(ropes)) {
            r.despawn();
        }
    }

    /**
     * Remove this rope and clean up the packets.
     */
    public void despawn() {
        holder = null;

        PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(batLocation.getId());

        for (Player player : Bukkit.getOnlinePlayers()) {
            CraftPlayer cP = (CraftPlayer) player;
            cP.getHandle().playerConnection.sendPacket(destroy);
        }
        ropes.remove(this);
    }

    /**
     * Glue the end of the rope to an entity.
     *
     * @param toAttachTo The entity to glue to.
     */
    public void glueEndTo(Entity toAttachTo) {
        PacketPlayOutAttachEntity attach = new PacketPlayOutAttachEntity(0, batLocation, ((CraftEntity) toAttachTo).getHandle());
        for (Player player : Bukkit.getOnlinePlayers()) {
            CraftPlayer cP = (CraftPlayer) player;
            cP.getHandle().playerConnection.sendPacket(attach);
        }

        GlueUpdater u = new GlueUpdater(toAttachTo);
        u.task = Bukkit.getServer().getScheduler().runTaskTimer(Bukkit.getPluginManager().getPlugin("LokaPvP"), u, 0L, 1L);
    }

    private class GlueUpdater implements Runnable {
        BukkitTask task;
        Entity glued;

        public GlueUpdater(Entity glued) {
            this.glued = glued;
        }

        public void run() {
            if (holder == null || glued == null || glued.isDead() || (glued instanceof Arrow && ((Arrow) glued).isOnGround())) {
                if (holder == null && glued != null) {
                    glued.remove();
                }
                task.cancel();
                result = RopeResult.GROUND;
                if (glued != null) {
                    for (Entity e : glued.getNearbyEntities(1, 0.5, 1))
                        if (!e.equals(glued)) {
                            if (e instanceof Item)
                                result = RopeResult.ITEM;
                            else
                                result = RopeResult.MOB;

                            hook = e;
                            glueEndTo(e);
                            return;
                        }
                }

                return;
            }

            endLocation = glued.getLocation().subtract(0,
                    result == RopeResult.ITEM || (result == RopeResult.MOB && hook instanceof Arrow) ? 0 : 2, 0);
            if (glued instanceof Arrow) {
                setEnd(endLocation);
            }
        }
    }

    /**
     * Get a rope held by a certain entity.
     *
     * @param holder The entity that might be holding a rope.
     * @return The rope the holder is holding or null if there is no rope
     * associated with.
     */
    public static Rope getRope(Entity holder) {
        for (Rope r : ropes) {
            if (r.holder != null && r.holder.equals(holder)) {
                return r;
            }
        }
        return null;
    }

    /**
     * The result of the hook.
     *
     * @return The hook result.
     */
    public RopeResult getResult() {
        return result;
    }

    /**
     * Get the item on the hook end of the rope.
     *
     * @return The hook item.
     */
    public Entity getHook() {
        return hook;
    }

    /**
     * Whether or not the player should be able to pull in the line.
     *
     * @return True if the hook landed or hit an entity/item.
     */
    public boolean canPull() {
        return result != RopeResult.AIR;
    }
}