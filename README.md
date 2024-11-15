# RoseResourcepack

![Banner](https://github.com/ladakx/RoseResourcepack/blob/master/banner.png?raw=true)

**RoseResourcepack** is a powerful plugin that simplifies the process of creating and distributing resource packs for Minecraft servers. With just two commands, you can generate, zip, and apply resource packs to your game—all without needing to manually upload them to cloud storage!

## Key Commands

- **`/roserp zip`**: Compress resource pack files into a zip archive.
- **`/roserp texture`**: Apply the resource pack archive to the game.

The plugin automatically hosts resource packs on your server, eliminating the need for external storage solutions. Just configure, command, and play!

---

## How It Works

RoseResourcepack launches a lightweight web server on a separate port to host and deliver resource packs. This allows seamless downloading and installation for players without impacting server performance. No more issues with third-party cloud storage like Dropbox or Google Drive!

---

## Plugin Features

- **Integrated Resource Pack Hosting**: Host resource packs directly from your server.
- **Multi-Version Support**: Compatible with Minecraft versions 1.16.5 through 1.21.3.
- **MiniMessage Support**: Customize messages using MiniMessage.
- **Multiple Resource Packs**: Apply multiple resource packs simultaneously.
- **Integration with Other Plugins**: Works with plugins like BetterHUD, ItemsAdder, and Oraxen.
- **Resource Pack Protection**: Protect your resource packs from being unpacked.
- **Asynchronous Processing**: Builds resource packs asynchronously for minimal lag.
- **Automatic SHA1 Hashing**: Generate SHA1 hashes automatically.
- **Auto-Delivery**: Automatically delivers resource packs to players when they log in.
- **Command to Reset Resource Pack**: Allows players to reset their resource packs via command.
- **Multi-Pack Application**: Apply multiple resource packs for players (Minecraft 1.20.3+).
- **Forced Installation**: Enforce resource pack installation for clients (Minecraft 1.17+).
- **Custom Message Support**: Display a custom message on the client’s prompt screen.

---

## Installation and Configuration

1. **Open a Port**: Open a port on your server (e.g., `8085`) for resource pack hosting. Instructions on opening ports can be found online or by contacting your hosting provider.

2. **Configure the Port in Plugin Settings**: Enter the chosen port in the plugin configuration file. *Note:* Changing the port requires a server restart.

---

## Requirements

- **Minecraft Server Software**: Spigot or Paper (versions 1.16.5 to 1.21.3)
- **Java**: Java 16 or newer

---

## Roadmap

The following features are planned for future releases:

- **WorldGuard Flags**: Integrate with WorldGuard for region-specific resource packs.
- **Extended Version Support**: Add support for Minecraft versions 1.8 and up.
- **Enforced Installation for Kicked Players**: Re-apply resource packs for rejoining players.
- **Resource Pack Obfuscation**: Enhance resource pack security.

---

## Support

Need help? Contact us through:

- **Issue Tracker**: Report issues directly on GitHub.
- **Discussions**: Join the conversation in our [SpigotMC forum](https://www.spigotmc.org/resources/roseresourcepack-premium-auto-host-resourcepack.120602/).
- **Discord**: Join our community on [Discord](https://discord.gg/EzXaxQCFQR) for quick responses.

For private or sensitive inquiries, you can send a direct message. Responses typically come within 12 hours, or less during work hours.

---

## Useful Links

- [Documentation](https://www.spigotmc.org/resources/roseresourcepack-premium-auto-host-resourcepack.120602/field?field=documentation)
- [Discord Support](https://discord.gg/EzXaxQCFQR)
- [SpigotMC Page](https://www.spigotmc.org/resources/roseresourcepack-premium-auto-host-resourcepack.120602/)

## Plugin Statistics

[![Plugin Statistics](https://bstats.org/signatures/bukkit/RoseResourcepack.svg)](https://bstats.org/plugin/bukkit/RoseResourcepack/23796)

---

Thank you for choosing **RoseResourcepack** for your Minecraft server needs! We’re excited to help make resource pack management easy and efficient.