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

import mindustry.mod.Mod;

import fr.zetamap.morecommands.command.*;
import fr.zetamap.morecommands.util.Logger;
import fr.zetamap.morecommands.util.Strings;


public abstract class AbstractModule implements Module {
  protected final Logger logger;
  private final String name, internalName;
  private boolean disposed, initialized;

  {
    String n = getClass().getSimpleName();
    n = n.endsWith("Module") ? n.substring(0, n.length()-6) : n;
    name = Strings.insertSpaces(n);
    internalName = Strings.camelToKebab(n);
    
    logger = new Logger(name());
  }
  
  @Override
  public String name() {
    return name;
  }
  
  @Override
  public String internalName() {
    return internalName;
  }
  
  /** Register this module to the {@link ModuleFactory}. Do nothing if the module is already registered. */
  public/* final*/ void register(Mod context) {
    ModuleFactory.add(context, this);
  }
  
  @Override
  public final void init() {
    if (initialized()) return;
    initImpl();
    initialized = true;
  }

  protected void initImpl() {}
  
  @Override
  public boolean initialized() {
    return initialized;
  }
  
  @Override
  public void registerServerCommands(ServerCommandHandler handler) {}

  @Override
  public void registerClientCommands(ClientCommandHandler handler) {}

  @Override
  public final void dispose() {
    if (isDisposed()) return;
    disposeImpl();
    disposed = true;
  }
  
  protected void disposeImpl() {}
  
  @Override
  public boolean isDisposed() {
    return disposed;
  }
}
