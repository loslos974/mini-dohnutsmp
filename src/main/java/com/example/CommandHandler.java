package com.example;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CommandHandler implements CommandExecutor {
   public static final String TITLE_ROLLING = "§e§lRolling...";
   public static final String TITLE_CASE_SHOP = "§5Case Shop";
   public static final String TITLE_KEY_INVENTORY = "§eKey Inventory";
   public static final String TITLE_OPEN_CASES = "§6§lOpen Cases";
   public static final String TITLE_HOMES = "§bHomes";
   public static final String TITLE_HOME_CONFIRM = "§c§lConfirm Action";
   private final Main plugin;
   private final Map<UUID, Deque<MenuId>> backStack = new HashMap();

   public CommandHandler(Main plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (sender instanceof Player) {
         Player player = (Player)sender;
         switch (command.getName().toLowerCase()) {
            case "money":
               double balance = (Double)this.plugin.balances.getOrDefault(player.getUniqueId(), (double)0.0F);
               Object[] var10002 = new Object[]{balance};
               player.sendMessage("§6Balance: §a$" + String.format("%.2f", var10002));
               break;
            case "pay":
               this.handlePay(player, args);
               break;
            case "menu":
               this.openMenu(player, CommandHandler.MenuId.MAIN, true);
         }

         return true;
      } else {
         return true;
      }
   }

   public void openMainMenu(Player player) {
      this.openMenu(player, CommandHandler.MenuId.MAIN, false);
   }

   private MenuId getMenuIdFromTitle(String title) {
      if (title == null) {
         return null;
      } else {
         switch (title) {
            case "§8Main Menu" -> {
               return CommandHandler.MenuId.MAIN;
            }
            case "§6Auction House" -> {
               return CommandHandler.MenuId.AUCTION;
            }
            case "§bMy Listings" -> {
               return CommandHandler.MenuId.MY_LISTINGS;
            }
            case "§dBattle Pass Levels" -> {
               return CommandHandler.MenuId.BATTLE_PASS;
            }
            case "§6Token Shop" -> {
               return CommandHandler.MenuId.TOKEN_SHOP;
            }
            case "§5Case Shop" -> {
               return CommandHandler.MenuId.CASE_SHOP;
            }
            case "§eKey Inventory" -> {
               return CommandHandler.MenuId.KEY_INVENTORY;
            }
            case "§6§lOpen Cases" -> {
               return CommandHandler.MenuId.OPEN_CASES;
            }
            case "§0§l» §2§lSELL MACHINE" -> {
               return CommandHandler.MenuId.SELL_MACHINE;
            }
            case "§bHomes" -> {
               return CommandHandler.MenuId.HOMES;
            }
            case "§c§lConfirm Action" -> {
               return CommandHandler.MenuId.HOME_CONFIRM;
            }
            case EconomyPrices.TITLE_ITEM_SHOP -> {
               return CommandHandler.MenuId.ITEM_SHOP;
            }
            case EconomyPrices.TITLE_TOKEN_PREMIUM -> {
               return CommandHandler.MenuId.TOKEN_PREMIUM;
            }
            case EconomyPrices.TITLE_SHOP_CHECKOUT -> {
               return CommandHandler.MenuId.SHOP_CHECKOUT;
            }
            default -> {
               if (title != null && title.endsWith(" Shop")) {
                  return CommandHandler.MenuId.ITEM_SHOP_CATEGORY;
               } else {
                  return null;
               }
            }
         }
      }
   }

   private int getBackSlotFor(MenuId id) {
      if (id == null) {
         return -1;
      } else {
         switch (id.ordinal()) {
            case 3 -> {
               return 26;
            }
            default -> {
               return 53;
            }
         }
      }
   }

   public ItemStack createBackButton() {
      return this.createGuiItem(Material.SPECTRAL_ARROW, "§fBack", "§7Go back to the previous menu");
   }

   private void pushBack(Player player) {
      MenuId current = this.getMenuIdFromTitle(player.getOpenInventory() != null ? player.getOpenInventory().getTitle() : null);
      if (current != null) {
         ((Deque)this.backStack.computeIfAbsent(player.getUniqueId(), (k) -> new ArrayDeque())).push(current);
      }
   }

   public void goBack(Player player) {
      Deque<MenuId> stack = (Deque)this.backStack.get(player.getUniqueId());
      if (stack != null && !stack.isEmpty()) {
         MenuId prev = (MenuId)stack.pop();
         this.openMenu(player, prev, false);
      } else {
         player.closeInventory();
      }
   }

   public void openMenu(Player player, MenuId id, boolean recordBack) {
      if (recordBack) {
         this.pushBack(player);
      }

      switch (id) {
         case MAIN -> this.openMainMenuInternal(player);
         case AUCTION -> this.openAHMainInternal(player);
         case MY_LISTINGS -> this.openMyListingsInternal(player);
         case BATTLE_PASS -> this.openBPGUIInternal(player);
         case TOKEN_SHOP -> this.openTokenShopInternal(player);
         case SELL_MACHINE -> this.openSellMachineInternal(player);
         case CASE_SHOP -> this.openCaseShopInternal(player);
         case KEY_INVENTORY -> this.openKeyInventoryInternal(player);
         case OPEN_CASES -> this.openOpenCasesInternal(player);
         case HOMES -> this.openHomesInternal(player);
         case HOME_CONFIRM -> this.openHomeConfirmInternal(player);
         case ITEM_SHOP -> this.openItemShopInternal(player);
         case ITEM_SHOP_CATEGORY -> this.openLastShopCategory(player);
         case TOKEN_PREMIUM -> this.openTokenPremiumInternal(player);
         case SHOP_CHECKOUT -> this.openShopCheckoutInternal(player);
      }

   }

   public void openItemShopFrom(Player player) {
      this.openMenu(player, CommandHandler.MenuId.ITEM_SHOP, true);
   }

   public void openItemShop(Player player) {
      this.openMenu(player, CommandHandler.MenuId.ITEM_SHOP, false);
   }

   public void openShopCategory(Player player, EconomyPrices.ShopCategory category) {
      this.pushBack(player);
      this.openShopCategoryInternal(player, category);
   }

   public void openTokenPremiumFrom(Player player) {
      this.openMenu(player, CommandHandler.MenuId.TOKEN_PREMIUM, true);
   }

   public void openTokenPremium(Player player) {
      this.openMenu(player, CommandHandler.MenuId.TOKEN_PREMIUM, false);
   }

   private void openMainMenuInternal(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, "§8Main Menu");
      inv.setItem(11, this.createGuiItem(Material.CHEST, "§6Auction House", "§7Open the Auction House"));
      inv.setItem(13, this.createGuiItem(Material.NETHER_STAR, "§dBattle Pass", "§7View quests and levels"));
      inv.setItem(15, this.createGuiItem(Material.EMERALD, "§aList Item on AH", "§7Lists the item in your hand", "§7You'll type the price in chat"));
      inv.setItem(19, this.createGuiItem(Material.EMERALD, "§2Item Shop", "§7Buy items with cash"));
      inv.setItem(29, this.createGuiItem(Material.COMPOSTER, "§aSell Machine", "§7Open the sell machine"));
      inv.setItem(31, this.createGuiItem(Material.GOLD_INGOT, "§6Token Shop", "§7Buy, sell, and spend tokens"));
      inv.setItem(21, this.createGuiItem(Material.TRIAL_KEY, "§dCrate Shop", "§7Buy themed crate keys"));
      inv.setItem(23, this.createGuiItem(Material.CHEST, "§eKey Inventory", "§7View owned crate keys"));
      inv.setItem(25, this.createGuiItem(Material.ENDER_CHEST, "§6Open Crates", "§7Roll themed loot crates"));
      inv.setItem(33, this.createGuiItem(Material.RED_BED, "§bHomes", "§7Manage and teleport to homes"));
      int tokens = (Integer)this.plugin.tokens.getOrDefault(player.getUniqueId(), 0);
      inv.setItem(49, this.createGuiItem(Material.NETHER_STAR, "§eYour Tokens", "§7Current: §b" + tokens));
      inv.setItem(53, this.createBackButton());
      player.openInventory(inv);
   }

   public void openSellMachine(Player player) {
      this.openMenu(player, CommandHandler.MenuId.SELL_MACHINE, true);
   }

   private void openSellMachineInternal(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, "§0§l» §2§lSELL MACHINE");
      ItemStack border = this.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");

      for(int i = 45; i < 54; ++i) {
         inv.setItem(i, border);
      }

      double mult = this.plugin.getMultiplier(player.getUniqueId());
      inv.setItem(48, this.createGuiItem(Material.GOLD_BLOCK, "§6§lYour Bonus", "§7Multiplier: §a" + mult + "x"));
      inv.setItem(49, this.createGuiItem(Material.LIME_WOOL, "§a§lCONFIRM SALE", "§7Click to sell items!"));
      inv.setItem(50, this.createGuiItem(Material.BARRIER, "§c§lCANCEL", "§7Returns items to you"));
      inv.setItem(53, this.createBackButton());
      player.openInventory(inv);
   }

   public void openCaseShopFrom(Player player) {
      this.openMenu(player, CommandHandler.MenuId.CASE_SHOP, true);
   }

   public void openKeyInventoryFrom(Player player) {
      this.openMenu(player, CommandHandler.MenuId.KEY_INVENTORY, true);
   }

   public void openOpenCasesFrom(Player player) {
      this.openMenu(player, CommandHandler.MenuId.OPEN_CASES, true);
   }

   public void openCaseShop(Player player) {
      this.openMenu(player, CommandHandler.MenuId.CASE_SHOP, false);
   }

   public void openKeyInventory(Player player) {
      this.openMenu(player, CommandHandler.MenuId.KEY_INVENTORY, false);
   }

   public void openOpenCases(Player player) {
      this.openMenu(player, CommandHandler.MenuId.OPEN_CASES, false);
   }

   private void openCaseShopInternal(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, "§5Case Shop");
      int tok = this.plugin.tokens.getOrDefault(player.getUniqueId(), 0);
      inv.setItem(20, this.crateShopListing(CrateType.AGRICULTURE, tok));
      inv.setItem(22, this.crateShopListing(CrateType.COMBAT, tok));
      inv.setItem(24, this.crateShopListing(CrateType.MINERAL, tok));
      inv.setItem(31, this.crateShopListing(CrateType.SPAWNER, tok));
      inv.setItem(53, this.createBackButton());
      player.openInventory(inv);
   }

   private void openKeyInventoryInternal(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, "§eKey Inventory");
      UUID uuid = player.getUniqueId();
      inv.setItem(20, this.crateKeyDisplay(CrateType.AGRICULTURE, this.plugin.getCrateKeyCount(uuid, CrateType.AGRICULTURE)));
      inv.setItem(22, this.crateKeyDisplay(CrateType.COMBAT, this.plugin.getCrateKeyCount(uuid, CrateType.COMBAT)));
      inv.setItem(24, this.crateKeyDisplay(CrateType.MINERAL, this.plugin.getCrateKeyCount(uuid, CrateType.MINERAL)));
      inv.setItem(31, this.crateKeyDisplay(CrateType.SPAWNER, this.plugin.getCrateKeyCount(uuid, CrateType.SPAWNER)));
      inv.setItem(53, this.createBackButton());
      player.openInventory(inv);
   }

   private void openOpenCasesInternal(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, "§6§lOpen Cases");
      UUID uuid = player.getUniqueId();
      inv.setItem(20, this.crateRollSlot(CrateType.AGRICULTURE, this.plugin.getCrateKeyCount(uuid, CrateType.AGRICULTURE)));
      inv.setItem(22, this.crateRollSlot(CrateType.COMBAT, this.plugin.getCrateKeyCount(uuid, CrateType.COMBAT)));
      inv.setItem(24, this.crateRollSlot(CrateType.MINERAL, this.plugin.getCrateKeyCount(uuid, CrateType.MINERAL)));
      inv.setItem(31, this.crateRollSlot(CrateType.SPAWNER, this.plugin.getCrateKeyCount(uuid, CrateType.SPAWNER)));
      inv.setItem(53, this.createBackButton());
      player.openInventory(inv);
   }

   private ItemStack crateShopListing(CrateType type, int tokens) {
      List<String> lore = new ArrayList(CaseLootTable.previewLore(type));
      lore.add("§7Cost: §b" + type.tokenCost() + " tokens");
      lore.add("§7Your tokens: §f" + tokens);
      lore.add("");
      lore.add("§eClick to purchase");
      ItemStack item = this.createGuiItem(type.keyMaterial(), type.keyLabel(), lore.toArray(new String[0]));
      return item;
   }

   private ItemStack crateKeyDisplay(CrateType type, int owned) {
      List<String> lore = new ArrayList(CaseLootTable.previewLore(type));
      lore.add("§7Owned: §f" + owned);
      return this.createGuiItem(type.keyMaterial(), type.keyLabel(), lore.toArray(new String[0]));
   }

   private ItemStack crateRollSlot(CrateType type, int owned) {
      if (owned < 1) {
         return this.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§8Locked", "§7You don't have this key.", "§7Buy one in the §dCrate Shop§7.");
      } else {
         List<String> lore = new ArrayList(CaseLootTable.previewLore(type));
         lore.add("§7Owned: §f" + owned);
         lore.add("");
         lore.add("§e§lCLICK TO OPEN CRATE");
         return this.createGuiItem(type.keyMaterial(), "§6Open " + type.displayName(), lore.toArray(new String[0]));
      }
   }

   public void openShopCheckout(Player player, Material material, EconomyPrices.ShopCategory category) {
      PendingShopPurchase purchase = new PendingShopPurchase(material, category);
      this.plugin.pendingShopPurchases.put(player.getUniqueId(), purchase);
      this.openMenu(player, MenuId.SHOP_CHECKOUT, true);
   }

   public void refreshShopCheckout(Player player) {
      this.openMenu(player, MenuId.SHOP_CHECKOUT, false);
   }

   private void openShopCheckoutInternal(Player player) {
      PendingShopPurchase purchase = this.plugin.pendingShopPurchases.get(player.getUniqueId());
      if (purchase == null) {
         this.openItemShop(player);
      } else {
         Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, EconomyPrices.TITLE_SHOP_CHECKOUT);
         Material material = purchase.material();
         int qty = purchase.quantity();
         ItemStack preview = new ItemStack(material, Math.min(qty, material.getMaxStackSize()));
         ItemMeta previewMeta = preview.getItemMeta();
         if (previewMeta != null) {
            List<String> lore = new ArrayList(EconomyPrices.buildPriceLore(material));
            lore.add("§7Selected: §f" + qty);
            lore.add("§7Total: §c" + EconomyPrices.formatMoney(purchase.totalCost()));
            previewMeta.setLore(lore);
            preview.setItemMeta(previewMeta);
         }

         inv.setItem(4, preview);
         inv.setItem(19, this.createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§aBuy x1", "§7Set quantity to 1"));
         inv.setItem(20, this.createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§aBuy x16", "§7Set quantity to 16"));
         inv.setItem(21, this.createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§aBuy x64", "§7Set quantity to 64"));
         inv.setItem(22, this.createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§aFill", "§7Buy as many as you can afford"));
         double balance = this.plugin.balances.getOrDefault(player.getUniqueId(), 0.0);
         inv.setItem(31, this.createGuiItem(Material.PAPER, "§eOrder Summary", "§7Item: §f" + material.name().toLowerCase().replace('_', ' '), "§7Quantity: §f" + qty, "§7Total: §c" + EconomyPrices.formatMoney(purchase.totalCost()), "§7Balance: §a" + EconomyPrices.formatMoney(balance)));
         inv.setItem(38, this.createGuiItem(Material.LIME_WOOL, "§a§lCONFIRM PURCHASE", "§7Buy §f" + qty + " §7for §c" + EconomyPrices.formatMoney(purchase.totalCost())));
         inv.setItem(42, this.createGuiItem(Material.RED_WOOL, "§c§lCANCEL", "§7Return to shop"));
         inv.setItem(53, this.createBackButton());
         player.openInventory(inv);
      }
   }

   private void handlePay(Player player, String[] args) {
      if (args.length < 2) {
         player.sendMessage("§cUsage: /pay <player> <amount>");
      } else {
         Player target = Bukkit.getPlayer(args[0]);
         if (target == null) {
            player.sendMessage("§cPlayer not found.");
         } else if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cYou cannot pay yourself.");
         } else {
            try {
               double amount = Double.parseDouble(args[1]);
               if (amount <= (double)0.0F) {
                  player.sendMessage("§cAmount must be greater than 0.");
                  return;
               }

               UUID senderId = player.getUniqueId();
               double senderBalance = (Double)this.plugin.balances.getOrDefault(senderId, (double)0.0F);
               if (senderBalance < amount) {
                  player.sendMessage("§cInsufficient funds.");
                  return;
               }

               this.plugin.balances.put(senderId, senderBalance - amount);
               this.plugin.balances.put(target.getUniqueId(), (Double)this.plugin.balances.getOrDefault(target.getUniqueId(), (double)0.0F) + amount);
               this.plugin.saveData();
               String var10001 = String.format("%.2f", amount);
               player.sendMessage("§aSent §6$" + var10001 + " §ato " + target.getName());
               var10001 = String.format("%.2f", amount);
               target.sendMessage("§aYou received §6$" + var10001 + " §afrom " + player.getName());
            } catch (Exception var10) {
               player.sendMessage("§cInvalid amount.");
            }
         }
      }

   }

   public void openAHMain(Player player) {
      this.openMenu(player, CommandHandler.MenuId.AUCTION, false);
   }

   public void openAHMainFrom(Player player) {
      this.openMenu(player, CommandHandler.MenuId.AUCTION, true);
   }

   private void openAHMainInternal(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, "§6Auction House");

      for(int i = 0; i < Math.min(this.plugin.auctionItems.size(), 45); ++i) {
         AuctionListing listing = (AuctionListing)this.plugin.auctionItems.get(i);
         ItemStack display = listing.item().clone();
         ItemMeta meta = display.getItemMeta();
         if (meta != null) {
            List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList(meta.getLore()) : new ArrayList();
            lore.add("§8§m----------------");
            Object[] var10002 = new Object[]{listing.price()};
            lore.add("§7Price: §6$" + String.format("%.2f", var10002));
            String sellerName = Bukkit.getOfflinePlayer(listing.seller()).getName();
            String var10001 = sellerName != null ? sellerName : listing.seller().toString();
            lore.add("§7Seller: §f" + var10001);
            lore.add("§eClick to Purchase");
            lore.add("§8§m----------------");
            meta.setLore(lore);
            display.setItemMeta(meta);
            EconomyPrices.applyPriceLore(display);
         }

         inv.setItem(i, display);
      }

      inv.setItem(49, this.createGuiItem(Material.CHEST, "§bMy Listings", "§7Click to view/cancel your items"));
      inv.setItem(53, this.createBackButton());
      player.openInventory(inv);
   }

   public void openMyListings(Player player) {
      this.openMenu(player, CommandHandler.MenuId.MY_LISTINGS, false);
   }

   public void openMyListingsFrom(Player player) {
      this.openMenu(player, CommandHandler.MenuId.MY_LISTINGS, true);
   }

   private void openMyListingsInternal(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, "§bMy Listings");
      int slot = 0;

      for(AuctionListing listing : this.plugin.auctionItems) {
         if (listing.seller().equals(player.getUniqueId())) {
            ItemStack display = listing.item().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
               List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList(meta.getLore()) : new ArrayList();
               lore.add("§8§m----------------");
               Object[] var10002 = new Object[]{listing.price()};
               lore.add("§7Listed for: §6$" + String.format("%.2f", var10002));
               lore.add("§c§lCLICK TO CANCEL");
               lore.add("§8§m----------------");
               meta.setLore(lore);
               display.setItemMeta(meta);
               EconomyPrices.applyPriceLore(display);
            }

            inv.setItem(slot++, display);
            if (slot >= 45) {
               break;
            }
         }
      }

      inv.setItem(49, this.createBackButton());
      player.openInventory(inv);
   }

   public void openBPGUI(Player player) {
      this.openMenu(player, CommandHandler.MenuId.BATTLE_PASS, false);
   }

   public void openBPGUIFrom(Player player) {
      this.openMenu(player, CommandHandler.MenuId.BATTLE_PASS, true);
   }

   private void openBPGUIInternal(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, "§dBattle Pass Levels");
      int level = (Integer)this.plugin.playerLevels.getOrDefault(player.getUniqueId(), 1);
      inv.setItem(10, this.getQuestItem(1, level, Material.ZOMBIE_HEAD, "Kill 10 Zombies", "§a" + EconomyPrices.formatMoney(EconomyPrices.getQuestReward(1))));
      inv.setItem(11, this.getQuestItem(2, level, Material.SKELETON_SKULL, "Kill 10 Skeletons", "§a" + EconomyPrices.formatMoney(EconomyPrices.getQuestReward(2))));
      inv.setItem(13, this.getQuestItem(3, level, Material.CREEPER_HEAD, "Kill 5 Creepers", "§a" + EconomyPrices.formatMoney(EconomyPrices.getQuestReward(3))));
      inv.setItem(15, this.getQuestItem(4, level, Material.SPIDER_EYE, "Kill 15 Spiders", "§a" + EconomyPrices.formatMoney(EconomyPrices.getQuestReward(4))));
      inv.setItem(16, this.getQuestItem(5, level, Material.BLAZE_ROD, "Kill 10 Blazes", "§a" + EconomyPrices.formatMoney(EconomyPrices.getQuestReward(5))));
      inv.setItem(26, this.createBackButton());
      player.openInventory(inv);
   }

   public void openTokenShop(Player player) {
      this.openMenu(player, CommandHandler.MenuId.TOKEN_SHOP, true);
   }

   public void refreshTokenShop(Player player) {
      this.openMenu(player, CommandHandler.MenuId.TOKEN_SHOP, false);
   }

   public void openHomesFrom(Player player) {
      this.openMenu(player, CommandHandler.MenuId.HOMES, true);
   }

   public void openHomes(Player player) {
      this.openMenu(player, CommandHandler.MenuId.HOMES, false);
   }

   public void openHomeConfirm(Player player, PendingHomeAction action) {
      this.plugin.pendingHomeActions.put(player.getUniqueId(), action);
      this.openMenu(player, CommandHandler.MenuId.HOME_CONFIRM, true);
   }

   public static int getHomeBedSlot(int index) {
      return new int[]{20, 21, 22, 23, 24}[index];
   }

   public static int getHomeDeleteSlot(int index) {
      return getHomeBedSlot(index) - 9;
   }

   public static int getHomeSetSlot(int index) {
      return getHomeBedSlot(index) + 9;
   }

   public static int getHomeIndexFromSlot(int slot) {
      for(int i = 0; i < Main.HOME_SLOT_COUNT; ++i) {
         if (slot == getHomeBedSlot(i) || slot == getHomeDeleteSlot(i) || slot == getHomeSetSlot(i)) {
            return i;
         }
      }

      return -1;
   }

   private void openHomesInternal(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, TITLE_HOMES);
      UUID uuid = player.getUniqueId();

      for(int i = 0; i < Main.HOME_SLOT_COUNT; ++i) {
         boolean unlocked = this.plugin.isHomeSlotUnlocked(uuid, i);
         PlayerHome home = this.plugin.getHome(uuid, i);
         inv.setItem(getHomeDeleteSlot(i), this.homeDeleteButton(i, unlocked, home));
         inv.setItem(getHomeBedSlot(i), this.homeBedButton(i, unlocked, home));
         inv.setItem(getHomeSetSlot(i), this.homeSetButton(i, unlocked));
      }

      inv.setItem(53, this.createBackButton());
      player.openInventory(inv);
   }

   private ItemStack homeBedButton(int index, boolean unlocked, PlayerHome home) {
      String name = "§fHome " + (index + 1);
      if (!unlocked) {
         return this.createGuiItem(Material.GRAY_BED, name, "§cLocked", "§7Unlock for §b" + Main.HOME_UNLOCK_COST + " tokens", "", "§eClick to unlock");
      } else if (home == null) {
         return this.createGuiItem(Material.WHITE_BED, name, "§7No home set", "", "§eClick bed after setting one");
      } else {
         return this.createGuiItem(Material.RED_BED, name, "§7World: §f" + home.world(), "§7X: §f" + (int)home.x() + " §7Y: §f" + (int)home.y() + " §7Z: §f" + (int)home.z(), "", "§a§lCLICK TO TELEPORT");
      }
   }

   private ItemStack homeSetButton(int index, boolean unlocked) {
      if (!unlocked) {
         return this.createGuiItem(Material.GRAY_DYE, "§8Set Home", "§cUnlock this slot first");
      } else {
         return this.createGuiItem(Material.LIME_DYE, "§aSet Home", "§7Save your current location", "§7to Home " + (index + 1));
      }
   }

   private ItemStack homeDeleteButton(int index, boolean unlocked, PlayerHome home) {
      if (!unlocked) {
         return this.createGuiItem(Material.GRAY_DYE, "§8Delete Home", "§cUnlock this slot first");
      } else if (home == null) {
         return this.createGuiItem(Material.GRAY_DYE, "§8Delete Home", "§7No home to delete");
      } else {
         return this.createGuiItem(Material.RED_DYE, "§cDelete Home", "§7Remove Home " + (index + 1));
      }
   }

   private void openHomeConfirmInternal(Player player) {
      PendingHomeAction action = this.plugin.pendingHomeActions.get(player.getUniqueId());
      if (action == null) {
         this.openHomes(player);
      } else {
         Inventory inv = Bukkit.createInventory((InventoryHolder)null, 27, TITLE_HOME_CONFIRM);
         int homeNum = action.homeIndex() + 1;
         String detail;
         switch (action.type().ordinal()) {
            case 0 -> detail = "§7Set Home §f" + homeNum + " §7to your current location?";
            case 1 -> detail = "§7Delete Home §f" + homeNum + "§7?";
            case 2 -> detail = "§7Teleport to Home §f" + homeNum + "§7?";
            default -> detail = "§7Unlock Home §f" + homeNum + " §7for §b" + Main.HOME_UNLOCK_COST + " tokens§7?";
         }

         inv.setItem(11, this.createGuiItem(Material.LIME_WOOL, "§a§lCONFIRM", detail));
         inv.setItem(15, this.createGuiItem(Material.RED_WOOL, "§c§lCANCEL", "§7Go back without changes"));
         inv.setItem(22, this.createBackButton());
         player.openInventory(inv);
      }
   }

   private void openTokenShopInternal(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, "§6Token Shop");
      double buyEach = EconomyPrices.TOKEN_BUY_PRICE;
      double sellEach = EconomyPrices.TOKEN_SELL_PRICE;
      inv.setItem(10, this.createGuiItem(Material.GOLD_INGOT, "§aBuy 1 Token", "§7Price: §c" + EconomyPrices.formatMoney(buyEach), "§7Rate: §f$15,000/token (+50% tax)"));
      inv.setItem(11, this.createGuiItem(Material.GOLD_INGOT, "§aBuy 5 Tokens", "§7Price: §c" + EconomyPrices.formatMoney(buyEach * 5.0)));
      inv.setItem(12, this.createGuiItem(Material.GOLD_INGOT, "§aBuy 10 Tokens", "§7Price: §c" + EconomyPrices.formatMoney(buyEach * 10.0)));
      inv.setItem(13, this.createGuiItem(Material.GOLD_INGOT, "§aBuy 100 Tokens", "§7Price: §c" + EconomyPrices.formatMoney(buyEach * 100.0)));
      inv.setItem(28, this.createGuiItem(Material.IRON_INGOT, "§eSell 1 Token", "§7Payout: §a" + EconomyPrices.formatMoney(sellEach), "§7Rate: §f$5,000/token"));
      inv.setItem(29, this.createGuiItem(Material.IRON_INGOT, "§eSell 5 Tokens", "§7Payout: §a" + EconomyPrices.formatMoney(sellEach * 5.0)));
      inv.setItem(30, this.createGuiItem(Material.IRON_INGOT, "§eSell 10 Tokens", "§7Payout: §a" + EconomyPrices.formatMoney(sellEach * 10.0)));
      inv.setItem(31, this.createGuiItem(Material.NETHER_STAR, "§dPremium Token Shop", "§71 Token = §f$10,000 §7value", "§7Spawners, netherite, totems", "", "§eClick to open"));
      int tokens = (Integer)this.plugin.tokens.getOrDefault(player.getUniqueId(), 0);
      inv.setItem(49, this.createGuiItem(Material.NETHER_STAR, "§eYour Tokens", "§7Current: §b" + tokens, "§7Worth: §6" + EconomyPrices.formatMoney((double)tokens * EconomyPrices.TOKEN_CASH_EQUIVALENT)));
      inv.setItem(53, this.createBackButton());
      player.openInventory(inv);
   }

   private void openItemShopInternal(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, EconomyPrices.TITLE_ITEM_SHOP);
      int slot = 10;

      for(EconomyPrices.ShopCategory category : EconomyPrices.ShopCategory.values()) {
         inv.setItem(slot++, this.createGuiItem(category.icon(), category.title(), "§7Browse buy prices", "§eClick to open"));
      }

      inv.setItem(53, this.createBackButton());
      player.openInventory(inv);
   }

   private void openLastShopCategory(Player player) {
      EconomyPrices.ShopCategory category = this.plugin.lastShopCategory.get(player.getUniqueId());
      if (category != null) {
         this.openShopCategoryInternal(player, category);
      } else {
         this.openItemShopInternal(player);
      }
   }

   public void openShopCategoryInternal(Player player, EconomyPrices.ShopCategory category) {
      this.plugin.lastShopCategory.put(player.getUniqueId(), category);
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, category.title());
      Material[] items = EconomyPrices.getCategoryItems(category);
      int slot = 10;

      for(Material material : items) {
         if (slot > 43) {
            break;
         }

         int amount = EconomyPrices.defaultPurchaseAmount(material);
         inv.setItem(slot++, this.createShopListing(material, amount));
      }

      inv.setItem(49, this.createGuiItem(Material.BOOK, "§ePrice Guide", "§7Hover items to see", "§7buy and sell values"));
      inv.setItem(53, this.createBackButton());
      player.openInventory(inv);
   }

   private void openTokenPremiumInternal(Player player) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 54, EconomyPrices.TITLE_TOKEN_PREMIUM);
      int[] slots = new int[]{10, 11, 12, 13, 14, 19, 20, 21, 22};
      TokenPremiumItems[] items = TokenPremiumItems.values();

      for(int i = 0; i < slots.length && i < items.length; ++i) {
         inv.setItem(slots[i], items[i].createDisplayItem());
      }

      int tokens = (Integer)this.plugin.tokens.getOrDefault(player.getUniqueId(), 0);
      inv.setItem(49, this.createGuiItem(Material.NETHER_STAR, "§eYour Tokens", "§7Current: §b" + tokens));
      inv.setItem(53, this.createBackButton());
      player.openInventory(inv);
   }

   public ItemStack createShopListing(Material material, int amount) {
      ItemStack item = new ItemStack(material, Math.min(amount, material.getMaxStackSize()));
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         List<String> lore = new ArrayList(EconomyPrices.buildPriceLore(material));
         lore.add("");
         lore.add("§eClick to open checkout");
         meta.setLore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack getQuestItem(int itemLevel, int playerLevel, Material mat, String task, String reward) {
      String status = playerLevel > itemLevel ? "§a§lCOMPLETED" : (playerLevel == itemLevel ? "§e§lCLICK TO START" : "§c§lREACH LEVEL " + itemLevel);
      return this.createGuiItem(mat, "§fLevel " + itemLevel + ": " + task, "§7Reward: " + reward, "", status);
   }

   public ItemStack createGuiItem(Material mat, String name, String... lore) {
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         meta.setDisplayName(name);
         List<String> l = new ArrayList();

         for(String s : lore) {
            l.add(s);
         }

         meta.setLore(l);
         item.setItemMeta(meta);
         return item;
      }
   }

   private static enum MenuId {
      MAIN,
      AUCTION,
      MY_LISTINGS,
      BATTLE_PASS,
      TOKEN_SHOP,
      SELL_MACHINE,
      CASE_SHOP,
      KEY_INVENTORY,
      OPEN_CASES,
      HOMES,
      HOME_CONFIRM,
      ITEM_SHOP,
      ITEM_SHOP_CATEGORY,
      TOKEN_PREMIUM,
      SHOP_CHECKOUT;

      // $FF: synthetic method
      private static MenuId[] $values() {
         return new MenuId[]{MAIN, AUCTION, MY_LISTINGS, BATTLE_PASS, TOKEN_SHOP, SELL_MACHINE, CASE_SHOP, KEY_INVENTORY, OPEN_CASES, HOMES, HOME_CONFIRM, ITEM_SHOP, ITEM_SHOP_CATEGORY, TOKEN_PREMIUM, SHOP_CHECKOUT};
      }
   }
}
