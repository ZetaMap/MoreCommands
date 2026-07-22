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

import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.CommandHandler.*;

import mindustry.gen.Player;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.util.Logger;


public class ClientCommandHandler {
  private static final Logger logger = new Logger("Client Commands");
  public final CommandHandler handler;
  public final Seq<Command> all = new Seq<>(), admin = new Seq<>();
  /** 
   * Non-exhaustive list of potential admin commands from mindustry server or some other plugins. <br>
   * More Commands admin commands will be added to this list.
   */
  public final ObjectSet<String> defaultAdminCommands = ObjectSet.with(
      "a", "js", "vanish", "killall", "pause", "rollback", "saves", "restart", "blacklist", "whitelist"
  );
  
  public ClientCommandHandler(CommandHandler handler) {
    this.handler = handler;
  }

  public void add(String name, String desc, CommandRunner<PlayerData> runner) {
    add(name, "", desc, runner);
  }
  
  public void add(String name, String params, String desc, CommandRunner<PlayerData> runner) {
    // If the command already exist, try to place the new at the same position
    int index = handler.getCommandList().indexOf(c -> c.text.equals(name));
    all.add(handler.<Player>register(name, params, desc, (args, player) -> {
      PlayerData p = getcheck(player);
      if (p == null) return;
      
      try { runner.accept(args, p); }
      catch (Exception e) { 
        logger.err("Error while running command '@' for player '@'", e, name, player.uuid()); 
        Players.err(player, "Error while running the command. Please report this error.");
      }
    }));
    if (index != -1) handler.getCommandList().insert(index, handler.getCommandList().pop());
  }
  
  public void addAdmin(String name, String desc, CommandRunner<PlayerData> runner) {
    addAdmin(name, "", desc, runner);
  }
  
  public void addAdmin(String name, String params, String desc, CommandRunner<PlayerData> runner) {
    // If the command already exist, try to place the new at the same position
    int index = handler.getCommandList().indexOf(c -> c.text.equals(name));
    defaultAdminCommands.add(admin.add(all.add(handler.<Player>register(name, params, desc, (args, player) -> {
      if (!player.admin) {
        Players.errCommandUseDenied(player);
        return;
      } 
      
      PlayerData p = getcheck(player);
      if (p == null) return;
      
      try { runner.accept(args, p); }
      catch (Exception e) { 
        logger.err("Error while running command '@' for admin player '@'", e, name, player.uuid());
        Players.err(player, "Error while running the command: @", e.toString());
      }
    })).peek()).peek().text);
    if (index != -1) handler.getCommandList().insert(index, handler.getCommandList().pop());
  }
  
  private PlayerData getcheck(Player player) {
    PlayerData p = PlayerData.get(player);
    // Should never happen
    if (p == null) {
      logger.err("FATAL: Player '@' is not in PlayerData! Please report this error at: @.", player.uuid(),
                 "https://github.com/ZetaMap/MoreCommands/issues/new");
      Players.err(p, "FATAL: Operation not permitted! Please report this error.");
    }
    return p;
  }

  public boolean isAdmin(String name) {
    return defaultAdminCommands.contains(name);
  }

  public Command get(String name) {
    return handler.getCommandList().find(c -> c.text.equals(name));
  }
  
  public void remove(String name) {
    handler.removeCommand(name);
    all.remove(c -> c.text.equals(name));
    admin.remove(c -> c.text.equals(name));
  }
  
  public void clear() {
    all.each(c -> handler.removeCommand(c.text));
    all.clear();
    admin.clear();
  }
}
