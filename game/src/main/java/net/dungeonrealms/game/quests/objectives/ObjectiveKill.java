package net.dungeonrealms.game.quests.objectives;

import com.google.gson.JsonObject;
import net.dungeonrealms.game.mastery.MetadataUtils;
import net.dungeonrealms.game.mastery.Utils;
import net.dungeonrealms.game.player.chat.Chat;
import net.dungeonrealms.game.quests.QuestNPC;
import net.dungeonrealms.game.quests.QuestPlayerData;
import net.dungeonrealms.game.quests.QuestPlayerData.QuestProgress;
import net.dungeonrealms.game.quests.QuestStage;
import net.dungeonrealms.game.quests.Quests;
import net.dungeonrealms.game.quests.gui.GuiBase;
import net.dungeonrealms.game.quests.gui.GuiStageEditor;
import net.dungeonrealms.game.world.entity.type.monster.DRMonster;
import net.dungeonrealms.game.world.entity.type.monster.type.EnumMonster;
import net.dungeonrealms.game.world.item.CC;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.inventivetalent.glow.GlowAPI;
import org.inventivetalent.glow.GlowAPI.Color;

import java.util.ArrayList;
import java.util.function.Consumer;

public class ObjectiveKill implements QuestObjective {

    private int amount = 1;
    private int tier = 1, radius = -1;

    private Location radiusCenter;
    private String customMonsterName = null;
    private EnumMonster monsterType = EnumMonster.Bandit;
    private QuestStage questStage;

    private ArrayList<Player> selectors = new ArrayList<Player>();

    @Override
    public boolean isCompleted(Player player, QuestStage stage, QuestNPC currentNPC) {
        QuestPlayerData data = Quests.getInstance().playerDataMap.get(player);
        if (data != null)
            return data.getQuestProgress(stage.getQuest()).getObjectiveCounter() >= this.amount;
        return false;
    }

    @Override
    public String getName() {
        return "Kill";
    }

    @Override
    public String getTaskDescription(Player player, QuestStage stage) {
        QuestPlayerData data = Quests.getInstance().playerDataMap.get(player);
        if (data != null && player != null) {
            int killed = data.getQuestProgress(stage.getQuest()).getObjectiveCounter();
            //Have they killed enough yet?
            if (killed >= this.amount) {
                if (stage.getNPC() == null)
                    return "Continue.";
                return "Talk to " + stage.getNPC().getName();
            }
            //Have they killed any?
            if (killed > 0)
                return "Kill " + (this.amount == 1 && this.customMonsterName != null ? this.customMonsterName : ((this.amount - killed) + " more " + getDisplayTypeName() + ((this.amount - killed) > 1 ? "s" : ""))) + "!";
        }
        return "Kill " + (this.amount == 1 && this.customMonsterName != null ? customMonsterName : (this.amount + " " + getDisplayTypeName() + (this.amount > 1 ? "s" : "")));
    }

    public void setEntityType(EnumMonster monster, int tier) {
        this.monsterType = monster;
        this.tier = tier;
    }

    @Override
    public JsonObject saveJSON() {
        JsonObject o = new JsonObject();
        o.addProperty("amt", this.amount);
        o.addProperty("tier", this.tier);
        o.addProperty("entityType", this.monsterType.name());
        if (this.customMonsterName != null)
            o.addProperty("customName", this.customMonsterName);

        if (this.radius != -1)
            o.addProperty("radius", this.radius);

        if (this.radiusCenter != null)
            o.addProperty("radiusCenter", Utils.getStringFromLocation(this.radiusCenter, true));

        return o;
    }

    @Override
    public void loadJSON(JsonObject o) {
        this.amount = o.get("amt").getAsInt();
        if (o.has("entityType"))
            this.setEntityType(EnumMonster.valueOf(o.get("entityType").getAsString()), 1);
        if (o.has("tier"))
            this.tier = o.get("tier").getAsInt();

        if (o.has("customName"))
            this.customMonsterName = o.get("customName").getAsString();

        if (o.has("radiusCenter")) {
            this.radiusCenter = Utils.getLocation(o.get("radiusCenter").getAsString());
        }

        if (o.has("radius"))
            this.radius = o.get("radius").getAsInt();
    }

