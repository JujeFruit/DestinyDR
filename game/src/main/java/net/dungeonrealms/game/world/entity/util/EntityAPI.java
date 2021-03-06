package net.dungeonrealms.game.world.entity.util;

import lombok.Getter;
import net.dungeonrealms.GameAPI;
import net.dungeonrealms.database.PlayerWrapper;
import net.dungeonrealms.game.enchantments.EnchantmentAPI;
import net.dungeonrealms.game.handler.HealthHandler;
import net.dungeonrealms.game.item.ItemType;
import net.dungeonrealms.game.item.items.core.ItemArmor;
import net.dungeonrealms.game.item.items.core.ItemWeapon;
import net.dungeonrealms.game.mastery.AttributeList;
import net.dungeonrealms.game.mastery.MetadataUtils;
import net.dungeonrealms.game.mastery.MetadataUtils.Metadata;
import net.dungeonrealms.game.mastery.Utils;
import net.dungeonrealms.game.mechanic.dungeons.Dungeon;
import net.dungeonrealms.game.mechanic.dungeons.DungeonBoss;
import net.dungeonrealms.game.mechanic.dungeons.DungeonManager;
import net.dungeonrealms.game.world.entity.EnumEntityType;
import net.dungeonrealms.game.world.entity.type.monster.DRMonster;
import net.dungeonrealms.game.world.entity.type.monster.type.EnumMonster;
import net.dungeonrealms.game.world.entity.type.monster.type.EnumMonster.CustomEntityType;
import net.dungeonrealms.game.world.entity.type.monster.type.EnumNamedElite;
import net.dungeonrealms.game.world.item.Item;
import net.dungeonrealms.game.world.item.Item.ElementalAttribute;
import net.dungeonrealms.game.world.item.Item.ItemRarity;
import net.dungeonrealms.game.world.item.itemgenerator.ItemGenerator;
import net.minecraft.server.v1_9_R2.EntityInsentient;
import net.minecraft.server.v1_9_R2.PathfinderGoalSelector;
import net.minecraft.server.v1_9_R2.World;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftLivingEntity;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * EntityAPI - Basic Entity utilities.
 * <p>
 * Redone by Kneesnap on April 19th, 2017.
 */
public class EntityAPI {

    public static WeakHashMap<Entity, ItemStack> lastUsedHelmetItem = new WeakHashMap<>();

    private static Random random = ThreadLocalRandom.current();

    @Getter
    //TODO: Prevent memory leaks, on death, on despawn. Every few minutes go through this list and clean up the trash.
    private static Map<DRMonster, AttributeList> entityAttributes = new ConcurrentHashMap<>();

    public static Entity spawnElite(Location loc, Location spawnerLocation, EnumNamedElite elite) {
        return spawnElite(loc, spawnerLocation, elite, elite.getMonster(), elite.getTier(), elite.randomLevel(), null);
    }

    public static Entity spawnElite(Location loc, Location spawnerLocation, EnumNamedElite elite, String displayName) {
        return spawnElite(loc, spawnerLocation, elite, elite.getMonster(), elite.getTier(), elite.randomLevel(), displayName);
    }

