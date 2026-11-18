# Fizzy Client

Fizzy Client is being developed for **Minecraft Java 1.21.11** on Fabric.

This migration workspace uses the **Aoba Client 1.21.6** source as its upstream reference. Aoba is licensed under **GNU GPL v3 or later**; Fizzy preserves that license for the port and retains upstream attribution. This project is not affiliated with or endorsed by the Aoba developers.

## Current status

This branch is a porting workspace, **not an installable release yet**. The existing independent Fizzy scaffold is still the working baseline while Aoba's source is migrated in stages. A version-number change alone is not a compatibility port.

## Security decisions

- Aoba Global Chat is not enabled or started.
- Aoba's legacy Alt Manager token storage is not carried over unchanged.
- Discord Rich Presence and third-party add-on loading must remain opt-in and be reviewed before use.
- No bundled third-party add-ons or automatic downloads.
- No claim is made that an unbuilt or untested JAR is malware-free.

## Build

Requires JDK 21. Run `./gradlew build` from a machine with internet access so Gradle can resolve Fabric/Minecraft dependencies.

## Upstream

- Aoba Client source: https://github.com/Cocolots/Aoba-Client/tree/1.21.6
- Aoba license: GNU GPL v3 or later
