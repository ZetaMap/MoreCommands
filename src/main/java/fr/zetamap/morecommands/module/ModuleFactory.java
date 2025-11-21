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

package fr.zetamap.morecommands.module;

import arc.ApplicationListener;
import arc.Core;
import arc.files.Fi;
import arc.func.Boolf;
import arc.func.Cons;
import arc.func.Cons2;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.util.CommandHandler;

import mindustry.mod.Mod;

import fr.zetamap.morecommands.command.*;
import fr.zetamap.morecommands.util.Autosaver;
import fr.zetamap.morecommands.util.JsonSettings;
import fr.zetamap.morecommands.util.Logger;


public class ModuleFactory {
  protected static final Logger logger = new Logger();
  protected static final OrderedMap<Module, Mod> modules = new OrderedMap<>();
  protected static final ObjectMap<SaveableModule, JsonSettings> settings = new ObjectMap<>();
  protected static JsonSettings modulesSettings;
  protected static boolean initialized, disposed, error;
  
  public static ServerCommandHandler serverCommands;
  public static ClientCommandHandler clientCommands;
  /** Configuration file where to store disabled modules and general configuration. */
  public static Fi configFile = Core.settings.getDataDirectory().child("modules.json");
  /** Called when a module failed to initialize, load or save. */
  public static Cons2<Module, Throwable> errorHandler;

  /** 
   * @return a file named {@link Module#internalName()}+".json", in the mod's config directory.
   * @throws IllegalArgumentException if the module is not registered in the {@link ModuleFactory}.
   */
  public static Fi getFile(Module module) {
    Mod context = modules.get(module);
    if (context == null) throw new IllegalArgumentException("the module '"+module.internalName()+"' is not registed");
    return context.getConfigFolder().child(module.internalName() + ".json");
  }
  
  public static JsonSettings getSettings(SaveableModule module) {
    return getSettings(module, false);
  }
  /** @return the settings handler of the specified {@code module}. */
  public static JsonSettings getSettings(SaveableModule module, boolean backuped) {
    // Modules settings should be a table in a database, but i'm too lazy and sql drivers are too big.
    return settings.get(module, () -> {
      Fi file = getFile(module);
      return backuped ? new JsonSettings(file, file.parent().child("backup").child(file.name())) : 
             new JsonSettings(file);
    });
  }
  
  protected static JsonSettings mSettings() {
    if (modulesSettings == null) {
      modulesSettings = new JsonSettings(configFile);
      modulesSettings.load();
    }
    return modulesSettings;
  }
  
  /** @return whether {@link #init()} finished successfully. */
  public static boolean initialized() {
    return initialized;
  }
  
  public static boolean disposed() {
    return disposed;
  }
  
  public static boolean hasError() {
    return error;
  }
  
  public static void setError() {
    error = true;
  }
  
  /** 
   * Register a module in the factory. Registering should be done before calling {@link #init()}. <br>
   * Do nothing if the module is already registered.
   * @throws IllegalArgumentException if another module have the same name
   */
  public static void add(Mod context, Module module) {
    if (has(module)) return;
    if (has(module.internalName()))
      throw new IllegalArgumentException("Another module is named '"+module.internalName()+"'");
    modules.put(module, context);
    if (module instanceof Autosaver.Saveable)
      // Use high save priority to be sure the module is saved before his file
      Autosaver.add((Autosaver.Saveable)module, Autosaver.SavePriority.high); 
  }
  
  /** Remove a module from the factory. */
  public static void remove(Module module) {
    modules.remove(module);
    if (module instanceof Autosaver.Saveable)
      Autosaver.remove((Autosaver.Saveable)module);
  }
  
  /** Remove a module from the factory by his name. */
  public static boolean remove(String moduleName) {
    Module m = get(moduleName);
    if (m == null) return false;
    modules.remove(m);
    if (m instanceof Autosaver.Saveable)
      Autosaver.remove((Autosaver.Saveable)m);
    return true;
  }
  
  /** @return whether the factory already contains the {@code module}. */
  public static boolean has(Module module) {
    return modules.containsKey(module);
  }
  
  /** @return whether the factory already contains a module with the {@code moduleName}. */
  public static boolean has(String moduleName) {
    return get(moduleName) != null;
  }
  
  /** @return the number of registered modules. */
  public static int size() {
    return modules.size;
  }
  
  /** @return the number of enabled modules. */
  public static int count(Boolf<Module> predicate) {
    return modules.orderedKeys().count(predicate);
  }
  
  /** Iterate to all modules. */
  public static void each(Cons<Module> cons) {
    modules.orderedKeys().each(cons);
  }
  
  /** Iterate to all modules. */
  public static void each(Boolf<Module> predicate, Cons<Module> cons) {
    modules.orderedKeys().each(predicate, cons);
  }
   
  /** Find a module by his name. */
  @SuppressWarnings("unchecked")
  public static <T extends Module> T get(String moduleName) {
    return (T)modules.orderedKeys().find(m -> m.internalName().equals(moduleName));
  }
  
