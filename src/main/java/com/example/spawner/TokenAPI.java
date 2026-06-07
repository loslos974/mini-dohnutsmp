package com.example.spawner;

import com.example.Main;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Placeholder token API — swap these methods with your real token plugin calls.
 */
public final class TokenAPI {

   private static Main plugin;

   private TokenAPI() {
   }

   public static void init(Main main) {
      plugin = main;
   }

   public static int getTokens(Player player) {
      return plugin.tokens.getOrDefault(player.getUniqueId(), 0);
   }

   public static boolean takeTokens(Player player, int amount) {
      if (amount <= 0) {
         return true;
      }

      UUID uuid = player.getUniqueId();
      int current = plugin.tokens.getOrDefault(uuid, 0);
      if (current < amount) {
         return false;
      }

      plugin.tokens.put(uuid, current - amount);
      plugin.saveData();
      return true;
   }
}
