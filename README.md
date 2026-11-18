# Fizzy Client

An independent Fabric client project for Minecraft 1.21.11.

## Current state

- Original Fizzy Click GUI screen (not Meteor's GUI).
- Five category columns with a dark translucent theme and purple accents.
- Right Shift opens the GUI.
- Left-click a module card to toggle its saved UI state.
- Right-click a module card to open its settings panel.
- Module toggle states are stored in `config/fizzy-client.properties`.

The current commit is the UI foundation. The listed module cards are placeholders; their actual gameplay and HUD features will be implemented independently in later steps. A saved toggle does not imply that a module's feature is implemented yet.

## Build

Requires JDK 21.

```sh
./gradlew build
```
