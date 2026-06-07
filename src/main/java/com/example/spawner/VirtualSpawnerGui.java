package com.example.spawner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class VirtualSpawnerGui {

   public static final int CLAIM_SLOT = 49;
   public static final int SPEED_SLOT = 47;
   public static final int INFO_SLOT = 4;
   public static final int AMOUNT_SLOT = 51;
   public static final int BOOST_SLOT = 53;

   private final SpawnerConfig config;
   private final Map<Player, Location> openSessions = new HashMap<>();

   public VirtualSpawnerGui(SpawnerConfig config, VirtualSpawnerStorage storage) {
      this.config = config;
   }

   public void open(Player player, Location location, VirtualSpawnerData data) {
      Inventory inventory = Bukkit.createInventory(null, 54, SpawnerConfig.GUI_TITLE);
      this.populate(inventory, data);
      this.openSessions.put(player, location.clone());
      player.openInventory(inventory);
   }

   public void refresh(Player player, VirtualSpawnerData data) {
      Inventory inventory = player.getOpenInventory().getTopInventory();
      if (inventory == null || inventory.getSize() != 54) {
         return;
      }

      this.populate(inventory, data);
   }

   public Location getOpenLocation(Player player) {
      return this.openSessions.get(player);
   }

   public void closeSession(Player player) {
      this.openSessions.remove(player);
   }

   public boolean isSpawnerGui(String title) {
      return SpawnerConfig.GUI_TITLE.equals(title);
   }

   public void populate(Inventory inventory, VirtualSpawnerData data) {
      inventory.clear();
      ItemStack filler = this.createButton(Material.GRAY_STAINED_GLASS_PANE, " ", null);

      for (int slot = 0; slot < inventory.getSize(); ++slot) {
         inventory.setItem(slot, filler);
      }

      List<ItemStack> stored = data.getStoredItems();
      int storageSlots = Math.min(this.config.getStorageSlots(), 36);

      for (int i = 0; i < storageSlots && i < stored.size(); ++i) {
         inventory.setItem(i, stored.get(i).clone());
      }

      int cycleSeconds = Math.max(1, this.config.getCycleTicks(data) / 20);
      int itemsPerCycle = this.config.getItemsPerCycle(data);
      Material drop = MobDropRegistry.getDrop(data.getMobType());

      inventory.setItem(INFO_SLOT, this.createButton(
         Material.SPAWNER,
         "§e" + VirtualSpawnerStorage.formatMobName(data.getMobType()) + " Spawner",
         List.of(
            "§7Stacked: §f" + data.getStackSize() + "x",
            "§7Speed Level: §c" + data.getSpeedLevel(),
            "§7Amount Level: §a" + data.getAmountLevel(),
            "",
            "§7Cycle: §f" + cycleSeconds + "s",
            "§7Yield: §f" + itemsPerCycle + "x " + formatMaterial(drop),
            "",
            "§7Farms virtually — no mobs spawn"
         )
      ));

      inventory.setItem(CLAIM_SLOT, this.createButton(
         Material.CHEST,
         "§aClaim All",
         List.of("§7Move all stored items", "§7into your inventory")
      ));

      int speedCost = this.config.getSpeedUpgradeCost(data.getSpeedLevel());
      inventory.setItem(SPEED_SLOT, this.createButton(
         Material.REDSTONE,
         "§cSpeed Upgrade",
         List.of(
            "§7Current: §cLevel " + data.getSpeedLevel(),
            data.getSpeedLevel() >= this.config.getMaxSpeedLevel()
               ? "§cMax level reached"
               : "§7Cost: §b" + speedCost + " tokens",
            "",
            data.getSpeedLevel() >= this.config.getMaxSpeedLevel()
               ? "§8Cannot upgrade further"
               : "§eClick to upgrade"
         )
      ));

      int amountCost = this.config.getAmountUpgradeCost(data.getAmountLevel());
      inventory.setItem(AMOUNT_SLOT, this.createButton(
         Material.EMERALD,
         "§aAmount Upgrade",
         List.of(
            "§7Current: §aLevel " + data.getAmountLevel(),
            data.getAmountLevel() >= this.config.getMaxAmountLevel()
               ? "§cMax level reached"
               : "§7Cost: §b" + amountCost + " tokens",
            "",
            data.getAmountLevel() >= this.config.getMaxAmountLevel()
               ? "§8Cannot upgrade further"
               : "§eClick to upgrade"
         )
      ));

      int boostCost = 10;
      inventory.setItem(BOOST_SLOT, this.createButton(
         Material.GOLD_INGOT,
         "§6Token Boost",
         List.of(
            "§7Cost: §b" + boostCost + " tokens",
            "§7Effect: §e2x drop chance",
            "§7Duration: §a1 cycle",
            "",
            "§eClick to boost next cycle"
         )
      ));
   }

   private ItemStack createButton(Material material, String name, List<String> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(name);
         if (lore != null) {
            meta.setLore(new ArrayList<>(lore));
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   private static String formatMaterial(Material material) {
      return material.name().toLowerCase().replace('_', ' ');
   }
}
