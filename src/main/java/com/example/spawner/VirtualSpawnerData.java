package com.example.spawner;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public final class VirtualSpawnerData {

   private EntityType mobType;
   private int stackSize;
   private int speedLevel;
   private int amountLevel;
   private final List<ItemStack> storedItems;

   public VirtualSpawnerData(EntityType mobType) {
      this(mobType, 1, 0, 0, new ArrayList<>());
   }

   public VirtualSpawnerData(EntityType mobType, int stackSize, int speedLevel, int amountLevel, List<ItemStack> storedItems) {
      this.mobType = mobType;
      this.stackSize = Math.max(1, stackSize);
      this.speedLevel = Math.max(0, speedLevel);
      this.amountLevel = Math.max(0, amountLevel);
      this.storedItems = storedItems == null ? new ArrayList<>() : new ArrayList<>(storedItems);
   }

   public EntityType getMobType() {
      return this.mobType;
   }

   public void setMobType(EntityType mobType) {
      this.mobType = mobType;
   }

   public int getStackSize() {
      return this.stackSize;
   }

   public void setStackSize(int stackSize) {
      this.stackSize = Math.max(1, stackSize);
   }

   public int getSpeedLevel() {
      return this.speedLevel;
   }

   public void setSpeedLevel(int speedLevel) {
      this.speedLevel = Math.max(0, speedLevel);
   }

   public int getAmountLevel() {
      return this.amountLevel;
   }

   public void setAmountLevel(int amountLevel) {
      this.amountLevel = Math.max(0, amountLevel);
   }

   public List<ItemStack> getStoredItems() {
      return this.storedItems;
   }

   public VirtualSpawnerData copy() {
      List<ItemStack> cloned = new ArrayList<>();

      for (ItemStack item : this.storedItems) {
         if (item != null && !item.getType().isAir()) {
            cloned.add(item.clone());
         }
      }

      return new VirtualSpawnerData(this.mobType, this.stackSize, this.speedLevel, this.amountLevel, cloned);
   }
}
