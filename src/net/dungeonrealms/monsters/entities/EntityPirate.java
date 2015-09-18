package net.dungeonrealms.monsters.entities;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import net.dungeonrealms.monsters.CustomMeleeEntity;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_8_R3.World;

/**
 * Created by Nick on 9/17/2015.
 */
public class EntityPirate extends CustomMeleeEntity {
	public EntityPirate(World world, int tier) {
		super(world, "Pirate", "samsamsam1234", tier);
		this.setCustomName(ChatColor.GOLD + "Pirate");
		this.setCustomNameVisible(true);
		setArmor(1);
	}
	public EntityPirate(World world) {
		super(world);
		this.setCustomName(ChatColor.GOLD + "Pirate");
		this.setCustomNameVisible(true);
		setArmor(1);
	}
	@Override
	public ItemStack[] getTierArmor(int tier) {
		if (tier == 1) {
			return new ItemStack[] { new ItemStack(Material.LEATHER_BOOTS, 1),
					new ItemStack(Material.LEATHER_LEGGINGS, 1), new ItemStack(Material.LEATHER_CHESTPLATE, 1),
					new ItemStack(Material.LEATHER_HELMET, 1) };
		}
		return null;
	}

	@Override
	public void setArmor(int tier) {
		ItemStack[] armor = getTierArmor(tier);
		// weapon, boots, legs, chest, helmet/head
		ItemStack weapon = getTierWeapon(tier);
		this.setEquipment(0, CraftItemStack.asNMSCopy(weapon));
		this.setEquipment(1, CraftItemStack.asNMSCopy(armor[0]));
		this.setEquipment(2, CraftItemStack.asNMSCopy(armor[1]));
		this.setEquipment(3, CraftItemStack.asNMSCopy(armor[2]));
		this.setEquipment(4, this.getHead("samsamsam1234"));
	}

	@Override
	public ItemStack getTierWeapon(int tier) {
		return new ItemStack(Material.WOOD_SWORD, 1);
	}

	@Override
	protected String z() {
		return "mob.zombie.say";
	}

	@Override
	protected String bo() {
		return "random.bowhit";
	}

	@Override
	protected String bp() {
		return "mob.zombie.death";
	}

	@Override
	public void setStats() {
	}
}
