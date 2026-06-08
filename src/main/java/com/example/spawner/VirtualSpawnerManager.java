package com.example.spawner;

import com.example.Main;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualSpawnerManager {
   private final Main plugin;
   private final SpawnerConfig config;
   private final VirtualSpawnerKeys keys;
   private final VirtualSpawnerStorage storage;
   private final VirtualSpawnerGui gui;
   private final Map<Block, SpawnerRuntime> activeSpawners;
   private final Map<Block, Integer> boostedSpawners;

   public VirtualSpawnerManager(Main plugin) {
      this.plugin = plugin;
      this.keys = new VirtualSpawnerKeys(plugin);
      this.storage = new VirtualSpawnerStorage(this.keys);
      this.config = new SpawnerConfig();
      this.gui = new VirtualSpawnerGui(this.config, this.storage);
      this.activeSpawners = new HashMap<>();
      this.boostedSpawners = new HashMap<>();
      load();
   }

   public void load() {
      // Safe initialization hooks for populating or restoring active world spawner runtimes
      this.activeSpawners.clear();
      this.boostedSpawners.clear();
   }

   public void registerSpawner(Block block, VirtualSpawnerData data) {
      if (block == null || data == null) return;
      this.activeSpawners.put(block, new SpawnerRuntime(block, data, 0));
   }

   public void unregisterSpawner(Block block) {
      if (block == null) return;
      this.activeSpawners.remove(block);
      this.boostedSpawners.remove(block);
   }

   public VirtualSpawnerData getData(Block block) {
      if (block == null) return null;
      SpawnerRuntime runtime = this.activeSpawners.get(block);
      return runtime != null ? runtime.getData() : null;
   }

   public VirtualSpawnerGui getGui() {
      return this.gui;
   }

   public VirtualSpawnerStorage getStorage() {
      return this.storage;
   }

   public SpawnerConfig getConfig() {
      return this.config;
   }

   public void tickSpawners() {
      if (this.activeSpawners.isEmpty()) return;

      for (SpawnerRuntime runtime : this.activeSpawners.values()) {
         runtime.incrementProgress();
         VirtualSpawnerData data = runtime.getData();

         int targetTicks = this.config.getCycleSeconds(data);

         if (runtime.getProgressTicks() >= targetTicks) {
            runtime.resetProgress();

            EntityType mobType = data.getMobType();
            if (!MobDropRegistry.isSupported(mobType)) continue;

            int amountMultiplier = this.config.getItemsPerCycle(data);
            if (amountMultiplier <= 0) continue;

            List<ItemStack> storedItems = data.getStoredItems();
            int maxStorageSlots = this.config.getStorageSlots();

            // Generate drops using the new chance system with token boost
            Block block = runtime.getBlock();
            double chanceMultiplier = this.boostedSpawners.containsKey(block) ? 2.0 : 1.0;
            List<MobDropRegistry.DropResult> drops = MobDropRegistry.generateDrops(mobType, amountMultiplier, chanceMultiplier);
            
            for (MobDropRegistry.DropResult drop : drops) {
               addDropsToStorage(storedItems, drop.material(), drop.amount(), maxStorageSlots);
            }

            this.storage.writeBlock(block, data);

            // Consume boost after one cycle
            if (this.boostedSpawners.containsKey(block)) {
               this.boostedSpawners.remove(block);
            }

            // Instantly update any open interface panels for matching coordinates
            for (Player player : this.plugin.getServer().getOnlinePlayers()) {
               if (this.gui.getOpenLocation(player) != null &&
                       this.gui.getOpenLocation(player).equals(runtime.getBlock().getLocation())) {

                  Inventory topInventory = player.getOpenInventory().getTopInventory();
                  if (topInventory != null) {
                     this.gui.populate(topInventory, data);
                  }
               }
            }
         }
      }
   }

   private void addDropsToStorage(List<ItemStack> storedItems, Material material, int amount, int maxSlots) {
      int remaining = amount;

      // Step 1: Attempt filling out matching current inventory items up to max stack count
      for (ItemStack item : storedItems) {
         if (item != null && item.getType() == material) {
            int availableSpace = item.getMaxStackSize() - item.getAmount();
            if (availableSpace > 0) {
               int addAmount = Math.min(remaining, availableSpace);
               item.setAmount(item.getAmount() + addAmount);
               remaining -= addAmount;
               if (remaining <= 0) return;
            }
         }
      }

      // Step 2: Fill out completely empty remaining slots if capacity limit allows
      while (remaining > 0 && storedItems.size() < maxSlots) {
         int stackAmount = Math.min(remaining, material.getMaxStackSize());
         storedItems.add(new ItemStack(material, stackAmount));
         remaining -= stackAmount;
      }
   }

   public void enable() {
      this.config.load(this.plugin);
      TokenAPI.init(this.plugin);

      for (World world : this.plugin.getServer().getWorlds()) {
         this.scanWorld(world);
      }

      new VirtualSpawnerTask(this).runTaskTimer(this.plugin, 20L, 20L);
      this.plugin.getServer().getPluginManager().registerEvents(new VirtualSpawnerListener(this), this.plugin);
      this.plugin.getServer().getPluginManager().registerEvents(new VirtualSpawnerGuiListener(this), this.plugin);
   }

   public void disable() {
      for (SpawnerRuntime runtime : this.activeSpawners.values()) {
         this.storage.writeBlock(runtime.getBlock(), runtime.getData());
      }

      this.activeSpawners.clear();
   }

   public void unregisterSpawner(@NotNull Location location) {
      if (location == null) return;
      Block block = location.getBlock();
      if (block != null) {
         this.activeSpawners.remove(block);
         this.boostedSpawners.remove(block);
      }
   }

   public void applyBoost(Block block) {
      if (block == null) return;
      this.boostedSpawners.put(block, 1);
   }

   public void saveData(Block block, VirtualSpawnerData data) {
      this.storage.writeBlock(block, data);
      SpawnerRuntime runtime = this.activeSpawners.get(block);
      if (runtime != null) {
         runtime.setData(data.copy());
         runtime.resetProgress();
      } else {
         this.activeSpawners.put(block, new SpawnerRuntime(block, data.copy(), 0));
      }
   }

   public void addToStorage(VirtualSpawnerData data, ItemStack item) {
      if (item == null || item.getType().isAir()) {
         return;
      }

      int maxSlots = this.config.getStorageSlots();

      for (ItemStack stored : data.getStoredItems()) {
         if (stored == null || stored.getType().isAir()) {
            continue;
         }

         if (!stored.isSimilar(item)) {
            continue;
         }

         int maxStack = stored.getMaxStackSize();
         int space = maxStack - stored.getAmount();
         if (space <= 0) {
            continue;
         }

         int moved = Math.min(space, item.getAmount());
         stored.setAmount(stored.getAmount() + moved);
         item.setAmount(item.getAmount() - moved);
         if (item.getAmount() <= 0) {
            return;
         }
      }

      while (item.getAmount() > 0 && data.getStoredItems().size() < maxSlots) {
         int stackAmount = Math.min(item.getAmount(), item.getMaxStackSize());
         ItemStack stack = item.clone();
         stack.setAmount(stackAmount);
         data.getStoredItems().add(stack);
         item.setAmount(item.getAmount() - stackAmount);
      }
   }

   public void scanChunk(Chunk chunk) {
      for (BlockState state : chunk.getTileEntities()) {
         Block block = state.getBlock();
         if (block.getType() != Material.SPAWNER) {
            continue;
         }

         VirtualSpawnerData data = this.storage.readBlock(block);
         if (data != null) {
            this.registerSpawner(block, data);
         }
      }
   }

   private void scanWorld(World world) {
      for (Chunk chunk : world.getLoadedChunks()) {
         this.scanChunk(chunk);
      }
   }

   public static class SpawnerRuntime {
      private final Block block;
      private VirtualSpawnerData data;
      private int progressTicks;

      public SpawnerRuntime(Block block, VirtualSpawnerData data, int progressTicks) {
         this.block = block;
         this.data = data;
         this.progressTicks = progressTicks;
      }

      public Block getBlock() {
         return this.block;
      }

      public VirtualSpawnerData getData() {
         return this.data;
      }

      public void setData(VirtualSpawnerData data) {
         this.data = data;
      }

      public int getProgressTicks() {
         return this.progressTicks;
      }

      public void incrementProgress() {
         this.progressTicks++;
      }

      public void resetProgress() {
         this.progressTicks = 0;
      }
   }
}