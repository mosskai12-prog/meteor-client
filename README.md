# Fizzy Client

Fizzy Client is an independent Minecraft Java client project for Minecraft 1.21.11, built on Fabric.

This repository is being rebuilt from a clean project structure. It does not use Meteor Client, Meteor modules, Meteor GUI classes, Orbit, or Meteor's internal libraries.

## Requirements
- JDK 21
- Minecraft 1.21.11
- Fabric Loader

## Build

Windows:
```powershell
.\gradlew.bat build
```

Linux/macOS:
```bash
./gradlew build
```

The remapped mod JAR is generated in `build/libs/`.

## Project structure
- `com.fizzy.client` — Fizzy's own entry point and client systems.
- `com.fizzy.client.gui` — Fizzy-owned interface code.
- `com.fizzy.client.modules` — Fizzy-owned module framework.

The old Meteor-derived implementation is not part of this branch's source tree.