  /** @return whether the {@code module} is a {@link SaveableModule}. */
  public static boolean saveable(Module module) {
    return module instanceof SaveableModule;
  }
  
  /** @return whether the {@code module} is enabled. */
  public static boolean enabled(Module module) {
    return mSettings().getBool(module.internalName(), true);
  }
  
  /** 
   * Enable the {@code module}. <br>
   * The initialization and commands registration will happen the next time {@link #init()} is called.
    */
  public static void enable(Module module) {
    mSettings().put(module.internalName(), true);
  }

  /** 
   * Disable the {@code module}. <br>
   * The initialization and commands registration will not happen the next time {@link #init()} is called. 
   */
  public static void disable(Module module) {
    mSettings().put(module.internalName(), false);
  }
  
  /** 
   * Pre-initialization of the {@link ModuleFactory}. <br> 
   * Must be the first called method called before registering all the modules and calling
   * {@link #registerServerCommands(CommandHandler)}, {@link #registerClientCommands(CommandHandler)} and
   * {@link #init()}.
   */
  public static void preInit() {
    mSettings(); // Ensure modules settings are loaded
    // Add a listener to dispose modules
    Core.app.addListener(new ApplicationListener() {
      public void dispose() { 
        if (!initialized() || disposed()) return;
        ModuleFactory.save();
        ModuleFactory.dispose(); 
      }
    });
  }
  
  /** Initializes the registered modules ands loads the configuration of the {@link SaveableModule} ones. */
  public static boolean init() {
    if (initialized()) return true;
    disposed = false;

    boolean[] init = {true};
    each(ModuleFactory::enabled, m -> {
      try { m.init(); }
      catch (Exception e) { 
        logger.err("Failed to initialize @ (@) module" , e, m.name(), m.internalName()); 
        if (errorHandler != null) errorHandler.get(m, e);
        init[0] = false;
      }
    });
    return initialized = init[0] && reload(true);
  }
  
  /** Reloads the {@link SaveableModule}s configuration. */
  public static boolean reload() {
    return reload(false);
  }
  
  /** 
   * Reloads the {@link SaveableModule}s configuration. 
   * @param force if {@code true}, will ignores initialization and dispose checks
   */
  public static boolean reload(boolean force) {
    if (!force && (disposed() || !initialized())) return false;
    boolean[] reloaded = {true};
    each(m -> saveable(m) && enabled(m), m -> {
      try { ((SaveableModule)m).load(); }
      catch (Exception e) { 
        logger.err(initialized ? "Failed to reload @ (@) settings" : "Failed to load @ settings" , e, m.name(), 
                   m.internalName()); 
        if (errorHandler != null) errorHandler.get(m, e);
        reloaded[0] = false;
      }
    });
    return reloaded[0];
  }
  
  /** 
   * Reloads the {@link Module}'s configuration.
   * @return whether the {@code module} has been successfully reloaded.
   */
  public static boolean reload(Module module) {
    return reload(module, false);
  }
  
  /** 
   * Reloads the {@link Module}'s configuration.
   * @param force if {@code true}, will ignores enable, initialization and dispose checks
   * @return whether the {@code module} has been successfully reloaded.
   */
  public static boolean reload(Module module, boolean force) {
    if (!force && (disposed() || !initialized() || !enabled(module))) return false;
    if (!saveable(module)) return false;
    ((SaveableModule)module).load();
    return true;
  }
  
  /** Save the {@link SaveableModule}s configuration. */
  public static boolean save() {
    return save(false);
  }
  
  /** 
   * Save the {@link SaveableModule}s configuration. 
   * @param force if {@code true}, will ignores initialization and dispose checks
   * */
  public static boolean save(boolean force) {
    if (!force && (disposed() || !initialized())) return false;
    boolean[] saved = {true};
    each(ModuleFactory::saveable, m -> { // Also include disabled modules
      try { ((SaveableModule)m).save(); }
      catch (Exception e) { 
        logger.err("Failed to save @ (@) settings" , e, m.name(), m.internalName()); 
        if (errorHandler != null) errorHandler.get(m, e);
        saved[0] = false;
      }
    });
    return saved[0];
  }

  /** Registers the modules' server commands. */
  public static void registerServerCommands(CommandHandler handler) {
    if (disposed()) return;
    if (serverCommands == null) serverCommands = new ServerCommandHandler(handler);
    each(ModuleFactory::enabled, m -> m.registerServerCommands(serverCommands));
  }

  /** Registers the modules' client commands. */
  public static void registerClientCommands(CommandHandler handler) {
    if (disposed()) return;
    if (clientCommands == null) clientCommands = new ClientCommandHandler(handler);
    each(ModuleFactory::enabled, m -> m.registerClientCommands(clientCommands));
  }
  
  /** Disposes all modules and removes all created commands. */
  public static void dispose() {
    if (disposed()) return;
    disposed = true;
    initialized = false;
    each(Module::dispose);
    if (serverCommands != null) serverCommands.clear();
    if (clientCommands != null) clientCommands.clear();
  }
}
