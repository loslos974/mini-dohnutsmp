package com.example;

import com.example.spawner.VirtualSpawnerData;
import com.example.spawner.VirtualSpawnerStorage;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.CreatureSpawner;

public enum TokenPremiumItems {
   ZOMBIE_SPAWNER("zombie_spawner", Material.SPAWNER, "§2Zombie Spawner", 100, EntityType.ZOMBIE),
   SPIDER_SPAWNER("spider_spawner", Material.SPAWNER, "§8Spider Spawner", 150, EntityType.SPIDER),
   SKELETON_SPAWNER("skeleton_spawner", Material.SPAWNER, "§fSkeleton Spawner", 250, EntityType.SKELETON),
   BLAZE_SPAWNER("blaze_spawner", Material.SPAWNER, "§6Blaze Spawner", 500, EntityType.BLAZE),
   IRON_GOLEM_SPAWNER("iron_golem_spawner", Material.SPAWNER, "§7Iron Golem Spawner", 1000, EntityType.IRON_GOLEM),
   GOD_APPLE("god_apple", Material.ENCHANTED_GOLDEN_APPLE, "§6God Apple", 10, null),
   TOTEM("totem", Material.TOTEM_OF_UNDYING, "§eTotem of Undying", 25, null),
   NETHERITE_INGOT("netherite_ingot", Material.NETHERITE_INGOT, "§5Netherite Ingot", 100, null),
   NETHERITE_SET("netherite_set", Material.NETHERITE_CHESTPLATE, "§bProt IV Netherite Set", 500, null);

   private final String id;
   private final Material material;
   private final String displayName;
   private final int tokenCost;
   private final EntityType spawnerType;

   TokenPremiumItems(String id, Material material, String displayName, int tokenCost, EntityType spawnerType) {
      this.id = id;
      this.material = material;
      this.displayName = displayName;
      this.tokenCost = tokenCost;
      this.spawnerType = spawnerType;
   }

   public int tokenCost() {
      return this.tokenCost;
   }

   public double cashEquivalent() {
      return (double)this.tokenCost * EconomyPrices.TOKEN_CASH_EQUIVALENT;
   }

   public ItemStack createDisplayItem() {
      ItemStack item = new ItemStack(this.material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(this.displayName);
         List<String> lore = new ArrayList();
         lore.add("§7Cash value: §6" + EconomyPrices.formatMoney(this.cashEquivalent()));
         lore.add("§7Cost: §b" + this.tokenCost + " tokens");
         lore.add("");
         lore.add("§eClick to purchase");
         meta.setLore(lore);
         item.setItemMeta(meta);
      }

      if (this.spawnerType != null) {
         ItemMeta updated = item.getItemMeta();
         if (updated instanceof BlockStateMeta blockMeta) {
            CreatureSpawner spawner = (CreatureSpawner)blockMeta.getBlockState();
            spawner.setSpawnedType(this.spawnerType);
            blockMeta.setBlockState(spawner);
            item.setItemMeta(blockMeta);
         }
      }

      return item;
   }

   public static TokenPremiumItems fromSlot(int slot) {
      int[] slots = new int[]{10, 11, 12, 13, 14, 19, 20, 21, 22};
      TokenPremiumItems[] items = values();

      for(int i = 0; i < slots.length && i < items.length; ++i) {
         if (slots[i] == slot) {
            return items[i];
         }
      }

      return null;
   }

   public List<ItemStack> createRewards() {
      List<ItemStack> rewards = new ArrayList();
      switch (this.ordinal()) {
         case 0:
         case 1:
         case 2:
         case 3:
         case 4:
            rewards.add(this.createSpawner(this.spawnerType));
            break;
         case 5:
            rewards.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
            break;
         case 6:
            rewards.add(new ItemStack(Material.TOTEM_OF_UNDYING));
            break;
         case 7:
            rewards.add(new ItemStack(Material.NETHERITE_INGOT));
            break;
         default:
            rewards.add(this.createEnchantedArmor(Material.NETHERITE_HELMET));
            rewards.add(this.createEnchantedArmor(Material.NETHERITE_CHESTPLATE));
            rewards.add(this.createEnchantedArmor(Material.NETHERITE_LEGGINGS));
            rewards.add(this.createEnchantedArmor(Material.NETHERITE_BOOTS));
      }

      return rewards;
   }

   private ItemStack createSpawner(EntityType type) {
      Main plugin = (Main)org.bukkit.Bukkit.getPluginManager().getPlugin("EconomyPlugin");
      if (plugin != null && plugin.getVirtualSpawnerManager() != null) {
         VirtualSpawnerStorage storage = plugin.getVirtualSpawnerManager().getStorage();
         return storage.createSpawnerItem(new VirtualSpawnerData(type));
      }

      ItemStack spawner = new ItemStack(Material.SPAWNER);
      ItemMeta meta = spawner.getItemMeta();
      if (meta instanceof BlockStateMeta blockMeta) {
         CreatureSpawner state = (CreatureSpawner)blockMeta.getBlockState();
         state.setSpawnedType(type);
         blockMeta.setBlockState(state);
         spawner.setItemMeta(blockMeta);
      }

      return spawner;
   }

   private ItemStack createEnchantedArmor(Material material) {
      ItemStack armor = new ItemStack(material);
      ItemMeta meta = armor.getItemMeta();
      if (meta != null) {
         meta.addEnchant(Enchantment.PROTECTION, 4, true);
         meta.addEnchant(Enchantment.UNBREAKING, 3, true);
         armor.setItemMeta(meta);
      }

      return armor;
   }
}
