package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.CreatureSpawner;

public final class CaseLootTable {
   private CaseLootTable() {
   }

   private record WeightedDrop(double weight, Supplier<RollOutcome> outcome) {
   }

   public static RollOutcome roll(CrateType type, Random r) {
      return switch (type) {
         case AGRICULTURE -> rollFromPool(r, agriculturePool());
         case COMBAT -> rollFromPool(r, combatPool());
         case MINERAL -> rollFromPool(r, mineralPool());
         case SPAWNER -> rollFromPool(r, spawnerPool());
      };
   }

   public static List<String> previewLore(CrateType type) {
      List<String> lore = new ArrayList();
      lore.add("§8§m----------------");
      lore.add("§7Possible drops:");

      for(WeightedDrop drop : poolFor(type)) {
         RollOutcome sample = drop.outcome().get();
         String label = describeOutcome(sample);
         lore.add("§7• " + label + " §8(" + formatPercent(drop.weight(), totalWeight(poolFor(type))) + ")");
      }

      lore.add("§8§m----------------");
      return lore;
   }

   private static WeightedDrop[] poolFor(CrateType type) {
      return switch (type) {
         case AGRICULTURE -> agriculturePool();
         case COMBAT -> combatPool();
         case MINERAL -> mineralPool();
         case SPAWNER -> spawnerPool();
      };
   }

   private static RollOutcome rollFromPool(Random r, WeightedDrop[] pool) {
      double total = totalWeight(pool);
      double roll = r.nextDouble() * total;
      double cumulative = 0.0;

      for(WeightedDrop drop : pool) {
         cumulative += drop.weight();
         if (roll < cumulative) {
            return drop.outcome().get();
         }
      }

      return pool[pool.length - 1].outcome().get();
   }

   private static double totalWeight(WeightedDrop[] pool) {
      double total = 0.0;

      for(WeightedDrop drop : pool) {
         total += drop.weight();
      }

      return total;
   }

   private static String formatPercent(double weight, double total) {
      return String.format("%.1f%%", weight / total * 100.0);
   }

   private static String describeOutcome(RollOutcome outcome) {
      if (outcome.money > 0.0) {
         return "§6" + EconomyPrices.formatMoney(outcome.money) + " cash";
      } else {
         ItemStack item = outcome.displayItem;
         int amt = item.getAmount();
         String name = item.getType().name().toLowerCase().replace('_', ' ');
         return amt > 1 ? amt + "x " + name : name;
      }
   }

   private static WeightedDrop[] agriculturePool() {
      return new WeightedDrop[]{
         w(18.0, () -> items(Material.WHEAT, 64)),
         w(18.0, () -> items(Material.CARROT, 64)),
         w(14.0, () -> items(Material.NETHER_WART, 64)),
         w(12.0, () -> items(Material.SUGAR_CANE, 64)),
         w(10.0, () -> items(Material.BONE_MEAL, 64)),
         w(10.0, () -> items(Material.GOLDEN_CARROT, 32)),
         w(8.0, () -> items(Material.HAY_BLOCK, 32)),
         w(5.0, () -> enchantedHoe()),
         w(5.0, () -> money(15000.0, 35000.0))
      };
   }

   private static WeightedDrop[] combatPool() {
      return new WeightedDrop[]{
         w(16.0, () -> items(Material.GOLDEN_APPLE, 16)),
         w(14.0, () -> items(Material.OBSIDIAN, 32)),
         w(12.0, () -> items(Material.END_CRYSTAL, 4)),
         w(12.0, () -> items(Material.GLOWSTONE, 32)),
         w(10.0, () -> enchantedSword(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 3)),
         w(10.0, () -> items(Material.ENDER_PEARL, 16)),
         w(8.0, () -> items(Material.ENCHANTED_GOLDEN_APPLE, 1)),
         w(8.0, () -> enchantedArmor(Material.DIAMOND_CHESTPLATE)),
         w(5.0, () -> items(Material.TOTEM_OF_UNDYING, 1)),
         w(5.0, () -> money(75000.0, 150000.0))
      };
   }

