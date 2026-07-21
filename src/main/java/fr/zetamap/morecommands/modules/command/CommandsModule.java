/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2021-2026  ZetaMap
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

package fr.zetamap.morecommands.modules.command;

import arc.Core;
import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.CommandHandler.Command;
import arc.util.Reflect;

import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.server.ServerControl;

import fr.zetamap.morecommands.command.ServerCommandHandler;
import fr.zetamap.morecommands.module.AbstractSaveableModule;
import fr.zetamap.morecommands.module.ModuleRegistry;
import fr.zetamap.morecommands.util.JsonSettings;
import fr.zetamap.morecommands.util.Strings;


public class CommandsModule extends AbstractSaveableModule {
  /** Used by {@link ManagerModule} as it is initialized before this one. */
  public static final String INTERNAL_NAME = "commands", NAME = "Commands";

  private final ObjectSet<String> clientDisabled = new ObjectSet<>(), serverDisabled = new ObjectSet<>();
  private Seq<Command> clientCommands, serverCommands;
  private ObjectMap<String, Command> clientHandlerCommands, serverHandlerCommands;
  private Seq<Command> clientHandlerOrderedCommands, serverHandlerOrderedCommands;

  public final ObjectSet<String> protectedServerCommands = ObjectSet.with("commands", "morecommands"),
                                 protectedClientCommands = ObjectSet.with();
  public CommandHandler clientHandler, serverHandler;

  protected void addCommand(Seq<Command> commands, ObjectMap<String, Command> handlerCommands,
                            Seq<Command> handlerOrderedCommands, String command) {
    if (handlerCommands.containsKey(command)) return;
    Command c = commands.find(cc -> cc.text.equals(command));
    if (c == null) return;
    handlerOrderedCommands.add(c);
    handlerCommands.put(command, c);
  }

  protected void removeCommand(CommandHandler handler, String command) {
    handler.removeCommand(command);
  }

  public void reset() {
    clientDisabled.clear();
    serverDisabled.clear();
    setModified();
  }

  public void enableClientCommand(String command) {
    clientDisabled.remove(command);
    setModified();
  }

  public void enableClientCommandNow(String command) {
    enableClientCommand(command);
    addCommand(clientCommands, clientHandlerCommands, clientHandlerOrderedCommands, command);
  }

  public void disableClientCommand(String command) {
    clientDisabled.add(command);
    setModified();
  }

  public void disableClientCommandNow(String command) {
    disableClientCommand(command);
    if (!protectedClientCommands.contains(command)) removeCommand(clientHandler, command);
  }

  public boolean isClientCommandDisabled(String command) {
    return clientDisabled.contains(command);
  }

  public boolean isClientAdminCommand(String command) {
    return ModuleRegistry.clientCommands.isAdmin(command);
  }

  public void enableServerCommand(String command) {
    serverDisabled.remove(command);
    setModified();
  }

  /** Try to re-enable the commands */
  public void enableServerCommandNow(String command) {
    enableServerCommand(command);
    addCommand(serverCommands, serverHandlerCommands, serverHandlerOrderedCommands, command);
  }

  public void disableServerCommand(String command) {
    if (protectedServerCommands.contains(command)) return;
    serverDisabled.add(command);
    setModified();
  }

  public void disableServerCommandNow(String command) {
    disableServerCommand(command);
    if (!protectedServerCommands.contains(command)) removeCommand(serverHandler, command);
  }

