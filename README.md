# Fizzy Client

Fizzy Client is a Fabric client project targeting **Minecraft Java 1.21.11**.

## Click GUI

Fizzy keeps the Aoba-derived Click GUI framework and its visual layout: category windows, navigation tabs, draggable/pinnable HUD windows, module rows, and per-module settings opened with right-click. The default accent is purple, and the interface uses Minecraft's built-in pixel font.

The Click GUI opens with **Right Shift**. Left-click a module to toggle it; right-click a module to open its settings.

## Source and licence

The GUI framework and portions of the client are derived from **Aoba Client**. Aoba is licensed under **GNU GPL v3 or later**. Fizzy preserves the GPL-3.0-or-later licence and upstream attribution. This project is not affiliated with or endorsed by the Aoba developers.

Upstream: https://github.com/Cocolots/Aoba-Client/tree/1.21.6

## Safety choices in this branch

- Aoba Global Chat is not started.
- Third-party Fabric entrypoints for Aoba add-ons are not automatically loaded.
- Discord Rich Presence is removed from the default module registry.
- The legacy Alt Manager is not initialized and its saved tokens are not written during shutdown.
- The password-cracking window is not exposed in the Tools tab.
- The main menu no longer makes an automatic request to Aoba's release API or opens an Aoba Discord invite.

These changes reduce unnecessary external behavior; they are not a guarantee that every source file or dependency is vulnerability-free. Review the code and dependencies before running a built JAR.

## Build status

This is a development branch, **not yet a verified release**. It has not been confirmed to compile successfully against Minecraft 1.21.11. Build with JDK 21 and run `./gradlew build` on a machine with internet access to resolve Gradle dependencies.
