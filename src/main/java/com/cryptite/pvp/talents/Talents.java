package com.cryptite.pvp.talents;

import com.cryptite.pvp.ConfigFile;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.cryptite.pvp.talents.Talent.valueOf;


/**
 * Talent System
 *
 * @author Cryptite
 */

public class Talents implements Listener {
    private final Logger log = Logger.getLogger("Help");
    private final LokaVotA plugin;
    private final ConfigFile config;
    public final List<String> viewingPlayers = new ArrayList<>();
    private final List<String> inspectingPlayers = new ArrayList<>();
    private final Map<Integer, Material> talentItems = new HashMap<>();
    private final Map<Integer, Integer> prereqTalents = new HashMap<>();
    public final Map<Talent, List<String>> talentLore = new HashMap<>();
    public final Map<Material, Talent> talentMaterials = new HashMap<>();
    private final Map<Talent, TalentAbility> talentAbilities = new HashMap<>();
    private final Map<Integer, List<Integer>> talentGroups = new HashMap<>();
    public final List<Talent> itemlessTalents = new ArrayList<>();
    private final int maxTalentPoints = 8;

    public Talents(final LokaVotA plugin) {
        this.plugin = plugin;
        config = new ConfigFile(plugin, "talents.yml");
        loadTalents(null, null, true, false);
        itemlessTalents.add(Talent.LIFE_STEAL);
        itemlessTalents.add(Talent.REFLEXES);
        itemlessTalents.add(Talent.IRON_FORM);
        itemlessTalents.add(Talent.SWORD_DAMAGE);
        log.info("Talents loaded");
    }

    Inventory loadTalents(String player, Inventory i, boolean loadOnly, boolean inspect) {
        PvPPlayer p = plugin.getAccount(player);
        String section = "talents";

        talentItems.clear();
        prereqTalents.clear();
        talentMaterials.clear();
        talentAbilities.clear();

        //Pre-add groups
        talentGroups.put(0, new ArrayList<>());
        talentGroups.put(1, new ArrayList<>());
        talentGroups.put(2, new ArrayList<>());

        if (config.configFile.exists()) {
            for (String infoItem : config.getAll(section, null)) {
                String title = ChatColor.translateAlternateColorCodes('&',
                        config.get(section + "." + infoItem + ".title", null));

                Material material = Material.getMaterial(config.get(section + "." + infoItem + ".item", Material.PAPER));
                short itemId = Short.parseShort(config.get(section + "." + infoItem + ".itemid", 0));
                int slot = Integer.parseInt(config.get(section + "." + infoItem + ".slot", 0));
                String enchant = config.get(section + "." + infoItem + ".enchantment", null);
                int requires = Integer.parseInt(config.get(section + "." + infoItem + ".requires", 0));
                int cooldown = Integer.parseInt(config.get(section + "." + infoItem + ".cooldown", 0));
                int length = Integer.parseInt(config.get(section + "." + infoItem + ".length", 0));
                int group = Integer.parseInt(config.get(section + "." + infoItem + ".group", -1));

                talentItems.put(slot, material);
                talentGroups.get(group).add(slot);
                talentMaterials.put(material, valueOf(slot));

                if (requires > 0) {
                    //Required talents shown as Slime Balls
                    prereqTalents.put(slot, requires);
                }

                if (cooldown > 0) {
                    talentAbilities.put(valueOf(slot),
                            new TalentAbility(plugin, valueOf(slot), ChatColor.stripColor(title), material, length, cooldown));
                }

                //If load only, we're just loading talents for use elsewhere, not for a player to view.
                if (loadOnly) continue;

                List<String> lore = new ArrayList<>();

                for (String loreItem : config.getAll(section + "." + infoItem + ".lore", null)) {
                    String loreString = ChatColor.translateAlternateColorCodes('&',
                            config.get(section + "." + infoItem + ".lore." + loreItem, null));
                    lore.add(loreString);
                }

                ItemStack item;
                if (itemId > 0) {
                    item = new ItemStack(material, 1, itemId);
                } else {
                    item = new ItemStack(material);
                }
                ItemMeta itemMeta = item.getItemMeta();
                itemMeta.setDisplayName(title);
                itemMeta.setLore(lore);
                item.setItemMeta(itemMeta);
                talentLore.put(valueOf(slot), lore);

                //Slot 99 is the bandage
                if (slot == 99) continue;

                if (p != null) {
                    if (p.canChangeTalents()) {
                        if (!p.talents.contains(Talent.valueOf(slot))) {
                            if (p.talents.size() < maxTalentPoints && slot > 8) {
                                //Only need to display "tier numbers" if they can pick up talents.
                                //If slot greater than 9, this is at least a 2nd tier talent.
                                item.setAmount((slot / 9) + 1);
                            } else if (p.talents.size() >= maxTalentPoints || inspect) {
                                //Otherwise, set all all talents as coal to indicate the talents are locked.
                                item.setType(Material.COAL);
                            }
                        } else {
                            if (enchant != null) {
                                item.addEnchantment(Enchantment.getByName(enchant), 1);
                            }
                        }
                    } else {
                        if (!p.talents.contains(valueOf(slot))) {
                            item.setType(Material.COAL);
                        }
                    }
                }

                i.setItem(slot, item);
            }
        }

        if (player != null
                && p != null && p.canChangeTalents()
                && !inspectingPlayers.contains(player)) {
            i.setItem(48, talentPointIndicator(p, maxTalentPoints - p.talents.size()));
            i.setItem(50, saveButton());
        }
        return i;
    }

