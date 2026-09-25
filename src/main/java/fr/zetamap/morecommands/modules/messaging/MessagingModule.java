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

package fr.zetamap.morecommands.modules.messaging;

import arc.util.Log.LogLevel;

import mindustry.Vars;
import mindustry.gen.Call;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.*;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.util.Strings;


public class MessagingModule extends AbstractModule {
  public static final String[] tags = {"[green]", "[cyan]", "[white]", "[orange]", "[scarlet]", ""};
  protected static final Object[] empty = {};
  public boolean chatEnabled;

  public void serverMessage(LogLevel level, String topic, String message) { serverMessage(level, topic, message, empty); }
  public void serverMessage(LogLevel level, String topic, String message, Object... args) {
    if (level == LogLevel.none) return;
    String tag = tags[level == null ? 0 : level.ordinal()+1];
    if (args.length > 0) {
      String color = level == LogLevel.err ? "[orange]" : "[accent]";
      message = Strings.format(message.replace("@", color + '@' + tag), args);
    }
    topic = (topic == null ? "" : "[scarlet][[" + topic + "]: ") + tag;
    Call.sendMessage(topic + message.replace("\n", "\n" + topic));

    /*
    int i = 0, nl = message.indexOf('\n');
    while (nl >= 0) {
      Call.sendMessage(topic + message.substring(i, nl));
      i = nl + 1;
      nl = message.indexOf('\n', i);
    }
    Call.sendMessage(topic + (i == 0 ? message : message.substring(i)));
    */
  }
  public void serverOk(String topic, String message) { serverMessage(null, topic, message, empty); }
  public void serverOk(String topic, String message, Object... args) { serverMessage(null, topic, message, args); }
  public void serverDebug(String topic, String message) { serverMessage(LogLevel.debug, topic, message, empty); }
  public void serverDebug(String topic, String message, Object... args) { serverMessage(LogLevel.debug, topic, message, args); }
  public void serverInfo(String topic, String message) { serverMessage(LogLevel.info, topic, message, empty); }
  public void serverInfo(String topic, String message, Object... args) { serverMessage(LogLevel.info, topic, message, args); }
  public void serverWarn(String topic, String message) { serverMessage(LogLevel.warn, topic, message, empty); }
  public void serverWarn(String topic, String message, Object... args) { serverMessage(LogLevel.warn, topic, message, args); }
  public void serverErr(String topic, String message) { serverMessage(LogLevel.err, topic, message, empty); }
  public void serverErr(String topic, String message, Object... args) { serverMessage(LogLevel.err, topic, message, args); }

  public void playerMessage(PlayerData player, LogLevel level, String text) { playerMessage(player, level, text, empty); }
  public void playerMessage(PlayerData player, LogLevel level, String text, Object... args) {
    if (level == LogLevel.none) return;
    String tag = tags[level == null ? 0 : level.ordinal()+1];
    if (args.length > 0) {
      String color = level == LogLevel.err ? "[orange]" : "[accent]";
      text = Strings.format(text.replace("@", color + '@' + tag), args);
    }
    Players.info(player, tag + text);
  }
  public void playerOk(PlayerData player, String text) { playerMessage(player, null, text, empty); }
  public void playerOk(PlayerData player, String text, Object... args) { playerMessage(player, null, text, args); }
  public void playerDebug(PlayerData player, String text) { playerMessage(player, LogLevel.debug, text, empty); }
  public void playerDebug(PlayerData player, String text, Object... args) { playerMessage(player, LogLevel.debug, text, args); }
  public void playerInfo(PlayerData player, String text) { playerMessage(player, LogLevel.info, text, empty); }
  public void playerInfo(PlayerData player, String text, Object... args) { playerMessage(player, LogLevel.info, text, args); }
  public void playerWarn(PlayerData player, String text) { playerMessage(player, LogLevel.warn, text, empty); }
  public void playerWarn(PlayerData player, String text, Object... args) { playerMessage(player, LogLevel.warn, text, args); }
  public void playerErr(PlayerData player, String text) { playerMessage(player, LogLevel.err, text, empty); }
  public void playerErr(PlayerData player, String text, Object... args) { playerMessage(player, LogLevel.err, text, args); }

  public void sendWhisper(PlayerData src, PlayerData dest, String msg) {
    Players.info(dest, "[coral][[@[coral]] [gold]--> me[]: [white]@", src.getName(), msg);
    Players.info(src, "[gold]me -->[] [coral][[@[coral]]: [white]@", dest.getName(), msg);
  }

  @Override
  protected void initImpl() {
    chatEnabled = true;

    // Disabled chat or muted peoples filter
    Vars.netServer.admins.addChatFilter((p, m) -> {
      PlayerData player = PlayerData.get(p);
      if (player == null || chatEnabled || player.admin()) return m;
      player.err("The chat is disabled, you can't speak!");
      return null;
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

      logger.info("Chat @.", chatEnabled ? "Enabled" : "Disabled");
      if (old != chatEnabled)
        serverWarn("Chat", "@ by the console.", chatEnabled ? "enabled" : "disabled");
    });
  }

  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    handler.add("w", "<player> <message...>", "Whisper to a player.", (args, player) -> {
      Players.SearchResult target = Players.find(args);

      if (!target.found) {
        player.errPlayerNotFound();
        return;
      } else if (target.player == player) {
        player.err("You cannot whisper to yourself.");
        return;
      }

      String message = String.join(" ", target.rest);
      // Allow whispering only to an admin if muted, chat is disabled, or something else
      if (!target.player.admin()) message = Vars.netServer.admins.filterMessage(player.player, message);
      if (message == null) return;

      if (Strings.stripColors(message).isBlank()) {
        player.err("The message is empty.");
        return;
      }

      player.setWhisper(target.player);
      sendWhisper(player, target.player, message);
    });

    handler.add("r", "<message...>", "Reply to the last whispered message.", (args, player) -> {
      if (!player.hasWhispered()) {
        player.err("No received whisper.");
        return;
      } else if (player.whisperTarget == null) {
        player.err("The player has disconnected.");
        return;
      } else if (Strings.stripColors(args[0]).isBlank()) {
        player.err("The message is empty.");
        return;
      }

      sendWhisper(player, player.whisperTarget, args[0]);
    });

    handler.addAdmin("chat", "[on|off]", "Toggle the chat.", (args, player) -> {
      boolean old = chatEnabled;

      if (args.length == 0) {
        player.info("The chat is currently @.", chatEnabled ? "enabled" : "disabled");
        return;
      } else if (Strings.isTrue(args[0])) chatEnabled = true;
      else if (Strings.isFalse(args[0])) chatEnabled = false;
      else {
        player.err("Invalid argument!");
        return;
      }

      logger.info("Chat @ by '@' (@).", chatEnabled ? "enabled" : "disabled", player.stripedName, player);
      if (old != chatEnabled) serverWarn("Chat", "@ by @.", chatEnabled ? "Enabled" : "Disabled", player.getName());
    });
  }
}
