/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2021-2025  ZetaMap
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

package fr.zetamap.morecommands;

import arc.util.CommandHandler;
import arc.util.Reflect;
import arc.util.Time;

import mindustry.Vars;
import mindustry.mod.Mods;
import mindustry.net.Administration.Config;

import fr.zetamap.morecommands.migration.SettingsMigrator;
import fr.zetamap.morecommands.misc.Gatekeeper;
import fr.zetamap.morecommands.module.ModuleFactory;
import fr.zetamap.morecommands.util.Autosaver;
import fr.zetamap.morecommands.util.Logger;
import fr.zetamap.morecommands.util.VersionChecker;


public class Main extends mindustry.mod.Plugin {
  private static final Logger logger = new Logger(true);

  {
    ModuleFactory.configFile = getConfig();
    ModuleFactory.preInit();
    Modules.register(this);
  }
  
  @Override
  public void init() {
    // TODO: should start after the server, because of possible added modules
    long time = Time.nanos();
    
    Logger.init(this);
    logger.info("&lc>>>&fr MoreCommands plugin is loading...\n");
    
    VersionChecker.checkFor(this);
    
    PlayerData.init();
    Gatekeeper.init();
    if (check(ModuleFactory::init)) return;
    SettingsMigrator.enabled = false; //TODO: debug
    if (check(SettingsMigrator::migrateAllTheShittySettingsIMade)) return;
    
    Autosaver.spacing(Config.autosaveSpacing.num());
    Autosaver.start();
    Autosaver.save(); // just to be sure
    
    // Use reflection to follow the server autosave spacing
    Reflect.set(Config.autosaveSpacing, "changed", (Runnable)() -> Autosaver.spacing(Config.autosaveSpacing.num()));
    
    logger.info("\n&lc>>>&fr MoreCommands plugin loaded in @ seconds! enjoy the fun =)", 
                Time.timeSinceNanos(time) / 1_000_000_000f);
  }
  
  @Override
  public void registerServerCommands(CommandHandler handler) {
    ModuleFactory.registerServerCommands(handler);
  }

  @Override
  public void registerClientCommands(CommandHandler handler) {
    ModuleFactory.registerClientCommands(handler);
  }
  
  private static Mods.ModMeta meta;
  public static Mods.ModMeta getMeta() {
    if (meta != null) return meta;
    Mods.LoadedMod load = Vars.mods.getMod(Main.class);
    if(load == null) throw new IllegalArgumentException("Mod is not loaded yet (or missing)!");
    return meta = load.meta;
  }
  
  private static boolean check(arc.util.UnsafeRunnable run) {
    try {
      run.run();
      return false;
    } catch (Throwable t) {
      logger.err(t);
      error();
      return true;
    }
  }
  
  private static boolean check(arc.func.Boolp run) {
    try {
      if (run.get()) return false;
    } catch (Throwable t) { logger.err(t); }
    error();
    return true;
  }
  
  private static void error() {
    logger.err("\n########################################\n\n"
             + "&lc>>>&fr MoreCommands plugin failed to initialize due to previous error(s)!\n"
             + "&lc>>>&fr Most often, this is caused by an invalid or corrupted configuration files.\n"
             + "&lc>>>&fr If this is not the case and this/these error(s) are recurring, please report them here:\n"
             + "&lc>>>&fr   @", "https://github.com/ZetaMap/MoreCommands/issues/new");
    ModuleFactory.setError();
    ModuleFactory.dispose(); // "Uninitialize" MoreCommands. At least, removes the commands and stops the automated things.
    logger.err("&lc>>>&fr\n"
             + "&lc>>>&fr MoreCommands has been uninitialized to avoid further issues.\n"
             + "\n########################################\n");
  }
}
