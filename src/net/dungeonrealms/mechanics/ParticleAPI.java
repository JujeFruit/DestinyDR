package net.dungeonrealms.mechanics;

import net.dungeonrealms.mastery.Utils;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Kieran on 9/20/2015.
 */
public class ParticleAPI {

    public enum ParticleEffect {
        FIREWORKS_SPARK(0, EnumParticle.FIREWORKS_SPARK),
        BUBBLE(1, EnumParticle.WATER_BUBBLE),
        TOWN_AURA(2, EnumParticle.TOWN_AURA),
        CRIT(3, EnumParticle.CRIT),
        MAGIC_CRIT(4, EnumParticle.CRIT_MAGIC),
        WITCH_MAGIC(5, EnumParticle.SPELL_WITCH),
        NOTE(6, EnumParticle.NOTE),
        PORTAL(7, EnumParticle.PORTAL),
        ENCHANTMENT_TABLE(8, EnumParticle.ENCHANTMENT_TABLE),
        FLAME(9, EnumParticle.FLAME),
        LAVA(10, EnumParticle.LAVA),
        SPLASH(11, EnumParticle.WATER_SPLASH),
        LARGE_SMOKE(12, EnumParticle.SMOKE_LARGE),
        RED_DUST(13, EnumParticle.REDSTONE),
        SNOWBALL_POOF(14, EnumParticle.SNOWBALL);

        private int id;
        private EnumParticle particle;

        ParticleEffect(int id, EnumParticle particle) {
            this.id = id;
            this.particle = particle;
        }

        public static ParticleEffect getById(int id) {
            for (ParticleEffect particleEffect : values()) {
                if (particleEffect.id == id) {
                    return particleEffect;
                }
            }
            return null;
        }

        public EnumParticle getParticle() {
            return particle;
        }
    }

    private static List<Player> getNearbyPlayers(Location location, int radius) {
        return location.getWorld().getPlayers().stream().filter(player -> location.distance(player.getLocation()) <= radius).collect(Collectors.toList());
    }

    public static void sendParticleToLocation(final ParticleEffect particleEffect, final Location location, final float xOffset, final float yOffset, final float zOffset, final float particleSpeed, final int particleCount) {
        Object packet = null;
        try {
            packet = newPacket(particleEffect, location, xOffset, yOffset, zOffset, particleSpeed, particleCount);
        } catch (Exception e) {
            Utils.log.info("Something went wrong creating a packet");
        }

        for (Player player : getNearbyPlayers(location, 25)) {
            try {
                sendPacketToPlayer(player, packet);
            } catch (Exception e) {
                Utils.log.info("Unable to send particle packet to player " + player.getName());
            }
        }
    }

    private static Object newPacket(ParticleEffect particleEffect, Location location, float xOffset, float yOffset, float zOffset, float particleSpeed, int particleCount) throws Exception {
        Object packet = new PacketPlayOutWorldParticles();
        setPacketValue(packet, "a", particleEffect.getParticle());
        setPacketValue(packet, "b", (float) location.getX());
        setPacketValue(packet, "c", (float) location.getY());
        setPacketValue(packet, "d", (float) location.getZ());
        setPacketValue(packet, "e", xOffset);
        setPacketValue(packet, "f", zOffset);
        setPacketValue(packet, "g", yOffset);
        setPacketValue(packet, "h", particleSpeed);
        setPacketValue(packet, "i", particleCount);
        return packet;
    }

    private static void setPacketValue(Object instance, String fieldName, Object value) throws Exception {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }

    private static void sendPacketToPlayer(Player player, Object packet) {
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket((Packet) packet);
    }
}
