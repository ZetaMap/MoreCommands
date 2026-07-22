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

package fr.zetamap.morecommands.modules.misc;

import arc.Core;
import arc.Events;
import arc.func.Floatp;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Reflect;
import arc.util.Time;
import mindustry.Vars;
import mindustry.core.GameState.State;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.io.JsonIO;
import mindustry.mod.Mods.LoadedMod;
import mindustry.net.Administration.Config;
import mindustry.net.ArcNetProvider;
import mindustry.net.Packets.KickReason;
import mindustry.type.Item;
import mindustry.world.blocks.storage.CoreBlock;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.*;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.module.ModuleRegistry;
import fr.zetamap.morecommands.modules.selector.SelectorParser;
import fr.zetamap.morecommands.util.Strings;


public class MiscModule extends AbstractModule {
  // Remove logger topic
  { logger.topic(null); }

  protected boolean reconnectSupported;
  public Config reconnectOnExit;
  public boolean reconnectOnExitForced;
  public float gameSpeed = 1;

  public boolean isReconnectOnExitSupported() {
    return reconnectSupported;
  }

  public void exitServer() { exitServer(0); }
  public void exitServer(int status) {
    // Avoid to add a hook for a zero status
    if (status != 0) {
      // This is hacky but I have no fucking choice, this is so badly made;
      // Calling System.exit() while running shutdown hooks leads to an infinite waiting...
      if (Core.app.getMainThread() != null)
        new Thread(() -> {
          try { Core.app.getMainThread().join(); }
          catch (InterruptedException ignored) {}
          System.exit(status);
        }, "ExitHook").start();
      // No other choice, we need to halt the app while shutting down
      else Runtime.getRuntime().addShutdownHook(new Thread(() -> Runtime.getRuntime().halt(status), "ExitHook"));
    }

    // Prefer existing 'exit' command
    CommandHandler.CommandResponse result = ModuleRegistry.serverCommands.handler.handleMessage("exit");
    if (result.type == CommandHandler.ResponseType.valid) return;

    // Else fallback to manual implementation
    logger.info("Shutting down server.");
    Vars.net.dispose();
    Core.app.exit();
  }

  public boolean exitServer(boolean reconnectPlayers) { return exitServer(reconnectPlayers, 0); }
  /** @return whether the players can be reconnected or not if {@code reconnectPlayers} is {@code true}. */
  public boolean exitServer(boolean reconnectPlayers, int status) {
    boolean reconnect = reconnectPlayers && initialized();
    if (reconnectPlayers) reconnectOnExitForced = true;
    exitServer(status);
    return reconnect;
  }

  private final byte[] buf = new byte[Float.BYTES];
  private byte[] encodeFloat(float value) {
    int intBits =  Float.floatToIntBits(value);
    buf[0] = (byte)(intBits >> 24);
    buf[1] = (byte)(intBits >> 16);
    buf[2] = (byte)(intBits >> 8);
    buf[3] = (byte)(intBits >> 0);
    return buf;
  }

  public void setGameSpeed(float value) {
    if (value < 0) throw new IllegalArgumentException("negative value");
    gameSpeed = value;
    // Inform all clients about the change. This only work if the client handle the packet.
    sendGameSpeed();
  }

  public void sendGameSpeed(PlayerData player) {
    Call.clientBinaryPacketReliable(player.player.con, "gamespeed", encodeFloat(gameSpeed));
  }

  public void sendGameSpeed() {
    Call.clientBinaryPacketReliable("gamespeed", encodeFloat(gameSpeed));
  }

  public void fillCore(Team team) { fillCore(team, Vars.content.items()); }
  public void fillCore(Team team, Seq<Item> items) {
    CoreBlock.CoreBuild core = team.core();
    if (core != null) items.each(i -> core.items.set(i, core.storageCapacity));
  }

  public void fillCores() { fillCores(Vars.content.items()); }
  public void fillCores(Seq<Item> items) {
    for (Team t : Team.all) fillCore(t, items);
  }

  public void setGamemode(Gamemode mode) {
    boolean wasPvp = Vars.state.rules.pvp;
    Vars.state.rules = Vars.state.map.applyRules(mode);
    if (Vars.state.rules.pvp) Groups.player.each(Vars.netServer::assignTeam);
    else if (wasPvp) Groups.player.each(p -> p.team(Vars.state.rules.defaultTeam));
    Modules.worldEdit.sendWorld();
  }