    /**
     * Creates an elite without spawning it into the world.
     */
    public static Entity spawnElite(Location loc, Location spawnerLocation, EnumNamedElite elite, EnumMonster monster, int tier, int level, String name) {
        name = name != null ? name : monster.getName();
        LivingEntity entity = spawnEntity(loc, monster, elite != null ? elite.getEntity() : monster.getCustomEntity(), tier, level, name);

        boolean isDungeon = DungeonManager.isDungeon(loc.getWorld());
        // For non-Named elites that don't have custom gear.
        if (elite == null && !isDungeon) {
            ItemWeapon weapon = new ItemWeapon();
            weapon.setTier(tier).setRarity(ItemRarity.getRandomRarity(true)).setGlowing(true);
            ItemType type = monster.getWeaponType();

            if (type != null)
                weapon.setType(type);

            // These have an extra special chance.
            if ((monster == EnumMonster.Zombie || monster == EnumMonster.Undead) && random.nextBoolean())
                weapon.setType(ItemType.AXE);

            ItemArmor armor = (ItemArmor) new ItemArmor().setRarity(ItemRarity.getRandomRarity(true)).setTier(tier).setGlowing(true);

            EntityEquipment e = entity.getEquipment();
            e.setItemInMainHand(weapon.generateItem());
            e.setArmorContents(armor.generateArmorSet());
        } else if (elite != null) {
            // Load elite custom gear.
            Map<EquipmentSlot, ItemStack> gear = ItemGenerator.getEliteGear(elite);
            for (EquipmentSlot e : gear.keySet()) {
                ItemStack i = gear.get(e);
                EnchantmentAPI.addGlow(i);
                GameAPI.setItem(entity, e, i);
            }
        }


        Metadata.ELITE.set(entity, true);
        if (elite != null)
            Metadata.NAMED_ELITE.set(entity, elite);


        if (entity != null && ((CraftLivingEntity) entity).getHandle() instanceof DRMonster) {
            DRMonster eli = (DRMonster) ((CraftLivingEntity) entity).getHandle();
            calculateAttributes(eli);
            HealthHandler.calculateHP(entity);
            HealthHandler.setHP(entity, HealthHandler.getMaxHP(entity));
        }

        if(entity != null && isDungeon) {
            Dungeon dungeon = DungeonManager.getDungeon(loc.getWorld());
            if(dungeon != null)dungeon.getTrackedMonsters().put(entity, loc);
        }

        if (entity != null)
            entity.setRemoveWhenFarAway(false);

        if (entity != null) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, true, Color.AQUA));
            MetadataUtils.Metadata.SPAWN_LOCATION.set(entity, loc);
            if (spawnerLocation != null)
                MetadataUtils.Metadata.SPAWNER_LOCATION.set(entity, spawnerLocation);
        }
        return entity;
    }

    public static Entity spawnCustomMonster(Location loc, Location spawnedLocation, EnumMonster monster, String levelRange, int tier, ItemType weaponType) {
        return spawnCustomMonster(loc, spawnedLocation, monster, Utils.getRandomFromTier(tier, levelRange), tier, weaponType);
    }

    public static Entity spawnCustomMonster(Location loc, Location spawnedLocation, EnumMonster monster, int level, int tier, ItemType weaponType) {
        return spawnCustomMonster(loc, spawnedLocation, monster, level, tier, weaponType, null);
    }

    public static Entity spawnCustomMonster(Location loc, Location spawnedLocation, EnumMonster monster, int level, int tier, ItemType weaponType, String customName) {
        return spawnCustomMonster(loc,spawnedLocation,monster,level,tier,weaponType,customName,-1,-1,0,0);
    }

    /**
     * Spawns a custom monster.
     */
    public static Entity spawnCustomMonster(Location loc, Location spawnedLocation, EnumMonster monster, int level, int tier, ItemType weaponType, String customName, int minMobScore, int maxMobScore, double minRarityScore, double maxRarityScore) {
        LivingEntity e = spawnEntity(loc, monster, monster.getCustomEntity(), tier, level, customName,minMobScore,maxMobScore,minRarityScore,maxRarityScore);

        // Register mob element.
        if (!monster.isFriendly() && ThreadLocalRandom.current().nextInt(100) < monster.getElementalChance())
            setMobElement(e, monster.getRandomElement());

        if (monster.isPassive())
            Metadata.PASSIVE.set(e, true);

        if (e != null)
            e.setRemoveWhenFarAway(false);

        MetadataUtils.Metadata.SPAWN_LOCATION.set(e, loc);
        if (spawnedLocation != null)
            Metadata.SPAWNER_LOCATION.set(e, spawnedLocation);
        return e;
    }

    /**
     * Updates the entity's display name to its friendly display name.
     */
    public static void updateName(Entity entity) {
        if (!Metadata.CUSTOM_NAME.has(entity)) {
            // They don't have a custom name set.
            Bukkit.getLogger().warning(entity.getName() + " has no custom name!");
            return;
        }

        String prefix = "";
        String name = Metadata.CUSTOM_NAME.get(entity).asString();

        // Apply elemental name.
        if (isElemental(entity)) {
            ElementalAttribute ea = getElement(entity);
            String[] splitName = name.split(" ", 2);

            boolean shortName = splitName.length == 1;
            String ePrefix = shortName ? splitName[0] : "";
            String eSuffix = shortName ? "" : " " + splitName[1];

            if (shortName) {
                //Fire Acolyte, Fire Daemon etc.
                name = ea.getColor() + ea.getPrefix() + " " + ePrefix;
            } else {
                name = ea.getColor() + ePrefix + ea.getPrefix() + eSuffix;
            }
        }

        // Apply elite.
        if (Metadata.ELITE.get(entity).asBoolean())
            prefix = ChatColor.BOLD + "";

        int tier = Metadata.TIER.get(entity).asInt();
        if (!name.contains(ChatColor.COLOR_CHAR + "") && tier != -1) {
            //Add the tier color to the front?
            prefix = Item.ItemTier.getByTier(tier).getColor() + "";
        }

        // Apply boss.
        if (isBoss(entity))
            prefix = ChatColor.RED + ChatColor.BOLD.toString();

        entity.setCustomName(prefix + name);
        entity.setCustomNameVisible(true);
    }

    public static boolean isElemental(Entity e) {
        return Metadata.ELEMENT.has(e);
    }

    public static ElementalAttribute getElement(Entity e) {
        return Metadata.ELEMENT.getEnum(e);
    }

    public static void setMobElement(Entity entity, ElementalAttribute ea) {
        Metadata.ELEMENT.set(entity, ea);
        updateName(entity);
    }

    public static void registerMonster(Entity entity, int level, int tier) {
        registerMonster(entity, level, tier, null, null, null);
    }

    /**
     * Adds metadata that identifies this as a custom monster.
     * Sets tier and level, health, etc.
     * Weapon / Armor should only be supplied if you wish to forcefully set the gear, since gear is normally generated in DRMonster's setup.
     * <p>
     * Formerly: setMonsterRandomStats
     */
    public static void registerMonster(Entity entity, int level, int tier, ItemArmor armorSet, ItemWeapon weapon, String name) {
        MetadataUtils.registerEntityMetadata(entity, EnumEntityType.HOSTILE_MOB, tier, level);

        LivingEntity le = (LivingEntity) entity;

        if (armorSet != null)
            le.getEquipment().setArmorContents(armorSet.generateArmorSet());

        if (weapon != null)
            le.getEquipment().setItemInMainHand(weapon.generateItem());

        HealthHandler.calculateHP(le);
        HealthHandler.setHP(le, HealthHandler.getMaxHP(le));
        if (name != null && name.length() > 0) {
            Metadata.CUSTOM_NAME.set(entity, name);
        }
        updateName(entity);
    }

    /**
     * Setup the supplied entity as a dungeon boss.
     */
    public static void registerBoss(DungeonBoss boss, int level, int tier) {
        LivingEntity le = boss.getBukkit();
        Metadata.BOSS.set(le, boss.getBossType().name());
        registerMonster(le, level, tier, null, null, boss.getBossType().getName());

        for (ItemStack item : le.getEquipment().getArmorContents())
            if (item != null && item.getType() != Material.AIR)
                EnchantmentAPI.addGlow(item);
        le.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
    }

    /**
     * Get all nearby entities within a certain radius to untarget another entity.
     *
     * @param entToUntarget
     * @param radius
     */
    public static void untargetEntity(LivingEntity entToUntarget, int radius) {
        entToUntarget.getNearbyEntities(radius, radius, radius).stream().forEach(ent -> {
            // Has to be targettable.
            if (!(ent instanceof Creature))
                return;
            // Make sure the target was actually who we said to untarget.
            if (((Creature) ent).getTarget() == null || !((Creature) ent).getTarget().equals(entToUntarget))
                return;
            //Untarget
            ((Creature) ent).setTarget(null);
        });
    }

    /**
     * Gets this mob tier.
     */
    public static int getTier(Entity entity) {
        return Metadata.TIER.get(entity).asInt();
    }

    /**
     * Is this an elite?
     */
    public static boolean isElite(Entity ent) {
        return Metadata.ELITE.has(ent);
    }
    public static boolean isMarksmanElite(Entity ent) {
        return Metadata.ELITE.has(ent);
    }

    public static boolean isNamedElite(Entity ent) { return  Metadata.NAMED_ELITE.has(ent);}

    /**
     * Is this entity a boss?
     */
    public static boolean isBoss(Entity ent) {
        return Metadata.BOSS.has(ent);
    }

    public static String getCustomName(Entity entity) {
        return Metadata.CUSTOM_NAME.get(entity).asString();
    }

    /**
     * Get an entity's level.
     */
    public static int getLevel(Entity ent) {
        return Metadata.LEVEL.get(ent).asInt();
    }

    /**
     * Is this a DR Monster?
     */
    public static boolean isMonster(Entity monster) {
        return ((CraftEntity) monster).getHandle() instanceof DRMonster;
    }

    /**
     * Gets attributes of the specified entity.
     */
    public static AttributeList getAttributes(Entity e) {
        if (GameAPI.isPlayer(e)) {
            return PlayerWrapper.getWrapper((Player) e).getAttributes();
        } else if (isMonster(e)) {
            return getMonster(e).getAttributes();
        }

//        Utils.printTrace();
        return new AttributeList();
    }


    /**
     * Returns the supplied bukkit entity as a DRMonster.
     */
    public static DRMonster getMonster(Entity monster) {
        return (DRMonster) ((CraftEntity) monster).getHandle();
    }

    @SuppressWarnings("rawtypes")
    public static void clearAI(PathfinderGoalSelector goal, PathfinderGoalSelector target) {
        try {
            Field a = PathfinderGoalSelector.class.getDeclaredField("b");
            Field b = PathfinderGoalSelector.class.getDeclaredField("c");
            a.setAccessible(true);
            b.setAccessible(true);
            ((LinkedHashSet) a.get(goal)).clear();
            ((LinkedHashSet) b.get(goal)).clear();

            if (target != null) {
                ((LinkedHashSet) a.get(target)).clear();
                ((LinkedHashSet) b.get(target)).clear();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static LivingEntity spawnEntity(Location loc, EnumMonster mType, CustomEntityType type, int tier, int level, String displayName) {
        return spawnEntity(loc,mType, type, tier, level, displayName, -1, -1,0,0);
    }

    public static LivingEntity spawnEntity(Location loc, EnumMonster mType, CustomEntityType type, int tier, int level, String displayName, int minMobScore, int maxMobScore, double minRarityScore, double maxRarityScore) {
        DRMonster monster = null;

        try {
            // Setup monster.
            World nmsWorld = ((CraftWorld) loc.getWorld()).getHandle();
            EntityInsentient entity = type.getClazz().getDeclaredConstructor(World.class).newInstance(nmsWorld);
            if (entity instanceof DRMonster) {
                monster = (DRMonster) entity;
                getEntityAttributes().put(monster, new AttributeList());
                monster.setMonster(mType);
                monster.setupMonster(tier,minMobScore,maxMobScore, minRarityScore, maxRarityScore);
                // Add to world.
                monster.getNMS().setLocation(loc.getX(), loc.getY(), loc.getZ(), 0, 0);
            }
            nmsWorld.addEntity(monster == null ? entity : monster.getNMS(), SpawnReason.CUSTOM);

            // Setup bukkit data and return.
            LivingEntity le = (LivingEntity) entity.getBukkitEntity();
            le.teleport(loc);
            le.setCollidable(true);

            Dungeon mDungeon = DungeonManager.getDungeon(loc.getWorld());
            boolean dungeon = mDungeon != null;
            ItemWeapon weapon = dungeon && (monster == null || monster.getWeapon() == null) ? mDungeon.getGeneralMobWeapon() : monster != null && monster.getWeapon() != null ? new ItemWeapon(monster.getWeapon()) : null;
            ItemArmor armor = dungeon && (monster == null || Arrays.stream(monster.getBukkit().getEquipment().getArmorContents()).filter(e -> e != null && e.getType() != Material.AIR).count() < 4) ? mDungeon.getGeneralMobArmorSet() : null;

            // Register monster data.
            if (mType != null && !mType.isFriendly()) {
                registerMonster(le, level, tier, armor, weapon, displayName);

                // Mark as dungeon mob.
                if (dungeon) {
                    Metadata.DUNGEON.set(le, true);
                    Dungeon dung = DungeonManager.getDungeon(loc.getWorld());
                    if (dung != null) {
//                        dung.getTrackedMonsters().put(le, loc);
                        Metadata.DUNGEON_FROM.set(le, dung.getType().getName());
                    }
                }
            }

            if (monster != null) {
                calculateAttributes(monster);
                HealthHandler.calculateHP(le);
                HealthHandler.setHP(le, HealthHandler.getMaxHP(le));
            }
            return le;
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().warning("Failed to create " + type.getClazz().getSimpleName());
        }
        return null;
    }

    /**
     * Recalculates a monster's attributes.
     */
    public static void calculateAttributes(DRMonster m) {
        AttributeList attributes = m.getAttributes();
        attributes.clear();

        ItemStack[] armorSet = m.getBukkit().getEquipment().getArmorContents().clone();
        int tier = EntityAPI.getTier(m.getBukkit());

        // if we have a skull we need to generate a helmet so mob stats are calculated correctly
        // TODO: Verify 3 is the correct slot.
        if (armorSet[3].getType() == Material.SKULL_ITEM && (tier >= 3 || ThreadLocalRandom.current().nextInt(10) <= (6 + tier))) {
            ItemStack helmet = lastUsedHelmetItem.get(m.getBukkit());
            if(helmet == null) {
                int minMobScore = m.getMinMobScore();
                int maxMobScore = m.getMaxMobScore();

                double minRarityScore = m.getMinRarityScore();
                double maxRarityScore = m.getMaxRarityScore();
                if (minMobScore > -1 && maxMobScore > -1 && maxMobScore > minMobScore) {
                    double mobRarityScore = minMobScore;
                    if (tier == minMobScore) mobRarityScore = minRarityScore;
                    else if(tier == maxMobScore) mobRarityScore = maxRarityScore;
                    else mobRarityScore = ThreadLocalRandom.current().nextDouble(minRarityScore, maxRarityScore);

//                    System.out.println("The rarity score we picked: " + mobRarityScore + " for the tier: " + tier);
                    double commonIncrease = m.getPercentIncreaseFromScore(Item.ItemRarity.COMMON, mobRarityScore);
                    double unCommonIncrease = m.getPercentIncreaseFromScore(Item.ItemRarity.UNCOMMON, mobRarityScore);
                    double rareIncrease = m.getPercentIncreaseFromScore(Item.ItemRarity.RARE, mobRarityScore);
                    double uniqueIncrease = m.getPercentIncreaseFromScore(Item.ItemRarity.UNIQUE, mobRarityScore);
//                    System.out.println("The common increase: " + commonIncrease);
//                    System.out.println("The uncommon increase: " + unCommonIncrease);
//                    System.out.println("The rare increase: " + rareIncrease);
//                    System.out.println("The unique increase: " + uniqueIncrease);

                    helmet = new ItemArmor().setTier(tier).setType(ItemType.HELMET).setRarity(ItemRarity.getRandomRarity(EntityAPI.isElite(m.getBukkit()),commonIncrease,unCommonIncrease,rareIncrease,uniqueIncrease)).generateItem();
                    lastUsedHelmetItem.put(m.getBukkit(), helmet);
                    //return gear.setTier(Item.ItemTier.getByTier(tier)).generateItem();
                } else {
                    helmet = new ItemArmor().setTier(tier).setType(ItemType.HELMET).setRarity(ItemRarity.getRandomRarity(EntityAPI.isElite(m.getBukkit()))).generateItem();
                    lastUsedHelmetItem.put(m.getBukkit(), helmet);
                }
            }
            armorSet[3] = helmet;
        } else if (lastUsedHelmetItem.containsKey(m.getBukkit())) {
            lastUsedHelmetItem.remove(m.getBukkit());
        }

        attributes.addStats(m.getBukkit().getEquipment().getItemInMainHand());

        //Calc off hand.
        if (m.getBukkit().getEquipment().getItemInOffHand() != null)
            attributes.addStats(m.getBukkit().getEquipment().getItemInOffHand());

        for (ItemStack armor : armorSet)
            attributes.addStats(armor);
        attributes.applyStatBonuses(null);
    }

    public static void showHPBar(DRMonster monster) {
        Entity ent = monster.getBukkit();
        boolean boss = isBoss(ent);
        boolean bold = boss || isElite(ent);
        int barSize = (boss ? 15 : 10) * 2;

        double hpPercentDecimal = HealthHandler.getHPPercent(ent);
        double hpPercent = hpPercentDecimal * 100D;
        hpPercent = Math.max(1, hpPercent);

        String fullBar = new String(new char[barSize]).replace("\0", "|");

        String full = ChatColor.GREEN + (bold ? ChatColor.BOLD + "" : "");
        String empty = ChatColor.DARK_RED + (bold ? ChatColor.BOLD + "" : "");

        // Apply color.
        int greenBars = (int) Math.ceil(hpPercentDecimal * barSize);
        fullBar = full + fullBar.substring(0, greenBars) + empty + fullBar.substring(greenBars);

        // Apply name to entity.
        String[] bar = splitHalfColor(fullBar, bold, String.valueOf(monster.getHP()).length());
        ent.setCustomName("[" + bar[0] + " " + ChatColor.WHITE + monster.getHP() + " " + bar[1] + ChatColor.WHITE + "]");
        ent.setCustomNameVisible(true);
    }

    private static String[] splitHalfColor(String spl, boolean bold, int healthLength) {
        //Need to offset for the health.
        int splitIndex = spl.length() / 2 - healthLength / 2 - 1;

        // Add any color.
        ChatColor lastColor = null;
        String colorChar = ChatColor.WHITE.toString().substring(0, 1);
        for (int i = 0; i < splitIndex; i++) {
            if (spl.substring(i).startsWith(colorChar)) {
                splitIndex += 2;
                ChatColor c = ChatColor.getByChar(spl.charAt(i + 1));
                if (c != null && c != ChatColor.BOLD)
                    lastColor = c;
            }
        }

        return new String[]{spl.substring(0, splitIndex), (lastColor != null ? lastColor + (bold ? ChatColor.BOLD + "" : "") : "") + spl.substring(splitIndex)};
    }
}