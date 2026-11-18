# Fizzy Client — Aoba port notes

## Target

- Minecraft Java: **1.21.11** (keep this target; do not downgrade)
- Java: **21**
- Loader: Fabric
- Working branch: `fizzy-aoba-port`
- Starting point: the independent Fizzy scaffold on `fizzy-rewrite`

## Source reviewed

This port is based on the uploaded `Aoba-Client` `1.21.6` branch archive. The archive passed ZIP integrity testing and contains 398 Java source files.

Aoba's source headers and README identify the project as **GNU GPL version 3 or later**. Its `fabric.mod.json` says `CC0-1.0`, which conflicts with the repository's LICENSE and source headers. Fizzy will preserve the GPL-3.0-or-later license and original copyright notices for any Aoba-derived code; the metadata will not be used to silently relicense that code.

## Initial security review

The source review found no proof that Aoba is a RAT, but this is not a complete security audit and does not certify the compiled JAR as malware-free.

Items that need to be disabled, removed, or redesigned before they are enabled in Fizzy:

- **Global Chat** opens a background TCP connection to `chat.aobaclient.com:42069` and sends the Minecraft username. It is not required for the core client and will not be started by default.
- **Alt Manager** stores account data in `aoba_alts.json` using AES/ECB with a key derived from machine/environment values. This is not strong credential protection. The old token store will not be carried over as-is.
- **Discord Rich Presence** sends game/status information to Discord when enabled. It must remain opt-in and clearly disclosed.
- **Addon loading** can execute third-party mod code. No untrusted add-ons will be bundled or auto-downloaded.
- The Discord command launches the user's default browser through an OS process. This is not needed for the client core and will be reviewed rather than copied blindly.

Microsoft authentication code communicates with Microsoft/Xbox/Minecraft service endpoints in the inspected source. Do not enter or save Microsoft credentials in an unreviewed build.

## Porting rules

1. Keep the existing independent Fizzy branch intact; work on this separate branch.
2. Rebrand package names, mod ID, resources, menus, and metadata to Fizzy.
3. Preserve Aoba attribution and GPL terms for derived files.
4. Port Minecraft API/mapping changes from 1.21.6 to 1.21.11; changing only the version strings is not a port.
5. Do not bundle Meteor Client source or APIs.
6. Do not include stealth, anti-detection, server-ban bypass, or exploit-oriented behavior.
7. Keep network/account features disabled until their data flows and storage are reviewed.
8. Run the 1.21.11 Gradle build and fix compiler/mixin failures before calling the branch usable.

## Current status

- [x] Source archive received and ZIP integrity checked
- [x] Project configuration, license, metadata, and mixin declarations inspected
- [x] Initial source-level network, process-launch, and credential-storage review
- [x] Isolated migration branch created
- [ ] Aoba source transplanted and rebranded
- [ ] Minecraft 1.21.11 API port completed
- [ ] Build and runtime smoke tests passed
- [ ] Final security/dependency review completed

**This branch is a migration workspace, not a finished or safe-to-install release yet.**
