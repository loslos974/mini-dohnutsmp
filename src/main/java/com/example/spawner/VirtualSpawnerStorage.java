package com.example.spawner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TileState;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;

public final class VirtualSpawnerStorage {

   private final VirtualSpawnerKeys keys;

   public VirtualSpawnerStorage(VirtualSpawnerKeys keys) {
      this.keys = keys;
   }

   public boolean isVirtual(PersistentDataHolder holder) {
      if (holder == null) {
         return false;
      }

      Byte flag = holder.getPersistentDataContainer().get(this.keys.virtual(), VirtualSpawnerKeys.FLAG);
      return flag != null && flag == (byte)1;
   }

   public VirtualSpawnerData read(PersistentDataHolder holder) {
      PersistentDataContainer container = holder.getPersistentDataContainer();
      if (!this.isVirtual(holder)) {
         return null;
      }

      String mobName = container.get(this.keys.mobType(), VirtualSpawnerKeys.STRING);
      if (mobName == null) {
         return null;
      }

      EntityType mobType;
      try {
         mobType = EntityType.valueOf(mobName);
      } catch (IllegalArgumentException ex) {
         return null;
      }

      int stackSize = this.getInt(container, this.keys.stackSize(), 1);
      int speedLevel = this.getInt(container, this.keys.speedLevel(), 0);
      int amountLevel = this.getInt(container, this.keys.amountLevel(), 0);
      UUID owner = this.readUuid(container, this.keys.owner());
      List<ItemStack> storedItems = ItemStackCodec.deserialize(container.get(this.keys.storedItems(), VirtualSpawnerKeys.BYTE_ARRAY));
      return new VirtualSpawnerData(mobType, stackSize, speedLevel, amountLevel, storedItems, owner);
   }

   public VirtualSpawnerData readBlock(Block block) {
      if (block == null || block.getType() != Material.SPAWNER) {
         return null;
      }

      BlockState state = block.getState();
      if (state instanceof TileState tileState) {
         return this.read(tileState);
      }

      return null;
   }

   public void write(PersistentDataHolder holder, VirtualSpawnerData data) {
      PersistentDataContainer container = holder.getPersistentDataContainer();
      container.set(this.keys.virtual(), VirtualSpawnerKeys.FLAG, (byte)1);
      container.set(this.keys.mobType(), VirtualSpawnerKeys.STRING, data.getMobType().name());
      container.set(this.keys.stackSize(), VirtualSpawnerKeys.INTEGER, data.getStackSize());
      container.set(this.keys.speedLevel(), VirtualSpawnerKeys.INTEGER, data.getSpeedLevel());
      container.set(this.keys.amountLevel(), VirtualSpawnerKeys.INTEGER, data.getAmountLevel());

      // SnakeYAML/PDC cannot serialize a raw java.util.UUID — always store it as its String form.
      if (data.getOwner() != null) {
         container.set(this.keys.owner(), VirtualSpawnerKeys.STRING, data.getOwner().toString());
      } else {
         container.remove(this.keys.owner());
      }

      container.set(this.keys.storedItems(), VirtualSpawnerKeys.BYTE_ARRAY, ItemStackCodec.serialize(data.getStoredItems()));
   }

   public void writeBlock(Block block, VirtualSpawnerData data) {
      if (block == null || block.getType() != Material.SPAWNER) {
         return;
      }

      BlockState state = block.getState();
      if (!(state instanceof CreatureSpawner spawner)) {
         return;
      }

      this.write(spawner, data);
      spawner.setSpawnedType(data.getMobType());
      spawner.update(true, false);
   }

   public ItemStack createSpawnerItem(VirtualSpawnerData data) {
      ItemStack item = new ItemStack(Material.SPAWNER);
      ItemMeta meta = item.getItemMeta();
      if (!(meta instanceof BlockStateMeta blockMeta)) {
         return item;
      }

      BlockState state = blockMeta.getBlockState();
      if (state instanceof CreatureSpawner spawner) {
         spawner.setSpawnedType(data.getMobType());
         blockMeta.setBlockState(spawner);
      }

      this.write(blockMeta, data);
      blockMeta.setDisplayName("§e" + formatMobName(data.getMobType()) + " Spawner");
      blockMeta.setLore(this.buildItemLore(data));
      item.setItemMeta(blockMeta);
      return item;
   }

   public VirtualSpawnerData readItem(ItemStack item) {
      if (item == null || item.getType() != Material.SPAWNER) {
         return null;
      }

      ItemMeta meta = item.getItemMeta();
      if (!(meta instanceof BlockStateMeta blockMeta)) {
         return null;
      }

      return this.read(blockMeta);
   }

   public boolean isMatchingSpawnerItem(ItemStack item, EntityType mobType) {
      VirtualSpawnerData data = this.readItem(item);
      return data != null && data.getMobType() == mobType;
   }

   private int getInt(PersistentDataContainer container, org.bukkit.NamespacedKey key, int defaultValue) {
      Integer value = container.get(key, VirtualSpawnerKeys.INTEGER);
      return value == null ? defaultValue : value;
   }

   private UUID readUuid(PersistentDataContainer container, org.bukkit.NamespacedKey key) {
      String raw = container.get(key, VirtualSpawnerKeys.STRING);
      if (raw == null || raw.isBlank()) {
         return null;
      }

      try {
         return UUID.fromString(raw);
      } catch (IllegalArgumentException ex) {
         return null;
      }
   }

   private List<String> buildItemLore(VirtualSpawnerData data) {
      List<String> lore = new ArrayList<>();
      lore.add("§7Virtual Spawner");
      lore.add("§7Stacked: §f" + data.getStackSize() + "x");
      lore.add("§7Speed: §c" + data.getSpeedLevel());
      lore.add("§7Amount: §a" + data.getAmountLevel());
      lore.add("");
      lore.add("§eShift + Right-Click to stack");
      return lore;
   }

   public static String formatMobName(EntityType type) {
      String raw = type.name().toLowerCase().replace('_', ' ');
      StringBuilder builder = new StringBuilder();
      boolean capitalize = true;

      for (char c : raw.toCharArray()) {
         if (c == ' ') {
            builder.append(c);
            capitalize = true;
         } else if (capitalize) {
            builder.append(Character.toUpperCase(c));
            capitalize = false;
         } else {
            builder.append(c);
         }
      }

      return builder.toString();
   }
}
