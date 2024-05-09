package org.yusaki.itemskinchanger;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemOwnershipListener implements Listener {
    private final ItemSkinChanger plugin;

    public ItemOwnershipListener(ItemSkinChanger plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        Entity entity = event.getEntity();
        // Check if the entity is a player
        if (!(entity instanceof Player player)){
            return;
        }
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            String ownerUUID = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "ownerUUID"), PersistentDataType.STRING);
            if (ownerUUID != null) {
                if (!player.getUniqueId().toString().equals(ownerUUID)) {
                    // If the player who picked up the item is not the owner, reset the skin to default
                    plugin.clearCustomModelData(player, false);
                } else {
                    // If the player who picked up the item is the owner, set the skin back to the custom skin
                    // You need to store the custom model data name in the item's metadata as well
                    String customModelDataName = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "customModelDataName"), PersistentDataType.STRING);
                    // check if the item has custom model data and if the custom model data name is on the config file
                    if (customModelDataName != null) {
                        if (!plugin.hasCustomModelData(item, customModelDataName)) {
                            if (plugin.getConfig().getBoolean("debug")) {
                                plugin.sendMessage(player, "Custom model data doesn't exist, please set again.");
                            }
                            return;
                        }
                        if (plugin.getConfig().getBoolean("debug")) {
                            plugin.sendMessage(player, "Setting custom model data to " + customModelDataName + " for the item in your hand.");
                        }

                        event.setCancelled(true);
                        plugin.setCustomModelData(player, item, customModelDataName);
                        event.getItem().remove();
                    }
                    else {
                        if (plugin.getConfig().getBoolean("debug")) {
                            plugin.sendMessage(entity, "The item in your hand does not have custom model data.");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            String ownerUUID = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "ownerUUID"), PersistentDataType.STRING);
            if (ownerUUID != null && event.getPlayer().getUniqueId().toString().equals(ownerUUID)) {
                // If the player who dropped the item is the owner, reset the skin to default
                plugin.clearCustomModelData(event.getPlayer(), false);
            }
        }
    }
}