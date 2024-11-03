![Banner](https://github.com/ladakx/RoseResourcepack/blob/master/banner.png?raw=true)

With this plugin, you will be able to download resource pack files and compress them into an archive using the `/roserp zip` command. After creating the resource pack archive, you can apply it to the game using the `/roserp texture` command. This requires only two commands—no need to manually upload the resource pack to cloud servers; the plugin will handle it for you!

## How Does the Resource Pack Host Work?

The plugin creates a small web server on a different port that hosts the resource packs it distributes. This saves a lot of time and eliminates the common issue of not being able to download resource packs from Dropbox, Google Drive, or other services. The creation of such a web server does not affect the server's performance.

## Features of the Plugin

- Resource pack hosting on the same server.
- Support for multiple versions.
- Supports MiniMessage messages.
- Use multiple resource packs simultaneously.
- Linking resource packs from other plugins (e.g., BetterHUD, ItemsAdder, Oraxen).
- Protect resource packs from unpacking.
- Asynchronous building of resource packs.
- Automatic SHA1 Hash generation.
- Automatic resource pack delivery to a player upon logging into the server.
- Reset resource pack via command.
- Apply multiple resource packs to a player (1.20.3 and later).
- Force resource pack installation (1.17 and later).
- Display a custom message on the prompt screen for clients.

## How to Configure the Plugin

1. **Open a port** on your server for the resource pack (e.g., `8085`). You can find information on how to open ports through a quick Google search or by contacting your hosting company.

2. **Enter the opened port** into the plugin configuration. Note that changing the port requires a server restart; reloading the plugin will not suffice.

## Requirements

- Spigot/Paper version 1.16.5 to 1.21.3
- Java 16 or newer

## Roadmap

- Implement WorldGuard flags.
- Support versions from 1.8 to the latest.
- Enforce forced installation for kicked players.
- Resource pack obfuscation.

## Support

If you have any questions or problems with this resource that are not covered in the documentation, feel free to contact me anytime! The best way to reach me is through the issue tracker, the discussion for this resource, or the Discord channel. For confidential information, you can send me a private message.

I will do my best to respond within 12 hours—during work hours, it may take less than an hour!

## Useful Links

- [Documentation](https://www.spigotmc.org/resources/roseresourcepack-premium-auto-host-resourcepack.120602/field?field=documentation)
- [Discord Support](https://discord.gg/EzXaxQCFQR)
- [SpigotMC](https://www.spigotmc.org/resources/roseresourcepack-premium-auto-host-resourcepack.120602/)

## [Statistics](https://bstats.org/plugin/bukkit/RoseResourcepack/23796)
![Banner](https://bstats.org/signatures/bukkit/RoseResourcepack.svg)
