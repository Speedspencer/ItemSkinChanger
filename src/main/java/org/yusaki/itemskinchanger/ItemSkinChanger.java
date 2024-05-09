package org.yusaki.itemskinchanger;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public final class ItemSkinChanger extends JavaPlugin implements TabCompleter {

    private final Map<UUID, Long> messageCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("ItemSkinChanger has been enabled!");
        getServer().getPluginManager().registerEvents(new ItemOwnershipListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("ItemSkinChanger has been disabled!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            if (cmd.getName().equalsIgnoreCase("itsc")) {
                if (args.length > 0) {
                    String subcommand = args[0];
                    if (subcommand.equalsIgnoreCase("set")) {
                        if (!player.hasPermission("itemskinchanger.setmodeldata")) {
                            if (getConfig().getBoolean("errorShowPermission")){
                                sendMessage(player, "Required permission: itemskinchanger.setmodeldata");
                                return true;
                            }
                            sendMessage(player, "You do not have permission to use this command.");
                            return true;
                        }
                        if (args.length == 2) {
                            String customModelDataName = args[1];
                            ItemStack item = player.getInventory().getItemInMainHand();
                            // Check if the player is holding an item
                            if (item.getType() != Material.AIR) {
                                setCustomModelData(player, item, customModelDataName);
                            } else {
                                sendMessage(player, "You must be holding an item to use this command.");
                            }
                        } else {
                            sendMessage(player, "Usage: /itsc set <customModelDataName>");
                        }
                    } else if (subcommand.equalsIgnoreCase("reload")) {
                        if (!player.hasPermission("itemskinchanger.reload")) {
                            if (getConfig().getBoolean("errorShowPermission")) {
                                sendMessage(player, "Required permission: itemskinchanger.reload");
                                return true;
                            }
                            sendMessage(player, "You do not have permission to use this command.");
                            return true;
                        }
                        reloadPlugin();
                        sendMessage(player, "Plugin and configuration reloaded.");
                    } else if (subcommand.equalsIgnoreCase("clear")) {
                        if (!player.hasPermission("itemskinchanger.clearmodeldata")) {
                            if (getConfig().getBoolean("errorShowPermission")) {
                                sendMessage(player, "Required permission: itemskinchanger.clearmodeldata");
                                return true;
                            }
                            sendMessage(player, "You do not have permission to use this command.");
                            return true;
                        }
                        clearCustomModelData(player, true);
                    } else {
                        sendMessage(player, "Invalid subcommand. Use /itsc set <customModelDataName>, /itsc clear, or /itsc reload");
                    }

                } else {
                    sendMessage(player, "Usage: /itsc <subcommand> [arguments]");
                }
                return true;
            }
        }
        return false;
    }

    private void reloadPlugin() {
        reloadConfig();
    }
    void setCustomModelData(Player player, ItemStack item, String customModelDataName) {
        item = item.clone();
        String materialName = item.getType().name();

        // Check if the player has the required permission
        if (!player.hasPermission("itemskinchanger.setmodeldata." + materialName.toLowerCase() + "." + customModelDataName.toLowerCase())) {
            sendMessage(player, "You do not have permission to set the custom model data for this item.");
            if (getConfig().getBoolean("errorShowPermission")){
                sendMessage(player, "Required permission: itemskinchanger.setmodeldata." + materialName.toLowerCase() + "." + customModelDataName.toLowerCase());
            }
            return;
        }


        // Get the custom model data from the config
        ConfigurationSection section = getConfig().getConfigurationSection("customModelData." + materialName);
        if (section != null && section.contains(customModelDataName)) {
            int customModelData = section.getInt(customModelDataName);
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(customModelData);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "customModelDataName"), PersistentDataType.STRING, customModelDataName);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "ownerUUID"), PersistentDataType.STRING, player.getUniqueId().toString());
            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);
            sendMessage(player, "Custom model data set to " + customModelDataName + " for the item in your hand.");
        } else {
            sendMessage(player, "The custom model data does not exist for this item material.");
        }
    }

    void clearCustomModelData(Player player, boolean sendMessage) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            if (sendMessage) {
                sendMessage(player, "You must be holding an item to use this command.");
            }
            return;
        }
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(null);
            meta.getPersistentDataContainer().remove(new NamespacedKey(this, "ownerUUID"));
            meta.getPersistentDataContainer().remove(new NamespacedKey(this, "customModelDataName"));
            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);
            if (sendMessage) {
                sendMessage(player, "Custom model data cleared for the item in your hand.");
            }
        } else if (sendMessage) {
            sendMessage(player, "The item in your hand does not have custom model data.");
        }
    }

    void sendMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            UUID playerId = ((Player) sender).getUniqueId();
            long currentTime = System.currentTimeMillis();
            long lastMessageTime = messageCooldowns.getOrDefault(playerId, 0L);

            if (currentTime - lastMessageTime < getConfig().getInt("messageCooldown") * 1000L) {
                return;
            }

            // Update the time of the last message
            messageCooldowns.put(playerId, currentTime);
        }

        sender.sendMessage(message);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("itsc") && sender instanceof Player) {
            if (args.length == 1) {
                // Autocomplete for subcommands
                List<String> subcommands = Arrays.asList("set", "clear", "reload");
                return subcommands.stream().filter(subcommand -> subcommand.startsWith(args[0])).collect(Collectors.toList());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                // Autocomplete for custom model data names
                Player player = (Player) sender;
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() != Material.AIR) {
                    String materialName = item.getType().name();
                    ConfigurationSection section = getConfig().getConfigurationSection("customModelData." + materialName);
                    if (section != null) {
                        return new ArrayList<>(section.getKeys(false));
                    }
                }
            }
        }
        return null;
    }

    public boolean hasCustomModelData(ItemStack item, String customModelDataName) {
        String materialName = item.getType().name();
        // Get config section for the item's material
        ConfigurationSection section = getConfig().getConfigurationSection("customModelData." + materialName);
        // Check if any key in the section matches the customModelDataName
        assert section != null;
        return section.getKeys(false).stream().anyMatch(key -> key.equalsIgnoreCase(customModelDataName));
    }
}
