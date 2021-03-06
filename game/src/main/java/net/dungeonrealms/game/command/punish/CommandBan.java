package net.dungeonrealms.game.command.punish;

import net.dungeonrealms.GameAPI;
import net.dungeonrealms.common.game.command.BaseCommand;
import net.dungeonrealms.common.game.database.player.Rank;
import net.dungeonrealms.common.game.database.player.PlayerRank;
import net.dungeonrealms.common.game.punishment.TimeFormat;
import net.dungeonrealms.database.PlayerWrapper;
import net.dungeonrealms.database.punishment.PunishAPI;
import net.dungeonrealms.game.mastery.UUIDFetcher;
import net.dungeonrealms.game.mastery.Utils;
import net.dungeonrealms.game.player.combat.CombatLog;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Class written by APOLLOSOFTWARE.IO on 6/2/2016
 */
public class CommandBan extends BaseCommand {

    public CommandBan(String command, String usage, String description, String... aliases) {
        super(command, usage, description, Arrays.asList(aliases));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!Rank.isPMOD(player)) return false;
        }

        if (args.length < 2) {
            sender.sendMessage(usage);
            return true;
        }


        String p_name = args[0];

        if (p_name.equalsIgnoreCase(sender.getName())) {
            sender.sendMessage(ChatColor.RED + "You cannot ban yourself.");
            return true;
        }

        UUIDFetcher.getUUID(args[0], p_uuid -> {
            if (p_uuid == null) {
                sender.sendMessage(ChatColor.RED + args[0] + " is not a player, have they changed their name recently?");
                return;
            }

            long duration = 0;
            boolean isNull = true;

            for (TimeFormat d : TimeFormat.values())
                if (d.getKey().equalsIgnoreCase(args[1].substring(args[1].length() - 1))) {
                    String n = args[1].substring(0, args[1].length() - 1);
                    if (!Utils.isInt(n)) continue;
                    duration = Integer.parseInt(n) * d.convert();
                    isNull = false;
                    break;
                }

            if (!Utils.isInt(args[1]) && isNull) {
                sender.sendMessage(ChatColor.RED + args[1] + " is not a valid number.");
                return;
            }

            if (isNull)
                duration = Integer.parseInt(args[1]);

            if (duration < -1) {
                sender.sendMessage(ChatColor.RED + args[1] + " is not a valid number.");
                return;
            }

            if (duration == -1) duration = 0;

            final long finalDuration = duration;
            PunishAPI.isBanned(p_uuid, banned -> {
                if (banned) {
                    sender.sendMessage(ChatColor.RED + "This player is already banned.");
                    return;
                }
                if (sender instanceof Player) {
                    if (!Rank.isTrialGM((Player) sender) && Rank.isPMOD((Player) sender)) {


                        if (finalDuration > 1209_600L) {
                            sender.sendMessage(ChatColor.RED + "You cannot ban players for more than 14 days.");
                            return;
                        }

                        if (finalDuration == 0) {
                            sender.sendMessage(ChatColor.RED + "You cannot permanently ban players");
                            return;
                        }
                    }
                }

                if (Rank.isPMOD(Bukkit.getOfflinePlayer(p_uuid))) {
                    if (sender instanceof Player && !Rank.isTrialGM((Player) sender)) {
                        sender.sendMessage(ChatColor.RED + "You cannot ban that player.");
                        return;
                    }
                }


                String reasonString = "";

                if (args.length >= 3) {
                    StringBuilder reason = new StringBuilder(args[2]);

                    for (int arg = 3; arg < args.length; arg++)
                        reason.append(" ").append(args[arg]);

                    reasonString = reason.toString();
                }

                String friendlyMessage = ChatColor.RED + (reasonString != "" ? ".\nReason: " + ChatColor.ITALIC + reasonString : "");
                if (finalDuration != 0) {
                    String punishExpiry = ChatColor.BOLD + PunishAPI.timeString((int) (finalDuration / 60));
                    sender.sendMessage(ChatColor.RED.toString() + "You have banned " + ChatColor.BOLD + p_name + ChatColor.RED + " for " + punishExpiry + ".");
                    GameAPI.sendStaffMessage(PlayerRank.PMOD, ChatColor.RED + ChatColor.BOLD.toString() + sender.getName() + ChatColor.RED + " has banned " + ChatColor.BOLD + p_name + ChatColor.RED + " for " + punishExpiry + friendlyMessage + ".");
                } else {
                    sender.sendMessage(ChatColor.RED.toString() + "You have permanently banned " + ChatColor.BOLD + p_name + ChatColor.RED + ".");
                    GameAPI.sendStaffMessage(PlayerRank.PMOD, ChatColor.RED + ChatColor.BOLD.toString() + sender.getName() + ChatColor.RED + " has permanently banned " + ChatColor.BOLD + p_name + friendlyMessage + ".");
                }

                //  BROADCASTS TO DISCORD  //
                GameAPI.sendNetworkMessage("BanMessage", sender.getName() + ": /ban " + String.join(" ", args));

                Player online = Bukkit.getPlayer(p_uuid);
                if (online != null) {
                    CombatLog.removeFromCombat(online);
                    CombatLog.removeFromPVP(online);
                }
                int accountID = sender instanceof Player ? PlayerWrapper.getPlayerWrapper((Player) sender).getAccountID() : 0;
//                PlayerWrapper wrapper = PlayerWrapper.getPlayerWrapper(player);
                PunishAPI.ban(p_uuid, p_name, accountID, finalDuration, reasonString + " [" + sender.getName() + "]", null);
            });
        });

        return false;

    }
}
