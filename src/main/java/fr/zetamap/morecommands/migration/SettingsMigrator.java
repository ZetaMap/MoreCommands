/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2025  ZetaMap
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package fr.zetamap.morecommands.migration;

import arc.Core;
import arc.struct.StringMap;
import arc.struct.Seq;

import mindustry.Vars;
import mindustry.mod.Mods;
import mindustry.net.Administration;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.module.ModuleRegistry;
import fr.zetamap.morecommands.modules.effect.Effects;
import fr.zetamap.morecommands.modules.security.Punishment;
import fr.zetamap.morecommands.modules.security.PunishmentDuration;
import fr.zetamap.morecommands.modules.server.Server;
import fr.zetamap.morecommands.util.Logger;
import fr.zetamap.morecommands.util.Strings;


public class SettingsMigrator {
  private static final Logger logger = new Logger(SettingsMigrator.class);
  public static boolean enabled = true;
  
  /** Migrate all the all moreCommands settings to an appropriate config file. */
  public static void migrateAllTheShittySettingsIMade() {
    if (!enabled || !isNeeded()) return;
    logger.warn("Detected an old MoreCommands configuration. Migrating settings...");
    
    migrateTags();
    migrateAutopause();
    migrateBanReasons();
    migrateEffects();
    migrateSwitcher();
    migrateSelectors();
    migrateCommandsDisabler();
    migrateAntiVpn();
    migrateAdminLogs();
    migrateBlacklist();
    
    logger.warn("MoreCommands settings migrated.");
  }
  
  public static boolean isNeeded() {
    return isTagsMigrationNeeded()             || isAutopauseMigrationNeeded()
        || isBanReasonsMigrationNeeded()       || isEffectsMigrationNeeded() 
        || isSwitcherMigrationNeeded()         || isSelectorsMigrationNeeded() 
        || isCommandsDisablerMigrationNeeded() || isAntiVpnMigrationNeeded()
        || isAdminLogsMigrationNeeded()        || isBlacklistMigrationNeeded();
  }
  
  public static boolean isTagsMigrationNeeded() {
    return Core.settings.has("Tags") || Core.settings.has("PlayersTags");
  }
  
  public static void migrateTags() {
    if (!isTagsMigrationNeeded()) return;
    
    if (Core.settings.has("Tags")) 
      try {
        boolean enabled = Core.settings.getBool("Tags"); 
        if (enabled) Modules.tags.enable();
        else Modules.tags.disable();
      } catch (ClassCastException e) { logger.err("Error while migrating player tags: @. Skipping...", e.toString()); }
    
    if (Core.settings.has("PlayersTags")) 
      Core.settings.getJson("PlayersTags", StringMap.class, StringMap::new).each(Modules.tags::put);
    
    Core.settings.remove("Tags");
    Core.settings.remove("PlayersTags");
    Modules.tags.save();
    logger.info("Player tags migrated.");
  }
  
  public static boolean isAutopauseMigrationNeeded() {
    return Core.settings.has("AutoPause");
  }
  
  public static void migrateAutopause() {
    if (!isAutopauseMigrationNeeded()) return;
    
    // Just remove the field and r-enable the server auto pause.
    Core.settings.remove("AutoPause");
    Administration.Config.autoPause.set(true);
    logger.info("Auto pause migrated. &fi(removed)&fr");
  }
  
  public static boolean isBanReasonsMigrationNeeded() {
    return Core.settings.has("BansReason");
  }
  
  public static void migrateBanReasons() {
    if (!isBanReasonsMigrationNeeded()) return;
    
    StringMap reasons = Core.settings.getJson("BansReason", StringMap.class, StringMap::new);
    
    // Copy also bans from the default system.
    Vars.netServer.admins.getBanned().each(p -> {
      Modules.punishments.punish(null, p.id, p.lastIP, Punishment.Type.ban, PunishmentDuration.permanant, reasons.remove(p.id));
      // Remove bans to properly gets control of the ban system
      Vars.netServer.admins.getBannedIPs().removeAll(p.ips);
      p.banned = false; 
    });
    Vars.netServer.admins.save();
    
    reasons.each((p, r) -> {
      Administration.PlayerInfo info = Vars.netServer.admins.getInfoOptional(p);
      if (info != null) {
        Modules.punishments.punish(null, p, info.lastIP, Punishment.Type.ban, PunishmentDuration.permanant, r);
        Vars.netServer.admins.getBannedIPs().removeAll(info.ips);
      } else Modules.punishments.punish(null, p, null, Punishment.Type.ban, PunishmentDuration.permanant, r);
    });
    
    Core.settings.remove("BansReason");
    logger.info("Ban reasons migrated. \nPlease note that bans are considered as permanent, "
              + "so you will need to manually edit them to make them temporary.");
  }
  
