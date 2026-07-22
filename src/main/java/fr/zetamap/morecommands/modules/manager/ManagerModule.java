/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2025-2026  ZetaMap
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
import fr.zetamap.morecommands.module.ModuleRegistry;
import fr.zetamap.morecommands.modules.command.CommandsModule;
import fr.zetamap.morecommands.util.Autosaver;
import fr.zetamap.morecommands.util.Strings;


/** MoreCommands manager module. */
public class ManagerModule extends AbstractModule {
  public final ObjectSet<String> protectedModules = ObjectSet.with(internalName(), CommandsModule.INTERNAL_NAME);

  public boolean enabled(Module module) {
    return ModuleRegistry.enabled(module);
  }

  public void enable(Module module) {
    if (isProtected(module)) return;
    ModuleRegistry.enable(module);
  }

  public void disable(Module module) {
    if (isProtected(module)) return;
    ModuleRegistry.disable(module);
  }

  public boolean isProtected(Module module) {
    return protectedModules.contains(module.internalName());
  }

  @Override
  protected void initImpl() {
    //TODO: find a way to ensure that protected modules are not disabled before loading this one.
    //      As it can be force disabled by editing the config
    protectedModules.each(n -> {
      Module m = ModuleRegistry.get(n);
      if (m != null) ModuleRegistry.enable(m);
    });
  }

  @Override
  public void registerServerCommands(ServerCommandHandler handler) {
    handler.add("morecommands", "[on|off|reload|save] [moduleName]",
                "Manage the MoreCommands modules. Requires a server restart to apply the changes.", args -> {
      int action = 0; // 0: enabled, 1: disable, 2: reload
      if (args.length == 0) {
        logger.info("MoreCommands modules: [total: @, enabled: @, saveable: @]", ModuleRegistry.size(),
                    ModuleRegistry.count(ModuleRegistry::enabled), ModuleRegistry.count(ModuleRegistry::saveable));
        Seq<String> left = new Seq<>(ModuleRegistry.size()), right = new Seq<>(ModuleRegistry.size());
        ModuleRegistry.each(m -> left.add("&lk|&fr &fb&lb" + m.internalName() + "&fr (&fb&lb" + m.name() + "&fr):"));
        ModuleRegistry.each(m -> right.add((isProtected(m) ? "&lyprotected&fr" :
                                           ModuleRegistry.enabled(m) ? "&lgenabled&fr" : "&lrdisabled&fr") +
                                          (ModuleRegistry.saveable(m) ? ", &lbsaveable&fr" : "")));
        Strings.sJust(Strings.lJust(left, Strings.maxLength(left) + 1), right, 0).each(logger::info);
        return;
      }
      else if (Strings.isTrue(args[0], false)) action = 0;
      else if (Strings.isFalse(args[0], false)) action = 1;
      else if (args[0].equals("reload")) action = 2;
      else if (args[0].equals("save")) {
        if (ModuleRegistry.disposed()) logger.err("Cannot save: registry is disposed!");
        else if (!ModuleRegistry.initialized()) logger.err("Cannot save: registry is not initialized!");
        else if (ModuleRegistry.forceSave()) {
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
        else if (ModuleRegistry.disposed()) logger.err("Cannot reload: registry is disposed!");
        else if (!ModuleRegistry.initialized()) logger.err("Cannot reload: registry is not initialized!");
        else if (ModuleRegistry.reload()) logger.info("MoreCommands modules reloaded.");
        // error already printed if error
        return;
      }

      Module module = ModuleRegistry.get(args[1]);
      if (module == null) {
        logger.err("No module named '@' found.", args[1]);
      } else if (action == 0) {
        if (ModuleRegistry.enabled(module)) logger.err("Module already enabled.");
        else {
          ModuleRegistry.enable(module);
          logger.info("Module enabled.");
          logger.warn("A server restart is required to apply the change.");
        }
      } else if (action == 1) {
        if (isProtected(module)) logger.err("This module is protected, you cannot disable it.");
        else if (!ModuleRegistry.enabled(module)) logger.err("Module already disabled.");
        else {
          ModuleRegistry.disable(module);
          logger.info("Module disabled.");
          logger.warn("A server restart is required to apply the change.");
        }
      }
      else if (!ModuleRegistry.saveable(module))
        logger.warn("This module has no settings support.");
      else if (!ModuleRegistry.enabled(module))
        logger.warn("The module is disabled. Please enable it before reloading.");
      else if (ModuleRegistry.disposed())
        logger.err("Cannot reload module @: registry is disposed!", module.internalName());
      else if (!ModuleRegistry.initialized())
        logger.err("Cannot reload module @: registry is not initialized!", module.internalName());
      else if (ModuleRegistry.reload(module))
        logger.info("Module settings reloaded.");
      else ;// Error will be logged by the ModuleRegistry
    });
  }
}
