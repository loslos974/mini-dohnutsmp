package com.example;

import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public record AuctionListing(UUID seller, double price, ItemStack item) {
   public static AuctionListing fromSection(ConfigurationSection section) {
      if (section == null) {
         return null;
      } else {
         String sellerStr = section.getString("seller");
         ItemStack item = section.getItemStack("item");
         double price = section.getDouble("price", (double)-1.0F);
         if (sellerStr != null && item != null && !(price <= (double)0.0F)) {
            try {
               return new AuctionListing(UUID.fromString(sellerStr), price, item);
            } catch (IllegalArgumentException var6) {
               return null;
            }
         } else {
            return null;
         }
      }
   }
}
