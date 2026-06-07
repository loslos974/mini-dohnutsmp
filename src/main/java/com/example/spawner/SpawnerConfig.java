package com.example.spawner;

import com.example.Main;
import org.bukkit.configuration.file.FileConfiguration;

public final class SpawnerConfig {

   public static final String GUI_TITLE = "§8Virtual Spawner";

   private int baseCycleSeconds = 10;
   private double speedReductionPerLevel = 0.2D;
   private double amountBonusPerLevel = 0.5D;
   private int maxSpeedLevel = 10;
   private int maxAmountLevel = 10;
   private int maxStackSize = 64;
   private int storageSlots = 36;
   private int speedUpgradeBaseCost = 25;
   private int amountUpgradeBaseCost = 25;

   public void load(Main plugin) {
      plugin.saveDefaultConfig();
      FileConfiguration config = plugin.getConfig();

      this.baseCycleSeconds = config.getInt("spawners.base-cycle-seconds", 10);
      this.speedReductionPerLevel = config.getDouble("spawners.speed-reduction-per-level", 0.2D);
      this.amountBonusPerLevel = config.getDouble("spawners.amount-bonus-per-level", 0.5D);
      this.maxSpeedLevel = config.getInt("spawners.max-speed-level", 10);
      this.maxAmountLevel = config.getInt("spawners.max-amount-level", 10);
      this.maxStackSize = config.getInt("spawners.max-stack-size", 64);
      this.storageSlots = config.getInt("spawners.storage-slots", 36);
      this.speedUpgradeBaseCost = config.getInt("spawners.speed-upgrade-base-cost", 25);
      this.amountUpgradeBaseCost = config.getInt("spawners.amount-upgrade-base-cost", 25);
   }

   public int getBaseCycleSeconds() {
      return this.baseCycleSeconds;
   }

   public double getSpeedReductionPerLevel() {
      return this.speedReductionPerLevel;
   }

   public double getAmountBonusPerLevel() {
      return this.amountBonusPerLevel;
   }

   public int getMaxSpeedLevel() {
      return this.maxSpeedLevel;
   }

   public int getMaxAmountLevel() {
      return this.maxAmountLevel;
   }

   public int getMaxStackSize() {
      return this.maxStackSize;
   }

   public int getStorageSlots() {
      return this.storageSlots;
   }

   public int getSpeedUpgradeCost(int currentLevel) {
      return this.speedUpgradeBaseCost * (currentLevel + 1);
   }

   public int getAmountUpgradeCost(int currentLevel) {
      return this.amountUpgradeBaseCost * (currentLevel + 1);
   }

   public int getCycleTicks(VirtualSpawnerData data) {
      double multiplier = 1.0D + data.getSpeedLevel() * this.speedReductionPerLevel;
      double seconds = this.baseCycleSeconds / multiplier;
      return Math.max(20, (int)Math.round(seconds * 20.0D));
   }

   public int getItemsPerCycle(VirtualSpawnerData data) {
      double amountMultiplier = 1.0D + data.getAmountLevel() * this.amountBonusPerLevel;
      return Math.max(1, (int)Math.round(data.getStackSize() * amountMultiplier));
   }

    public int getCycleSeconds(VirtualSpawnerData data) {
        double multiplier = 1.0D + data.getSpeedLevel() * this.speedReductionPerLevel;
        double seconds = this.baseCycleSeconds / multiplier;
        return Math.max(1, (int)Math.round(seconds));
    }
}
