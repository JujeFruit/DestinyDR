package net.dungeonrealms.game.command.test;

import net.dungeonrealms.common.game.command.BaseCommand;
import net.dungeonrealms.common.game.database.player.Rank;
import net.dungeonrealms.game.item.items.functional.ItemFish;
import net.dungeonrealms.game.mechanic.data.FishingTier;
import net.dungeonrealms.game.mechanic.dot.DotManager;
import net.dungeonrealms.game.mechanic.dot.impl.FireDot;
import net.dungeonrealms.game.mechanic.dot.impl.HealingDot;
import net.dungeonrealms.game.profession.Fishing;
import net.dungeonrealms.game.profession.fishing.FishRegenBuff;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Rar349 on 6/6/2017.
 */
public class CommandTestDot extends BaseCommand {

    public CommandTestDot() {
        super("drdot");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length < 1) return true;
        if(!(sender instanceof Player)) return true;
        Player senda = (Player) sender;
        if(!Rank.isDev(senda)) return true;
        ItemFish fish = new ItemFish(FishingTier.TIER_5, Fishing.EnumFish.Anchovie);
        fish.setFishBuff(new FishRegenBuff(FishingTier.TIER_5));
        senda.getInventory().addItem(fish.generateItem());

        Player other = Bukkit.getPlayer(args[0]);
        if(other == null) {
            sender.sendMessage("This player is not online!");
            return true;
        }

        DotManager.addDamageOverTime(other, new FireDot(senda,other,50,5), true);
//        DotManager.addDamageOverTime(other,new HealingDot(senda,other,50,5),true);
        return true;
    }
}
