package net.dungeonrealms.game.command;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import net.dungeonrealms.DungeonRealms;
import net.dungeonrealms.common.game.command.BaseCommand;
import net.dungeonrealms.common.game.database.player.rank.Rank;
import net.dungeonrealms.game.world.spawning.EliteMobSpawner;
import net.dungeonrealms.game.world.spawning.MobSpawner;
import net.dungeonrealms.game.world.spawning.SpawningMechanics;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Map;
import java.util.WeakHashMap;

public class CommandSpawner extends BaseCommand {

    public CommandSpawner() {
        super("spawner", "/command", "Spawner command");
    }

    public static Map<Location, MobSpawner> shownMobSpawners = new WeakHashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;
        if (!Rank.isHeadGM(player)) {
            return true;
        }


        //Dank holograms to show spawners.
        //coords=type*(name):tier;amount<high/low (lvl range)>@SpawnTime#rangeMin-rangMax$
        //x,y,z=type*(Name):4;1-@400#1-1$
        if (args.length == 2) {
            //spawner show <radius>
            //spawner hide <radius>/all
            if (args[0].equalsIgnoreCase("show") || args[0].equalsIgnoreCase("hide")) {
                boolean showing = args[0].equalsIgnoreCase("show");

                boolean hideAll = !showing && args[1].equalsIgnoreCase("all");
                if (!StringUtils.isNumeric(args[1]) && !hideAll) {
                    player.sendMessage(ChatColor.RED + "Please enter a valid radius");
                    return true;
                }

                int radius = hideAll ? 0 : Integer.parseInt(args[1]);

                int found = 0;
                for (MobSpawner spawner : SpawningMechanics.getAllSpawners()) {

                    if (spawner.getLoc() != null && (hideAll || spawner.getLoc().distance(player.getLocation()) <= radius)) {

                        if (showing) {

                            //Already showing?
                            if (shownMobSpawners.containsKey(spawner.getLoc())) continue;

//                            if (spawner instanceof EliteMobSpawner)
//                                holo.appendTextLine(ChatColor.GREEN + ChatColor.BOLD.toString() + "ELITE MOB");


                            spawner.getLoc().getBlock().setType(Material.MOB_SPAWNER);

                            spawner.createEditInformation();
                            shownMobSpawners.put(spawner.getLoc(), spawner);
                            found++;
                        } else {
                            if (shownMobSpawners.containsKey(spawner.getLoc())) {
                                found++;
                                if (spawner.getLoc().getBlock().getType() == Material.MOB_SPAWNER) {
                                    spawner.getLoc().getBlock().setType(Material.AIR);
                                }
                                if (spawner.getEditHologram() != null) {
                                    spawner.getEditHologram().delete();
                                    shownMobSpawners.remove(spawner.getLoc());
                                }
                            }

                        }
                    }
                }

                player.sendMessage(ChatColor.RED + (showing ? "Showing " : "Hiding ") + found + " spawners..");
            }

        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("edit")) {
                if (player.hasMetadata("editting")) {
                    player.removeMetadata("editting", DungeonRealms.getInstance());
                    player.sendMessage(ChatColor.RED + "No longer in spawner create mode.");
                } else {
                    player.setMetadata("editting", new FixedMetadataValue(DungeonRealms.getInstance(), ""));
                    player.sendMessage(ChatColor.GREEN + "You are now in Spawner create mode!");
                    player.sendMessage(ChatColor.GRAY + "Place a spawner to create a spawner location.");
                }
            }
        }
        return false;
    }
}
