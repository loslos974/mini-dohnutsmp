package com.example.spawner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

final class ItemStackCodec {

   private ItemStackCodec() {
   }

   static byte[] serialize(List<ItemStack> items) {
      List<ItemStack> compact = new ArrayList<>();

      for (ItemStack item : items) {
         if (item != null && !item.getType().isAir()) {
            compact.add(item.clone());
         }
      }

      try (ByteArrayOutputStream output = new ByteArrayOutputStream();
           BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(output)) {
         dataOutput.writeInt(compact.size());

         for (ItemStack item : compact) {
            dataOutput.writeObject(item.serialize());
         }

         return output.toByteArray();
      } catch (IOException ex) {
         return new byte[0];
      }
   }

   @SuppressWarnings("unchecked")
   static List<ItemStack> deserialize(byte[] bytes) {
      List<ItemStack> items = new ArrayList<>();
      if (bytes == null || bytes.length == 0) {
         return items;
      }

      try (ByteArrayInputStream input = new ByteArrayInputStream(bytes);
           BukkitObjectInputStream dataInput = new BukkitObjectInputStream(input)) {
         int size = dataInput.readInt();

         for (int i = 0; i < size; ++i) {
            Object raw = dataInput.readObject();
            if (raw instanceof Map) {
               ItemStack item = ItemStack.deserialize((Map<String, Object>)raw);
               if (item != null && !item.getType().isAir()) {
                  items.add(item);
               }
            }
         }
      } catch (IOException | ClassNotFoundException ex) {
         return new ArrayList<>();
      }

      return items;
   }
}
