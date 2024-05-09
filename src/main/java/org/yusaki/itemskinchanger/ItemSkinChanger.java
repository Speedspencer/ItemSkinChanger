package org.yusaki.itemskinchanger;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.bukkit.ChatColor;

import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public final class ItemSkinChanger extends JavaPlugin implements TabCompleter {

    private final Map<UUID, Long> messageCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        updateConfig();
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
                                sendMessage(player, "requiredPermission", "itemskinchanger.setmodeldata");
                                return true;
                            }
                            sendMessage(player, "usage");
                            return true;
                        }
                        if (args.length == 2) {
                            String customModelDataName = args[1];
                            ItemStack item = player.getInventory().getItemInMainHand();
                            // Check if the player is holding an item
                            if (item.getType() != Material.AIR) {
                                setCustomModelData(player, item, customModelDataName);
                            } else {
                                sendMessage(player, "mustHoldItem");
                            }
                        } else {
                            sendMessage(player, "usage");
                        }
                    } else if (subcommand.equalsIgnoreCase("reload")) {
                        if (!player.hasPermission("itemskinchanger.reload")) {
                            if (getConfig().getBoolean("errorShowPermission")) {
                                sendMessage(player, "requiredPermission", "itemskinchanger.reload");
                                return true;
                            }
                            sendMessage(player, "noPermission");
                            return true;
                        }
                        reloadPlugin();
                        sendMessage(player, "pluginReloaded");
                    } else if (subcommand.equalsIgnoreCase("clear")) {
                        if (!player.hasPermission("itemskinchanger.clearmodeldata")) {
                            if (getConfig().getBoolean("errorShowPermission")) {
                                sendMessage(player, "requiredPermission", "itemskinchanger.clearmodeldata");
                                return true;
                            }
                            sendMessage(player, "noPermission");
                            return true;
                        }
                        clearCustomModelData(player, player.getInventory().getItemInMainHand(),true, true);
                    } else {
                        sendMessage(player, "invalidSubcommand");
                    }

                } else {
                    sendMessage(player, "usage");
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
            sendMessage(player, "customModelDataNoPermission");
            if (getConfig().getBoolean("errorShowPermission")){
                sendMessage(player, "requiredPermission", "itemskinchanger.setmodeldata." + materialName.toLowerCase() + "." + customModelDataName.toLowerCase());
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
            sendMessage(player, "customModelDataSet", customModelDataName);
        } else {
            sendMessage(player, "customModelDataNotExist");
        }
    }

    void clearCustomModelData(Player player,ItemStack item ,boolean clearOwner ,boolean sendMessage) {

        if (item.getType() == Material.AIR) {
            if (sendMessage) {
                sendMessage(player, "customModelDataSet");
            }
            return;
        }
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            ItemMeta meta = item.getItemMeta();
            meta.setCustomModelData(null);
            if (clearOwner) {
                meta.getPersistentDataContainer().remove(new NamespacedKey(this, "ownerUUID"));
                meta.getPersistentDataContainer().remove(new NamespacedKey(this, "customModelDataName"));
            }
            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);
            if (sendMessage) {
                sendMessage(player, "customModelDataCleared");
            }
        } else if (sendMessage) {
            sendMessage(player, "itemNoCustomModelData");
        }
    }


    void sendMessage(CommandSender sender, String key ,Object... args) {
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

        // Retrieve the message from the configuration
        String message = getConfig().getString("messages." + key);
        String prefix = getConfig().getString("messages.prefix");
        if (message != null && prefix != null) {
            // Format the message with the provided arguments
            message = String.format(message, args);

            // Translate color codes
            message = ChatColor.translateAlternateColorCodes('&', message);
            prefix = ChatColor.translateAlternateColorCodes('&', prefix);

            sender.sendMessage(prefix + message);
        } else {
            sender.sendMessage("Raw message: " + key);
        }
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

    public void updateConfig() {
        reloadConfig();
        // Load the default configuration from the JAR file
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(getResource("config.yml"))));

        // Get the version of the default configuration
        double defaultVersion = defaultConfig.getDouble("version");
        if (getConfig().getBoolean("debug")){
            getLogger().info("Plugin config version: " + defaultVersion);
        }

        // Get the version of the configuration on the file system
        double currentVersion = getConfig().getDouble("version");
        if (getConfig().getBoolean("debug")){
            getLogger().info("Current config version: " + currentVersion);
        }
        // If the default configuration is newer
        if (defaultVersion > currentVersion) {
            // Add new values
            if (getConfig().getBoolean("debug")){
                getLogger().info("Config Version Mismatched, Updating config file...");
            }
            for (String key : defaultConfig.getKeys(true)) {
                getLogger().info("Checking key: " + key);
                if (!getConfig().isSet(key)) {
                    if (getConfig().getBoolean("debug")) {
                        getLogger().info("Missing Config, Adding new config value: " + key);
                    }
                    getConfig().set(key, defaultConfig.get(key));
                } else {
                    if (getConfig().getBoolean("debug")){
                        getLogger().info("Config value already exists: " + key);
                    }
                }
                // change the version to the default version
                getConfig().set("version", defaultVersion);
            }
            // Save the configuration file
            saveConfig();
        }
        else {
            if (getConfig().getBoolean("debug")){
                getLogger().info("Config file is up to date.");
            }
        }

        // Reload the configuration file to get any changes
        reloadConfig();
    }
}
