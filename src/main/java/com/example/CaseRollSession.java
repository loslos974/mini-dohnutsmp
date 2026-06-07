package com.example;

import org.bukkit.scheduler.BukkitTask;

public final class CaseRollSession {
   public final CrateType crateType;
   public BukkitTask task;
   public CaseLootTable.RollOutcome outcome;
   public boolean revealDone;

   public CaseRollSession(CrateType crateType) {
      this.crateType = crateType;
   }
}
