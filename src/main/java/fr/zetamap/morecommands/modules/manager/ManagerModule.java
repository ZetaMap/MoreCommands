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

package fr.zetamap.morecommands.modules.manager;

import arc.struct.ObjectSet;
import arc.struct.Seq;

import fr.zetamap.morecommands.command.ServerCommandHandler;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.module.Module;
import fr.zetamap.morecommands.module.ModuleFactory;
import fr.zetamap.morecommands.util.Autosaver;
import fr.zetamap.morecommands.util.Strings;


/** MoreCommands manager module. */
public class ManagerModule extends AbstractModule {
  public final ObjectSet<String> protectedModules = ObjectSet.with("manager", "commands");
  
  public boolean enabled(Module module) {
    return ModuleFactory.enabled(module);
  }
  
  public void enable(Module module) {
    if (isProtected(module)) return;
    ModuleFactory.enable(module);
  }
  
  public void disable(Module module) {
    if (isProtected(module)) return;
    ModuleFactory.disable(module);
  }
  
  public boolean isProtected(Module module) {
    return protectedModules.contains(module.internalName());
  }
  
  @Override
  protected void initImpl() {
    //TODO: find a way to ensure that protected modules are not disabled
    protectedModules.each(n -> {
      Module m = ModuleFactory.get(n);
      if (m != null) ModuleFactory.enable(m);
    });
  }

  @Override
  public void registerServerCommands(ServerCommandHandler handler) {
    handler.add("morecommands", "[on|off|reload|save] [moduleName]", 
                "Manage the MoreCommands modules. Requires a server restart to apply the changes.", args -> {
      int action = 0; // 0: enabled, 1: disable, 2: reload
      if (args.length == 0) {
        logger.info("MoreCommands modules: [total: @, enabled: @, saveable: @]", ModuleFactory.size(), 
                    ModuleFactory.count(ModuleFactory::enabled), ModuleFactory.count(ModuleFactory::saveable));
        Seq<String> left = new Seq<>(ModuleFactory.size()), right = new Seq<>(ModuleFactory.size());
        ModuleFactory.each(m -> left.add("&lk|&fr &fb&lb" + m.internalName() + "&fr (&fb&lb" + m.name() + "&fr):"));
        ModuleFactory.each(m -> right.add((protectedModules.contains(m.internalName()) ? "&lyprotected&fr" : 
                                           ModuleFactory.enabled(m) ? "&lgenabled&fr" : "&lrdisabled&fr") +
                                          (ModuleFactory.saveable(m) ? ", &lbsaveable&fr" : "")));
        Strings.sJust(Strings.lJust(left, Strings.maxLength(left) + 1), right, 0).each(logger::info);
        return;
      } 
      else if (Strings.isTrue(args[0], false)) action = 0;
      else if (Strings.isFalse(args[0], false)) action = 1;
      else if (args[0].equals("reload")) action = 2;
      else if (args[0].equals("save")) {
        if (ModuleFactory.disposed()) logger.err("Cannot save: factory is disposed!");
        else if (!ModuleFactory.initialized()) logger.err("Cannot save: factory is not initialized!");
        else if (ModuleFactory.save()) {
          logger.info("MoreCommands modules saved.");
          Autosaver.save();
        }
        // error already printed if error
        return;
        
      } else {
        logger.err("Invalid argument! Must be 'on', 'off', 'reload' or 'save'.");
        return;
      }
      
      if (args.length == 1) {
        if (action < 2) logger.err("Missing 'moduleName' argument.");
        else if (ModuleFactory.disposed()) logger.err("Cannot reload: factory is disposed!");
        else if (!ModuleFactory.initialized()) logger.err("Cannot reload: factory is not initialized!");
        else if (ModuleFactory.reload()) logger.info("MoreCommands modules reloaded.");
        // error already printed if error
        return;
      } 
      
      Module module = ModuleFactory.get(args[1]);
      if (module == null) {
        logger.err("No module named '@' found.", args[1]);
      } else if (action == 0) {
        if (ModuleFactory.enabled(module)) logger.err("Module already enabled.");
        else {
          ModuleFactory.enable(module);
          logger.info("Module enabled.");
          logger.warn("A server restart is required to apply the change.");
        }
      } else if (action == 1) {
        if (isProtected(module)) logger.err("This module is protected, you cannot disable it.");
        else if (!ModuleFactory.enabled(module)) logger.err("Module already disabled.");
        else {
          ModuleFactory.disable(module);
          logger.info("Module disabled.");
          logger.warn("A server restart is required to apply the change.");
        }
      } 
      else if (!ModuleFactory.saveable(module)) logger.warn("This module has no settings support.");
      else if (!ModuleFactory.enabled(module)) logger.warn("The module is disabled. Please enable it before reloading.");
      else if (ModuleFactory.disposed()) logger.err("Cannot reload module @: factory is disposed!", module.internalName());
      else if (!ModuleFactory.initialized())
        logger.err("Cannot reload module @: factory is not initialized!", module.internalName());
      else {
        try { 
          ModuleFactory.reload(module);
          logger.info("Module settings reloaded.");
        } catch (Exception e) { logger.err("Failed to reload @ settings" , e, module.name()); }
      }
    });
  }
}
