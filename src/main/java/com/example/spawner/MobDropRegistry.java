package com.example.spawner;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public final class MobDropRegistry {

   private static final Map<EntityType, List<DropChance>> DROPS = new EnumMap<>(EntityType.class);
   private static final Random RANDOM = new Random();

   static {
      // Zombie drops
      List<DropChance> zombieDrops = new ArrayList<>();
      zombieDrops.add(new DropChance(Material.ROTTEN_FLESH, 0.7, 1, 3));
      zombieDrops.add(new DropChance(Material.IRON_NUGGET, 0.1, 1, 2));
      zombieDrops.add(new DropChance(Material.CARROT, 0.05, 1, 1));
      zombieDrops.add(new DropChance(Material.POTATO, 0.05, 1, 1));
      DROPS.put(EntityType.ZOMBIE, zombieDrops);

      // Spider drops
      List<DropChance> spiderDrops = new ArrayList<>();
      spiderDrops.add(new DropChance(Material.STRING, 0.6, 1, 3));
      spiderDrops.add(new DropChance(Material.SPIDER_EYE, 0.3, 1, 2));
      DROPS.put(EntityType.SPIDER, spiderDrops);

      // Skeleton drops
      List<DropChance> skeletonDrops = new ArrayList<>();
      skeletonDrops.add(new DropChance(Material.BONE, 0.7, 1, 4));
      skeletonDrops.add(new DropChance(Material.ARROW, 0.5, 1, 4));
      DROPS.put(EntityType.SKELETON, skeletonDrops);

      // Blaze drops
      List<DropChance> blazeDrops = new ArrayList<>();
      blazeDrops.add(new DropChance(Material.BLAZE_ROD, 0.5, 1, 2));
      DROPS.put(EntityType.BLAZE, blazeDrops);

      // Iron Golem drops
      List<DropChance> ironGolemDrops = new ArrayList<>();
      ironGolemDrops.add(new DropChance(Material.IRON_INGOT, 0.4, 3, 6));
      ironGolemDrops.add(new DropChance(Material.POPPY, 0.3, 1, 2));
      DROPS.put(EntityType.IRON_GOLEM, ironGolemDrops);

      // Creeper drops
      List<DropChance> creeperDrops = new ArrayList<>();
      creeperDrops.add(new DropChance(Material.GUNPOWDER, 0.6, 1, 3));
      DROPS.put(EntityType.CREEPER, creeperDrops);

      // Enderman drops
      List<DropChance> endermanDrops = new ArrayList<>();
      endermanDrops.add(new DropChance(Material.ENDER_PEARL, 0.5, 1, 2));
      DROPS.put(EntityType.ENDERMAN, endermanDrops);

      // Cow drops
      List<DropChance> cowDrops = new ArrayList<>();
      cowDrops.add(new DropChance(Material.LEATHER, 0.6, 1, 3));
      cowDrops.add(new DropChance(Material.BEEF, 0.5, 1, 4));
      DROPS.put(EntityType.COW, cowDrops);

      // Pig drops
      List<DropChance> pigDrops = new ArrayList<>();
      pigDrops.add(new DropChance(Material.PORKCHOP, 0.7, 1, 4));
      DROPS.put(EntityType.PIG, pigDrops);

      // Chicken drops
      List<DropChance> chickenDrops = new ArrayList<>();
      chickenDrops.add(new DropChance(Material.FEATHER, 0.6, 1, 3));
      chickenDrops.add(new DropChance(Material.CHICKEN, 0.5, 1, 2));
      DROPS.put(EntityType.CHICKEN, chickenDrops);
   }

   private MobDropRegistry() {
   }

   public static List<DropResult> generateDrops(EntityType type, int amountMultiplier, double chanceMultiplier) {
      List<DropResult> results = new ArrayList<>();
      List<DropChance> dropChances = DROPS.get(type);
      
      if (dropChances == null) {
         return results;
      }

      for (DropChance drop : dropChances) {
         double adjustedChance = Math.min(1.0, drop.chance() * chanceMultiplier);
         if (RANDOM.nextDouble() < adjustedChance) {
            int amount = drop.minAmount() + RANDOM.nextInt(drop.maxAmount() - drop.minAmount() + 1);
            amount = Math.max(1, amount * amountMultiplier);
            results.add(new DropResult(drop.material(), amount));
         }
      }

      return results;
   }

   public static boolean isSupported(EntityType type) {
      return DROPS.containsKey(type);
   }

   public static Material getDrop(EntityType type) {
      List<DropChance> dropChances = DROPS.get(type);
      if (dropChances == null || dropChances.isEmpty()) {
         return Material.AIR;
      }
      return dropChances.get(0).material();
   }

   public record DropChance(Material material, double chance, int minAmount, int maxAmount) {}
   public record DropResult(Material material, int amount) {}
}
