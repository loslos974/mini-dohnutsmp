package com.example.spawner;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

public final class VirtualSpawnerListener implements Listener {

   private final VirtualSpawnerManager manager;

   public VirtualSpawnerListener(VirtualSpawnerManager manager) {
      this.manager = manager;
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onPlace(BlockPlaceEvent event) {
      if (event.getBlockPlaced().getType() != Material.SPAWNER) {
         return;
      }

      Block block = event.getBlockPlaced();
      ItemStack item = event.getItemInHand();
      VirtualSpawnerData itemData = this.manager.getStorage().readItem(item);
      
      if (itemData != null) {
         VirtualSpawnerData data = itemData.copy();
         if (data.getOwner() == null) {
            data.setOwner(event.getPlayer().getUniqueId());
         }
         this.manager.registerSpawner(block, data);
         this.manager.getStorage().writeBlock(block, data);
      } else {
         // Vanilla / case-won spawner items: in Survival the block entity tag is
         // stripped on placement, so the placed block reports no spawn type. Read
         // the entity type from the ITEM's BlockStateMeta instead and apply it
         // explicitly so it works in every gamemode.
         EntityType mobType = readSpawnerItemType(item);
         if (mobType == null || mobType == EntityType.UNKNOWN) {
            if (block.getState() instanceof CreatureSpawner placed) {
               mobType = placed.getSpawnedType();
            }
         }

         if (mobType != null && mobType != EntityType.UNKNOWN) {
            VirtualSpawnerData defaultData = new VirtualSpawnerData(mobType);
            defaultData.setOwner(event.getPlayer().getUniqueId());
            this.manager.registerSpawner(block, defaultData);
            this.manager.getStorage().writeBlock(block, defaultData);
         }
      }
   }

   private EntityType readSpawnerItemType(ItemStack item) {
      if (item == null || item.getType() != Material.SPAWNER || !item.hasItemMeta()) {
         return null;
      }

      if (item.getItemMeta() instanceof BlockStateMeta blockMeta && blockMeta.hasBlockState()
            && blockMeta.getBlockState() instanceof CreatureSpawner spawner) {
         return spawner.getSpawnedType();
      }

      return null;
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onBreak(BlockBreakEvent event) {
      Block block = event.getBlock();
      if (block.getType() != Material.SPAWNER) {
         return;
      }

      VirtualSpawnerData data = this.manager.getData(block);
      if (data == null) {
         return;
      }

      // Check for silk touch
      ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
      boolean hasSilkTouch = tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH);
      
      if (!hasSilkTouch) {
         // No silk touch - don't drop the spawner
         this.manager.unregisterSpawner(block.getLocation());
         return;
      }

      event.setDropItems(false);
      event.setExpToDrop(0);
      this.manager.unregisterSpawner(block.getLocation());

      Location dropLoc = block.getLocation().add(0.5D, 0.5D, 0.5D);

      // Each dropped item is a single, clean spawner of this mob type. The number
      // of items dropped equals the stored stack size (stack of 2 -> 2 items).
      int stackCount = Math.max(1, data.getStackSize());
      ItemStack template = this.manager.getStorage().createSpawnerItem(new VirtualSpawnerData(data.getMobType()));

      int remaining = stackCount;
      while (remaining > 0) {
         int amount = Math.min(remaining, template.getMaxStackSize());
         ItemStack drop = template.clone();
         drop.setAmount(amount);
         block.getWorld().dropItemNaturally(dropLoc, drop);
         remaining -= amount;
      }

      // Don't silently lose accumulated virtual loot — drop it alongside the spawners.
      for (ItemStack stored : data.getStoredItems()) {
         if (stored != null && !stored.getType().isAir()) {
            block.getWorld().dropItemNaturally(dropLoc, stored.clone());
         }
      }
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onInteract(PlayerInteractEvent event) {
      if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.SPAWNER) {
         return;
      }

      Block block = event.getClickedBlock();
      VirtualSpawnerData data = this.manager.getData(block);
      
      // If no custom data, try to create default data from vanilla spawner
      if (data == null) {
         CreatureSpawner spawner = (CreatureSpawner) block.getState();
         EntityType mobType = spawner.getSpawnedType();
         if (mobType != null && mobType != EntityType.UNKNOWN) {
            data = new VirtualSpawnerData(mobType);
            this.manager.registerSpawner(block, data);
            this.manager.getStorage().writeBlock(block, data);
         } else {
            return;
         }
      }

      Player player = event.getPlayer();
      if (player.isSneaking() && event.getAction().isRightClick()) {
         ItemStack hand = this.getMatchingSpawnerHand(player, data.getMobType());
         if (hand == null) {
            return;
         }

         event.setCancelled(true);
         int maxStack = this.manager.getConfig().getMaxStackSize();
         if (data.getStackSize() >= maxStack) {
            player.sendMessage("§cThis spawner is already at max stack size (" + maxStack + ").");
            return;
         }

         hand.setAmount(hand.getAmount() - 1);
         data.setStackSize(data.getStackSize() + 1);
         this.manager.saveData(block, data);
         player.sendMessage("§aStacked spawner: §f" + data.getStackSize() + "x");
         return;
      }

      if (!event.getAction().isRightClick() || player.isSneaking()) {
         return;
      }

      event.setCancelled(true);
      this.manager.getGui().open(player, block.getLocation(), data.copy());
   }

   @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
   public void onSpawnerSpawn(SpawnerSpawnEvent event) {
      CreatureSpawner spawner = event.getSpawner();
      if (this.manager.getData(spawner.getBlock()) != null) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onChunkLoad(ChunkLoadEvent event) {
      this.manager.scanChunk(event.getChunk());
   }

   private ItemStack getMatchingSpawnerHand(Player player, org.bukkit.entity.EntityType mobType) {
      ItemStack main = player.getInventory().getItemInMainHand();
      if (this.manager.getStorage().isMatchingSpawnerItem(main, mobType)) {
         return main;
      }

      ItemStack off = player.getInventory().getItemInOffHand();
      if (this.manager.getStorage().isMatchingSpawnerItem(off, mobType)) {
         return off;
      }

      return null;
   }
}
