package com.example.spawner;

import org.bukkit.scheduler.BukkitRunnable;

public class VirtualSpawnerTask extends BukkitRunnable {
   private final VirtualSpawnerManager manager;

   public VirtualSpawnerTask(VirtualSpawnerManager manager) {
      this.manager = manager;
   }

   @Override
   public void run() {
      manager.tickSpawners();
   }
}