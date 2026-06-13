package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MenuListener implements Listener {
   private static final long REVEAL_LINGER_TICKS = 45L;
   private final Main plugin;
   private final CommandHandler handler;
   private static final Material[] GLASS_PANES;

   public MenuListener(Main plugin, CommandHandler handler) {
      this.plugin = plugin;
      this.handler = handler;
   }

   private Material randomGlassPane() {
      return GLASS_PANES[ThreadLocalRandom.current().nextInt(GLASS_PANES.length)];
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onGlobalClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         InventoryView view = event.getView();
         String title = view.getTitle();
         Inventory clicked = event.getClickedInventory();
         Inventory top = view.getTopInventory();
         boolean topClick = clicked != null && clicked.equals(top);
         if (title.equals("§0§l» §2§lSELL MACHINE")) {
            if (topClick) {
               if (event.getRawSlot() >= 45) {
                  event.setCancelled(true);
                  if (event.getRawSlot() == 49) {
                     this.processSale(player, top);
                  } else if (event.getRawSlot() == 50) {
                     player.closeInventory();
                  } else if (event.getRawSlot() == 53) {
                     this.handler.goBack(player);
                  }
               } else {
                  ItemStack current = event.getCurrentItem();
                  if (current != null && current.getType() != Material.AIR) {
                     this.updateItemValueLore(current);
                  }
               }
            }

            // Items may move into/out of the machine on this click — recompute the live total next tick.
            this.scheduleSellValueUpdate(player);
         } else if ("§e§lRolling...".equals(title)) {
            if (topClick) {
               event.setCancelled(true);
            }

         } else {
            if (this.isMenuGui(title)) {
               if (!topClick) {
                  return;
               }

               event.setCancelled(true);
               ItemStack clickedItem = event.getCurrentItem();
               if (clickedItem != null && clickedItem.getType() == Material.SPECTRAL_ARROW) {
                  ItemMeta meta = clickedItem.getItemMeta();
                  if (meta != null && "§fBack".equals(meta.getDisplayName())) {
                     this.handler.goBack(player);
                     return;
                  }
               }

               int slot = event.getRawSlot();
               if (title.equals("§8Main Menu")) {
                  if (slot == 11) {
                     this.handler.openAHMainFrom(player);
                  } else if (slot == 13) {
                     this.handler.openBPGUIFrom(player);
                  } else if (slot == 15) {
                     this.beginAuctionSellFlow(player);
                  } else if (slot == 19) {
                     this.handler.openItemShopFrom(player);
                  } else if (slot == 29) {
                     this.handler.openSellMachine(player);
                  } else if (slot == 31) {
                     this.handler.openTokenShop(player);
                  } else if (slot == 21) {
                     this.handler.openCaseShopFrom(player);
                  } else if (slot == 23) {
                     this.handler.openKeyInventoryFrom(player);
                  } else if (slot == 25) {
                     this.handler.openOpenCasesFrom(player);
                  } else if (slot == 33) {
                     this.handler.openHomesFrom(player);
                  }
               } else if (CommandHandler.TITLE_HOMES.equals(title)) {
                  this.handleHomesClick(player, slot);
               } else if (CommandHandler.TITLE_HOME_CONFIRM.equals(title)) {
                  if (slot == 11) {
                     this.confirmHomeAction(player);
                  } else if (slot == 15 || slot == 22) {
                     this.plugin.pendingHomeActions.remove(player.getUniqueId());
                     this.handler.openHomes(player);
                  }
               } else if (title.equals("§6Auction House")) {
                  if (slot >= 0 && slot < Math.min(45, this.plugin.auctionItems.size())) {
                     this.buyListing(player, slot);
                  } else if (slot == 49) {
                     this.handler.openMyListingsFrom(player);
                  }
               } else if (title.equals("§bMy Listings")) {
                  if (slot >= 0 && slot < 45) {
                     this.cancelListing(player, slot);
                  } else if (slot == 49) {
                     this.handler.goBack(player);
                  }
               } else if (title.equals("§dBattle Pass Levels")) {
                  int currentLevel = (Integer)this.plugin.playerLevels.getOrDefault(player.getUniqueId(), 1);
                  if (currentLevel <= 5 && slot == this.getQuestSlotForLevel(currentLevel)) {
                     this.startQuest(player);
                  }
               } else if (title.equals("§6Token Shop")) {
                  if (slot == 10) {
                     this.buyTokens(player, 1, EconomyPrices.TOKEN_BUY_PRICE);
                  } else if (slot == 11) {
                     this.buyTokens(player, 5, EconomyPrices.TOKEN_BUY_PRICE * 5.0);
                  } else if (slot == 12) {
                     this.buyTokens(player, 10, EconomyPrices.TOKEN_BUY_PRICE * 10.0);
                  } else if (slot == 13) {
                     this.buyTokens(player, 100, EconomyPrices.TOKEN_BUY_PRICE * 100.0);
                  } else if (slot == 28) {
                     this.sellTokens(player, 1);
                  } else if (slot == 29) {
                     this.sellTokens(player, 5);
                  } else if (slot == 30) {
                     this.sellTokens(player, 10);
                  } else if (slot == 31) {
                     this.handler.openTokenPremiumFrom(player);
                  }
               } else if (EconomyPrices.TITLE_ITEM_SHOP.equals(title)) {
                  EconomyPrices.ShopCategory[] categories = EconomyPrices.ShopCategory.values();
                  if (slot >= 10 && slot < 10 + categories.length) {
                     this.handler.openShopCategory(player, categories[slot - 10]);
                  }
               } else if (this.getShopCategoryFromTitle(title) != null) {
                  if (slot >= 10 && slot <= 43 && clickedItem != null && EconomyPrices.isListed(clickedItem.getType())) {
                     this.handler.openShopCheckout(player, clickedItem.getType(), this.getShopCategoryFromTitle(title));
                  }
               } else if (EconomyPrices.TITLE_SHOP_CHECKOUT.equals(title)) {
                  this.handleShopCheckoutClick(player, slot);
               } else if (EconomyPrices.TITLE_TOKEN_PREMIUM.equals(title)) {
                  TokenPremiumItems premium = TokenPremiumItems.fromSlot(slot);
                  if (premium != null) {
                     this.purchasePremiumItem(player, premium);
                  }
               } else if ("§5Case Shop".equals(title)) {
                  CrateType crateType = CrateType.fromShopSlot(slot);
                  if (crateType != null) {
                     this.purchaseCrateKey(player, crateType);
                  }
               } else if ("§6§lOpen Cases".equals(title)) {
                  CrateType crateType = CrateType.fromInventorySlot(slot);
                  if (crateType != null) {
                     this.startCrateRoll(player, crateType);
                  }
               }
            }

         }
      }
   }

   private void handleHomesClick(Player player, int slot) {
      int index = CommandHandler.getHomeIndexFromSlot(slot);
      if (index < 0) {
         return;
      } else {
         UUID uuid = player.getUniqueId();
         boolean unlocked = this.plugin.isHomeSlotUnlocked(uuid, index);
         if (slot == CommandHandler.getHomeSetSlot(index)) {
            if (!unlocked) {
               player.sendMessage("§cUnlock this home slot first.");
            } else {
               this.handler.openHomeConfirm(player, new PendingHomeAction(PendingHomeAction.Type.SET, index));
            }
         } else if (slot == CommandHandler.getHomeDeleteSlot(index)) {
            if (!unlocked) {
               player.sendMessage("§cUnlock this home slot first.");
            } else if (this.plugin.getHome(uuid, index) == null) {
               player.sendMessage("§cNo home set in this slot.");
            } else {
               this.handler.openHomeConfirm(player, new PendingHomeAction(PendingHomeAction.Type.DELETE, index));
            }
         } else if (slot == CommandHandler.getHomeBedSlot(index)) {
            if (!unlocked) {
               this.handler.openHomeConfirm(player, new PendingHomeAction(PendingHomeAction.Type.UNLOCK, index));
            } else if (this.plugin.getHome(uuid, index) == null) {
               player.sendMessage("§eUse the green dye below to set this home.");
            } else {
               this.handler.openHomeConfirm(player, new PendingHomeAction(PendingHomeAction.Type.TELEPORT, index));
            }
         }
      }
   }

   private void confirmHomeAction(Player player) {
      UUID uuid = player.getUniqueId();
      PendingHomeAction action = this.plugin.pendingHomeActions.remove(uuid);
      if (action == null) {
         this.handler.openHomes(player);
      } else {
         int index = action.homeIndex();
         switch (action.type().ordinal()) {
            case 0 -> {
               this.plugin.setHome(uuid, index, PlayerHome.fromLocation(player.getLocation()));
               this.plugin.saveData();
               player.sendMessage("§aHome " + (index + 1) + " set!");
               this.handler.openHomes(player);
            }
            case 1 -> {
               this.plugin.clearHome(uuid, index);
               this.plugin.saveData();
               player.sendMessage("§eHome " + (index + 1) + " deleted.");
               this.handler.openHomes(player);
            }
            case 2 -> {
               PlayerHome home = this.plugin.getHome(uuid, index);
               if (home == null) {
                  player.sendMessage("§cThat home no longer exists.");
                  this.handler.openHomes(player);
               } else {
                  Location loc = home.toLocation();
                  if (loc == null) {
                     player.sendMessage("§cHome world is unavailable.");
                     this.handler.openHomes(player);
                  } else {
                     player.closeInventory();
                     player.teleport(loc);
                     player.sendMessage("§aTeleported to Home " + (index + 1) + "!");
                     player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
                  }
               }
            }
            default -> {
               if (this.plugin.isHomeSlotUnlocked(uuid, index)) {
                  player.sendMessage("§eThis home slot is already unlocked.");
                  this.handler.openHomes(player);
               } else {
                  int tokens = this.plugin.tokens.getOrDefault(uuid, 0);
                  if (tokens < Main.HOME_UNLOCK_COST) {
                     player.sendMessage("§cNot enough tokens. Need §b" + Main.HOME_UNLOCK_COST + "§c.");
                     this.handler.openHomes(player);
                  } else {
                     this.plugin.tokens.put(uuid, tokens - Main.HOME_UNLOCK_COST);
                     this.plugin.unlockHomeSlot(uuid, index);
                     this.plugin.saveData();
                     player.sendMessage("§aUnlocked Home " + (index + 1) + " for §b" + Main.HOME_UNLOCK_COST + " tokens§a!");
                     this.handler.openHomes(player);
                  }
               }
            }
         }
      }
   }

   private void purchaseCrateKey(Player player, CrateType type) {
      int price = type.tokenCost();
      UUID uuid = player.getUniqueId();
      int tokenBal = this.plugin.tokens.getOrDefault(uuid, 0);
      if (tokenBal < price) {
         player.sendMessage("§cNot enough tokens. Need §b" + price + "§c.");
      } else {
         this.plugin.tokens.put(uuid, tokenBal - price);
         this.plugin.addCrateKey(uuid, type, 1);
         this.plugin.saveData();
         player.sendMessage("§aPurchased " + type.keyLabel() + "§a!");
         this.handler.openCaseShop(player);
      }
   }

   private void handleShopCheckoutClick(Player player, int slot) {
      PendingShopPurchase purchase = this.plugin.pendingShopPurchases.get(player.getUniqueId());
      if (purchase == null) {
         this.handler.openItemShop(player);
      } else if (slot == 19) {
         purchase.setQuantity(1);
         this.handler.refreshShopCheckout(player);
      } else if (slot == 20) {
         purchase.setQuantity(16);
         this.handler.refreshShopCheckout(player);
      } else if (slot == 21) {
         purchase.setQuantity(64);
         this.handler.refreshShopCheckout(player);
      } else if (slot == 22) {
         purchase.setQuantity(this.calculateFillAmount(player, purchase.material()));
         this.handler.refreshShopCheckout(player);
      } else if (slot == 38) {
         this.confirmShopPurchase(player);
      } else if (slot == 42) {
         EconomyPrices.ShopCategory category = purchase.category();
         this.plugin.pendingShopPurchases.remove(player.getUniqueId());
         if (category != null) {
            this.handler.openShopCategoryInternal(player, category);
         } else {
            this.handler.openItemShop(player);
         }
      }
   }

   private int calculateFillAmount(Player player, Material material) {
      double balance = this.plugin.balances.getOrDefault(player.getUniqueId(), 0.0);
      int affordable = EconomyPrices.maxAffordableAmount(material, balance);
      int space = 0;

      for(ItemStack stack : player.getInventory().getStorageContents()) {
         if (stack == null || stack.getType() == Material.AIR) {
            space += material.getMaxStackSize();
         } else if (stack.getType() == material) {
            space += material.getMaxStackSize() - stack.getAmount();
         }
      }

      return Math.max(1, Math.min(affordable, space));
   }

   private void confirmShopPurchase(Player player) {
      UUID uuid = player.getUniqueId();
      PendingShopPurchase purchase = this.plugin.pendingShopPurchases.remove(uuid);
      if (purchase == null) {
         this.handler.openItemShop(player);
      } else {
         Material material = purchase.material();
         int quantity = purchase.quantity();
         double cost = purchase.totalCost();
         double balance = this.plugin.balances.getOrDefault(uuid, 0.0);
         if (quantity < 1) {
            player.sendMessage("§cInvalid quantity.");
            this.handler.openShopCategoryInternal(player, purchase.category());
         } else if (balance < cost) {
            player.sendMessage("§cInsufficient funds. Need §6" + EconomyPrices.formatMoney(cost));
            this.plugin.pendingShopPurchases.put(uuid, purchase);
            this.handler.refreshShopCheckout(player);
         } else {
            this.plugin.balances.put(uuid, balance - cost);
            this.plugin.saveData();
            this.giveOrDrop(player, new ItemStack(material, quantity));
            player.sendMessage("§aPurchased §f" + quantity + "x " + material.name().toLowerCase().replace('_', ' ') + " §afor §6" + EconomyPrices.formatMoney(cost));
            this.handler.openShopCategoryInternal(player, purchase.category());
         }
      }
   }

   private void startCrateRoll(final Player player, CrateType type) {
      final UUID uuid = player.getUniqueId();
      if (this.plugin.caseRollSessions.containsKey(uuid)) {
         player.sendMessage("§cFinish your current roll first.");
      } else if (this.plugin.getCrateKeyCount(uuid, type) < 1) {
         player.sendMessage("§cYou don't have this key.");
      } else if (!this.plugin.takeCrateKey(uuid, type)) {
         player.sendMessage("§cYou don't have this key.");
      } else {
         this.plugin.saveData();
         CaseLootTable.RollOutcome outcome = CaseLootTable.roll(type, ThreadLocalRandom.current());
         final CaseRollSession session = new CaseRollSession(type);
         session.outcome = outcome;
         this.plugin.caseRollSessions.put(uuid, session);
         Inventory hopperInv = Bukkit.createInventory((InventoryHolder)null, InventoryType.HOPPER, "§e§lRolling...");
         player.openInventory(hopperInv);
         session.task = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, new Runnable() {
            private int tick;

            public void run() {
               CaseRollSession tracked = (CaseRollSession)MenuListener.this.plugin.caseRollSessions.get(uuid);
               if (tracked == session) {
                  if (player.isOnline()) {
                     if ("§e§lRolling...".equals(player.getOpenInventory().getTitle())) {
                        Inventory top = player.getOpenInventory().getTopInventory();
                        if (this.tick < 80) {
                           for(int i = 0; i < 5; ++i) {
                              top.setItem(i, new ItemStack(MenuListener.this.randomGlassPane()));
                           }

                           ++this.tick;
                        } else {
                           if (session.task != null) {
                              session.task.cancel();
                              session.task = null;
                           }

                           MenuListener.this.finishCaseReveal(player, top, session);
                        }

                     }
                  }
               }
            }
         }, 0L, 1L);
      }
   }

   private void finishCaseReveal(Player player, Inventory hopper, CaseRollSession session) {
      CaseLootTable.RollOutcome out = session.outcome;

      for(int i = 0; i < 5; ++i) {
         hopper.setItem(i, (ItemStack)null);
      }

      hopper.setItem(2, out.displayItem.clone());
      UUID uuid = player.getUniqueId();
      if (out.money > (double)0.0F) {
         double bal = (Double)this.plugin.balances.getOrDefault(uuid, (double)0.0F);
         this.plugin.balances.put(uuid, bal + out.money);
         Object[] var10002 = new Object[]{out.money};
         player.sendMessage("§aYou won §6$" + String.format("%.2f", var10002) + "§a!");
      } else {
         this.giveOrDrop(player, out.displayItem.clone());
         player.sendMessage("§aCrate opened! Check your inventory.");
      }

      this.plugin.saveData();
      session.revealDone = true;
      player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);

      // Let the reward reveal linger briefly, then drop the player back into the marketplace.
      final UUID rollerId = player.getUniqueId();
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         Player roller = this.plugin.getServer().getPlayer(rollerId);
         if (roller != null && roller.isOnline()) {
            if (this.plugin.getMarketplace() != null) {
               this.plugin.getMarketplace().openHome(roller);
            } else {
               this.handler.openMainMenu(roller);
            }
         }
      }, REVEAL_LINGER_TICKS);
   }

   private void beginAuctionSellFlow(Player player) {
      UUID uuid = player.getUniqueId();
      ItemStack inHand = player.getInventory().getItemInMainHand();
      if (inHand != null && inHand.getType() != Material.AIR) {
         ItemStack existingPending = (ItemStack)this.plugin.pendingAuctionSell.remove(uuid);
         if (existingPending != null) {
            this.giveOrDrop(player, existingPending);
         }

         ItemStack toSell = inHand.clone();
         player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
         this.plugin.pendingAuctionSell.put(uuid, toSell);
         player.closeInventory();
         player.sendMessage("§aType the price in chat to list your item. Type §c'cancel' §ato abort.");
      } else {
         player.sendMessage("§cHold an item in your main hand to list it.");
         player.closeInventory();
      }
   }

   private void giveOrDrop(Player player, ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         int remaining = item.getAmount();
         Material material = item.getType();
         int maxStack = material.getMaxStackSize();

         while(remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            ItemStack stack = item.clone();
            stack.setAmount(stackAmount);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);

            for(ItemStack rest : leftover.values()) {
               player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }

            remaining -= stackAmount;
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onChatPriceInput(AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (this.plugin.pendingAuctionSell.containsKey(uuid)) {
         event.setCancelled(true);
         String message = event.getMessage() != null ? event.getMessage().trim() : "";
         Bukkit.getScheduler().runTask(this.plugin, () -> {
            ItemStack item = (ItemStack)this.plugin.pendingAuctionSell.remove(uuid);
            if (item != null) {
               if (message.equalsIgnoreCase("cancel")) {
                  this.giveOrDrop(player, item);
                  player.sendMessage("§eCancelled. Item returned.");
               } else {
                  double price;
                  try {
                     price = Double.parseDouble(message);
                  } catch (Exception var8) {
                     this.giveOrDrop(player, item);
                     player.sendMessage("§cInvalid price. Item returned.");
                     return;
                  }

                  if (price <= (double)0.0F) {
                     this.giveOrDrop(player, item);
                     player.sendMessage("§cPrice must be greater than 0. Item returned.");
                  } else {
                     this.plugin.auctionItems.add(new AuctionListing(uuid, price, item));
                     this.plugin.saveData();
                     Object[] var10002 = new Object[]{price};
                     player.sendMessage("§aListed for §6$" + String.format("%.2f", var10002) + "§a!");
                  }
               }
            }
         });
      }
   }

   @EventHandler
   public void onDisconnect(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      ItemStack pending = (ItemStack)this.plugin.pendingAuctionSell.remove(uuid);
      if (pending != null) {
         this.giveOrDrop(player, pending);
      }

      this.plugin.pendingHomeActions.remove(uuid);
      this.plugin.pendingShopPurchases.remove(uuid);
      CaseRollSession roll = (CaseRollSession)this.plugin.caseRollSessions.remove(uuid);
      if (roll != null) {
         if (roll.task != null) {
            roll.task.cancel();
         }

         if (!roll.revealDone) {
            this.plugin.addCrateKey(uuid, roll.crateType, 1);
            this.plugin.saveData();
         }
      }

   }

   private void buyTokens(Player player, int tokenAmount, double price) {
      double balance = (Double)this.plugin.balances.getOrDefault(player.getUniqueId(), (double)0.0F);
      if (balance < price) {
         player.sendMessage("§cInsufficient funds.");
      } else {
         int currentTokens = (Integer)this.plugin.tokens.getOrDefault(player.getUniqueId(), 0);
         this.plugin.balances.put(player.getUniqueId(), balance - price);
         this.plugin.tokens.put(player.getUniqueId(), currentTokens + tokenAmount);
         this.plugin.saveData();
         player.sendMessage("§aBought §b" + tokenAmount + " §atoken(s) for §6" + EconomyPrices.formatMoney(price));
         this.handler.refreshTokenShop(player);
      }
   }

   private void sellTokens(Player player, int tokenAmount) {
      UUID uuid = player.getUniqueId();
      int currentTokens = this.plugin.tokens.getOrDefault(uuid, 0);
      if (currentTokens < tokenAmount) {
         player.sendMessage("§cNot enough tokens.");
      } else {
         double payout = EconomyPrices.TOKEN_SELL_PRICE * (double)tokenAmount;
         this.plugin.tokens.put(uuid, currentTokens - tokenAmount);
         this.plugin.balances.put(uuid, this.plugin.balances.getOrDefault(uuid, 0.0) + payout);
         this.plugin.saveData();
         player.sendMessage("§aSold §b" + tokenAmount + " §atoken(s) for §6" + EconomyPrices.formatMoney(payout));
         this.handler.refreshTokenShop(player);
      }
   }

   private void purchasePremiumItem(Player player, TokenPremiumItems premium) {
      UUID uuid = player.getUniqueId();
      int tokens = this.plugin.tokens.getOrDefault(uuid, 0);
      if (tokens < premium.tokenCost()) {
         player.sendMessage("§cNot enough tokens. Need §b" + premium.tokenCost());
      } else {
         this.plugin.tokens.put(uuid, tokens - premium.tokenCost());
         this.plugin.saveData();

         for(ItemStack reward : premium.createRewards()) {
            this.giveOrDrop(player, reward);
         }

         player.sendMessage("§aPurchased §f" + premium.name().toLowerCase().replace('_', ' ') + " §afor §b" + premium.tokenCost() + " tokens§a!");
         this.handler.openTokenPremium(player);
      }
   }

   private boolean isMenuGui(String title) {
      if (title != null && !title.isEmpty()) {
         return title.equals("§8Main Menu") || title.equals("§6Auction House") || title.equals("§bMy Listings") || title.equals("§dBattle Pass Levels") || title.equals("§6Token Shop") || "§5Case Shop".equals(title) || "§eKey Inventory".equals(title) || "§6§lOpen Cases".equals(title) || CommandHandler.TITLE_HOMES.equals(title) || CommandHandler.TITLE_HOME_CONFIRM.equals(title) || EconomyPrices.TITLE_ITEM_SHOP.equals(title) || EconomyPrices.TITLE_TOKEN_PREMIUM.equals(title) || EconomyPrices.TITLE_SHOP_CHECKOUT.equals(title) || this.getShopCategoryFromTitle(title) != null;
      } else {
         return false;
      }
   }

   private EconomyPrices.ShopCategory getShopCategoryFromTitle(String title) {
      for(EconomyPrices.ShopCategory category : EconomyPrices.ShopCategory.values()) {
         if (category.title().equals(title)) {
            return category;
         }
      }

      return null;
   }

   private void cancelListing(Player player, int guiSlot) {
      int foundIndex = -1;
      int currentMyListSlot = 0;

      for(int i = 0; i < this.plugin.auctionItems.size(); ++i) {
         if (((AuctionListing)this.plugin.auctionItems.get(i)).seller().equals(player.getUniqueId())) {
            if (currentMyListSlot == guiSlot) {
               foundIndex = i;
               break;
            }

            ++currentMyListSlot;
         }
      }

      if (foundIndex != -1) {
         AuctionListing listing = (AuctionListing)this.plugin.auctionItems.get(foundIndex);
         this.plugin.auctionItems.remove(foundIndex);
         this.plugin.saveData();
         this.giveOrDrop(player, listing.item().clone());
         player.sendMessage("§eListing cancelled. Item returned.");
         this.handler.openMyListings(player);
      }

   }

   private void buyListing(Player buyer, int slot) {
      AuctionListing listing = (AuctionListing)this.plugin.auctionItems.get(slot);
      if (listing.seller().equals(buyer.getUniqueId())) {
         buyer.sendMessage("§cUse 'My Listings' to manage your items.");
      } else {
         double buyerBal = (Double)this.plugin.balances.getOrDefault(buyer.getUniqueId(), (double)0.0F);
         if (buyerBal < listing.price()) {
            buyer.sendMessage("§cInsufficient funds.");
         } else {
            this.plugin.balances.put(buyer.getUniqueId(), buyerBal - listing.price());
            this.plugin.balances.put(listing.seller(), (Double)this.plugin.balances.getOrDefault(listing.seller(), (double)0.0F) + listing.price());
            this.plugin.auctionItems.remove(slot);
            this.plugin.saveData();
            this.giveOrDrop(buyer, listing.item().clone());
            Object[] var10002 = new Object[]{listing.price()};
            buyer.sendMessage("§aBought for §6$" + String.format("%.2f", var10002));
            Player sellerOnline = this.plugin.getServer().getPlayer(listing.seller());
            if (sellerOnline != null) {
               sellerOnline.sendMessage("§aYour item was sold to " + buyer.getName());
            }

            buyer.closeInventory();
         }
      }

   }

   private void startQuest(Player player) {
      if (this.plugin.activeQuest.contains(player.getUniqueId())) {
         player.sendMessage("§eAlready on a quest!");
      } else {
         this.plugin.activeQuest.add(player.getUniqueId());
         this.plugin.questProgress.put(player.getUniqueId(), 0);
         player.sendMessage("§aQuest Started!");
         this.plugin.saveData();
         player.closeInventory();
      }

   }

   private void updateItemValueLore(ItemStack item) {
      if (item != null && item.getType() != Material.AIR && EconomyPrices.isListed(item.getType())) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            if (meta.hasLore()) {
               List<String> existingLore = meta.getLore();
               if (existingLore != null) {
                  for(String line : existingLore) {
                     if (line.contains("Buy Price:") || line.contains("Sell Price:") || line.contains("[SELLING DISABLED]")) {
                        return;
                     }
                  }
               }
            }

            List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList(meta.getLore()) : new ArrayList();
            lore.addAll(EconomyPrices.buildPriceLore(item.getType()));
            meta.setLore(lore);
            item.setItemMeta(meta);
         }
      }

   }

   private void processSale(Player player, Inventory inv) {
      double total = 0.0;
      double multiplier = this.plugin.getMultiplier(player.getUniqueId());
      boolean hadUnsellable = false;

      for(int i = 0; i < 45; ++i) {
         ItemStack item = inv.getItem(i);
         if (item != null && item.getType() != Material.AIR) {
            if (EconomyPrices.canSell(item.getType())) {
               total += this.plugin.getItemPrice(item.getType()) * (double)item.getAmount();
               inv.setItem(i, (ItemStack)null);
            } else {
               hadUnsellable = true;
            }
         }
      }

      if (total > 0.0) {
         double finalProfit = total * multiplier;
         this.plugin.balances.put(player.getUniqueId(), this.plugin.balances.getOrDefault(player.getUniqueId(), 0.0) + finalProfit);
         this.plugin.saveData();
         player.sendMessage(Component.text("Profit: ", NamedTextColor.GREEN)
            .append(Component.text(EconomyPrices.formatMoney(finalProfit), NamedTextColor.GOLD)));
         if (hadUnsellable) {
            player.sendMessage(Component.text("Some items cannot be sold and were left in the machine.", NamedTextColor.YELLOW));
         }

         player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1.2F);
         player.closeInventory();
      } else if (hadUnsellable) {
         player.sendMessage(Component.text("Those items cannot be sold here.", NamedTextColor.RED));
      } else {
         player.sendMessage(Component.text("Machine empty!", NamedTextColor.RED));
      }

   }

   private void scheduleSellValueUpdate(Player player) {
      this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
         if (!player.isOnline()) {
            return;
         }

         InventoryView view = player.getOpenInventory();
         if (view != null && "§0§l» §2§lSELL MACHINE".equals(view.getTitle())) {
            this.updateSellValue(player, view.getTopInventory());
         }
      });
   }

   private void updateSellValue(Player player, Inventory top) {
      double total = 0.0;

      for(int i = 0; i < 45; ++i) {
         ItemStack item = top.getItem(i);
         if (item != null && item.getType() != Material.AIR && EconomyPrices.canSell(item.getType())) {
            total += this.plugin.getItemPrice(item.getType()) * (double)item.getAmount();
         }
      }

      double finalValue = total * this.plugin.getMultiplier(player.getUniqueId());
      top.setItem(49, this.handler.buildSellConfirmButton(finalValue));
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      String title = event.getView().getTitle();
      if ("§e§lRolling...".equals(title)) {
         Player player = (Player)event.getPlayer();
         UUID uuid = player.getUniqueId();
         CaseRollSession session = (CaseRollSession)this.plugin.caseRollSessions.remove(uuid);
         if (session != null && session.task != null) {
            session.task.cancel();
         }

         if (session != null && !session.revealDone) {
            this.plugin.addCrateKey(uuid, session.crateType, 1);
            this.plugin.saveData();
            player.sendMessage("§eRoll cancelled — key returned.");
         }

      } else {
         if (event.getView().getTitle().equals("§0§l» §2§lSELL MACHINE")) {
            Player player = (Player)event.getPlayer();

            for(int i = 0; i < 45; ++i) {
               ItemStack item = event.getInventory().getItem(i);
               if (item != null && item.getType() != Material.AIR) {
                  if (player.getInventory().firstEmpty() == -1) {
                     player.getWorld().dropItemNaturally(player.getLocation(), item);
                  } else {
                     player.getInventory().addItem(new ItemStack[]{item});
                  }
               }
            }
         }

      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onInventoryDrag(InventoryDragEvent event) {
      if (!(event.getWhoClicked() instanceof Player player)) {
         return;
      }

      if (!"§0§l» §2§lSELL MACHINE".equals(event.getView().getTitle())) {
         return;
      }

      // Keep the button/border row (slots 45-53) clear; the upper grid stays drop-friendly.
      int topSize = event.getView().getTopInventory().getSize();
      for (int slot : event.getRawSlots()) {
         if (slot >= 45 && slot < topSize) {
            event.setCancelled(true);
            break;
         }
      }

      this.scheduleSellValueUpdate(player);
   }

   @EventHandler
   public void onEntityDeath(EntityDeathEvent event) {
      Player player = event.getEntity().getKiller();
      if (player != null && this.plugin.activeQuest.contains(player.getUniqueId())) {
         int level = (Integer)this.plugin.playerLevels.getOrDefault(player.getUniqueId(), 1);
         if (level <= 5) {
            EntityType req = this.getRequiredEntityType(level);
            if (event.getEntityType() == req) {
               int progress = (Integer)this.plugin.questProgress.getOrDefault(player.getUniqueId(), 0) + 1;
               int target = this.getQuestTarget(level);
               this.plugin.questProgress.put(player.getUniqueId(), progress);
               if (progress >= target) {
                  double reward = this.getQuestReward(level);
                  this.plugin.balances.put(player.getUniqueId(), (Double)this.plugin.balances.getOrDefault(player.getUniqueId(), (double)0.0F) + reward);
                  this.plugin.playerLevels.put(player.getUniqueId(), level + 1);
                  this.plugin.activeQuest.remove(player.getUniqueId());
                  player.sendMessage("§aLevel Up! Reward: §6$" + reward);
                  this.plugin.saveData();
               } else {
                  player.sendMessage("§dProgress: " + progress + "/" + target);
                  this.plugin.saveData();
               }
            }
         }
      }

   }

   private int getQuestSlotForLevel(int level) {
      switch (level) {
         case 1 -> {
            return 10;
         }
         case 2 -> {
            return 11;
         }
         case 3 -> {
            return 13;
         }
         case 4 -> {
            return 15;
         }
         case 5 -> {
            return 16;
         }
         default -> {
            return -1;
         }
      }
   }

   private EntityType getRequiredEntityType(int level) {
      switch (level) {
         case 1 -> {
            return EntityType.ZOMBIE;
         }
         case 2 -> {
            return EntityType.SKELETON;
         }
         case 3 -> {
            return EntityType.CREEPER;
         }
         case 4 -> {
            return EntityType.SPIDER;
         }
         case 5 -> {
            return EntityType.BLAZE;
         }
         default -> {
            return EntityType.UNKNOWN;
         }
      }
   }

   private int getQuestTarget(int level) {
      switch (level) {
         case 3 -> {
            return 5;
         }
         case 4 -> {
            return 15;
         }
         default -> {
            return 10;
         }
      }
   }

   private double getQuestReward(int level) {
      return EconomyPrices.getQuestReward(level);
   }

   static {
      GLASS_PANES = new Material[]{Material.WHITE_STAINED_GLASS_PANE, Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE, Material.BROWN_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE, Material.PINK_STAINED_GLASS_PANE};
   }
}
