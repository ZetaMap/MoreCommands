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

package fr.zetamap.morecommands.modules.help;

import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.CommandHandler.Command;

import fr.zetamap.morecommands.command.*;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.util.Strings;


public class HelpModule extends AbstractModule {
  @Override
  public void registerServerCommands(ServerCommandHandler handler) {
    
  }
  
  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    handler.add("help", "[command|page|selectors]", "Lists all commands.", (args, player) -> {
      String command;
      int perPage = 8, page = 1, pages;
      
      if (args.length == 1) {
        if (args[0].equals("selectors")) {
          mindustry.gen.Call.openURI(player.player.con,
            "https://github.com/ZetaMap/MoreCommands/blob/main/README.md#selectors");
          return;
        }
        
        page = Strings.parseInt(args[0]);
        
        if (page == Integer.MIN_VALUE) command = args[0];
        else command = null;
      } else command = null;
      
      if (command != null) {
        Command c = handler.handler.getCommandList().find(cc -> cc.text.equals(command));
        
        // Act like the command is not found when the player is not an administrator
        if (c == null || (!player.admin() && handler.isAdmin(c.text))) 
             Players.err(player, "No command named '[orange]@[]' found.", command);
        else Players.info(player, "[orange]@@[white] @ [lightgray]- @", handler.handler.prefix, c.text, c.paramText, 
                          c.description);
        return;
      }
      
      Seq<Command> commands = handler.handler.getCommandList();
      if (!player.admin()) commands = commands.select(c -> !handler.isAdmin(c.text));
      pages = Mathf.ceil((float)commands.size / perPage);
      
      if (page < 1 || page > pages) {
        Players.err(player, "'[orange]page[]' must be a number between [orange]1[] and [orange]@[].", pages);
        return;
      }
      
      Players.info(player, "[orange]-- Commands page [lightgray]@[gray]/[]@ [gray]([]@[gray])[][] --", page, pages,
                   commands.size);
      StringBuilder builder = new StringBuilder();
      for(int i=perPage*(page-1); i<Math.min(perPage*page, commands.size); i++) {
        Command c = commands.get(i);
        if (player.admin())
          builder.append("[lightgray][[").append(handler.isAdmin(c.text) ? "[scarlet]A[]" : "[blue]P[]").append("][]");
        builder.append("  [orange]/").append(c.text).append(" [white]").append(c.paramText).append(" [lightgray]- ")
               .append(c.description).append("\n");
      }
      Players.info(player, builder.toString());
    });
  }
}
