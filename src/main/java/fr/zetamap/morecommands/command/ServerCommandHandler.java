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

package fr.zetamap.morecommands.command;

import arc.func.Cons;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.CommandHandler.*;

import fr.zetamap.morecommands.util.Logger;


public class ServerCommandHandler {
  private static final Logger logger = new Logger("Server Commands");

  public final CommandHandler handler;
  public final Seq<Command> all = new Seq<>();
  
  public ServerCommandHandler(CommandHandler handler) {
    this.handler = handler;
  }
  
  public void add(String name, String desc, Cons<String[]> runner) {
    add(name, "", desc, runner);
  }
  
  public void add(String name, String params, String desc, Cons<String[]> runner) {
    // If the command already exist, try to place the new at the same position
    int index = handler.getCommandList().indexOf(c -> c.text.equals(name));
    all.add(handler.register(name, params, desc, args -> {
      try { runner.get(args); }
      catch (Exception e) { logger.err("Error while running server command '@'", e, name); }
    }));
    if (index != -1) handler.getCommandList().insert(index, handler.getCommandList().pop());
  }
  
  public Command get(String name) {
    return handler.getCommandList().find(c -> c.text.equals(name));
  }
  
  public void remove(String name) {
    handler.removeCommand(name);
    all.remove(c -> c.text.equals(name));
  }
  
  public void clear() {
    all.each(c -> handler.removeCommand(c.text));
    all.clear();
  }
}
