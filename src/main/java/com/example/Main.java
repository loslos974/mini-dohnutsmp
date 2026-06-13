package com.example;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import com.example.spawner.VirtualSpawnerManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
   public final Map<UUID, Double> balances = new HashMap();
   public final Map<UUID, Integer> tokens = new HashMap();
   public final Map<UUID, EnumMap<CrateType, Integer>> crateKeys = new HashMap();
   public final Map<UUID, CaseRollSession> caseRollSessions = new HashMap();
   public final Map<UUID, Integer> playerLevels = new HashMap();
   public final Map<UUID, Integer> questProgress = new HashMap();
   public final Set<UUID> activeQuest = new HashSet();
   public final List<AuctionListing> auctionItems = new ArrayList();
   public final Map<UUID, ItemStack> pendingAuctionSell = new HashMap();
   public final Map<UUID, PlayerHome[]> playerHomes = new HashMap();
   public final Map<UUID, Set<Integer>> unlockedHomeSlots = new HashMap();
   public final Map<UUID, PendingHomeAction> pendingHomeActions = new HashMap();
   public final Map<UUID, PendingShopPurchase> pendingShopPurchases = new HashMap();
   public final Map<UUID, EconomyPrices.ShopCategory> lastShopCategory = new HashMap();
   public static final int HOME_SLOT_COUNT = 5;
   public static final int FREE_HOME_SLOTS = 2;
   public static final int HOME_UNLOCK_COST = 35;
   private File dataFile;
   private FileConfiguration dataConfig;
   private CommandHandler commandHandler;
   private VirtualSpawnerManager virtualSpawnerManager;

   public void onEnable() {
      this.createDataConfig();
      this.loadData();
      this.commandHandler = new CommandHandler(this);
      ((PluginCommand)Objects.requireNonNull(this.getCommand("pay"))).setExecutor(this.commandHandler);
      ((PluginCommand)Objects.requireNonNull(this.getCommand("money"))).setExecutor(this.commandHandler);
      ((PluginCommand)Objects.requireNonNull(this.getCommand("menu"))).setExecutor(this.commandHandler);
      ((PluginCommand)Objects.requireNonNull(this.getCommand("grant"))).setExecutor(this.commandHandler);
      this.getServer().getPluginManager().registerEvents(new MenuListener(this, this.commandHandler), this);
      this.virtualSpawnerManager = new VirtualSpawnerManager(this);
      this.virtualSpawnerManager.enable();
      this.getLogger().info("Economy Pro V6 - Farming & Lore Active!");
   }

   public double getMultiplier(UUID uuid) {
      int level = (Integer)this.playerLevels.getOrDefault(uuid, 1);
      return (double)1.0F + (double)(level - 1) * (double)1.0F;
   }

   public void onDisable() {
      for(Map.Entry<UUID, CaseRollSession> e : new HashMap<>(this.caseRollSessions).entrySet()) {
         CaseRollSession session = (CaseRollSession)e.getValue();
         if (session.task != null) {
            session.task.cancel();
         }

         if (!session.revealDone) {
            this.addCrateKey((UUID)e.getKey(), session.crateType, 1);
         }
      }

      this.caseRollSessions.clear();
      if (this.virtualSpawnerManager != null) {
         this.virtualSpawnerManager.disable();
      }

      this.saveData();
   }

   public int getCrateKeyCount(UUID uuid, CrateType type) {
      EnumMap<CrateType, Integer> keys = this.crateKeys.get(uuid);
      return keys == null ? 0 : keys.getOrDefault(type, 0);
   }

   public void addCrateKey(UUID uuid, CrateType type, int amount) {
      if (amount > 0) {
         this.crateKeys.computeIfAbsent(uuid, (k) -> new EnumMap<>(CrateType.class)).merge(type, amount, Integer::sum);
      }

   }

   public boolean takeCrateKey(UUID uuid, CrateType type) {
      EnumMap<CrateType, Integer> keys = this.crateKeys.get(uuid);
      if (keys == null) {
         return false;
      } else {
         int count = keys.getOrDefault(type, 0);
         if (count < 1) {
            return false;
         } else {
            if (count <= 1) {
               keys.remove(type);
            } else {
               keys.put(type, count - 1);
            }

            if (keys.isEmpty()) {
               this.crateKeys.remove(uuid);
            }

            return true;
         }
      }
   }

   private void createDataConfig() {
      this.dataFile = new File(this.getDataFolder(), "data.yml");
      if (!this.dataFile.exists()) {
         this.dataFile.getParentFile().mkdirs();

         try {
            this.dataFile.createNewFile();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      this.dataConfig = YamlConfiguration.loadConfiguration(this.dataFile);
   }

   public void loadData() {
      if (this.dataConfig.contains("balances")) {
         ConfigurationSection balancesSection = this.dataConfig.getConfigurationSection("balances");
         if (balancesSection != null) {
            for(String key : balancesSection.getKeys(false)) {
               this.balances.put(UUID.fromString(key), this.dataConfig.getDouble("balances." + key));
            }
         }
      }

      if (this.dataConfig.contains("levels")) {
         ConfigurationSection levelsSection = this.dataConfig.getConfigurationSection("levels");
         if (levelsSection != null) {
            for(String key : levelsSection.getKeys(false)) {
               this.playerLevels.put(UUID.fromString(key), this.dataConfig.getInt("levels." + key));
            }
         }
      }

      if (this.dataConfig.contains("tokens")) {
         ConfigurationSection tokensSection = this.dataConfig.getConfigurationSection("tokens");
         if (tokensSection != null) {
            for(String key : tokensSection.getKeys(false)) {
               this.tokens.put(UUID.fromString(key), this.dataConfig.getInt("tokens." + key));
            }
         }
      }

      ConfigurationSection crateSection = this.dataConfig.getConfigurationSection("crateKeys");
      if (crateSection != null) {
         for(String uuidKey : crateSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidKey);
            ConfigurationSection playerCrates = crateSection.getConfigurationSection(uuidKey);
            if (playerCrates != null) {
               EnumMap<CrateType, Integer> map = new EnumMap<>(CrateType.class);

               for(CrateType type : CrateType.values()) {
                  int count = playerCrates.getInt(type.name(), 0);
                  if (count > 0) {
                     map.put(type, count);
                  }
               }

               if (!map.isEmpty()) {
                  this.crateKeys.put(uuid, map);
               }
            }
         }
      } else {
         this.migrateLegacyCrateKeys();
      }
      if (this.dataConfig.contains("questProgress")) {
         ConfigurationSection questSection = this.dataConfig.getConfigurationSection("questProgress");
         if (questSection != null) {
            for(String key : questSection.getKeys(false)) {
               this.questProgress.put(UUID.fromString(key), this.dataConfig.getInt("questProgress." + key));
            }
         }
      }

      for(String uuidStr : this.dataConfig.getStringList("activeQuest")) {
         try {
            this.activeQuest.add(UUID.fromString(uuidStr));
         } catch (IllegalArgumentException var6) {
            this.getLogger().warning("Invalid UUID in activeQuest: " + uuidStr);
         }
      }

      ConfigurationSection auctionSection = this.dataConfig.getConfigurationSection("auction");
      if (auctionSection != null) {
         for(String key : auctionSection.getKeys(false)) {
            ConfigurationSection listingSection = auctionSection.getConfigurationSection(key);
            AuctionListing listing = AuctionListing.fromSection(listingSection);
            if (listing != null) {
               this.auctionItems.add(listing);
            }
         }
      }

      ConfigurationSection homesSection = this.dataConfig.getConfigurationSection("homes");
      if (homesSection != null) {
         for(String uuidKey : homesSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidKey);
            ConfigurationSection playerSection = homesSection.getConfigurationSection(uuidKey);
            if (playerSection != null) {
               PlayerHome[] homes = new PlayerHome[HOME_SLOT_COUNT];

               for(int i = 0; i < HOME_SLOT_COUNT; ++i) {
                  homes[i] = PlayerHome.fromSection(playerSection.getConfigurationSection("slot" + i));
               }

               this.playerHomes.put(uuid, homes);
            }
         }
      }

      ConfigurationSection unlockedSection = this.dataConfig.getConfigurationSection("unlockedHomes");
      if (unlockedSection != null) {
         for(String uuidKey : unlockedSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidKey);
            List<Integer> slots = unlockedSection.getIntegerList(uuidKey);
            Set<Integer> set = new HashSet();

            for(Integer slot : slots) {
               if (slot != null && slot >= FREE_HOME_SLOTS && slot < HOME_SLOT_COUNT) {
                  set.add(slot);
               }
            }

            if (!set.isEmpty()) {
               this.unlockedHomeSlots.put(uuid, set);
            }
         }
      }

   }

   public boolean isHomeSlotUnlocked(UUID uuid, int index) {
      return index < FREE_HOME_SLOTS || this.unlockedHomeSlots.getOrDefault(uuid, Collections.emptySet()).contains(index);
   }

   public void unlockHomeSlot(UUID uuid, int index) {
      if (index >= FREE_HOME_SLOTS && index < HOME_SLOT_COUNT) {
         this.unlockedHomeSlots.computeIfAbsent(uuid, (k) -> new HashSet()).add(index);
      }

   }

   public PlayerHome getHome(UUID uuid, int index) {
      PlayerHome[] homes = this.playerHomes.get(uuid);
      return homes != null && index >= 0 && index < homes.length ? homes[index] : null;
   }

   public void setHome(UUID uuid, int index, PlayerHome home) {
      PlayerHome[] homes = this.playerHomes.computeIfAbsent(uuid, (k) -> new PlayerHome[HOME_SLOT_COUNT]);
      homes[index] = home;
   }

   public void clearHome(UUID uuid, int index) {
      PlayerHome[] homes = this.playerHomes.get(uuid);
      if (homes != null && index >= 0 && index < homes.length) {
         homes[index] = null;
      }

   }

   private void migrateLegacyCrateKeys() {
      Map<UUID, Integer> legacy1 = new HashMap();
      Map<UUID, Integer> legacy2 = new HashMap();
      Map<UUID, Integer> legacy3 = new HashMap();
      this.loadIntMap("trailKeys1", legacy1);
      this.loadIntMap("trailKeys2", legacy2);
      this.loadIntMap("ominousKeys", legacy3);

      for(Map.Entry<UUID, Integer> entry : legacy1.entrySet()) {
         this.addCrateKey(entry.getKey(), CrateType.AGRICULTURE, entry.getValue());
      }

      for(Map.Entry<UUID, Integer> entry : legacy2.entrySet()) {
         this.addCrateKey(entry.getKey(), CrateType.COMBAT, entry.getValue());
      }

      for(Map.Entry<UUID, Integer> entry : legacy3.entrySet()) {
         this.addCrateKey(entry.getKey(), CrateType.MINERAL, entry.getValue());
      }
   }

   private void loadIntMap(String path, Map<UUID, Integer> map) {
      ConfigurationSection section = this.dataConfig.getConfigurationSection(path);
      if (section != null) {
         for(String key : section.getKeys(false)) {
            map.put(UUID.fromString(key), this.dataConfig.getInt(path + "." + key));
         }
      }

   }

   public double getItemPrice(Material mat) {
      return EconomyPrices.getSellPricePerItem(mat);
   }

   public void saveData() {
      for(Map.Entry<UUID, Double> entry : this.balances.entrySet()) {
         this.dataConfig.set("balances." + ((UUID)entry.getKey()).toString(), entry.getValue());
      }

      for(Map.Entry<UUID, Integer> entry : this.playerLevels.entrySet()) {
         this.dataConfig.set("levels." + ((UUID)entry.getKey()).toString(), entry.getValue());
      }

      for(Map.Entry<UUID, Integer> entry : this.tokens.entrySet()) {
         this.dataConfig.set("tokens." + ((UUID)entry.getKey()).toString(), entry.getValue());
      }

      this.dataConfig.set("crateKeys", (Object)null);

      for(Map.Entry<UUID, EnumMap<CrateType, Integer>> entry : this.crateKeys.entrySet()) {
         for(Map.Entry<CrateType, Integer> crateEntry : entry.getValue().entrySet()) {
            this.dataConfig.set("crateKeys." + entry.getKey() + "." + crateEntry.getKey().name(), crateEntry.getValue());
         }
      }

      for(Map.Entry<UUID, Integer> entry : this.questProgress.entrySet()) {
         this.dataConfig.set("questProgress." + ((UUID)entry.getKey()).toString(), entry.getValue());
      }

      List<String> active = new ArrayList();

      for(UUID uuid : this.activeQuest) {
         active.add(uuid.toString());
      }

      this.dataConfig.set("activeQuest", active);
      this.dataConfig.set("auction", (Object)null);

      for(int i = 0; i < this.auctionItems.size(); ++i) {
         AuctionListing listing = (AuctionListing)this.auctionItems.get(i);
         String path = "auction." + i;
         this.dataConfig.set(path + ".seller", listing.seller().toString());
         this.dataConfig.set(path + ".price", listing.price());
         this.dataConfig.set(path + ".item", listing.item());
      }

      this.dataConfig.set("homes", (Object)null);

      for(Map.Entry<UUID, PlayerHome[]> entry : this.playerHomes.entrySet()) {
         PlayerHome[] homes = entry.getValue();
         if (homes != null) {
            String base = "homes." + entry.getKey();

            for(int i = 0; i < homes.length; ++i) {
               PlayerHome home = homes[i];
               if (home != null) {
                  String path = base + ".slot" + i;
                  this.dataConfig.set(path + ".world", home.world());
                  this.dataConfig.set(path + ".x", home.x());
                  this.dataConfig.set(path + ".y", home.y());
                  this.dataConfig.set(path + ".z", home.z());
                  this.dataConfig.set(path + ".yaw", home.yaw());
                  this.dataConfig.set(path + ".pitch", home.pitch());
               }
            }
         }
      }

      this.dataConfig.set("unlockedHomes", (Object)null);

      for(Map.Entry<UUID, Set<Integer>> entry : this.unlockedHomeSlots.entrySet()) {
         List<Integer> slots = new ArrayList();

         for(Integer slot : entry.getValue()) {
            slots.add(slot);
         }

         this.dataConfig.set("unlockedHomes." + entry.getKey().toString(), slots);
      }

      try {
         this.dataConfig.save(this.dataFile);
      } catch (IOException e) {
         e.printStackTrace();
      }

   }

   public VirtualSpawnerManager getVirtualSpawnerManager() {
      return this.virtualSpawnerManager;
   }

   private void saveIntMap(String path, Map<UUID, Integer> map) {
      for(Map.Entry<UUID, Integer> entry : map.entrySet()) {
         this.dataConfig.set(path + "." + ((UUID)entry.getKey()).toString(), entry.getValue());
      }

   }
}