    public void handleKill(Player killer, LivingEntity entity, DRMonster monster) {
        if (killer == null || monster.getEnum() == null || !this.isApplicable(entity))
            return;

        QuestPlayerData data = Quests.getInstance().playerDataMap.get(killer);
        if (data == null)
            return;
        QuestProgress progress = data.getQuestProgress(this.questStage.getQuest());

        //If the player is killing too low of a tier monster, or they are not on this quest stage, don't bring up their counter.
        if (progress == null || (progress.getStageIndex() - 1) != this.questStage.getQuest().getStageList().indexOf(this.questStage))
            return;

        int count = progress.getObjectiveCounter() + 1;
        progress.setObjectiveCounter(count);
        progress.activateActionBar();
        if (count == this.amount) {
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2, 2);
            if (this.customMonsterName != null)
                killer.sendMessage(ChatColor.YELLOW + "You have killed " + getDisplayTypeName() + "!");
            else
                killer.sendMessage(ChatColor.YELLOW + "You have killed enough " + getDisplayTypeName() + "s.");

        } else if (count < this.amount) {
            killer.playSound(killer.getLocation(), Sound.BLOCK_NOTE_PLING, 2, 2);
            killer.sendMessage(ChatColor.YELLOW + "Killed " + count + "/" + this.amount + " " + getDisplayTypeName() + (count > 1 ? "s" : "") + ".");
        }