  public boolean syncPlayer(PlayerData player) {
    if(Time.timeSinceMillis(player.player.getInfo().lastSyncTime) < 1000 * 5) return false;
    player.player.getInfo().lastSyncTime = Time.millis();
    Call.worldDataBegin(player.player.con);
    Vars.netServer.sendWorldData(player.player);
    return true;
  }

  @Override
  public void initImpl() {
    reconnectOnExit = new Config(
      "reconnectOnExit",
      "Reconnect players when the server exits instead of kicking them. (if supported)" +
      "This is usefull when a server restart is needed, without bothering peoples.",
      false
    );

    // Replace the current net provider using reflection, to handle the above feature
    Object original = Reflect.get(Vars.net, "provider");
    if (original.getClass() == ArcNetProvider.class) { // Be safe if another plugin is overriding it
      Reflect.set(Vars.net, "provider", new ArcNetProvider() {
        { JsonIO.json.copyFields(original, this, true); } // Copy fields from old provider

        @Override
        public void dispose() {
          if (reconnectOnExit.bool() || reconnectOnExitForced) {
            Vars.netServer.kickAll(KickReason.serverRestarting);
            reconnectOnExitForced = false;
          }
          super.dispose();
        }
      });
      reconnectSupported = true;
    } else reconnectSupported = false;

    Floatp deltaProvider = Reflect.get(Time.class, "deltaimpl");
    Time.setDeltaProvider(() -> deltaProvider.get() * gameSpeed);
    Events.on(EventType.PlayerJoin.class, e -> sendGameSpeed(PlayerData.get(e.player)));
  }

