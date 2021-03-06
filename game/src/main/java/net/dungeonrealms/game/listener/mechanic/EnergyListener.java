package net.dungeonrealms.game.listener.mechanic;

import net.dungeonrealms.DungeonRealms;
import net.dungeonrealms.GameAPI;
import net.dungeonrealms.game.handler.EnergyHandler;
import net.dungeonrealms.game.item.items.core.ItemPickaxe;
import net.dungeonrealms.game.mechanic.ParticleAPI;
import net.dungeonrealms.game.player.duel.DuelingMechanics;
import net.dungeonrealms.game.profession.Mining;
import net.dungeonrealms.game.title.TitleAPI;
import net.dungeonrealms.game.world.realms.Realms;
import net.minecraft.server.v1_9_R2.EntityExperienceOrb;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Kieran on 9/24/2015.
 */
public class EnergyListener implements Listener {

    /**
     * Checks for players starving
     * adds hunger potion effect and metadata
     * to be used at a later time
     *
     * @param event
     * @since 1.0
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerStarveDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.STARVATION) return;
        if (!(GameAPI.isPlayer(event.getEntity()))) return;
        event.setCancelled(true);
        event.setDamage(0);
        Player player = (Player) event.getEntity();
        if (!(player.hasPotionEffect(PotionEffectType.HUNGER))) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 50, 0));
            if (!(player.hasMetadata("starving"))) {
                player.setMetadata("starving", new FixedMetadataValue(DungeonRealms.getInstance(), "true"));
                player.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "**STARVING**");
            }
        }
    }

    /**
     * Checks for players starving
     * adds hunger potion effect and metadata
     * to be used at a later time
     *
     * @param event
     * @since 1.0
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDoSomething(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack weapon = event.getItem();

        if (weapon == null || !event.hasItem()) {
            weapon = new ItemStack(Material.AIR);
        }
        if (!(event.getAction() == Action.LEFT_CLICK_AIR || (event.getAction() == Action.LEFT_CLICK_BLOCK && Realms.getInstance().getRealm(event.getPlayer().getWorld()) == null))) {
            return;
        }
        if (GameAPI.isMainWorld(event.getPlayer().getWorld()) && event.hasBlock() && (event.getClickedBlock().getType() == Material.LONG_GRASS)) {
            event.setUseItemInHand(Event.Result.DENY);
            event.setCancelled(true);
            return;
        }
        if (weapon.getType() != Material.AIR && ItemPickaxe.isPickaxe(weapon)) {
            return;
        }
        if (weapon.getType() == Material.POTION) {
            return;
        }

        if (player.hasPotionEffect(PotionEffectType.SLOW_DIGGING) || EnergyHandler.getPlayerCurrentEnergy(player) <= 0) {
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            player.playSound(player.getLocation(), Sound.ENTITY_WOLF_PANT, 12F, 1.5F);
            TitleAPI.sendActionBar(player, ChatColor.RED.toString() + ChatColor.BOLD + "❢ LOW ENERGY ❢", 4 * 20);
            ParticleAPI.spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 40, .75F);
            return;
        }
        if (player.hasMetadata("lastAttack")) {
            if (System.currentTimeMillis() - player.getMetadata("lastAttack").get(0).asLong() < 80) {
                event.setUseItemInHand(Event.Result.DENY);
                event.setCancelled(true);
                return;
            }
        }
        player.setMetadata("lastAttack", new FixedMetadataValue(DungeonRealms.getInstance(), System.currentTimeMillis()));
        float energyToRemove = EnergyHandler.handleAirSwingItem(weapon);
        if (weapon.getType() == Material.BOW) {
            energyToRemove += 0.15F;
        }
        EnergyHandler.removeEnergyFromPlayerAndUpdate(player, energyToRemove, DuelingMechanics.isDueling(player.getUniqueId()));
    }

    /**
     * Checks for players starting sprinting or stopping sprinting
     * adds/removes correct metadata, applies initial energy reduction
     * cancels if player can't sprint
     *
     * @param event
     * @since 1.0
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        if (!(event.getPlayer().isSprinting())) {
            if (!(event.getPlayer().hasMetadata("starving"))) {
                EnergyHandler.removeEnergyFromPlayerAndUpdate(event.getPlayer(), 0.14F);
                event.getPlayer().setMetadata("sprinting", new FixedMetadataValue(DungeonRealms.getInstance(), "true"));
                event.getPlayer().setSprinting(true);
            } else {
                event.setCancelled(true);
                event.getPlayer().setSprinting(false);
            }
        } else {
            event.getPlayer().removeMetadata("sprinting", DungeonRealms.getInstance());
            event.getPlayer().setSprinting(false);
        }
    }

    /**
     * Checks players food level changes
     * removes hunger potion effect and metadata
     * to be used at a later time
     *
     * @param event
     * @since 1.0
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(GameAPI.isPlayer(event.getEntity()))) return;
        Player player = (Player) event.getEntity();
        if (event.getFoodLevel() < player.getFoodLevel()) {
            if (Math.random() <= .70) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getFoodLevel() > 0 && player.hasMetadata("starving")) {
            player.removeMetadata("starving", DungeonRealms.getInstance());
            player.removePotionEffect(PotionEffectType.HUNGER);
        }
    }

    /**
     * Cancels the base MC Exp change
     * as we use exp for energy
     *
     * @param event
     * @since 1.0
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMCExpChange(PlayerExpChangeEvent event) {
        event.setAmount(0);
    }

    /**
     * Safety check, cancels mobs/players
     * dropping MC exp
     *
     * @param event
     * @since 1.0
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDeathDropExp(EntityDeathEvent event) {
        event.setDroppedExp(0);
    }

    /**
     * Safety check, cancels players
     * picking up items
     *
     * @param event
     * @since 1.0
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemPickup(PlayerPickupItemEvent event) {
        if (!(event.getItem() instanceof ExperienceOrb) && (!(event.getItem() instanceof EntityExperienceOrb))) return;
        event.setCancelled(true);
    }

    /**
     * Handles players deaths, removing metadata
     * and potion effects
     *
     * @param event
     * @since 1.0
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.hasMetadata("starving")) {
            player.removeMetadata("starving", DungeonRealms.getInstance());
        }
        if (player.hasMetadata("sprinting")) {
            player.removeMetadata("sprinting", DungeonRealms.getInstance());
        }
    }
}