  public static boolean isEffectsMigrationNeeded() {
    return Core.settings.has("removedEffects") || Core.settings.has("adminEffects");
  }
  
  public static void migrateEffects() {
    if (!isEffectsMigrationNeeded()) return;
    
    if (Core.settings.has("removedEffects")) {
      try {
        for (String line : Core.settings.getString("removedEffects").split(" \\| ")) {
          Effects e = Modules.effects.get(line);
          if (e != null) e.disabled(true);
        }
      } catch (Exception e) { logger.err("Error while migrating disabled effects: @. Skipping...", e.toString()); }
    }
    
    if (Core.settings.has("adminEffects")) {
      try {
        for (String line : Core.settings.getString("adminEffects").split(" \\| ")) {
          Effects e = Modules.effects.get(line);
          if (e != null) e.adminOnly(true);
        }
      } catch (Exception e) { logger.err("Error while migrating admin effects: @. Skipping...", e.toString()); }
    }
    
    Core.settings.remove("removedEffects");
    Core.settings.remove("adminEffects");
    Modules.effects.save();
    logger.info("Effects settings migrated.");
  }
  
  public static boolean isSwitcherMigrationNeeded() {
    return Core.settings.has("SwitchList");
  }
  
  public static void migrateSwitcher() {
    if (!isSwitcherMigrationNeeded()) return;
    
    try {
      Core.settings.getJson("SwitchList", StringMap.class, StringMap::new).each((n, a) -> {
        String name = n.replace('_', ' ').strip(),
               strippedName = Strings.normalize(name),
               internalName = Strings.kebabize(strippedName),
               address = a.substring(0, a.lastIndexOf('-'));
        boolean admin = Boolean.valueOf(a.substring(a.lastIndexOf('-')+1));
        
        if (strippedName.toLowerCase().equals("lobby")) internalName = Modules.switcher.hubServerName;
        Modules.switcher.put(new Server(internalName, null, name, address, admin));
      });
    } catch (Exception e) { logger.err("Error while migrating switch list: @.", e.toString()); }
    
    Core.settings.remove("SwitchList");
    Modules.switcher.save();
    logger.info("Switch list migrated.");
  }
  
  public static boolean isSelectorsMigrationNeeded() {
    return Core.settings.has("ArgsFilter");
  }
  
  public static void migrateSelectors() {
    if (!isSelectorsMigrationNeeded()) return;
    
    if (Core.settings.has("ArgsFilter")) {
      if (Core.settings.getBool("ArgsFilter")) ModuleRegistry.enable(Modules.selector);
      else ModuleRegistry.disable(Modules.selector);
    }
    
    Core.settings.remove("ArgsFilter");
    logger.info("Selectors migrated.");
  }
  
  public static boolean isCommandsDisablerMigrationNeeded() {
    return Core.settings.has("handlerManager");
  }
  
  public static void migrateCommandsDisabler() {
    if (!isCommandsDisablerMigrationNeeded()) return;
    
    try {
      for (String line : Core.settings.getString("handlerManager").split(" \\| ")) {
        String[] temp = line.split("\\=");
        if (Boolean.parseBoolean(temp[1])) continue;
        else if (temp[0].startsWith(Modules.commands.clientHandler.prefix)) 
          Modules.commands.disableClientCommand(temp[0].substring(Modules.commands.clientHandler.prefix.length()));
        else if (temp[0].startsWith(Modules.commands.serverHandler.prefix))
          Modules.commands.disableServerCommand(temp[0].substring(Modules.commands.serverHandler.prefix.length()));
        else logger.warn("Invalid command name: '@'.", temp[0]);
      }
    } catch (Exception e) { logger.err("Error while migrating disabled commands: @. Skipping...", e.toString()); }
    
    Core.settings.remove("handlerManager");
    Modules.commands.save();
    logger.info("Disabled commands migrated.");
  }
  
  public static boolean isAntiVpnMigrationNeeded() {
    return Core.settings.has("anti-vpn") || Core.settings.has("anti-vpn-token");
  }
  
