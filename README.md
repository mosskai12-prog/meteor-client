# Fizzy Client

Independent Fabric client for Minecraft Java 1.21.11. This project does not use Meteor Client source or APIs.

## Working features
- Right Shift opens the Fizzy Click GUI.
- Toggle Sprint: automatically sprints while the forward key is held and sneak is not held.
- FPS Display: shows live FPS in the HUD.
- Coordinates: shows the player's current block X/Y/Z.
- Keystrokes: shows live W/A/S/D and jump input.
- Module states save to `config/fizzy-client.properties`.

Left-click a module to toggle it. Right-click for a short description. The other categories are reserved for future original modules.

## Build
Requires JDK 21. Run `./gradlew build`.
