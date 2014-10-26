package com.cryptite.pvp.listeners;

import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.talents.Talent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {
    private final LokaVotA plugin;

    public PlayerInteractListener(LokaVotA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUse(PlayerAnimationEvent e) {
        Player player = e.getPlayer();
        ItemStack itemInHand = player.getItemInHand();
        if (itemInHand == null) return;

        Material material = itemInHand.getType();
        PvPPlayer p = plugin.getAccount(player.getName());

        if (plugin.talents.talentMaterials.containsKey(material)) {
            p.activateTalent(p.talentMaterials.get(material));
            e.setCancelled(true);
        } else if (itemInHand.getType().equals(Material.BOW)) {
            if (p.talentAvailable(Talent.EXPLOSIVE_ARROW)) {
                p.activateTalent(Talent.EXPLOSIVE_ARROW);
                e.setCancelled(true);
            } else if (p.talentAvailable(Talent.HEAL_ARROW)) {
                p.activateTalent(Talent.HEAL_ARROW);
                e.setCancelled(true);
            } else if (p.talentAvailable(Talent.HOOK)) {
                p.activateTalent(Talent.HOOK);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            Material material = player.getItemInHand().getType();
            final PvPPlayer p = plugin.getAccount(player.getName());
            if (material.equals(Material.STONE_SWORD)) {
                if (!p.talents.contains(Talent.REFLEXES) || !p.talentAbilities.get(Talent.REFLEXES).available) return;
                if (e.getClickedBlock() != null && e.getClickedBlock().getType().equals(Material.CHEST)) return;
                p.activateTalent(Talent.REFLEXES);
            } else if (plugin.talentSigns.contains(e.getClickedBlock())) {
                plugin.talents.showTalentTree(player, null);
            } else if (plugin.inventoryPreferenceSigns.contains(e.getClickedBlock())) {
                p.saveInventoryOrder();
                p.sendMessage(ChatColor.AQUA + "Your inventory order preferences have been saved. " +
                        ChatColor.GRAY + "Whenever you respawn, your items will always be in the same slots.");
            } else if (material.equals(Material.BOOK_AND_QUILL)) {
                String tpTarget = player.getItemInHand().getItemMeta().getDisplayName();
                System.out.println("tpt: " + tpTarget);
                if (tpTarget == null) return;

                Player tpTo = plugin.server.getPlayer(tpTarget);
                System.out.println("tpTo: " + tpTo);
                if (tpTo == null) return;

                player.teleport(tpTo.getLocation());
            } else if (p.talentMaterials.containsKey(material)
                    && (!material.equals(Material.POTION) && !material.equals(Material.BOW))) {
                p.activateTalent(p.talentMaterials.get(material));
                e.setCancelled(true);
            }
        }

        //Left click with bow can activate Explosive/Freyjia's Arrow
        if (e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            Material inHand = player.getItemInHand().getType();
            PvPPlayer p = plugin.getAccount(player.getName());
            if (inHand.equals(Material.BOW)) {
                if (p.talentAvailable(Talent.EXPLOSIVE_ARROW)) {
                    p.activateTalent(Talent.EXPLOSIVE_ARROW);
                    e.setCancelled(true);
                } else if (p.talentAvailable(Talent.HEAL_ARROW)) {
                    p.activateTalent(Talent.HEAL_ARROW);
                    e.setCancelled(true);
                } else if (p.talentAvailable(Talent.HOOK)) {
                    p.activateTalent(Talent.HOOK);
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        PvPPlayer p = plugin.getAccount(event.getPlayer().getName());
        if (p.spectator) event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        PvPPlayer p = plugin.getAccount(event.getPlayer().getName());

        //Only dropping arrows and potions is allowed in BGs (except VoC)
        if (p.bg != null
                && (!event.getItemDrop().getItemStack().getType().equals(Material.ARROW)
                && !event.getItemDrop().getItemStack().getType().equals(Material.POTION))) event.setCancelled(true);
    }
}
