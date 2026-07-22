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

import arc.util.Disposable;

import fr.zetamap.morecommands.command.*;


public interface Module extends Disposable {
  /** Initialize the module. */
  void init();

  /** @return whether the module is initialized or not. */
  boolean initialized();
  
  /** The module name, used for logging. Default is capitalized class name without the 'Module' part. */
  String name();
  
  /** The module {@link #name()}, without spaces and in lower case. */
  String internalName();

  /** Register any commands to be used on the server side, e.g. from the console. */
  void registerServerCommands(ServerCommandHandler handler);
  
  /** Register any commands to be used on the client side, e.g. sent from an in-game player. */
  void registerClientCommands(ClientCommandHandler handler);
  
  /** {@inheritDoc} */
  void dispose();
}
