package com.example;

public class PendingHomeAction {
   public enum Type {
      SET,
      DELETE,
      TELEPORT,
      UNLOCK
   }

   private final Type type;
   private final int homeIndex;

   public PendingHomeAction(Type type, int homeIndex) {
      this.type = type;
      this.homeIndex = homeIndex;
   }

   public Type type() {
      return this.type;
   }

   public int homeIndex() {
      return this.homeIndex;
   }
}
