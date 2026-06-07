package com.example;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class PlayerHome {
   private final String world;
   private final double x;
   private final double y;
   private final double z;
   private final float yaw;
   private final float pitch;

   public PlayerHome(String world, double x, double y, double z, float yaw, float pitch) {
      this.world = world;
      this.x = x;
      this.y = y;
      this.z = z;
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public static PlayerHome fromLocation(Location loc) {
      return new PlayerHome(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
   }

   public static PlayerHome fromSection(ConfigurationSection section) {
      if (section == null) {
         return null;
      } else {
         String worldName = section.getString("world");
         if (worldName == null) {
            return null;
         } else {
            return new PlayerHome(worldName, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"), (float)section.getDouble("yaw"), (float)section.getDouble("pitch"));
         }
      }
   }

   public Location toLocation() {
      World w = Bukkit.getWorld(this.world);
      return w == null ? null : new Location(w, this.x, this.y, this.z, this.yaw, this.pitch);
   }

   public String world() {
      return this.world;
   }

   public double x() {
      return this.x;
   }

   public double y() {
      return this.y;
   }

   public double z() {
      return this.z;
   }

   public float yaw() {
      return this.yaw;
   }

   public float pitch() {
      return this.pitch;
   }
}
