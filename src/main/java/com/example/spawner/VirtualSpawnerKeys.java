package com.example.spawner;

import com.example.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public final class VirtualSpawnerKeys {

   public static final PersistentDataType<Byte, Byte> FLAG = PersistentDataType.BYTE;
   public static final PersistentDataType<Integer, Integer> INTEGER = PersistentDataType.INTEGER;
   public static final PersistentDataType<String, String> STRING = PersistentDataType.STRING;
   public static final PersistentDataType<byte[], byte[]> BYTE_ARRAY = PersistentDataType.BYTE_ARRAY;

   private final NamespacedKey virtual;
   private final NamespacedKey mobType;
   private final NamespacedKey stackSize;
   private final NamespacedKey speedLevel;
   private final NamespacedKey amountLevel;
   private final NamespacedKey owner;
   private final NamespacedKey storedItems;

   public VirtualSpawnerKeys(Main plugin) {
      this.virtual = new NamespacedKey(plugin, "virtual_spawner");
      this.mobType = new NamespacedKey(plugin, "spawner_mob_type");
      this.stackSize = new NamespacedKey(plugin, "spawner_stack_size");
      this.speedLevel = new NamespacedKey(plugin, "spawner_speed_level");
      this.amountLevel = new NamespacedKey(plugin, "spawner_amount_level");
      this.owner = new NamespacedKey(plugin, "spawner_owner");
      this.storedItems = new NamespacedKey(plugin, "spawner_stored_items");
   }

   public NamespacedKey virtual() {
      return this.virtual;
   }

   public NamespacedKey mobType() {
      return this.mobType;
   }

   public NamespacedKey stackSize() {
      return this.stackSize;
   }

   public NamespacedKey speedLevel() {
      return this.speedLevel;
   }

   public NamespacedKey amountLevel() {
      return this.amountLevel;
   }

   public NamespacedKey owner() {
      return this.owner;
   }

   public NamespacedKey storedItems() {
      return this.storedItems;
   }
}