   private static WeightedDrop[] mineralPool() {
      return new WeightedDrop[]{
         w(18.0, () -> items(Material.COAL, 64)),
         w(16.0, () -> items(Material.IRON_INGOT, 32)),
         w(14.0, () -> items(Material.GOLD_INGOT, 24)),
         w(12.0, () -> items(Material.DIAMOND, 16)),
         w(10.0, () -> items(Material.EMERALD, 24)),
         w(8.0, () -> items(Material.ANCIENT_DEBRIS, 4)),
         w(6.0, () -> items(Material.NETHERITE_INGOT, 2)),
         w(4.0, () -> items(Material.NETHERITE_SCRAP, 8)),
         w(2.0, () -> items(Material.NETHERITE_BLOCK, 1)),
         w(10.0, () -> money(500000.0, 1000000.0))
      };
   }

   private static WeightedDrop[] spawnerPool() {
      return new WeightedDrop[]{
         w(30.0, () -> spawner(EntityType.ZOMBIE)),
         w(25.0, () -> spawner(EntityType.SPIDER)),
         w(20.0, () -> spawner(EntityType.SKELETON)),
         w(12.0, () -> spawner(EntityType.BLAZE)),
         w(8.0, () -> spawner(EntityType.CREEPER)),
         w(5.0, () -> spawner(EntityType.IRON_GOLEM))
      };
   }

   private static WeightedDrop w(double weight, Supplier<RollOutcome> outcome) {
      return new WeightedDrop(weight, outcome);
   }

   private static RollOutcome items(Material material, int amount) {
      return new RollOutcome(new ItemStack(material, amount), 0.0);
   }

   private static RollOutcome money(double min, double max) {
      double amount = min + Math.random() * (max - min);
      ItemStack icon = new ItemStack(Material.GOLD_INGOT);
      ItemMeta meta = icon.getItemMeta();
      if (meta != null) {
         meta.setDisplayName("§6§lCash Prize");
         List<String> lore = new ArrayList();
         lore.add("§7You won §6" + EconomyPrices.formatMoney(amount));
         meta.setLore(lore);
         icon.setItemMeta(meta);
      }

      return new RollOutcome(icon, amount);
   }

   private static RollOutcome enchantedHoe() {
      ItemStack hoe = new ItemStack(Material.DIAMOND_HOE);
      ItemMeta meta = hoe.getItemMeta();
      if (meta != null) {
         meta.addEnchant(Enchantment.EFFICIENCY, 4, true);
         meta.addEnchant(Enchantment.UNBREAKING, 2, true);
         hoe.setItemMeta(meta);
      }

      return new RollOutcome(hoe, 0.0);
   }

   private static RollOutcome enchantedSword(Material material, Enchantment enchantment, int level) {
      ItemStack sword = new ItemStack(material);
      ItemMeta meta = sword.getItemMeta();
      if (meta != null) {
         meta.addEnchant(enchantment, level, true);
         meta.addEnchant(Enchantment.UNBREAKING, 2, true);
         sword.setItemMeta(meta);
      }

      return new RollOutcome(sword, 0.0);
   }

   private static RollOutcome enchantedArmor(Material material) {
      ItemStack armor = new ItemStack(material);
      ItemMeta meta = armor.getItemMeta();
      if (meta != null) {
         meta.addEnchant(Enchantment.PROTECTION, 4, true);
         meta.addEnchant(Enchantment.UNBREAKING, 3, true);
         armor.setItemMeta(meta);
      }

      return new RollOutcome(armor, 0.0);
   }

   private static RollOutcome spawner(EntityType type) {
      ItemStack spawner = new ItemStack(Material.SPAWNER);
      ItemMeta meta = spawner.getItemMeta();
      if (meta instanceof BlockStateMeta blockMeta) {
         CreatureSpawner state = (CreatureSpawner)blockMeta.getBlockState();
         state.setSpawnedType(type);
         blockMeta.setBlockState(state);
         spawner.setItemMeta(blockMeta);
      }

      return new RollOutcome(spawner, 0.0);
   }

   public static final class RollOutcome {
      public final ItemStack displayItem;
      public final double money;

      public RollOutcome(ItemStack displayItem, double money) {
         this.displayItem = displayItem;
         this.money = money;
      }
   }
}