  public static void migrateAntiVpn() {
    if (!isAntiVpnMigrationNeeded()) return;
    if (!checkForAntiVpnService("anti vpn")) return;
/* 
    if (Core.settings.has("anti-vpn-token")) {
      String token = Core.settings.getString("anti-vpn-token");
      com.xpdustry.avs.service.providers.type.AddressProvider p = com.xpdustry.avs.service.AntiVpnService.get("vpnapi");
      if (p == null) {
        logger.err("Error while migrating the anti vpn: @.", "Cannot find the 'vpnapi' provider");
        return;
      }
      ((com.xpdustry.avs.service.providers.online.VpnApi)p).addToken(token);
    }
    
    Core.settings.remove("anti-vpn");
    Core.settings.remove("anti-vpn-token");
    logger.info("Anti VPN settings migrated.");
*/
  }
  
  public static boolean isAdminLogsMigrationNeeded() {
    return Core.settings.has("ALogFiles") || Core.settings.has("ALogEnabled");
  }
  
  public static void migrateAdminLogs() {
    if (!isAdminLogsMigrationNeeded()) return;
    
    logger.info("Admin logs are useless and no one whatch them, so there were removed since MoreCommands 12.");
    Core.settings.remove("ALogFiles");
    Core.settings.remove("ALogEnabled");
    Core.files.local("config/admin-logs").deleteDirectory();
    logger.info("Admin logs successfully removed.");
  }
  
  public static boolean isBlacklistMigrationNeeded() {
    return Core.settings.has("bannedNamesList") || Core.settings.has("bannedIpsList");
  }
  
  public static void migrateBlacklist() {
    if (!isBlacklistMigrationNeeded()) return;
/*
    if (Core.settings.has("bannedNamesList") && checkForSimpleBlacklist("name blacklist")) {
      Core.settings.getJson("bannedNamesList", Seq.class, Seq::new)
                   .each(n -> com.xpdustry.simple_blacklist.Config.namesList.put((String)n, 0));
      Core.settings.remove("bannedNamesList");
      logger.info("Nickname blacklist migrated.");
    }
    
    if (Core.settings.has("bannedIpsList") && checkForAntiVpnService("ip blacklist")) {
      com.xpdustry.avs.service.providers.type.AddressProvider p = com.xpdustry.avs.service.AntiVpnService.get("blacklist");
      if (p == null) {
        logger.err("Error while migrating the IP blacklist: @.", "Cannot find the 'blacklist' provider");
        return;
      }
      Core.settings.getJson("bannedIpsList", Seq.class, Seq::new)
                   .each(a -> ((com.xpdustry.avs.service.providers.custom.Blacklist)p).add(
                                new com.xpdustry.avs.address.Address((String)a)));
      Core.settings.remove("bannedIpsList");
      logger.info("IP blacklist migrated.");
    }
*/
  }

  /** Check if the AVS (anti-vpn-service) plugin is present and has the right version. */
  private static boolean checkForAntiVpnService(String kind) {
    Mods.LoadedMod mod = Vars.mods.getMod("anti-vpn-service");
    if (mod == null) {
      logger.warn("Found " + kind + " settings, but the additional &lcAVS (anti-vpn-service)&fr plugin is missing.\n"
                + "Since MoreCommands 12, the anti vpn feature has been removed in favor of this additional plugin, "
                + "which provides a better service and more accurate results.\n"
                + "Please install this plugin at @ and restart the server, so that your current settings can be migrated.",
                  "https://github.com/xpdustry/anti-vpn-service/releases");
      return false;
    } else if (Strings.isVersionAtLeast("1.2", mod.meta.version)) {
      logger.warn("The currently installed AVS (anti-vpn-service) plugin is too old for a settings migration.\n"
                + "Please install the latest version at: @",
                "https://github.com/xpdustry/anti-vpn-service/releases/latest");
      return false;
    }
    return true;
  }

  /** Check if the simple-blacklist plugin is present and has the right version. */
  private static boolean checkForSimpleBlacklist(String kind) {
    Mods.LoadedMod mod = Vars.mods.getMod("simple-blacklist");
    if (mod == null) {
      logger.warn("Found " + kind + " settings, but the additional &lcsimple-blacklist&fr plugin is missing.\n"
                + "Since MoreCommands 11.4, the player nickname blacklist has been removed in favor of this additional plugin, "
                + "providing a better service and can also block nicknames with regex.\n"
                + "Please install this plugin at @ and restart the server, so that your current settings can be migrated.",
                  "https://github.com/xpdustry/simple-blacklist/releases");
      return false;
    } else if (Strings.isVersionAtLeast("1.7", mod.meta.version)) {
      logger.warn("The currently installed simple-blacklist plugin is too old for a settings migration.\n"
                + "Please install the latest version at: @",
                "https://github.com/xpdustry/simple-blacklist/releases/latest");
      return false;
    }
    return true;
  }
}