        Quests.getInstance().updateActionBar(killer);
    }

    private String getDisplayTypeName() {
        if (this.customMonsterName != null) return this.customMonsterName;
        return this.monsterType.getName();
    }

    public void handleEntityInteract(PlayerInteractAtEntityEvent evt) {
        if (evt.getHand() == EquipmentSlot.OFF_HAND) return;
        if (this.selectors.contains(evt.getPlayer())) {
            net.minecraft.server.v1_9_R2.Entity nmsEnt = ((CraftEntity) evt.getRightClicked()).getHandle();
            if (nmsEnt == null || !(nmsEnt instanceof DRMonster)) {
                evt.getPlayer().sendMessage(ChatColor.RED + "This is not a registered monster type.");
                evt.setCancelled(true);
                return;
            }

            DRMonster monster = (DRMonster) nmsEnt;
            if (monster.getEnum() == null) {
                evt.getPlayer().sendMessage(ChatColor.RED + "This monster doesn't have its type set? Try selecting a different mob of the same type.");
                return;
            }

            this.setEntityType(monster.getEnum(), monster.getTier());
            this.selectors.remove(evt.getPlayer());
            evt.getPlayer().sendMessage(ChatColor.GREEN + "Entity Type updated to Tier " + this.tier + " " + this.monsterType.getName());
        }
    }

    @Override
    public Material getIcon() {
        return Material.DIAMOND_SWORD;
    }

    @Override
    public String[] getDescription() {
        return new String[]{"Player kills entities."};
    }

    @Override
    public GuiBase createEditorGUI(Player player, QuestStage stage) {
        return new GuiKillEditor(player, stage, this);
    }

    @Override
    public void setQuestStage(QuestStage qs) {
        this.questStage = qs;
    }

    @Override
    public void onStart(Player player) {
        updateGlow(player);
    }

    public void updateGlow(Player player) {
        QuestPlayerData data = Quests.getInstance().playerDataMap.get(player);
        if (data == null)
            return;
        QuestProgress progress = data.getQuestProgress(this.questStage.getQuest());
        if (this.amount > progress.getObjectiveCounter())
            performNearby(player, 75, (ent) -> GlowAPI.setGlowing(ent, Color.RED, player));
    }

    public void onEnd(Player player) {
        disableGlow(player);
    }

    public void disableGlow(Player player) {
        performNearby(player, 100, (ent) -> GlowAPI.setGlowing(ent, false, player));
    }

    private void performNearby(Player player, int radius, Consumer<Entity> action) {
        player.getNearbyEntities(radius, radius, radius).stream().filter(this::isApplicable).forEach(action);
    }

    public boolean isApplicable(Entity ent) {
        net.minecraft.server.v1_9_R2.Entity nmsEnt = ((CraftEntity) ent).getHandle();

        //Isnt with the radius.
        if (this.radiusCenter != null && this.radius != -1) {
            if (!this.radiusCenter.getWorld().equals(ent.getWorld())) return false;
            if (this.radiusCenter.distanceSquared(ent.getLocation()) > radius * radius) return false;
        }

        if (nmsEnt instanceof DRMonster) {
            DRMonster monster = (DRMonster) nmsEnt;
            if (this.customMonsterName != null) {
                String name = MetadataUtils.Metadata.CUSTOM_NAME.get(ent).asString();
                return name != null && ChatColor.stripColor(name).equalsIgnoreCase(this.customMonsterName) && monster.getTier() >= tier;
            }
            return monster.getEnum() == monsterType && monster.getTier() >= tier;
        }
        return false;
    }

    public class GuiKillEditor extends GuiBase {

        private QuestStage stage;
        private ObjectiveKill objective;

        public GuiKillEditor(Player player, QuestStage stage, ObjectiveKill objective) {
            super(player, "Kill Objective", InventoryType.CHEST);
            this.stage = stage;
            this.objective = objective;
        }

        @Override
        public void createGUI() {

            this.setSlot(0, Material.EGG, ChatColor.RED + "Entity Type", new String[]{"Set the Entity to be killed", "Current Entity: " + ChatColor.RED + this.objective.monsterType.getName()}, (evt) -> {
                player.sendMessage(ChatColor.GREEN + "Please right click the entity type you'd like to select.");
                this.objective.selectors.add(player);
                player.closeInventory();
            });

            this.setSlot(1, Material.STICK, ChatColor.AQUA + "Entity Tier", new String[]{"Set the tier of this monster: " + ChatColor.LIGHT_PURPLE + this.objective.tier}, (evt) -> {
                player.sendMessage(ChatColor.YELLOW + "Please enter the tier of the target monsters.");
                Chat.listenForNumber(player, 1, 5, (tier) -> {
                    this.objective.tier = tier;
                    player.sendMessage(ChatColor.GREEN + "Tier updated to " + tier + ".");
                    new GuiKillEditor(player, stage, objective);
                }, () -> new GuiKillEditor(player, stage, objective));
            });

            this.setSlot(2, Material.IRON_SWORD, ChatColor.RED + "Amount", new String[]{"Set the minimum amount of killed monsters", "Current: " + ChatColor.RED + this.objective.amount}, (evt) -> {
                player.sendMessage(ChatColor.YELLOW + "How many kills should be required?");
                Chat.listenForNumber(player, 1, 1000, (num) -> {
                    this.objective.amount = num;
                    player.sendMessage(ChatColor.GREEN + "Kill Requirement set to " + this.objective.amount);
                    new GuiKillEditor(player, stage, objective);
                }, () -> new GuiKillEditor(player, stage, objective));
            });

            this.setSlot(3, Material.PAPER, CC.Red + "Radius",
                    new String[]{
                            "Set the radius they are required",
                            "to kill this objective from your location",
                            "Current: " + radius,
                            "Center: " + (radiusCenter != null ? Utils.getStringFromLocation(radiusCenter, true) : "None")}, (e) -> {
                        player.sendMessage(ChatColor.RED + "Please enter the radius around you they are required to kill the monster.");
                        Chat.listenForNumber(player, done -> {
                            radiusCenter = player.getLocation();
                            radius = done;
                            player.sendMessage(ChatColor.RED + "Radius set to " + done + " around you!");
                            new GuiKillEditor(player, stage, objective);
                        }, () -> new GuiKillEditor(player, stage, objective));
                    });

            this.setSlot(4, Material.NAME_TAG, CC.Aqua + "Custom Entity Name", new String[]{"Custom Entity name to defeat (Elite Name Etc)",
                    "Current: " + (customMonsterName == null ? "None" : customMonsterName)}, e -> {
                player.sendMessage(ChatColor.RED + "Please enter the custom monster name youd wish them to kill.");
                Chat.listenForMessage(player, done -> {
                    if (done.getMessage().equals("cancel")) {
                        new GuiKillEditor(player, stage, objective);
                        return;
                    }
                    customMonsterName = done.getMessage();
                    player.sendMessage(ChatColor.RED + "Custom Monster Name set to " + customMonsterName);
                    new GuiKillEditor(player, stage, objective);
                });
            });
            this.setSlot(5, GO_BACK, (evt) -> new GuiStageEditor(player, stage));
        }
    }

}
