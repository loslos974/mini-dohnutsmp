package com.example;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class EconomyPrices {
   public static final double TOKEN_BUY_PRICE = 15000.0;
   public static final double TOKEN_SELL_PRICE = 5000.0;
   public static final double TOKEN_CASH_EQUIVALENT = 10000.0;

   public enum PriceUnit {
      PER_ITEM,
      PER_STACK
   }

   public enum ShopCategory {
      FARMING("§aFarm Shop", Material.WHEAT),
      MOB_DROPS("§cMob Drops Shop", Material.BONE),
      COMBAT("§4Combat Shop", Material.OBSIDIAN),
      ORES("§bOre Shop", Material.DIAMOND),
      BUILDING("§7Building Shop", Material.COBBLESTONE);

      private final String title;
      private final Material icon;

      ShopCategory(String title, Material icon) {
         this.title = title;
         this.icon = icon;
      }

      public String title() {
         return this.title;
      }

      public Material icon() {
         return this.icon;
      }
   }

   public record PriceEntry(double buy, double sell, PriceUnit unit) {
   }

   public static final String TITLE_ITEM_SHOP = "§2Item Shop";
   public static final String TITLE_TOKEN_PREMIUM = "§dPremium Token Shop";
   public static final String TITLE_SHOP_CHECKOUT = "§e§lCheckout";

   private static final Map<Material, PriceEntry> PRICES = new EnumMap<>(Material.class);
   private static final Map<ShopCategory, Material[]> CATEGORY_ITEMS = new LinkedHashMap<>();

   static {
      stack(Material.NETHER_WART, 10000, 5000);
      stack(Material.SUGAR_CANE, 5000, 2500);
      stack(Material.KELP, 1200, 600);
      stack(Material.MELON_SLICE, 600, 300);
      stack(Material.PUMPKIN, 900, 450);
      stack(Material.SWEET_BERRIES, 1200, 300);
      stack(Material.WHEAT, 1200, 300);
      stack(Material.CARROT, 900, 225);
      stack(Material.POTATO, 900, 225);
      stack(Material.CACTUS, 1200, 600);
      stack(Material.BAMBOO, 600, 150);

      stack(Material.BONE, 12000, 6000);
      stack(Material.GUNPOWDER, 24000, 3000);
      stack(Material.STRING, 6000, 3000);
      stack(Material.ROTTEN_FLESH, 2400, 1200);
      stack(Material.ARROW, 4800, 1200);
      stack(Material.BLAZE_ROD, 20000, 10000);
      stack(Material.ENDER_PEARL, 12000, 1500);
      stack(Material.SPIDER_EYE, 4800, 1200);
      stack(Material.SLIME_BALL, 24000, 6000);

      each(Material.OBSIDIAN, 400, 40);
      each(Material.CRYING_OBSIDIAN, 2000, 80);
      each(Material.END_CRYSTAL, 4500, 0);
      each(Material.RESPAWN_ANCHOR, 12000, 0);
      each(Material.GLOWSTONE, 800, 80);
      each(Material.GOLDEN_APPLE, 4000, 400);
      each(Material.ENCHANTED_GOLDEN_APPLE, 85000, 8500);
      each(Material.TOTEM_OF_UNDYING, 200000, 0);
      each(Material.EXPERIENCE_BOTTLE, 400, 0);

      each(Material.NETHERITE_INGOT, 850000, 85000);
      each(Material.ANCIENT_DEBRIS, 200000, 20000);
      each(Material.DIAMOND, 8000, 800);
      each(Material.EMERALD, 4000, 400);
      each(Material.GOLD_INGOT, 2000, 200);
      each(Material.IRON_INGOT, 800, 80);
      each(Material.COAL, 150, 15);

      stack(Material.COBBLESTONE, 250, 0);
      stack(Material.DIRT, 120, 0);
      stack(Material.SAND, 250, 0);
      stack(Material.OAK_LOG, 1000, 125);
      stack(Material.SPRUCE_LOG, 1000, 125);
      stack(Material.GLASS, 500, 0);
      stack(Material.END_STONE, 500, 0);

      CATEGORY_ITEMS.put(ShopCategory.FARMING, new Material[]{
         Material.NETHER_WART, Material.SUGAR_CANE, Material.KELP, Material.MELON_SLICE, Material.PUMPKIN,
         Material.SWEET_BERRIES, Material.WHEAT, Material.CARROT, Material.POTATO, Material.CACTUS, Material.BAMBOO
      });
      CATEGORY_ITEMS.put(ShopCategory.MOB_DROPS, new Material[]{
         Material.BONE, Material.GUNPOWDER, Material.STRING, Material.ROTTEN_FLESH, Material.ARROW,
         Material.BLAZE_ROD, Material.ENDER_PEARL, Material.SPIDER_EYE, Material.SLIME_BALL
      });
      CATEGORY_ITEMS.put(ShopCategory.COMBAT, new Material[]{
         Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.END_CRYSTAL, Material.RESPAWN_ANCHOR,
         Material.GLOWSTONE, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.TOTEM_OF_UNDYING,
         Material.EXPERIENCE_BOTTLE
      });
      CATEGORY_ITEMS.put(ShopCategory.ORES, new Material[]{
         Material.NETHERITE_INGOT, Material.ANCIENT_DEBRIS, Material.DIAMOND, Material.EMERALD,
         Material.GOLD_INGOT, Material.IRON_INGOT, Material.COAL
      });
      CATEGORY_ITEMS.put(ShopCategory.BUILDING, new Material[]{
         Material.COBBLESTONE, Material.DIRT, Material.SAND, Material.OAK_LOG, Material.SPRUCE_LOG,
         Material.GLASS, Material.END_STONE
      });
   }

   private EconomyPrices() {
   }

   private static void stack(Material material, double buyPerStack, double sellPerStack) {
      PRICES.put(material, new PriceEntry(buyPerStack, sellPerStack, PriceUnit.PER_STACK));
   }

   private static void each(Material material, double buyEach, double sellEach) {
      PRICES.put(material, new PriceEntry(buyEach, sellEach, PriceUnit.PER_ITEM));
   }

   public static PriceEntry getEntry(Material material) {
      return PRICES.get(material);
   }

   public static boolean isListed(Material material) {
      return PRICES.containsKey(material);
   }

   public static boolean canSell(Material material) {
      PriceEntry entry = PRICES.get(material);
      return entry != null && entry.sell() > 0.0;
   }

   public static boolean canBuy(Material material) {
      PriceEntry entry = PRICES.get(material);
      return entry != null && entry.buy() > 0.0;
   }

   public static double getSellPricePerItem(Material material) {
      PriceEntry entry = PRICES.get(material);
      if (entry != null && entry.sell() > 0.0) {
         return entry.unit() == PriceUnit.PER_STACK ? entry.sell() / 64.0 : entry.sell();
      } else {
         return 0.0;
      }
   }

   public static double getBuyPriceForAmount(Material material, int amount) {
      PriceEntry entry = PRICES.get(material);
      if (entry != null && entry.buy() > 0.0 && amount > 0) {
         return entry.unit() == PriceUnit.PER_STACK ? entry.buy() * (amount / 64.0) : entry.buy() * amount;
      } else {
         return 0.0;
      }
   }

   public static int defaultPurchaseAmount(Material material) {
      PriceEntry entry = PRICES.get(material);
      return entry != null && entry.unit() == PriceUnit.PER_STACK ? 64 : 1;
   }

   public static int maxAffordableAmount(Material material, double balance) {
      PriceEntry entry = PRICES.get(material);
      if (entry != null && entry.buy() > 0.0 && balance > 0.0) {
         if (entry.unit() == PriceUnit.PER_STACK) {
            return (int)(balance / (entry.buy() / 64.0));
         } else {
            return (int)(balance / entry.buy());
         }
      } else {
         return 0;
      }
   }

   public static Material[] getCategoryItems(ShopCategory category) {
      return CATEGORY_ITEMS.get(category);
   }

   public static double getQuestReward(int level) {
      switch (level) {
         case 1 -> {
            return 20000.0;
         }
         case 2 -> {
            return 25000.0;
         }
         case 3 -> {
            return 100000.0;
         }
         case 4 -> {
            return 125000.0;
         }
         case 5 -> {
            return 750000.0;
         }
         default -> {
            return 0.0;
         }
      }
   }

   public static String formatMoney(double amount) {
      if (amount >= 1000.0) {
         return String.format("$%,.0f", amount);
      } else {
         return String.format("$%.2f", amount);
      }
   }

   public static List<String> buildPriceLore(Material material) {
      List<String> lore = new ArrayList();
      PriceEntry entry = PRICES.get(material);
      if (entry == null) {
         return lore;
      } else {
         lore.add("§8§m----------------");
         if (entry.unit() == PriceUnit.PER_STACK) {
            lore.add("§7Buy Price: §c" + formatMoney(entry.buy()) + " §7/ stack");
            if (entry.sell() > 0.0) {
               lore.add("§7Sell Price: §a" + formatMoney(entry.sell()) + " §7/ stack");
               lore.add("§7Each sell: §a" + formatMoney(entry.sell() / 64.0));
            } else {
               lore.add("§c[SELLING DISABLED]");
            }
         } else {
            lore.add("§7Buy Price: §c" + formatMoney(entry.buy()) + " §7each");
            if (entry.sell() > 0.0) {
               lore.add("§7Sell Price: §a" + formatMoney(entry.sell()) + " §7each");
            } else {
               lore.add("§c[SELLING DISABLED]");
            }
         }

         lore.add("§8§m----------------");
         return lore;
      }
   }

   public static void setShopPriceLore(ItemStack item) {
      if (item != null && item.getType() != Material.AIR && isListed(item.getType())) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList(meta.getLore()) : new ArrayList();
            lore.removeIf((line) -> line.contains("Buy Price:") || line.contains("Sell Price:") || line.contains("[SELLING DISABLED]") || line.contains("Each sell:") || line.contains("§8§m----------------"));
            lore.addAll(buildPriceLore(item.getType()));
            meta.setLore(lore);
            item.setItemMeta(meta);
         }
      }
   }

   public static void applyPriceLore(ItemStack item) {
      setShopPriceLore(item);
   }
}