  @Override
  public void registerServerCommands(ServerCommandHandler handler) {
    handler.add("restart",
                "Reconnects players and exits the server with code '2', to ask a restart from the launcher script.",
    args -> {
      if (!reconnectSupported)
        logger.warn("'@' is not supported due to another mod/plugin overriding '@'.", reconnectOnExit.name,
                    "Vars.net.provider");
      exitServer(true, 2);
    });

    handler.add("speed", "[value]", "Control the game speed. &lrUSE WITH CAUTION!&fr", args -> {
      if (args.length == 0) logger.info("Current game speed: @", gameSpeed);
      else {
        float s = Strings.parseFloat(args[0]);
        if (s <= 0f) {
          logger.err("The value must be a number greater than @.", 0f);
          return;
        }

        setGameSpeed(s);
        logger.info("Game speed set to: @", gameSpeed);
        logger.warn("This can only work correctly if the clients supports the 'gamespeed' packet.");
      }
    });

    handler.add("fillitems", "[team|all] [items...]",
                "Fill the core of the specified or all teams, with the selected or all items.", args -> {
      if (!Vars.state.isPlaying()) {
        logger.err("Not playing. Host or unpause first.");
        return;

      } else if (args.length == 0) {
        Seq<Team> teams = Seq.select(Team.all, t -> t.items().any());

        if (teams.isEmpty()) {
          logger.info("No team has items in their core.");
          return;
        }
        logger.info("Teams items: [@ team" + (teams.size > 1 ? "s]" : "]"), teams.size);

        int columns = 3, n = 0;
        @SuppressWarnings("unchecked")
        Seq<String>[] table = new Seq[columns];

        for (Team team : teams) {
          int items = (int)team.items().sum((ii, a) -> 1);
          Seq<String> col;
          if (table[n] != null) (col = table[n]).clear();
          else table[n] = col = new Seq<>(items + 1);

          col.add("&fb&lb" + team.name + "&fr: [items: &fb&lb" + items + "&fr, total: &fb&lb" + team.items().total() + "&fr]");
          team.items().each((i, a) -> col.add("&lk|&fr " + i.name + ": &fb&lb" + a + "&fr"));

          if (n >= columns-1) {
            n = 0;
            Strings_columnify(2, table).each(l -> logger.info("&lk|&fr " + l));
            logger.info("&lk|&fr");
          } else n++;
        }
        if (n > 0) {
          for (; n < columns; n++) table[n] = null;
          Strings_columnify(2, table).each(l -> logger.info("&lk|&fr " + l));
          logger.info("&lk|&fr");
        }
        return;
      }

      Team team = null;
      Seq<Item> items = Vars.content.items();

      if (!args[0].equals("all")) {
        team = Modules.team.searchTeam(args[0]);

        if (team == null) {
          logger.err("No team with that name or id found.");
          return;
        } else if (team.cores().isEmpty()) {
          logger.err("That team has no cores.");
          return;
        }
      }

      if (args.length == 2) {
        String[] rest = args[1].split("\\s+");
        items = new Seq<>(rest.length);
        for (String i : rest) {
          Item item = Vars.content.item(i);
          if (item == null) {
            logger.err("Unknown item '@'.", i);
            return;
          }
          items.add(item);
        }
      }

      if (team == null) fillCores(items);
      else fillCore(team, items);

      logger.info("Core of " + (team == null ? "all teams" : "team @") + " filled" +
                  (args.length == 2 ? " with item" + (items.size > 1 ? "s" : "") + ": @." : "."),
                  team != null ? team.name : args.length == 2 ? Strings.toSentence(items, i -> i.name) : null,
                  team != null && args.length == 2 ? Strings.toSentence(items, i -> i.name) : null);
    });

    handler.add("gamemode", "[name]", "Change the current map gamemode.", args -> {
      if (Vars.state.isPlaying()) {
        if (args.length == 1) {
          try {
            setGamemode(Gamemode.valueOf(args[0]));
            logger.info("Gamemode changed to '@'.", args[0]);
            Modules.messaging.serverWarn("Gamemode", "Changed to @ by the console.", args[0]);

          } catch (IllegalArgumentException e) { logger.err("Unknown gamemode '@'.", args[0]); }
        } else logger.info("The current gamemode is '@'.", Vars.state.rules.mode().name());
      } else logger.err("Not playing. Host or unpause first.");
    });

    handler.add("mod", "<on|off|name> [name...]", "Toggle a mod or display its information.", args -> {
      String name = args.length > 1 ? String.join(" ", args) : args[0];
      LoadedMod mod = Vars.mods.list().find(p -> p.meta.name.equalsIgnoreCase(name));

      if (mod != null) {
        logger.info("Name: @", mod.meta.displayName);
        logger.info("Internal Name: @", mod.name);
        logger.info("Description: @", mod.meta.description.replace("\n", "\n           | "));
        logger.info("Version: @", mod.meta.version);
        logger.info("Author(s): @", mod.meta.author);
        logger.info("Repo: @", mod.meta.repo);
        logger.info("Dependencies: @", mod.meta.dependencies);
        logger.info("Soft Dependencies: @", mod.meta.softDependencies);
        logger.info("Path: @", mod.file.path());
        logger.info("State: @", mod.state);
        return;
      }

      boolean enable;
      if (Strings.isTrue(args[0], false)) enable = true;
      else if (Strings.isFalse(args[0], false)) enable = false;
      else {
        logger.err("No mod with name '@' found.", name);
        return;
      }

      String name1 = Strings.join(" ", args, 1, args.length).strip();
      mod = Vars.mods.list().find(p -> p.meta.name.equalsIgnoreCase(name1));

      if (mod != null) {
        Vars.mods.setEnabled(mod, enable);
        logger.info("@ mod @.", enable ? "Enabled" : "Disabled", mod.name);
      } else logger.err("No mod with name '@' found.", name);
    });
  }

  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    handler.add("sync", "[selector|player...]", "Re-synchronize world state of a player.", (args, player) -> {
      if (args.length == 0) {
       if(!syncPlayer(player)) Players.err(player, "You may only /sync every [orange]5 seconds[].");
       return;
      } else if (!player.admin()) {
        Players.errArgUseDenied(player);
        return;
      }

      SelectorParser selector = Modules.selector.parse(player, args, true);
      if (selector == null) return;
      selector.execute((p, u) -> {
        if (syncPlayer(p) && player != p)
          Players.warn(p, "World state resynchronized by @[orange].", player.getName());
      });
      Players.ok(player, selector.formatMessage("Resynchronized", true) + "[green].");
    });

