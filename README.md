# SupaStaff

SupaStaff is a Paper/Purpur staff-utilities plugin converted from the supplied Skript.

## Features

- `/vanish` (`/v`) — hide from non-staff; players with `vanish.use` can still see vanished staff.
- `/staffchat [message]` (`/sc`) — toggle staff-only chat or send a one-off staff message.
- `/teleport <player> [target]` (`/tp`) — teleport yourself to a player, or teleport one player to another.
- `/tphere <player>` (`/s`) — bring an online player to you.
- `/gmc`, `/gms`, `/gma`, `/gmsp`, and `/gamemode <mode> [player]` — gamemode shortcuts.
- `/viewinventory <player>` and `/viewenderchest <player>` — inspect online players.
- `/god` (`/godmode`) — toggle damage immunity.
- Configurable messages, action bars, staff notifications, and persistence for vanish/god mode.
- Supports legacy ampersand colors and hex color tags such as `<#FC0000>`.

## Build

Requires Java 21 and Maven. Run:

```sh
mvn clean package
```

The output jar is `target/SupaStaff-1.0.0.jar`. Copy it into the server's `plugins` folder and restart.

## Permissions

All permissions default to operators. Configure them with your permissions plugin:

- `vanish.use`
- `staffchat.use`
- `teleport.use`
- `gamemode.use`
- `viewinventory.use`
- `viewenderchest.use`
- `godmode.use`

## Notes

Only online targets can be teleported to or inspected. Staff chat is staff-permission gated. Vanish and god mode state are saved in the plugin config when their respective persistence settings are enabled.
