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

package fr.zetamap.morecommands.modules.messaging;

import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.net.Administration;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.MessageBlock;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.*;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.util.Strings;


public class MessagingModule extends AbstractModule {
  public boolean chatEnabled;
  
  @Override
  protected void initImpl() {
    chatEnabled = true;
    
    // Disabled chat or muted peoples filter
    Vars.netServer.admins.addChatFilter((p, m) -> {
      PlayerData player = PlayerData.get(p);
      if (player == null || chatEnabled || player.admin()) return m;
      Players.err(p, "The chat is disabled, you can't speak!");
      return null;
    });
    
    // Also block messaging via logic and message blocks
    Vars.netServer.admins.addActionFilter(a -> {
      if (chatEnabled || a.player.admin) return true;
      if ((a.type == Administration.ActionType.configure && a.config instanceof String) ||
          (a.type == Administration.ActionType.placeBlock &&
           (a.block instanceof MessageBlock || a.block instanceof LogicBlock))) {
        Players.err(a.player, "The chat is disabled, you can't speak, even with logic blocks!");
        return false;
      }
      return true;
    });
    
    // Tag, rainbow and vanish message formatter
    Vars.netServer.chatFormatter = (p, m) -> {
      PlayerData player = PlayerData.get(p);
      return p == null ? m :
             player == null ? "[coral][[" + p.coloredName() + "[coral]]:[white] " + m : //in case of
             player.vanished() ? "[coral][[]:[white] " + m :
             player.tag != null ? player.tag + " [coral][[" + player.getName() + "[coral]]:[white] " + m :
             "[coral][[" + player.getName() + "[coral]]:[white] " + m;
    };
  }
  
  
  @Override
  public void registerServerCommands(ServerCommandHandler handler) {
    handler.add("chat", "[on|off]", "Toggle the in-game chat.", args -> {
      boolean old = chatEnabled;
      
      if (args.length == 0) {
        logger.info("The chat is currently @.", chatEnabled ? "enabled" : "disabled");
        return;
      } else if (Strings.isTrue(args[0])) chatEnabled = true;
      else if (Strings.isFalse(args[0])) chatEnabled = false;
      else {
        logger.err("Invalid argument!");
        return;
      }
      
      logger.info("Chat @.", chatEnabled ? "enabled" : "disabled");
      if (old != chatEnabled) 
        Call.sendMessage("\n[gold]-------------------- \n"
                       + "[scarlet]/!\\[][orange] The chat has been " + (chatEnabled ? "enabled" : "disabled") + ".\n"
                       + "[]--------------------\n");
    });
  }
  
  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    handler.add("w", "<player> <message...>", "Whisper to a player.", (args, player) -> {
      Players.SearchResult target = Players.find(args);

      if (!target.found) {
        Players.errPlayerNotFound(player);
        return;
      } else if (target.player == player) {
        Players.err(player, "You cannot whisper to yourself.");
        return;
      }
      
      String message = String.join(" ", target.rest);
      // Allow whispering only to an admin if muted, chat is disabled, or something else
      if (!target.player.admin()) message = Vars.netServer.admins.filterMessage(player.player, message);
      if (message == null) return;
      
      if (Strings.stripColors(message).isBlank()) {
        Players.err(player, "The message is empty.");
        return;
      }
      
      player.setWhisper(target.player);
      Players.info(target.player, "[coral][[@[coral]] [gold]--> me[]: [white]@", player.getName(), message);
      Players.info(player, "[gold]me -->[] [coral][[@[coral]]: [white]@", target.player.getName(), message);
    });

    handler.add("r", "<message...>", "Reply to the last whispered message.", (args, player) -> {
      if (!player.hasWhispered()) {
        Players.err(player, "No received whisper.");
        return;
      } else if (player.whisperTarget == null) {
        Players.err(player, "The player has disconnected.");
        return;
      } else if (Strings.stripColors(args[0]).isBlank()) {
        Players.err(player, "The message is empty.");
        return;
      }
      
      Players.info(player.whisperTarget, "[coral][[@[coral]] [gold]--> me[]: [white]@", player.getName(), args[0]);
      Players.info(player, "[gold]me -->[] [coral][[@[coral]]: [white]@", player.whisperTarget.getName(), args[0]);
    });

    handler.addAdmin("chat", "[on|off]", "Toggle the chat.", (args, player) -> {
      boolean old = chatEnabled;
      
      if (args.length == 0) {
        Players.info(player, "The chat is currently @.", chatEnabled ? "enabled" : "disabled");
        return;
      } else if (Strings.isTrue(args[0])) chatEnabled = true;
      else if (Strings.isFalse(args[0])) chatEnabled = false;
      else {
        Players.err(player, "Invalid argument!");
        return;
      }
      
      logger.info("Chat @ by '@' (@).", chatEnabled ? "enabled" : "disabled", player.stripedName, player.player.uuid());
      if (old != chatEnabled) {
        Call.sendMessage("\n[gold]-------------------- \n"
                       + "[scarlet]/!\\[][orange] The chat has been " + (chatEnabled ? "enabled" : "disabled") + " by "
                       + player.getName() + "[orange].\n"
                       + "[gold]--------------------\n");
      }
    });
  }
}