    handler.addAdmin("fillitems", "[team|all] [items...]",
                     "Fill the core of the specified or all teams, with the selected or all items.", (args, player) -> {
      if (args.length == 0) {
        Seq<Team> teams = Seq.select(Team.all, t -> t.items().any());

        if (teams.isEmpty()) {
          Players.info(player, "No team has items in their core.");
          return;
        }
        Players.info(player, "Teams items: [[[#1E90FF]@[] team" + (teams.size > 1 ? "s]" : "]"), teams.size);

        StringBuilder builder = new StringBuilder();
        Seq<String> lines = new Seq<>();


        for (Team team : teams) {
          int items = (int)team.items().sum((ii, a) -> 1);
          lines.ensureCapacity(items);

          builder.append("[gray]|[] [#1E90FF]").append(team.name).append("[]: [[items: [#1E90FF]").append(items)
                 .append("[], total: [#1E90FF]").append(team.items().total()).append("[]]\n");
          team.items().each((i, a) -> lines.add(i.emoji() + a));
          Strings.tableify(lines, 50, 2).each(l -> builder.append("[gray]| |[] ").append(l).append('\n'));
          builder.append("[gray]|[]");

          Players.info(player, builder.toString());
          builder.setLength(0);
          lines.clear();
        }
        return;
      }

      Team team = null;
      Seq<Item> items = Vars.content.items();

      if (!args[0].equals("all")) {
        team = Modules.team.searchTeam(args[0]);

        if (team == null) {
          Players.err(player, "No team with that name or id found.");
          return;
        } else if (team.cores().isEmpty()) {
          Players.err(player, "That team has no cores.");
          return;
        }
      }

      if (args.length == 2) {
        String[] rest = args[1].split("\\s+");
        items = new Seq<>(rest.length);
        for (String i : rest) {
          Item item = Vars.content.item(i);
          if (item == null) {
            Players.err(player, "Unknown item '[orange]@[]'.", i);
            return;
          }
          items.add(item);
        }
      }

      if (team == null) fillCores(items);
      else fillCore(team, items);

      Players.ok(player, "Core of " + (team == null ? "[accent]all teams[]" : "@ team") + " filled" +
                (args.length == 2 ? " with item" + (items.size > 1 ? "s" : "") + ": [accent]@[]." : "."),
                team != null ? team.coloredName() : args.length == 2 ? Strings.toSentence(items, i -> i.name) : null,
                team != null && args.length == 2 ? Strings.toSentence(items, i -> i.name) : null);
    });

    handler.addAdmin("gamemode", "[name]", "Change the current map gamemode.", (args, player) -> {
      if (args.length == 1) {
        try {
          setGamemode(Gamemode.valueOf(args[0]));
          Players.ok(player, "Gamemode changed to [accent]@[].", args[0]);
          logger.info("Gamemode has been changed to '@' by @.", args[0], player.stripedName);
          Modules.messaging.serverWarn("Gamemode", "Changed to @ by @.", args[0], player.getName());

        } catch (IllegalArgumentException e) { Players.err(player, "Unknown gamemode '[orange]@[]'.", args[0]); }
      } else Players.info(player, "The current gamemode is '@'.", Vars.state.rules.mode().name());
    });

    handler.addAdmin("pause", "<on|off>", "Toggle the game state.", (args, player) -> {
      boolean pause;
      if (Strings.isTrue(args[0])) pause = true;
      else if (Strings.isFalse(args[0])) pause = false;
      else {
        Players.err(player, "Invalid argument! Must be '[orange]on[]' or '[orange]off[]'.");
        return;
      }

      State old = Vars.state.getState(), news = pause ? State.paused : State.playing;
      // Set state twice because auto pause can modify it
      Vars.state.set(news);
      Core.app.post(() -> Core.app.post(() -> Vars.state.set(news)));
      Players.ok(player, "Game [accent]@[].", pause ? "paused" : "unpaused");
      logger.info("Game @ by @.", pause ? "paused" : "unpaused", player.stripedName);
      if (old == news) return;
      Modules.messaging.serverWarn("Game", "@ by @.", pause ? "Paused" : "Unpaused", player.getName());
    });
  }

  /** {@link Strings#columnify(Seq[])} that ignores logging color codes and {@code null columns}. */
  private static Seq<String> Strings_columnify(int gap, Seq<String>[] columns) {
    @SuppressWarnings("unchecked")
    Seq<Integer>[] scolumns = new Seq[columns.length];
    int[] lengths = new int[columns.length];
    for (int c=0; c<columns.length; c++) {
      if (columns[c] == null) continue;
      scolumns[c] = columns[c].map(l -> arc.util.Log.removeColors(l).length());
      lengths[c] = Strings.max(scolumns[c], i -> i);
    }

    int max = Strings.max(columns, a -> a == null ? 0 : a.size);
    Seq<String> arr = new Seq<>(max);
    StringBuilder builder = new StringBuilder();
    String[] fillers = new String[columns.length];

    for (int i=0, c; i<max; i++) {
      for (c=0; c<columns.length; c++) {
        if (columns[c] == null) continue;
        else if (i < columns[c].size) builder.append(columns[c].get(i) + " ".repeat(lengths[c] + gap - scolumns[c].get(i)));
        else if (fillers[c] != null) builder.append(fillers[c]);
        else builder.append(fillers[c] = " ".repeat(lengths[c] + gap));
      }
      arr.add(builder.toString());
      builder.setLength(0);
    }

    return arr;
  }
}