  public boolean isServerCommandDisabled(String command) {
    return serverDisabled.contains(command);
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String internalName() {
    return INTERNAL_NAME;
  }

  @Override
  protected void initImpl() {
    clientHandler = Vars.netServer.clientCommands;
    serverHandler = ServerControl.instance.handler;

    // Use reflection to get access to internal commands lists
    // This is needed to remove/add commands on-the-fly.
    clientHandlerCommands = Reflect.get(clientHandler, "commands");
    clientHandlerOrderedCommands = Reflect.get(clientHandler, "orderedCommands");
    serverHandlerCommands = Reflect.get(serverHandler, "commands");
    serverHandlerOrderedCommands = Reflect.get(serverHandler, "orderedCommands");

    // Delay the initialization to be sure all commands have been registered.
    // This assumes that no more commands will be added or removed during runtime.
    Events.on(EventType.ServerLoadEvent.class, e -> Core.app.post(() -> {
      if (isDisposed()) return;

      clientCommands = clientHandler.getCommandList().copy();
      serverCommands = serverHandler.getCommandList().copy();

      // Remove all the disabled commands.
      clientDisabled.each(clientHandler::removeCommand);
      serverDisabled.each(serverHandler::removeCommand);
    }));
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void loadImpl(JsonSettings settings) {
    clientDisabled.clear();
    clientDisabled.addAll(settings.getOrPut("disabled-client", Seq.class, String.class, Seq::new));
    serverDisabled.clear();
    serverDisabled.addAll(settings.getOrPut("disabled-server", Seq.class, String.class, Seq::new));

    // Make sure the protected commands are not disabled
    clientDisabled.removeAll(protectedClientCommands.toSeq());
    serverDisabled.removeAll(protectedServerCommands.toSeq());

    // Internal; used to help making the command list
    // This removes all commands not created by MoreCommands
    settings.defaults("demomode", false); // in case of
    if (settings.getBool("demomode")) {
      logger.warn("Demo mode has been enabled!");
      Core.app.post(() -> {
        Vars.netServer.clientCommands.getCommandList().copy().each(
          c -> !ModuleRegistry.clientCommands.all.contains(c),
          c -> ModuleRegistry.clientCommands.remove(c.text)
        );
        ServerControl.instance.handler.getCommandList().copy().each(
          c -> !ModuleRegistry.serverCommands.all.contains(c),
          c -> ModuleRegistry.serverCommands.remove(c.text)
        );
      });
    }
  }

  @Override
  protected void saveImpl(JsonSettings settings) {
    settings.put("disabled-client", String.class, clientDisabled.toSeq());
    settings.put("disabled-server", String.class, serverDisabled.toSeq());
  }

  @Override
  public void registerServerCommands(ServerCommandHandler handler) {
    handler.add("commands", "[list|reset|on|off] [command] [now]", "Toggle client/server commands.", args -> {
      if (clientCommands == null || clientCommands == null) {
        logger.err("FATAL: Module not properly initialized.");
        return;

      } else if (args.length == 0) {
        int clientEnabled = clientCommands.count(c -> !isClientCommandDisabled(c.text)),
            serverEnabled = serverCommands.count(c -> !isServerCommandDisabled(c.text)),
            clientLength = clientHandler.prefix.length() + Strings.max(clientCommands, c -> c.text.length()) + 5,
            serverLength = serverHandler.prefix.length() + Strings.max(serverCommands, c -> c.text.length()) + 5;
        Seq<String> left = new Seq<>(serverCommands.size+1), right = new Seq<>(clientCommands.size+1);

         left.add("Server commands: [total: &fb&lb" + serverCommands.size + "&fr, enabled: &fb&lb" + serverEnabled +"&fr]");
        right.add("Client commands: [total: &fb&lb" + clientCommands.size + "&fr, enabled: &fb&lb" + clientEnabled +"&fr]");

        serverCommands.each(c ->
           left.add("&lk|&fr &lb" + serverHandler.prefix + Strings.lJust(c.text + "&fr: ", serverLength) +
                    (protectedServerCommands.contains(c.text) ? "&lyprotected&fr" :
                     isServerCommandDisabled(c.text) ? "&lrdisabled&fr" : "&lgenabled&fr")));
        clientCommands.each(c ->
          right.add("&lk|&fr &lb" + clientHandler.prefix + Strings.lJust(c.text + "&fr: ", clientLength) +
                    (protectedClientCommands.contains(c.text) ? "&lyprotected&fr" :
                     isClientCommandDisabled(c.text) ? "&lrdisabled&fr" : "&lgenabled&fr")+
                    (isClientAdminCommand(c.text) ? ", &lradmin&fr" : "")));

        // Assumes the command list is never empty
        logger.info("Command lists:");
        Strings_columnify(2, left, right).each(l -> logger.info("&lk|&fr " + l));
        return;

      } else if (args[0].equals("reset")) {
        reset();
        logger.info("All commands have been reactivated.");
        logger.warn("A server restart is required to apply the changes.");
        return;

      } else if (args[0].equals("list")) {
        Seq<Command> players = ModuleRegistry.clientCommands.all.select(c -> !ModuleRegistry.clientCommands.admin.contains(c));
        if (players.any()) {
          logger.info("Player commands: [total: @]", players.size);
          players.each(c ->
            logger.info(Strings.format("&lk|&fr &b&lb@@@@&fr - &lw@&fr", clientHandler.prefix, c.text,
                                       c.paramText.isEmpty() ? "" : " &lc&fi", c.paramText,
                                       Strings.normalize(c.description))));
        } else logger.info("Player commands: []", "empty");
        logger.ln();

        if (ModuleRegistry.clientCommands.admin.any()) {
          logger.info("Admin commands: [total: @]", ModuleRegistry.clientCommands.admin.size);
          ModuleRegistry.clientCommands.admin.each(c ->
            logger.info(Strings.format("&lk|&fr &b&lb@@@@&fr - &lw@&fr", clientHandler.prefix, c.text,
                                       c.paramText.isEmpty() ? "" : " &lc&fi", c.paramText,
                                       Strings.normalize(c.description))));
        } else logger.info("Admin commands: []", "empty");
        logger.ln();

        if (ModuleRegistry.serverCommands.all.any()) {
          logger.info("Server commands: [total: @]", ModuleRegistry.serverCommands.all.size);
          ModuleRegistry.serverCommands.all.each(c ->
            logger.info(Strings.format("&lk|&fr &b&lb@@@@&fr - &lw@&fr", serverHandler.prefix, c.text,
                                       c.paramText.isEmpty() ? "" : " &lc&fi", c.paramText, c.description)));
        } else logger.info("Server commands: []", "empty");
        logger.ln();
        return;
      }

      boolean enable;

      if (Strings.isTrue(args[0], false)) enable = true;
      else if (Strings.isFalse(args[0], false)) enable = false;
      else {
        logger.err("Invalid argument! Must be 'reset', 'on' or 'off'.");
        return;
      }

      if (args.length == 1) {
        logger.err("Missing 'command' argument.");
        return;
      }

      String command = args[1];
      boolean client = false, server = false, found = false;

      if (!found && (client = args[1].startsWith(clientHandler.prefix))) {
        command = args[1].substring(clientHandler.prefix.length());
        final String c0 = command;
        found = clientCommands.contains(c -> c.text.equals(c0));
      }
      if (!found && (server = args[1].startsWith(serverHandler.prefix))) {
        command = args[1].substring(serverHandler.prefix.length());
        final String c0 = command;
        found = serverCommands.contains(c -> c.text.equals(c0));
      }

      if (!found) {
        logger.err("No" + (client ? " client" : server ? " server" : "") + " command named '@' found.", command);

      } else if (enable) {
        if (args.length == 2) {
          if (client) enableClientCommand(command);
          else if (server) enableServerCommand(command);
        } else if (args[2].equals("now")) {
          if (client) enableClientCommandNow(command);
          else if (server) enableServerCommandNow(command);
        } else {
          logger.err("Invalid argument! Must be 'now'.");
          return;
        }
        logger.info("Command enabled.");
        if (modified() && args.length == 2) logger.warn("A server restart is required to apply the change.");

      } else {
        // Special case
        if (protectedClientCommands.contains(command) || protectedServerCommands.contains(command)) {
          logger.err("This command is protected, you cannot disable it.");
          return;
        }

        if (args.length == 2) {
          if (client) disableClientCommand(command);
          else if (server) disableServerCommand(command);
        } else if (args[2].equals("now")) {
          if (client) disableClientCommandNow(command);
          else if (server) disableServerCommandNow(command);
        } else {
          logger.err("Invalid argument! Must be 'now'.");
          return;
        }
        logger.info("Command disabled.");
        if (modified() && args.length == 2) logger.warn("A server restart is required to apply the change.");
      }
    });
  }

  /** {@link Strings#columnify(Seq[])} for only two columns and ignores logging color codes. */
  private static Seq<String> Strings_columnify(int gap, Seq<String> left, Seq<String> right) {
    Seq<Integer> sl = left.map(l -> arc.util.Log.removeColors(l).length()),
                 sr = right.map(l -> arc.util.Log.removeColors(l).length());
    int l = gap + Strings.max(sl, e -> e), r = gap + Strings.max(sr, e -> e);
    String lf = " ".repeat(l),
           rf = " ".repeat(r);
    Seq<String> arr = left;
    int i = 0;

    for (; i<Integer.min(left.size, right.size); i++)
      arr.set(i, left.get(i) + " ".repeat(l - sl.get(i)) + right.get(i));
    // Fill the rest
    for (; i<left.size; i++) arr.set(i, left.get(i) + rf);
    for (; i<right.size; i++) arr.add(lf + right.get(i));

    return arr;
  }
}
