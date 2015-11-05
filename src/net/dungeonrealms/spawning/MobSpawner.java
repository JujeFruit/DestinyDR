package net.dungeonrealms.spawning;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.metadata.FixedMetadataValue;

import net.dungeonrealms.API;
import net.dungeonrealms.DungeonRealms;
import net.dungeonrealms.entities.EnumEntityType;
import net.dungeonrealms.entities.types.monsters.EnumMonster;
import net.dungeonrealms.entities.utils.EntityStats;
import net.dungeonrealms.mastery.MetadataUtils;
import net.dungeonrealms.mastery.Utils;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.World;

/**
 * Created by Chase on Sep 25, 2015
 */
public class MobSpawner {
	public Location loc;
	public String spawnType;
	public EntityArmorStand armorstand;
	public int tier;
	public List<Entity> SPAWNED_MONSTERS = new CopyOnWriteArrayList<>();
	public static ConcurrentHashMap<org.bukkit.entity.Entity, org.bukkit.entity.Entity> NAMETAGS = new ConcurrentHashMap<>();
	public boolean isElite = false;
	public int spawnAmount;
	public int id;
	public int timerID;
	public String lvlRange;
	public String eliteName;
	boolean firstSpawn = true;
	public boolean toSpawn;

	public MobSpawner(Location location, String type, int tier, int spawnAmount, int configid, String lvlRange) {
		if (type.contains("("))
			isElite = true;
		if (isElite) {
			eliteName = type.substring(type.indexOf("(") + 1, type.indexOf(")"));
			eliteName = eliteName.replace("_", " ");

			type = type.substring(0, type.indexOf("("));
			if (type.contains("*"))
				type = type.replace("*", "");

			spawnAmount = 1;
		}
		if (type.contains("*")) {
			type = type.replace("*", "");
		}
		if (spawnAmount > 6)
			spawnAmount = 6;
		this.lvlRange = lvlRange;
		this.spawnAmount = spawnAmount;
		this.loc = location;
		this.id = configid;
		this.spawnType = type;
		this.tier = tier;
		World world = ((CraftWorld) location.getWorld()).getHandle();
		armorstand = new EntityArmorStand(world);
		armorstand.getBukkitEntity().setMetadata("type",
		        new FixedMetadataValue(DungeonRealms.getInstance(), "spawner"));
		armorstand.getBukkitEntity().setMetadata("tier", new FixedMetadataValue(DungeonRealms.getInstance(), tier));
		armorstand.getBukkitEntity().setMetadata("monsters", new FixedMetadataValue(DungeonRealms.getInstance(), type));
		List<org.bukkit.entity.Entity> list = armorstand.getBukkitEntity().getNearbyEntities(loc.getX(), loc.getY(),
		        loc.getZ());
		if (list.size() > 0) {
			list.stream().filter(entity -> entity instanceof ArmorStand).forEach(entity -> {
				entity.remove();
				((ArmorStand) entity).setHealth(0);
				if (armorstand.getBukkitEntity().getWorld().getBlockAt(loc).getType() == Material.ARMOR_STAND)
					armorstand.getBukkitEntity().getWorld().getBlockAt(loc).setType(Material.AIR);
			});
		}
		armorstand.setPosition(loc.getX(), loc.getY(), loc.getZ());
		world.addEntity(armorstand, SpawnReason.CUSTOM);
		armorstand.setPosition(loc.getX(), loc.getY(), loc.getZ());
	}

