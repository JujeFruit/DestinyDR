package net.dungeonrealms.handlers;

import net.dungeonrealms.inventory.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Created by Nick on 10/2/2015.
 */
public class ClickHandler {

    static ClickHandler instance = null;

    public static ClickHandler getInstance() {
        if (instance == null) {
            instance = new ClickHandler();
        }
        return instance;
    }

    public void doGuildClick(InventoryClickEvent event) {
        String name = event.getInventory().getName();
        int slot = event.getRawSlot();
        if (slot == -999) return;
        if (name.endsWith(" - (Bank Logs)")) {
            event.setCancelled(true);
            if (slot == 0) {
                Menu.openPlayerGuildLog((Player) event.getWhoClicked());
            }
        } else if (name.endsWith("- (Invite Logs)")) {
            event.setCancelled(true);
            if (slot == 0) {
                Menu.openPlayerGuildLog((Player) event.getWhoClicked());
            }
        } else if (name.endsWith(" - (Login Logs)")) {
            event.setCancelled(true);
            if (slot == 0) {
                Menu.openPlayerGuildLog((Player) event.getWhoClicked());
            }
        } else if (name.endsWith("- (Logs)")) {
            event.setCancelled(true);
            if (slot > 18) return;
            switch (slot) {
                case 0:
                    Menu.openPlayerGuildInventory((Player) event.getWhoClicked());
                    break;
                case 12:
                    Menu.openPlayerGuildLogLogins((Player) event.getWhoClicked());
                    break;
                case 13:
                    Menu.openPlayerGuildLogInvitations((Player) event.getWhoClicked());
                    break;
                case 14:
                    Menu.openPlayerGuildLogBankClicks((Player) event.getWhoClicked());
                    break;

            }
        } else if (name.equals("Top Guilds")) {
            event.setCancelled(true);
        } else if (name.equals("Guild Management")) {
            event.setCancelled(true);
        } else if (name.startsWith("Guild - ")) {
            event.setCancelled(true);
            if (slot > 54) return;
            switch (slot) {
                case 0:
                    Menu.openPlayerGuildLog((Player) event.getWhoClicked());
                    break;
                case 1:
                    Menu.openGuildManagement((Player) event.getWhoClicked());
                    break;
                case 17:
                    Menu.openGuildRankingBoard((Player) event.getWhoClicked());
                    break;
            }
        }
    }
}
