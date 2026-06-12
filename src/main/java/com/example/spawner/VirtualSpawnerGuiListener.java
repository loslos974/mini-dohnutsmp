package com.example.spawner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class VirtualSpawnerGuiListener implements Listener {

   private final VirtualSpawnerManager manager;

   public VirtualSpawnerGuiListener(VirtualSpawnerManager manager) {
      this.manager = manager;
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onInventoryClick(InventoryClickEvent event) {
      if (!(event.getWhoClicked() instanceof Player player)) {
         return;
      }

      // Robust detection: identify the GUI by its holder type, not a title string.
      if (!(event.getInventory().getHolder() instanceof SpawnerGuiHolder holder)) {
         return;
      }

      int rawSlot = event.getRawSlot();
      Inventory top = event.getView().getTopInventory();
      boolean clickedTop = rawSlot >= 0 && rawSlot < top.getSize();

      // Clicks in the player's own inventory: only block shift-clicks that would
      // shovel items into the virtual GUI; leave normal inventory use intact.
      if (!clickedTop) {
         if (event.isShiftClick()) {
            event.setCancelled(true);
         }
         return;
      }

      // The whole top inventory is virtual/decorative — we manage every change manually.
      event.setCancelled(true);

      Block block = holder.getLocation().getBlock();
      VirtualSpawnerData data = this.manager.getData(block);
      if (data == null) {
         player.sendMessage("§cThat spawner no longer exists.");
         player.closeInventory();
         return;
      }

      switch (rawSlot) {
         case VirtualSpawnerGui.CLAIM_SLOT -> {
            this.claimAll(player, data);
            this.manager.saveData(block, data);
            this.manager.getGui().refresh(player, data);
         }
         case VirtualSpawnerGui.SPEED_SLOT -> this.upgradeSpeed(player, block, data);
         case VirtualSpawnerGui.AMOUNT_SLOT -> this.upgradeAmount(player, block, data);
         case VirtualSpawnerGui.BOOST_SLOT -> this.boostSpawner(player, block, data);
         case VirtualSpawnerGui.INFO_SLOT -> {
            // Informational button — no interaction.
         }
         default -> this.claimSingle(player, block, data, event.getCurrentItem());
      }
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onInventoryDrag(InventoryDragEvent event) {
      if (!(event.getInventory().getHolder() instanceof SpawnerGuiHolder)) {
         return;
      }

      int topSize = event.getView().getTopInventory().getSize();
      for (int slot : event.getRawSlots()) {
         if (slot < topSize) {
            event.setCancelled(true);
            return;
         }
      }
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      if (!(event.getInventory().getHolder() instanceof SpawnerGuiHolder)) {
         return;
      }

      if (event.getPlayer() instanceof Player player) {
         this.manager.getGui().closeSession(player);
      }
   }

   private void claimSingle(Player player, Block block, VirtualSpawnerData data, ItemStack clicked) {
      if (clicked == null || clicked.getType().isAir() || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
         return;
      }

      Iterator<ItemStack> iterator = data.getStoredItems().iterator();
      while (iterator.hasNext()) {
         ItemStack stored = iterator.next();
         if (stored == null || stored.getType().isAir() || !stored.isSimilar(clicked)) {
            continue;
         }

         ItemStack remaining = this.giveItem(player, stored.clone());
         iterator.remove();
         if (remaining != null && !remaining.getType().isAir() && remaining.getAmount() > 0) {
            this.manager.addToStorage(data, remaining);
            player.sendMessage("§eInventory full — some items remain in the spawner.");
         }

         this.manager.saveData(block, data);
         this.manager.getGui().refresh(player, data);
         return;
      }
   }

   private void claimAll(Player player, VirtualSpawnerData data) {
      if (data.getStoredItems().isEmpty()) {
         player.sendMessage("§7No items to claim.");
         return;
      }

      Map<Material, Integer> leftover = new HashMap<>();
      Iterator<ItemStack> iterator = data.getStoredItems().iterator();

      while (iterator.hasNext()) {
         ItemStack stored = iterator.next();
         if (stored == null || stored.getType().isAir()) {
            iterator.remove();
            continue;
         }

         ItemStack remaining = this.giveItem(player, stored.clone());
         if (remaining == null || remaining.getType().isAir() || remaining.getAmount() <= 0) {
            iterator.remove();
         } else {
            iterator.remove();
            leftover.merge(remaining.getType(), remaining.getAmount(), Integer::sum);
         }
      }

      for (Map.Entry<Material, Integer> entry : leftover.entrySet()) {
         ItemStack restored = new ItemStack(entry.getKey(), entry.getValue());
         this.manager.addToStorage(data, restored);
      }

      if (leftover.isEmpty()) {
         player.sendMessage("§aClaimed all stored items.");
      } else {
         player.sendMessage("§eInventory full — some items remain in the spawner.");
      }
   }

   private ItemStack giveItem(Player player, ItemStack item) {
      HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
      if (overflow.isEmpty()) {
         return null;
      }

      ItemStack left = null;
      for (ItemStack stack : overflow.values()) {
         left = stack;
      }

      return left;
   }

   private void upgradeSpeed(Player player, Block block, VirtualSpawnerData data) {
      SpawnerConfig config = this.manager.getConfig();
      if (data.getSpeedLevel() >= config.getMaxSpeedLevel()) {
         player.sendMessage("§cSpeed upgrade is already maxed.");
         return;
      }

      int cost = config.getSpeedUpgradeCost(data.getSpeedLevel());
      if (TokenAPI.getTokens(player) < cost) {
         player.sendMessage("§cYou need §b" + cost + " tokens §cfor this upgrade.");
         return;
      }

      if (!TokenAPI.takeTokens(player, cost)) {
         player.sendMessage("§cCould not deduct tokens.");
         return;
      }

      data.setSpeedLevel(data.getSpeedLevel() + 1);
      this.manager.saveData(block, data);
      this.manager.getGui().refresh(player, data);
      player.sendMessage("§aUpgraded speed to level §c" + data.getSpeedLevel() + "§a.");
   }

   private void upgradeAmount(Player player, Block block, VirtualSpawnerData data) {
      SpawnerConfig config = this.manager.getConfig();
      if (data.getAmountLevel() >= config.getMaxAmountLevel()) {
         player.sendMessage("§cAmount upgrade is already maxed.");
         return;
      }

      int cost = config.getAmountUpgradeCost(data.getAmountLevel());
      if (TokenAPI.getTokens(player) < cost) {
         player.sendMessage("§cYou need §b" + cost + " tokens §cfor this upgrade.");
         return;
      }

      if (!TokenAPI.takeTokens(player, cost)) {
         player.sendMessage("§cCould not deduct tokens.");
         return;
      }

      data.setAmountLevel(data.getAmountLevel() + 1);
      this.manager.saveData(block, data);
      this.manager.getGui().refresh(player, data);
      player.sendMessage("§aUpgraded amount to level §a" + data.getAmountLevel() + "§a.");
   }

   private void boostSpawner(Player player, Block block, VirtualSpawnerData data) {
      int cost = 10;
      if (TokenAPI.getTokens(player) < cost) {
         player.sendMessage("§cYou need §b" + cost + " tokens §cfor this boost.");
         return;
      }

      if (!TokenAPI.takeTokens(player, cost)) {
         player.sendMessage("§cCould not deduct tokens.");
         return;
      }

      this.manager.applyBoost(block);
      this.manager.getGui().refresh(player, data);
      player.sendMessage("§aSpawner boosted! §e2x drop chance §afor next cycle.");
   }
}
