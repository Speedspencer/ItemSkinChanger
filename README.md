# Item Skin Changer

Item Skin Changer is a Minecraft plugin developed in Java using the Bukkit/Spigot API. It allows players to change the skin of items in their inventory.

## Features

- Set custom model data for items
- Clear custom model data from items
- Debug mode for developers
- Handles item duplication issue when picked up by non-owner

## Commands

- `/itsc set <customModelDataName>`: Sets the custom model data for the item in the player's main hand.
- `/itsc clear`: Clears the custom model data from the item in the player's main hand.
- `/itsc reload`: Reloads the plugin's configuration.

## Permissions

- `itemskinchanger.setmodeldata.<materialName>.<customModelDataName>`: Allows a player to set the custom model data for a specific item material.

## Configuration

The `config.yml` file contains the following options:

- `debug`: Enables or disables debug mode. When enabled, messages will be sent to players when they pick up items.
- `messageCooldown`: The minimum time in seconds between messages sent to the same player.
- `errorShowPermission`: If true, the required permission will be shown when a player tries to set the custom model data without having the necessary permission.
- `customModelData`: A section for each item material, containing the custom model data names and their corresponding values.

## Handling Item Duplication

When a player who is not the owner of an item picks it up, the plugin prevents the item from being duplicated. This is done by manually adding the item to the player's inventory and then removing the item entity from the world.

## Installation

To install the plugin, place the `.jar` file in your server's `plugins` folder and restart the server.

## Building

This project uses Gradle for building. To build the project, run `./gradlew build`. The resulting `.jar` file can be found in the `build/libs` directory.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request on GitHub.

## License

This project is licensed under the MIT License. See the `LICENSE` file for more details.