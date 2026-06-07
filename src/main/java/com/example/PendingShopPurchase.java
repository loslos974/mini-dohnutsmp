package com.example;

import org.bukkit.Material;

public class PendingShopPurchase {
   private final Material material;
   private int quantity;
   private final EconomyPrices.ShopCategory category;

   public PendingShopPurchase(Material material, EconomyPrices.ShopCategory category) {
      this.material = material;
      this.quantity = 1;
      this.category = category;
   }

   public Material material() {
      return this.material;
   }

   public int quantity() {
      return this.quantity;
   }

   public void setQuantity(int quantity) {
      this.quantity = Math.max(1, quantity);
   }

   public EconomyPrices.ShopCategory category() {
      return this.category;
   }

   public double totalCost() {
      return EconomyPrices.getBuyPriceForAmount(this.material, this.quantity);
   }
}
