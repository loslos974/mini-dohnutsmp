package com.example.spawner;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Custom holder so the spawner GUI can be identified by type rather than by a
 * (fragile, deprecated) title string. Also carries the backing spawner location.
 */
public final class SpawnerGuiHolder implements InventoryHolder {

   private final Location location;
   private Inventory inventory;

   public SpawnerGuiHolder(Location location) {
      this.location = location;
   }

   public Location getLocation() {
      return this.location;
   }

   public void setInventory(Inventory inventory) {
      this.inventory = inventory;
   }

   @Override
   public @NotNull Inventory getInventory() {
      return this.inventory;
   }
}