    ItemStack talentPointIndicator(PvPPlayer p, int talentsLeft) {
        //Talent point indicator pearl
        ItemStack pearl = new ItemStack(Material.FIRE);
        ItemMeta meta = pearl.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "Talent Points");
        List<String> pearlLore = new ArrayList<>();
        if (!p.talentsSaved || p.talents.size() < maxTalentPoints) {
            pearlLore.add(ChatColor.GRAY + "You have " + ChatColor.GREEN + talentsLeft +
                    ChatColor.GRAY + " remaining.");
            pearlLore.add(ChatColor.GOLD + "Click to reset your talent points");
            pearl.setAmount(talentsLeft);
        } else if (p.talentsSaved) {
            pearlLore.add(ChatColor.GOLD + "Click to reset your talent points");
        } else {
            pearlLore.add(ChatColor.GRAY + "You have spent all your talent points.");
        }
        meta.setLore(pearlLore);
        pearl.setItemMeta(meta);
        return pearl;
    }

    ItemStack saveButton() {
        ItemStack paper = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = paper.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Save your talent points.");
        List<String> pearlLore = new ArrayList<>();
        pearlLore.add(ChatColor.GRAY + "You will have to pay to reset");
        pearlLore.add(ChatColor.GRAY + "your talent points later!.");
        meta.setLore(pearlLore);
        paper.setItemMeta(meta);
        return paper;
    }

    public void showTalentTree(Player p, Player inspector) {
        if (!viewingPlayers.contains(p.getName())) {
            viewingPlayers.add(p.getName());
        }

        String talentStr = "Talents";

        if (inspector != null) {
            inspectingPlayers.add(p.getName());
            talentStr += " - " + p.getName();
            inspector.openInventory(loadTalents(p.getName(), Bukkit.createInventory(p, 54, talentStr), true, true));
        } else {
            p.openInventory(loadTalents(p.getName(), Bukkit.createInventory(p, 54, talentStr), false, false));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void clickEvent(final InventoryClickEvent e) {
        if (!viewingPlayers.contains(e.getWhoClicked().getName())
                || e.getCurrentItem() == null
                || e.getRawSlot() >= 54
                || e.getSlotType().equals(InventoryType.SlotType.QUICKBAR)) return;

        e.setCancelled(true);
        Material material = e.getCurrentItem().getType();
        Player p = (Player) e.getWhoClicked();
        PvPPlayer pvpPlayer = plugin.getAccount(p.getName());

        //Inspecting players have no control over the inventory.
        if (inspectingPlayers.contains(p.getName())) return;

        if (!pvpPlayer.canChangeTalents()) {
            e.getWhoClicked().closeInventory();
            e.getWhoClicked().openInventory(e.getInventory());
            viewingPlayers.add(pvpPlayer.name);
            return;
        }

        if (e.getCurrentItem() != null
                && !material.equals(Material.AIR)) {

            //Already spent all 8 talent points.
            if (pvpPlayer.talents.size() >= maxTalentPoints
                    && !material.equals(Material.NAME_TAG)
                    && !material.equals(Material.FIRE)
                    && pvpPlayer.talentsSaved) {
                p.closeInventory();
                p.openInventory(e.getInventory());

                if (!viewingPlayers.contains(p.getName())) {
                    viewingPlayers.add(p.getName());
                }
                return;
            }

            Boolean tookTalent = false;
            ItemStack otherTopTierTalent = null;

            if (e.getAction().equals(InventoryAction.PICKUP_ALL)) {
                if (canGetTalent(e.getCurrentItem(), e.getSlot())
                        && hasRequiredTalent(e.getInventory(), e.getSlot())
                        && !material.equals(Material.COAL)
                        && !material.equals(Material.NAME_TAG)
                        && !material.equals(Material.FIRE)
                        && !material.equals(Material.SNOW_BALL)
                        && !pvpPlayer.talents.contains(Talent.valueOf(e.getSlot()))) {
                    //Selected a talent, so turn it to a snowball
                    e.getCurrentItem().setType(Material.SNOW_BALL);

                    updateNextTalentTier(e, false);

                    //If top tier, set other top tier as unavailable
                    if (isTopTierTalent(e.getSlot())) {
                        otherTopTierTalent = e.getInventory().getItem(getOtherTopTierTalent(e.getSlot()));
                    }

                    pvpPlayer.talents.add(Talent.valueOf(e.getSlot()));
                    tookTalent = true;
                } else if (material.equals(Material.NAME_TAG)) {
                    p.sendMessage(ChatColor.GRAY + "Your talent points have been set!");
                    pvpPlayer.talentsSaved = true;
                    pvpPlayer.update("talents", pvpPlayer.talentsToString());
                    viewingPlayers.remove(pvpPlayer.name);
                    p.closeInventory();

                    pvpPlayer.cleanPlayer();
                    pvpPlayer.loadTalents(pvpPlayer.talentsToString());
                    pvpPlayer.setPlayerGear(true);
                    return;
                } else if (material.equals(Material.FIRE)) {
                    p.sendMessage(ChatColor.GRAY + "Your talent points have been reset!");
                    pvpPlayer.talents.clear();
                    pvpPlayer.talentsSaved = false;
//                    pvpPlayer.save();

                    p.closeInventory();
                    showTalentTree(p, null);
                    return;
                }
            } else if (e.getAction().equals(InventoryAction.PICKUP_HALF)) {
                p.closeInventory();
                showTalentTree(p, null);
                return;
            }


            p.closeInventory();
            Inventory i = e.getInventory();
            if (tookTalent) {
                i.setItem(48, talentPointIndicator(pvpPlayer, i.getItem(48).getAmount() - 1));
                updateOutOfRangeTalents(i, maxTalentPoints - pvpPlayer.talents.size());
                if (otherTopTierTalent != null) {
                    otherTopTierTalent.setType(Material.COAL);
                    otherTopTierTalent.setAmount(1);
                }

                log.info(pvpPlayer.name + " picked up " + Talent.valueOf(e.getSlot()));
            }
            if (!viewingPlayers.contains(p.getName())) {
                viewingPlayers.add(p.getName());
            }
            p.openInventory(i);
        }
    }

    Boolean isTopTierTalent(int slot) {
        return slot >= 36 && slot <= 44;
    }

    int getOtherTopTierTalent(int slot) {
        //Hardcoding this is just fine
        if (slot == 36) return 37;
        if (slot == 37) return 36;

        if (slot == 39) return 41;
        if (slot == 41) return 39;

        if (slot == 43) return 44;
        if (slot == 44) return 43;
        return -1;
    }

    Boolean hasRequiredTalent(Inventory i, int slot) {
        if (prereqTalents.containsKey(slot)) {
            int requiredTalentSlot = prereqTalents.get(slot);
            if (!i.getItem(requiredTalentSlot).getType().equals(Material.SNOW_BALL)) {
                return false;
            }
        }
        return true;
    }

    Boolean canGetTalent(ItemStack item, int slot) {
        return slot <= 8 || (slot > 8 && item.getAmount() == 1);
    }

    void updateNextTalentTier(InventoryClickEvent e, boolean add) {
        int slotNum = e.getSlot();

        for (int slot : talentGroups.get(getTalentGroup(slotNum))) {
            if (slot == 99) continue;

            ItemStack nextItem = e.getInventory().getItem(slot);
            if (nextItem != null && !nextItem.getType().equals(Material.COAL)) {
                if (add) {
                    nextItem.setAmount(nextItem.getAmount() + 1);
                } else {
                    nextItem.setAmount(nextItem.getAmount() - 1);
                }
            }
        }
    }

    int getTalentGroup(int slot) {
        for (int group : talentGroups.keySet()) {
            if (talentGroups.get(group).contains(slot)) {
                return group;
            }
        }
        return -1;
    }

    void updateOutOfRangeTalents(Inventory inv, int talentPointsLeft) {
        for (int i = 0; i <= 44; i++) {
            ItemStack item = inv.getItem(i);

            if (item == null) continue;

            if (talentPointsLeft == 0) {
                if (!item.getType().equals(Material.SNOW_BALL)
                        && !item.getType().equals(Material.COAL)) {
                    item.setType(Material.COAL);
                }
                continue;
            }

            if (item.getAmount() > talentPointsLeft) {
                item.setType(Material.COAL);
            }
        }
    }

    @EventHandler
    private void helpDragEvent(InventoryDragEvent e) {
        if (viewingPlayers.contains(e.getWhoClicked().getName())) {
            e.getWhoClicked().closeInventory();
            viewingPlayers.add(e.getWhoClicked().getName());
            if (inspectingPlayers.contains(e.getWhoClicked().getName())) {
                Player inspector = plugin.server.getPlayerExact(e.getWhoClicked().getName());
                if (inspector == null) return;

                showTalentTree((Player) e.getWhoClicked(), inspector);
            } else {
                showTalentTree((Player) e.getWhoClicked(), null);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        String player = event.getPlayer().getName();
        viewingPlayers.remove(player);
        inspectingPlayers.remove(player);
    }
}