	/**
	 * Does 1 rotation of spawning for this mob spawner.
	 */
	public void spawnIn() {
		if (toSpawn)
			return;
		if (loc.getChunk().isLoaded()) {
			if (!SPAWNED_MONSTERS.isEmpty()) {
				for (Entity monster : SPAWNED_MONSTERS) {
					if (monster.isAlive()) {
						if (API.isInSafeRegion(monster.getBukkitEntity().getLocation())) {
							monster.setPosition(loc.getX() + 2, loc.getY(), loc.getZ() + 2);
						}
						double num = monster.getBukkitEntity().getLocation().distance(loc);
						if (num > 32) {
							monster.setPosition(loc.getX() + 2, loc.getY(), loc.getZ() + 2);
						}
					} else {
						SPAWNED_MONSTERS.remove(monster);
					}
				}
			}
			if (spawnType.contains("*")) {
				spawnType = spawnType.replace("*", "");
			}

			if (isElite) {
				if (SPAWNED_MONSTERS.size() == 0) {
					Location location = new Location(Bukkit.getWorlds().get(0),
					        loc.getBlockX() + new Random().nextInt(10), loc.getWorld().getHighestBlockYAt(loc),
					        loc.getBlockZ() + new Random().nextInt(10));
					if (location.getBlock().getType() != Material.AIR
					        || location.add(0, 1, 0).getBlock().getType() != Material.AIR)
						return;
					Material type = location.getBlock().getType();
					if (type == Material.ACACIA_STAIRS || type == Material.BIRCH_WOOD_STAIRS
					        || type == Material.COBBLESTONE_STAIRS || type == Material.DARK_OAK_STAIRS
					        || type == Material.JUNGLE_WOOD_STAIRS || type == Material.WOOD_STAIRS
					        || type == Material.STONE_SLAB2 || type == Material.DOUBLE_STONE_SLAB2
					        || type == Material.DOUBLE_STEP || type == Material.WOOD_DOUBLE_STEP)
						return;
					String mob = spawnType;
					World world = armorstand.getWorld();
					EnumMonster monsEnum = EnumMonster.getMonsterByString(mob);
					if (monsEnum == null)
						return;
					Entity entity = SpawningMechanics.getMob(world, tier, monsEnum);
					int level = Utils.getRandomFromTier(tier, lvlRange);
					MetadataUtils.registerEntityMetadata(entity, EnumEntityType.HOSTILE_MOB, tier, level);
					EntityStats.setMonsterRandomStats(entity, level, tier);

					if (entity == null)
						return;
					String customName = eliteName;
					entity.setCustomName(customName);
						ArmorStand stand = entity.getBukkitEntity().getLocation().getWorld()
					            .spawn(entity.getBukkitEntity().getLocation(), ArmorStand.class);
						stand.setRemoveWhenFarAway(false);
						stand.setVisible(false);
						stand.setSmall(true);
						stand.setBasePlate(false);
						stand.setMetadata("type", new FixedMetadataValue(DungeonRealms.getInstance(), "nametag"));
						stand.setGravity(false);
						stand.setArms(false);
						stand.setCustomNameVisible(true);
						stand.setCustomName(customName);
						stand.setRemoveWhenFarAway(false);
						entity.getBukkitEntity().setPassenger(stand);
						EntityStats.setMonsterElite(entity, level + 10, tier);
						stand.setCustomName(entity.getCustomName());
						entity.getBukkitEntity().setMetadata("isElite",
					            new FixedMetadataValue(DungeonRealms.getInstance(), "true"));
						Utils.log.info(eliteName + " set to spawn");
						toSpawn = true;
						NAMETAGS.put(entity.getBukkitEntity(), stand);
						if (!firstSpawn) {
							Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(), () -> {
								Utils.log.info(stand.getCustomName() + " spawned at " + location.toString());
								entity.setLocation(location.getX(), location.getY(), location.getZ(), 1, 1);
								world.addEntity(entity, SpawnReason.CUSTOM);
								entity.setLocation(location.getX(), location.getY(), location.getZ(), 1, 1);
								toSpawn = false;
								SPAWNED_MONSTERS.add(entity);
							} , 1200 * 2L);
						} else {
							Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(), () -> {
								Utils.log.info(stand.getCustomName() + " spawned at " + location.toString());
								entity.setLocation(location.getX(), location.getY(), location.getZ(), 1, 1);
								world.addEntity(entity, SpawnReason.CUSTOM);
								entity.setLocation(location.getX(), location.getY(), location.getZ(), 1, 1);
								toSpawn = false;
								firstSpawn = false;
								SPAWNED_MONSTERS.add(entity);
							});
						}
				}
			} else if (SPAWNED_MONSTERS.size() < spawnAmount) {
				Location location = new Location(Bukkit.getWorlds().get(0), loc.getBlockX() + new Random().nextInt(6),
				        loc.getBlockY(), loc.getBlockZ() + new Random().nextInt(6));
				if (location.getBlock().getType() != Material.AIR
				        || location.add(0, 1, 0).getBlock().getType() != Material.AIR)
					return;
				String mob = spawnType;
				World world = armorstand.getWorld();
				EnumEntityType type = EnumEntityType.HOSTILE_MOB;
				EnumMonster monsEnum = EnumMonster.getMonsterByString(mob);
				if (monsEnum == null) {
					return;
				}
				Entity entity = SpawningMechanics.getMob(world, tier, monsEnum);

				int level = Utils.getRandomFromTier(tier, lvlRange);
				MetadataUtils.registerEntityMetadata(entity, type, tier, level);
				EntityStats.setMonsterRandomStats(entity, level, tier);

				String lvl = ChatColor.LIGHT_PURPLE.toString() + "[" + level + "]" + ChatColor.RESET;
				String healthName = entity.getBukkitEntity().getMetadata("currentHP").get(0).asInt()
				        + ChatColor.RED.toString() + "❤ ";
				String customName;
				try {
					customName = entity.getBukkitEntity().getMetadata("customname").get(0).asString();
				} catch (Exception exc) {
					Utils.log.info(entity.getCustomName() + " doesn't have metadata 'customname' ");
					customName = monsEnum.name;
				}
				final String finalName = healthName + lvl + customName;
				toSpawn = true;
				Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(), () -> {
					ArmorStand stand = entity.getBukkitEntity().getLocation().getWorld()
				            .spawn(entity.getBukkitEntity().getLocation(), ArmorStand.class);
					stand.setRemoveWhenFarAway(false);
					stand.setVisible(false);
					stand.setSmall(true);
					stand.setBasePlate(false);
					stand.setMetadata("type", new FixedMetadataValue(DungeonRealms.getInstance(), "nametag"));
					stand.setGravity(false);
					stand.setArms(false);
					stand.setCustomNameVisible(true);
					stand.setCustomName(finalName);
					stand.setRemoveWhenFarAway(false);
					entity.getBukkitEntity().setPassenger(stand);
					NAMETAGS.put(entity.getBukkitEntity(), stand);
					entity.setLocation(location.getX(), location.getY(), location.getZ(), 1, 1);
					world.addEntity(entity, SpawnReason.CUSTOM);
					entity.setLocation(location.getX(), location.getY(), location.getZ(), 1, 1);
					SPAWNED_MONSTERS.add(entity);
					toSpawn = false;
				} , 200L);
			}
		} else {
			if (!SPAWNED_MONSTERS.isEmpty()) {
				for (Entity monster : SPAWNED_MONSTERS) {
						monster.passenger.die();
						monster.passenger.dead = true;
						if (monster.getBukkitEntity().getPassenger() != null
					            && monster.getBukkitEntity().getPassenger().hasMetadata("type")) {
							monster.getBukkitEntity().getPassenger().remove();
							((CraftEntity) monster.getBukkitEntity().getPassenger()).getHandle().die();
						}
						if (NAMETAGS.containsKey(monster.getBukkitEntity())) {
							org.bukkit.entity.Entity nameTag = NAMETAGS.get(monster.getBukkitEntity());
							((CraftEntity) nameTag).getHandle().die();
						}
						// monster.die();
						monster.dead = true;
						monster.getBukkitEntity().remove();
						// armorstand.getWorld().kill(monster);
						SPAWNED_MONSTERS.remove(monster);
				}
			}
		}
	}

	// public void newSpawnIn() {
	// if (toSpawn)
	// return;
	// for (Entity monster : SPAWNED_MONSTERS) {
	// if (monster.isAlive()) {
	// if (API.isInSafeRegion(monster.getBukkitEntity().getLocation())) {
	// monster.setPosition(loc.getX() + 2, loc.getY(), loc.getZ() + 2);
	// }
	// if (monster.getBukkitEntity().getLocation().distance(loc) > 30) {
	// monster.setPosition(loc.getX() + 1, loc.getY(), loc.getZ() + 1);
	// }
	// } else {
	// SPAWNED_MONSTERS.remove(monster);
	// }
	// }
	// if (SPAWNED_MONSTERS.size() < (spawnAmount / 2)) {
	// if (!API.getNearbyPlayers(loc, 40).isEmpty()) {
	// EnumMonster monsEnum = EnumMonster.getMonsterByString(spawnType);
	// if (monsEnum == null) {
	// return;
	// }
	// World world = ((CraftWorld) loc.getWorld()).getHandle();
	// Entity entity = SpawningMechanics.getMob(world, tier, monsEnum);
	// if (entity == null) {
	// return;
	// }
	// int level = Utils.getRandomFromTier(tier, lvlRange);
	// MetadataUtils.registerEntityMetadata(entity, EnumEntityType.HOSTILE_MOB,
	// tier, level);
	// EntityStats.setMonsterRandomStats(entity, level, tier);
	// String lvl = ChatColor.LIGHT_PURPLE.toString() + "[" + level + "] " +
	// ChatColor.RESET;
	// String healthName =
	// entity.getBukkitEntity().getMetadata("currentHP").get(0).asInt()
	// + ChatColor.RED.toString() + "❤ ";
	// String customName;
	// try {
	// customName =
	// entity.getBukkitEntity().getMetadata("customname").get(0).asString();
	// } catch (Exception exc) {
	// Utils.log.info(entity.getCustomName() + " doesn't have metadata
	// 'customname' ");
	// customName = monsEnum.name;
	// }
	// toSpawn = true;
	// final String finalName = healthName + lvl + customName;
	// if (!firstSpawn) {
	// if (!isElite) {
	// Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(),
	// () -> {
	// toSpawn = false;
	// ArmorStand stand = entity.getBukkitEntity().getLocation().getWorld()
	// .spawn(entity.getBukkitEntity().getLocation(), ArmorStand.class);
	// entity.getBukkitEntity().setMetadata("level",
	// new FixedMetadataValue(DungeonRealms.getInstance(), level));
	// stand.setRemoveWhenFarAway(false);
	// stand.setVisible(false);
	// stand.setSmall(true);
	// stand.setBasePlate(false);
	// stand.setMetadata("type", new
	// FixedMetadataValue(DungeonRealms.getInstance(), "nametag"));
	// stand.setGravity(false);
	// stand.setArms(false);
	// stand.setCustomNameVisible(true);
	// stand.setCustomName(finalName);
	// stand.setRemoveWhenFarAway(false);
	// entity.setLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 1,
	// 1);
	// world.addEntity(entity, SpawnReason.CUSTOM);
	// entity.setLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 1,
	// 1);
	// entity.getBukkitEntity().setPassenger(stand);
	// NAMETAGS.put(entity.getBukkitEntity(), stand);
	// SPAWNED_MONSTERS.add(entity);
	// } , 400);
	// } else {
	// Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(),
	// () -> {
	// toSpawn = false;
	// ArmorStand stand = entity.getBukkitEntity().getLocation().getWorld()
	// .spawn(entity.getBukkitEntity().getLocation(), ArmorStand.class);
	// entity.getBukkitEntity().setMetadata("level",
	// new FixedMetadataValue(DungeonRealms.getInstance(), level));
	// stand.setRemoveWhenFarAway(false);
	// stand.setVisible(false);
	// stand.setSmall(true);
	// stand.setBasePlate(false);
	// stand.setMetadata("type", new
	// FixedMetadataValue(DungeonRealms.getInstance(), "nametag"));
	// stand.setGravity(false);
	// stand.setArms(false);
	// stand.setCustomNameVisible(true);
	// stand.setCustomName(eliteName);
	// stand.setRemoveWhenFarAway(false);
	// entity.setLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 1,
	// 1);
	// world.addEntity(entity, SpawnReason.CUSTOM);
	// entity.setLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 1,
	// 1);
	// entity.getBukkitEntity().setPassenger(stand);
	// NAMETAGS.put(entity.getBukkitEntity(), stand);
	// SPAWNED_MONSTERS.add(entity);
	// EntityStats.setMonsterElite(entity, level + 10, tier);
	// entity.getBukkitEntity().setMetadata("level",
	// new FixedMetadataValue(DungeonRealms.getInstance(), level + 10));
	// stand.setCustomName(entity.getCustomName());
	// entity.getBukkitEntity().setMetadata("isElite",
	// new FixedMetadataValue(DungeonRealms.getInstance(), "true"));
	// Utils.log.info("Elite " + eliteName + " Spawned");
	// } , 1200 * 2);
	// }
	// } else {
	// firstSpawn = true;
	// if (!isElite) {
	// Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(),
	// () -> {
	// ArmorStand stand = entity.getBukkitEntity().getLocation().getWorld()
	// .spawn(entity.getBukkitEntity().getLocation(), ArmorStand.class);
	// stand.setRemoveWhenFarAway(false);
	// entity.getBukkitEntity().setMetadata("level",
	// new FixedMetadataValue(DungeonRealms.getInstance(), level));
	// if (isElite) {
	// EntityStats.setMonsterElite(entity, level + 10, tier);
	// entity.getBukkitEntity().setMetadata("level",
	// new FixedMetadataValue(DungeonRealms.getInstance(), level + 10));
	// stand.setCustomName(entity.getCustomName());
	// entity.getBukkitEntity().setMetadata("isElite",
	// new FixedMetadataValue(DungeonRealms.getInstance(), "true"));
	// }
	// toSpawn = false;
	// stand.setVisible(false);
	// stand.setSmall(true);
	// stand.setBasePlate(false);
	// stand.setMetadata("type", new
	// FixedMetadataValue(DungeonRealms.getInstance(), "nametag"));
	// stand.setGravity(false);
	// stand.setArms(false);
	// stand.setCustomNameVisible(true);
	// stand.setCustomName(finalName);
	// stand.setRemoveWhenFarAway(false);
	// NAMETAGS.put(entity.getBukkitEntity(), stand);
	// entity.setLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 1,
	// 1);
	// world.addEntity(entity, SpawnReason.CUSTOM);
	// entity.setLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 1,
	// 1);
	// entity.getBukkitEntity().setPassenger(stand);
	// SPAWNED_MONSTERS.add(entity);
	// });
	// } else {
	// Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(),
	// () -> {
	// toSpawn = false;
	// ArmorStand stand = entity.getBukkitEntity().getLocation().getWorld()
	// .spawn(entity.getBukkitEntity().getLocation(), ArmorStand.class);
	// entity.getBukkitEntity().setMetadata("level",
	// new FixedMetadataValue(DungeonRealms.getInstance(), level));
	// stand.setRemoveWhenFarAway(false);
	// stand.setVisible(false);
	// stand.setSmall(true);
	// stand.setBasePlate(false);
	// stand.setMetadata("type", new
	// FixedMetadataValue(DungeonRealms.getInstance(), "nametag"));
	// stand.setGravity(false);
	// stand.setArms(false);
	// stand.setCustomNameVisible(true);
	// stand.setCustomName(eliteName);
	// stand.setRemoveWhenFarAway(false);
	// NAMETAGS.put(entity.getBukkitEntity(), stand);
	// entity.setLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 1,
	// 1);
	// world.addEntity(entity, SpawnReason.CUSTOM);
	// entity.setLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), 1,
	// 1);
	// entity.getBukkitEntity().setPassenger(stand);
	// SPAWNED_MONSTERS.add(entity);
	// EntityStats.setMonsterElite(entity, level + 10, tier);
	// entity.getBukkitEntity().setMetadata("level",
	// new FixedMetadataValue(DungeonRealms.getInstance(), level + 10));
	// stand.setCustomName(entity.getCustomName());
	// entity.getBukkitEntity().setMetadata("isElite",
	// new FixedMetadataValue(DungeonRealms.getInstance(), "true"));
	// Utils.log.info("Elite " + eliteName + " Spawned");
	// });
	// }
	// }
	// } else {
	// if (SPAWNED_MONSTERS.size() > 0)
	// kill();
	// }
	// }
	// }

	/**
	 * Kill all spawnedMonsters for this Mob Spawner
	 */
	public void kill() {
		if (SPAWNED_MONSTERS.size() > 0)
			for (Entity spawnedMonster : SPAWNED_MONSTERS) {
					if (spawnedMonster.getBukkitEntity().getPassenger() != null) {
						spawnedMonster.getBukkitEntity().getPassenger().remove();
					}
					if (NAMETAGS.containsKey(spawnedMonster.getBukkitEntity())) {
						org.bukkit.entity.Entity ent = NAMETAGS.get(spawnedMonster.getBukkitEntity());
						((CraftEntity) ent).getHandle().die();
						armorstand.getWorld().kill(((CraftEntity) ent).getHandle());
						ent.remove();
						NAMETAGS.remove(spawnedMonster.getBukkitEntity());
					}
					spawnedMonster.getBukkitEntity().remove();
					spawnedMonster.dead = true;
					armorstand.getWorld().kill(spawnedMonster);
			}
		SPAWNED_MONSTERS.clear();
	}

	public void remove() {
		kill();
		armorstand.getWorld().removeEntity(armorstand);
		armorstand.getBukkitEntity().remove();
		SpawningMechanics.SPAWNER_CONFIG.set(id, null);
		DungeonRealms.getInstance().getConfig().set("spawners", SpawningMechanics.SPAWNER_CONFIG);
		DungeonRealms.getInstance().saveConfig();
		isRemoved = true;
	}

	public boolean isRemoved = false;

	/**
	 * @return
	 */
	public List<Entity> getSpawnedMonsters() {
		return SPAWNED_MONSTERS;
	}

	/**
	 * @return
	 */
	public boolean isSpawning() {
		return !API.getNearbyPlayers(loc, 40).isEmpty();
	}

	/**
	 * Initialize spawner
	 */
	public void init() {
		if (isSpawning()) {
			timerID = Bukkit.getScheduler().scheduleAsyncRepeatingTask(DungeonRealms.getInstance(), () -> {
				if (isRemoved) {
					Bukkit.getScheduler().cancelTask(timerID);
				} else
					Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(), this::spawnIn);
			} , 0, 100L);
		} else {
			Bukkit.getScheduler().scheduleSyncDelayedTask(DungeonRealms.getInstance(), this::kill, 5L);
		}
		Bukkit.getScheduler().scheduleAsyncRepeatingTask(DungeonRealms.getInstance(), this::checkNameTags, 0, 200L);
	}

	/**
	 * Force NameTags to be removed if the owner is dead.
	 */
	private void checkNameTags() {
        NAMETAGS.entrySet().stream().filter(mobNameTags -> !NAMETAGS.isEmpty()).filter(mobNameTags -> mobNameTags.getValue().getVehicle() == null).forEach(mobNameTags -> {
			Bukkit.getScheduler().scheduleAsyncDelayedTask(DungeonRealms.getInstance(), ()->{
            ((CraftEntity) mobNameTags.getValue()).getHandle().die();
            ((CraftEntity) mobNameTags.getValue()).getHandle().dead = true;
            NAMETAGS.remove(mobNameTags.getKey());
			});
        });
	}
}
