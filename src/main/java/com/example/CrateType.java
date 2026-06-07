package com.example;

import org.bukkit.Material;

public enum CrateType {
   AGRICULTURE(1, "§aAgriculture Crate", Material.WHEAT, Material.TRIAL_KEY, 18),
   COMBAT(2, "§cCombat Crate", Material.DIAMOND_SWORD, Material.TRIAL_KEY, 45),
   MINERAL(3, "§bMineral Crate", Material.DIAMOND, Material.OMINOUS_TRIAL_KEY, 85),
   SPAWNER(4, "§5Spawner Crate", Material.SPAWNER, Material.OMINOUS_TRIAL_KEY, 175);

   private final int id;
   private final String displayName;
   private final Material icon;
   private final Material keyMaterial;
   private final int tokenCost;

   CrateType(int id, String displayName, Material icon, Material keyMaterial, int tokenCost) {
      this.id = id;
      this.displayName = displayName;
      this.icon = icon;
      this.keyMaterial = keyMaterial;
      this.tokenCost = tokenCost;
   }

   public int id() {
      return this.id;
   }

   public String displayName() {
      return this.displayName;
   }

   public Material icon() {
      return this.icon;
   }

   public Material keyMaterial() {
      return this.keyMaterial;
   }

   public int tokenCost() {
      return this.tokenCost;
   }

   public String keyLabel() {
      return this.displayName.replace(" Crate", " Key");
   }

   public static CrateType fromId(int id) {
      for(CrateType type : values()) {
         if (type.id == id) {
            return type;
         }
      }

      return null;
   }

   public static CrateType fromLegacyTier(int tier) {
      return switch (tier) {
         case 1 -> AGRICULTURE;
         case 2 -> COMBAT;
         case 3 -> MINERAL;
         default -> null;
      };
   }

   public static CrateType fromShopSlot(int slot) {
      return switch (slot) {
         case 20 -> AGRICULTURE;
         case 22 -> COMBAT;
         case 24 -> MINERAL;
         case 31 -> SPAWNER;
         default -> null;
      };
   }

   public static CrateType fromInventorySlot(int slot) {
      return switch (slot) {
         case 20 -> AGRICULTURE;
         case 22 -> COMBAT;
         case 24 -> MINERAL;
         case 31 -> SPAWNER;
         default -> null;
      };
   }
}
