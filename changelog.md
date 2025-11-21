* The entire plugin has been refactored, cleaned up and optimized.
* Removed the anti-vpn system in favor of the [Anti-VPN-Service plugin](https://github.com/xpdustry/Anti-VPN-Service). <br> 
  The configuration will be migrated automatically to this one, if it's present.
* The blacklist system is removed since the 11.4 update, but now the configuration will be migrated automatically to [Anti-VPN-Service](https://github.com/xpdustry/Anti-VPN-Service) and [Simple-Blacklist](https://github.com/xpdustry/Simple-Blacklist), if these plugins are present.
* New configuration system, storing settings to independent files in ``config/mods/more-commands``. <br>
  The current configuration will be migrated automatically at server startup.
* Removed Admin logs because it's was a mess and nobody use it.
* The plugin has been split into independent and controllable modules with the ``morecommands`` command.
* New punishment system with reason and duration.
* New commands. *(e.g. ``/place``, ``/fill``, ``/freeze``, etc)*
* Removed useless commands.
* Added events to track a bit what the plugin is doing.
* API friendly modules: You can now expend this plugin to add more modules to the ``ModuleFactory`` or custom rules to the ``Gatekeeper``.
* Added minecraft-like target selectors *(e.g. ``@p``, ``@a``, ``@e``)*
* Added minecraft-like coordinates *(e.g. ``~``, ``~-10``, ``~,~``, ``~-10,~``)*
* Changed license from MIT to GPL-3
* Fixed GodMode instant build
* Added GodMode instant unit/building kill
* Added more commands
