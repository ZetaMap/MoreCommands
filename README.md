![Visitor Badge](https://visitor-badge.laobi.icu/badge?page_id=ZetaMap.moreCommands) ![Download](https://shields.io/github/downloads/ZetaMap/moreCommands/total)

# More Commands plugin
This plugin adds a bunch of commands (60+) to your server.

> [!IMPORTANT]
> This plugin requires Java 12 or higher. <br>
> To download Java 12, follow steps [here](https://www.oracle.com/fr/java/technologies/javase/jdk12-archive-downloads.html) or [here](https://www.oracle.com/java/technologies/downloads/) to download the latest version.


## <a name="player-commands">Player commands *(total: 14)*</a>
*  ``/help [command|page]`` - Lists all commands or selector examples.
*  ``/w <player> <message...>`` - Whisper to a player.
*  ``/r <message...>`` - Reply to the last whispered message.
*  ``/votekick [player] [reason...]`` - Vote to kick a player with a valid reason.
*  ``/vote <y|n|c>`` - Vote to kick the current player. Admins can cancel the vote with *'c'*.
*  ``/maps [page]`` - List all maps of the server.
*  ``/vnw [y|n|c|f|number]`` - Vote for sending a new wave.
*  ``/rtv [y|n|c|f|mapName...]`` - Vote to change the map.
*  ``/rainbow [on|off] [selector|player...]`` - RAINBOW!!
*  ``/effect [stop|list|search|name|id] [page|selector|player...]`` - Gives you a particle effect.
*  ``/hub`` - Connect you to the hub server. Shortcut of *'/switch hub'*.
*  ``/switch [name|alias...]`` - Connect you to another server.
*  ``/sync [selector|player...]`` - Re-synchronize world state of a player.
*  ``/pinfo [uuid|nickname...]`` - Get all player informations.

## <a name="admin-commands">Admin commands *(total: 25)*</a>
*  ``/chat [on|off]`` - Toggle the chat.
*  ``/tag [page|set|remove] [UUID] [tag...]`` - Configure the tag system.
*  ``/team [teamName|vanish|~] [player|selector...]`` - Change team.
*  ``/tp <player|selector|src-x,y> [player|dest-x,y...]`` - Teleport to a location or player.
*  ``/place <blockName> [player|x,y] [teamName|~] [buildData...]`` - Place a block.
*  ``/fill <blockName> <player|src-x,y> <player|dest-x,y> [teamName|~] [buildData...]`` - Fill a zone.
*  ``/core <small|medium|big|coreName> [player|x,y] [teamName|~...]`` - Build a core.
*  ``/spawn <unit> [count] [player|x,y] [teamName|~] [unitData...]`` - Spawn a unit.
*  ``/transform <unit> [player|selector] [unitData...]`` - Transform a player unit.
*  ``/kill [player|selector...]`` - Kill a player or a unit.
*  ``/clear-map [hard|y|n]`` - Kill all units and blocks, except cores, on the map.
*  ``/weather [clear|weatherName] [intensity] [inf|duration]`` - Control map weather.
*  ``/fillitems [team|all] [items...]`` - Fill the core of the specified or all teams, with the selected or all items.
*  ``/gamemode [name]`` - Change the current map gamemode.
*  ``/pause <on|off>`` - Toggle the game state.
*  ``/godmode [on|off] [player|selector...]`` - **[God]:** I'm divine!
*  ``/ban <player|uuid|ip|selector> [time] [reason...]`` - Ban a player.
*  ``/unban <uuid|ip> [reason...]`` - Unban a player.
*  ``/kick <player|uuid|selector> [time] [reason...]`` - Kick a player.
*  ``/warn <player|uuid|selector> <reason...>`` - Warn a player.
*  ``/mute <player|uuid|selector> [time] [reason...]`` - Mute a player.
*  ``/unmute <player|uuid|selector> [reason...]`` - Unmute a player.
*  ``/freeze <player|uuid|selector> [time] [reason...]`` - Freeze a player.
*  ``/unfreeze <player|uuid|selector> [reason...]`` - Unfreeze a player.
*  ``/pardon <uuid|punishmentId> [reason...]`` - Pardon a player or a punishment.

## <a name="server-commands">Server commands *(total: 23)*</a>
*  ``morecommands [on|off|reload|save] [moduleName]`` - Manage the MoreCommands modules. Requires a server restart to apply the changes.
*  ``commands [reset|on|off] [command] [now]`` - Toggle client/server commands.
*  ``chat [on|off]`` - Toggle the in-game chat.
*  ``tag [on|off|set|remove] [UUID] [tag...]`` - Configure the tag system.
*  ``effect [reset|id|name] [on|off|admins|everyone]`` - Manage the particles effects.
*  ``switch [help|arg0] [args...]`` - Configures the switch system. Use *'switch help'* for usage.
*  ``restart`` - Reconnects players and exits the server with code *'2'*, to ask a restart from the launcher script.
*  ``speed [value]`` - Control the game speed. **USE WITH CAUTION!**
*  ``fillitems [team|all] [items...]`` - Fill the core of the specified or all teams, with the selected or all items.
*  ``gamemode [name]`` - Change the current map gamemode.
*  ~~`blacklist <list|add|remove|clear> <name|ip> [value...]` Players using a nickname or ip in the blacklist cannot connect.~~ <br>
   **Use [this plugin](https://github.com/xpdustry/simple-blacklist) instead. (the current configuration will be migrated automatically if the plugin is present)**
*  ~~`anti-vpn [on|off|token] [your_token]` Anti VPN service.~~ <br>
   **Use [this plugin](https://github.com/xpdustry/anti-vpn-service) instead. (the current configuration will be migrated automatically if the plugin is present)**
*  ``bans [all]`` - List all banned players and IPs.
*  ``ban <player|uuid|ip|selector> [time] [reason...]`` - Ban a player.
*  ``unban <uuid|ip> [reason...]`` - Unban a player.
*  ``kick <player|uuid|selector> [time] [reason...]`` - Kick a player.
*  ``warn <player|uuid|selector> <reason...>`` - Warn a player.
*  ``mute <player|uuid|selector> [time] [reason...]`` - Mute a player.
*  ``unmute <player|uuid|selector> [reason...]`` - Unmute a player.
*  ``freeze <player|uuid|selector> [time] [reason...]`` - Freeze a player.
*  ``unfreeze <player|uuid|selector> [reason...]`` - Unfreeze a player.
*  ``pardon <uuid|punishmentId> [reason...]`` - Pardon a player or a punishment.
*  ``punishments [type|uuid|ip] [all|type]`` - View all or a player punishments.
*  ``reserved-names [on|off|add|remove|message] [name|text...]`` - Reserved nicknames can only be used by admins.
*  ``anti-evade [clear|minutes]`` - Control the anti evade system.


## Building
Pre-build releases can be found [here](https://github.com/ZetaMap/moreCommands/releases).<br>
Or you can build it yourself by running ``./gradlew build``. The file will be named ``more-commands.jar``


## Documentation
### Selectors
**TODO**
