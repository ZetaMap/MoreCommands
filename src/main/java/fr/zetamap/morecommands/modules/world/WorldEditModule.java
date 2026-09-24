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

package fr.zetamap.morecommands.modules.world;

import arc.Events;
import arc.math.geom.Point2;
import arc.math.geom.Position;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Timer;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.TechTree;
import mindustry.core.NetServer;
import mindustry.entities.Units;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.UnitType;
import mindustry.type.Weather;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.storage.CoreBlock;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.ClientCommandHandler;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.modules.selector.SelectorParser;
import fr.zetamap.morecommands.modules.selector.util.CoordinatesParser;
import fr.zetamap.morecommands.modules.selector.util.TargetResult;
import fr.zetamap.morecommands.util.DurationFormatter;
import fr.zetamap.morecommands.util.MindustryJson;
import fr.zetamap.morecommands.util.Strings;


public class WorldEditModule extends AbstractModule {
  private final ObjectMap<Block, CoreBlock[]> coresEvolutions = new ObjectMap<>();
  private Timer.Task cleanTask;
  private PlayerData cleanupTriggerer;
  private boolean hardClean;

  /** Try to find the associated core {@code evolution} for the current map. */
  public CoreBlock getCoreEvolution(int evolution) {
    // Assumes the default planet core is the smallest
    return getCoreEvolution((CoreBlock)Vars.state.getPlanet().defaultCore, evolution);
  }

  /**
   * Try to find the associated core {@code evolution} (in the tech tree) using the base {@code core}.
   * @return {@code null} if no evolution found for the core.
   */
  public CoreBlock getCoreEvolution(CoreBlock core, int evolution) {
    CoreBlock[] cores = coresEvolutions.get(core);
    if (cores == null || evolution < 0 || evolution >= cores.length) return null;
    return cores[evolution];
  }

  /** @return {@code null} if unable to transform player, probably due to a unit cap reached, else the new player unit. */
  public Unit transformPlayer(PlayerData player, UnitType unit) {
    Position p = player.dead() ? player.player.closestCore() : player.player;
    if (p == null) return null;
    Unit u = unit.spawn(player.team(), p);
    if (!u.isValid()) return null;
    if (!player.dead()) u.rotation = player.player.unit().rotation;
    u.controller(player.player);
    u.spawnedByCore = true; // this is a temporary unit
    return u;
  }

  /**
   * Kill everything, except cores, on the map.
   * @param hardClean just "remove" everything instead of killing them.
   * @param delay time before the inevitable. {@code <=0} for no delay.
   * @param notify shows a popup to everyone about this action and statistics after the clear.
   *        The console will always be notified about these.
   */
  public void clearMap(PlayerData executor, boolean hardClean, float delay, boolean notify) {
    if (notify)
      Call.infoMessage(
        Strings.format("[scarlet]The map will be cleaned @![] \n"
                     + "[lightgray]All units, players, and buildings (except core) will be destroyed.",
                       delay <= 0 ? "[orange]now[]" : "in [orange]" + DurationFormatter.format((long)(delay*1000)) + "[]"));
    if (executor == null) logger.warn("Map cleanup triggered!");
    else logger.warn("'@' [@] has triggered a map cleanup!", executor.stripedName, executor);

    Runnable task = () -> {
      int units = Groups.unit.size(), blocks = 0;

      if (hardClean) {
        Groups.unit.clear();
        for (Tile t : Vars.world.tiles) {
          if (t != null && t.build != null && !(t.block() instanceof CoreBlock)) {
            t.build.dead = true;
            // Fire event manually
            Events.fire(new BlockDestroyEvent(t));
            t.build.remove();
            t.remove();
            blocks++;
          }
        }
        sendWorld();

      } else {
        Groups.unit.each(Unit::kill);
        for (Tile t : Vars.world.tiles) {
          if (t != null && t.build != null && !(t.block() instanceof CoreBlock)) {
            t.build.kill();
            blocks++;
          }
        }
      }

      if (notify)
        Call.infoMessage(Strings.format("[green]Map cleaned![]\n [lightgray]Killed [gray]@[] units and [gray]@[] blocks.",
                                        units, blocks));
      logger.warn("Map cleaned! Killed @ units and @ blocks.", units, blocks);
    };

    if (delay <= 0) task.run();
    else Timer.schedule(task, delay);
  }

  public void scheludeMapCleanConfirmation(PlayerData executor, boolean hardClean, float timeout) {
    cleanupTriggerer = executor;
    this.hardClean = hardClean;
    cleanTask = Timer.schedule(this::cancelMapCleanConfirmation, timeout);
  }

  public void cancelMapCleanConfirmation() {
    if (cleanTask != null) cleanTask.cancel();
    if (cleanupTriggerer != null) cleanupTriggerer.warn("Cleanup confirmation cancelled.");
    cleanupTriggerer = null;
    hardClean = false;
  }

  public void confirmMapClean(boolean notify) {
    PlayerData triggerer = cleanupTriggerer;
    boolean hard = hardClean;
    cleanupTriggerer = null;
    hardClean = false;
    clearMap(triggerer, hard, 10, notify); // default delay is 10 seconds
  }

  public void sendWorld() {
    Call.worldDataBegin();
    Groups.player.each(Vars.netServer::sendWorldData);
  }

  public void sendWorld(PlayerData player) {
    Call.worldDataBegin(player.player.con);
    Vars.netServer.sendWorldData(player.player);
  }

  public void syncTile(Tile tile) {
    NetServer.syncBuilding(tile.build);
  }

  public void sendBlockSnapshot() {
    try {
      Vars.netServer.writeBlockSnapshots();
    } catch (Exception e) {
      logger.err("Failed to send block snapshots. Resending world data to players...");
      logger.err(e);
      sendWorld();
    }
  }

  public void sendEntitySnapshot() {
    // For simplicity, just resets NetServer#snapshotSyncTime and run a frame
    Vars.netServer.snapshotSyncTime = 0;
    Vars.netServer.update();
  }

  public void sendEntitySnapshot(PlayerData player) {
    try {
      Vars.netServer.writeStateSnapshot(); // This will send to all players but this is not important
      Vars.netServer.writeEntitySnapshotsTeam(player.team(), Seq.with(player.player));
      if (player.player.con.localEntities.size > 0)
        Vars.netServer.writeCustomEntitySnapshot(player.player, player.player.con.localEntities);
    } catch (Exception e) {
      logger.err("Failed to send entity snapshot to player '@'. Resending world data...", player);
      logger.err(e);
      sendWorld(player);
    }
  }

  protected JsonValue parseJson(PlayerData player, String[] args, int from, int to) {
    try {
      return new JsonReader().parse(Strings.join(" ", args, from, to));
    } catch (Exception e) {
      player.err(Strings.neatError(e, false));
      return null;
    }
  }

  protected boolean checkBuildingData(PlayerData player, Block block, Team team, JsonValue data) {
    if (data == null) return false;
    if (block.hasBuilding()) {
      // Validate custom data
      try {
        // Create a stub building for validation
        Building build = block.newBuilding();
        build.block = block;
        build.team = team;
        MindustryJson.get().readFields(build, data);
      } catch (Exception e) {
        player.err(Strings.neatError(e, false));
        return false;
      }
    } else player.warn("Ignored custom building data because @ doesn't create one.", block.name);
    return true;
  }

  protected boolean checkUnitData(PlayerData player, UnitType unit, Team team, JsonValue data) {
    if (data == null) return false;
    // Validate custom data
    try {
      // Create a stub unit for validation
      MindustryJson.get().readFields(unit.create(team), data);
      return true;
    } catch (Exception e) {
      player.err(Strings.neatError(e, false));
      return false;
    }
  }

  protected static final TileChangeEvent tileChange = new TileChangeEvent();
  protected static final TilePreChangeEvent preChange = new TilePreChangeEvent();

  /** Apply custom data to building and run tile change events. */
  protected void updateBuilding(Tile tile, JsonValue data) throws Exception {
    if (!Vars.world.isGenerating()) Events.fire(preChange.set(tile));
    //TODO: use the new patch system?
    MindustryJson.get().readFields(tile.build, data);
    if (!Vars.world.isGenerating()) {
      tile.build.updateProximity();
      Events.fire(tileChange.set(tile));
    }
    tile.block().blockChanged(tile);
    syncTile(tile); // TODO: not very optimized for bulk operations
  }

  @Override
  protected void initImpl() {
    // Load planets specific cores
    Vars.content.planets().each(
      p -> p.defaultCore.techNode != null && !coresEvolutions.containsKey(p.defaultCore),
      p -> {
        Seq<CoreBlock> cores = new Seq<>(4);
        cores.add((CoreBlock)p.defaultCore);
        // Unwrap tech-tree
        TechTree.TechNode node = p.defaultCore.techNode;
        while (node.children.any()) {
          for (TechTree.TechNode n : node.children) {
            if (n.content instanceof CoreBlock c) {
              cores.add(c);
              node = n;
              break;
            }
          }
        }
        coresEvolutions.put(p.defaultCore, cores.toArray(CoreBlock.class));
      }
    );
  }

  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    handler.addAdmin("place", "<blockName> [player|selector|x,y] [teamName|~] [buildData...]", "Place a block.",
    (args, player) -> {
      Block block = Vars.content.block(Strings.kebabize(args[0]));
      if (block == null) {
        player.err("No block named '@' found.", args[0]);
        return;
      } else if (block instanceof CoreBlock && player.vanished()) {
        player.err("You can't build a core in vanish mode!");
        return;
      }

      Tile tile = player.player.tileOn();
      Team team = player.team();
      JsonValue data = null;
      if (args.length > 1) {
        TargetResult dest = Modules.selector.parseOne(player, args, 1, args.length);
        if (dest == null) return;

        args = dest.rest;
        tile = Vars.world.tileWorld(dest.pos.getX(), dest.pos.getY());

        if (args.length > 0 && (team = Modules.team.getTeam(player, args[0])) == null) {
          player.err("Team not found. [gray]Use [lightgray]/team[] to list them.");
          return;
        }

        if (args.length > 1) {
          if (block.hasBuilding()) data = parseJson(player, args, 1, args.length);
          if (!checkBuildingData(player, block, team, data)) return;
        }
      } else if (player.dead()) {
        player.err("Unable to find player position.");
        return;
      }

      if (tile == null) {
        player.err("Coordinates out of map bounds.");
        return;
      }
      Building last = tile.build;
      if (block == Blocks.air) Call.deconstructFinish(tile, block, player.unit()); //tile.removeNet()
      else ConstructBlock.constructed(tile, block, player.unit(), (byte)0, team, null);

      String article = Strings.articleFor(block.name);
      if (block == Blocks.air)
        player.ok("Removed " + (last != null ? Strings.articleFor(last.block.name) + ' ' : "") + "@ at @[],@.",
                  last != null ? last.block.name : "nothing", tile.x, tile.y);
      else if (block.hasBuilding())
        player.ok("Built " + article + " @ at @[],@ for the @ team.", block.name, tile.x, tile.y, team.coloredName());
      else
        player.ok("Placed " + article + " @ block at @[],@.", block.name, tile.x, tile.y);

      if (block.hasBuilding() && data != null && tile.build != null && tile.build.isValid()) {
        try {
          updateBuilding(tile, data);
          player.ok("Succesfully applied custom building data.");
        } catch (Exception e) {
          player.err("Failed to apply custom building data: \n@", Strings.neatError(e, false));
        }
      }
    });

    handler.addAdmin("fill",
                     "<blockName> <player|selector|src-x,y> <player|selector|dest-x,y> [teamName|~] [buildData...]",
                     "Fill a zone.", (args, player) -> {
      Block block = Vars.content.block(Strings.kebabize(args[0]));
      if (block == null) {
        player.err("No block named '@' found.", args[0]);
        return;
      } else if (block instanceof CoreBlock && player.vanished()) {
        player.err("You can't build a core in vanish mode!");
        return;
      }

      TargetResult src = Modules.selector.parseOne(player, args, 1, args.length);
      if (src == null) return;
      TargetResult dest = Modules.selector.parseOne(player, src.rest);
      if (dest == null) return;
      args = dest.rest;

      Team team = args.length == 0 ? player.team() : Modules.team.getTeam(player, args[0]);
      if (team == null) {
        player.err("Team not found. [gray]Use [lightgray]/team[] to list them.");
        return;
      }

      JsonValue data = null;
      if (args.length > 1) {
        if (block.hasBuilding()) data = parseJson(player, args, 1, args.length);
        if (!checkBuildingData(player, block, team, data)) return;
      }

      int count = 0,
          x = Math.min(src.wpos.x, dest.wpos.x), xn = Math.max(src.wpos.x, dest.wpos.x),
          y = Math.min(src.wpos.y, dest.wpos.y), yn = Math.max(src.wpos.y, dest.wpos.y);
      Tile tile;

      if (block == Blocks.air) {
        for (int xx=x, yy; xx<xn; xx+=block.size) {
          for (yy=y; yy<yn; yy+=block.size) {
            tile = Vars.world.tile(xx, yy);
            if (tile == null) continue;
            if (tile.build != null) count++;
            //tile.removeNet();
            Call.deconstructFinish(tile, block, player.unit());
          }
        }
        player.ok("Removed @ blocks from @[],@ to @[],@.", count, x, y, xn, yn);

      } else {
        for (int xx=x, yy; xx<xn; xx+=block.size) {
          for (yy=y; yy<yn; yy+=block.size) {
            tile = Vars.world.tile(xx, yy);
            if (tile == null) continue;
            ConstructBlock.constructed(tile, block, player.unit(), (byte)0, team, null);
            if (!block.hasBuilding() ||
                tile.block() == block && tile.build != null && tile.build.isValid()) count++;
          }
        }
        if (block.hasBuilding())
          player.ok("Built @ @ from @[],@ to @[],@ for the @ team.", count, block.name, x, y, xn, yn,
                    team.coloredName());
        else
          player.ok("Placed @ @ blocks from @[],@ to @[],@.", count, block.name, x, y, xn, yn);
      }

      if (data == null || !block.hasBuilding()) return;
      count = 0;
      int errors = 0;
      Throwable lastError = null;

      for (int xx=x, yy; xx<xn; xx+=block.size) {
        for (yy=y; yy<yn; yy+=block.size) {
          tile = Vars.world.tile(xx, yy);
          if (tile.block() != block || tile.build == null || !tile.build.isValid()) continue;
          try {
            updateBuilding(tile, data);
            count++;
          } catch (Exception e) {
            lastError = e;
            errors++;
          }
        }
      }
      if (count > 0) player.ok("Succesfully applied custom data to @ buildings.", count);
      if (errors > 0)
        player.err("Failed to apply custom data to @ buildings: \n@", errors, Strings.neatError(lastError, false));
    });

    handler.addAdmin("core", "<small|medium|big|coreName> [player|selector|x,y] [teamName|~...]", "Build a core.",
    (args, player) -> {
      if (player.vanished()) {
        player.err("You can't build a core in vanish mode!");
        return;
      }

      Block core;
      switch (args[0]) {
        case "small":
          core = getCoreEvolution(0);
          break;
        case "medium":
          core = getCoreEvolution(1);
          break;
        case "big":
          core = getCoreEvolution(2);
          break;
        default:
          core = Vars.content.block("core-" + args[0].toLowerCase());
          if (core == null) {
            player.err("No core named '@' found.", args[0]);
            return;
          }
      }
      if (core == null) {
        player.err("Unable to find a core evolution for '@' based on the current map.", args[0]);
        return;
      }

      Tile tile = player.player.tileOn();
      Team team = player.team();
      if (args.length > 1) {
        TargetResult dest = Modules.selector.parseOne(player, args, 1, args.length);
        if (dest == null) return;

        args = dest.rest;
        tile = Vars.world.tileWorld(dest.pos.getX(), dest.pos.getY());

        if (args.length > 0 && (team = Modules.team.getTeam(player, args[0])) == null) {
          player.err("Team not found. [gray]Use [lightgray]/team[] to list them.");
          return;
        }
      } else if (player.dead()) {
        player.err("Unable to find player position.");
        return;
      }

      if (tile == null) {
        player.err("Coordinates out of map bounds.");
        return;
      }
      ConstructBlock.constructed(tile, core, player.unit(), (byte)0, team, null);
      player.ok("Built a @ at @[],@ for the @ team.", core.name, tile.x, tile.y, team.coloredName());
    });

    handler.addAdmin("spawn", "<unit> [count] [player|selector|x,y] [teamName|~] [unitData...]", "Spawn a unit.",
    (args, player) -> {
      UnitType unit = Vars.content.unit(Strings.kebabize(args[0]));
      if (unit == null) {
        player.err("No unit named '@' found.", args[0]);
        return;
      }

      int count = 1;
      if (args.length > 1) {
        count = Strings.parseInt(args[1]);
        if (count < 1) {
          player.err("'@' must be a number greater than @.", "count", "1");
          return;
        }
      }

      Position pos = player.player;
      Point2 wpos = CoordinatesParser.toWorld(pos);
      Team team = player.team();
      JsonValue data = null;

      if (args.length > 2) {
        TargetResult dest = Modules.selector.parseOne(player, args, 2, args.length);
        if (dest == null) return;

        args = dest.rest;
        pos = dest.pos;
        wpos = dest.wpos;

        if (args.length > 0 && (team = Modules.team.getTeam(player, args[0])) == null) {
          player.err("Team not found. [gray]Use [lightgray]/team[] to list them.");
          return;
        }

        if (args.length > 1) {
          data = parseJson(player, args, 1, args.length);
          if (!checkUnitData(player, unit, team, data)) return;
        }
      } else if (player.dead()) {
        player.err("Unable to find player position.");
        return;
      }

      if (team.core() == null) {
        player.err("No core available in the @ team.", team.coloredName());
        return;
      }

      Seq<Unit> spawned = new Seq<>(Math.min(count, Units.getCap(team)));
      while (count-- > 0) {
        Unit u = unit.spawn(pos, team);
        if (u.isValid()) spawned.add(u);
      }
      player.ok("Spawned @ @ at @[],@ for the @ team", spawned.size > 1 ? spawned.size : Strings.articleFor(unit.name),
                unit.name, wpos.x, wpos.y, team.coloredName());

      if (data == null || spawned.isEmpty()) return;
      count = 0;
      int errors = 0;
      Throwable lastError = null;

      for (Unit u : spawned) {
        try {
          MindustryJson.get().readFields(u, data);
          count++;
        } catch (Exception e) {
          lastError = e;
          errors++;
        }
      }
      if (count > 0) player.ok("Succesfully applied custom data to @ units.", count);
      if (errors > 0)
        player.err("Failed to apply custom data to @ units: \n@", errors, Strings.neatError(lastError, false));
    });

    handler.addAdmin("transform", "<unit> [player|selector] [unitData...]", "Transform a player unit.",
    (args, player) -> {
      UnitType unit = Vars.content.unit(Strings.kebabize(args[0]));
      if (unit == null) {
        player.err("No unit named '@' found.", args[0]);
        return;
      } else if (player.player.core() == null) {
        player.err("No core available in the @ team.", player.team().coloredName());
        return;
      }

      String article = Strings.articleFor(unit.name);

      if (args.length == 1) {
        if (player.dead()) {
          player.err("Unable to locate @ unit.", "your");
          return;
        } else if (transformPlayer(player, unit) != null)
          player.ok("Transformed to " + article + " @.", unit.name);
        else player.err("Unable to transform you to " + article + " @. Unit cap reached?", unit.name);
        return;
      }

      SelectorParser selector = Modules.selector.parse(player, args, 1, args.length, true);
      if (selector == null) return;

      JsonValue data = null;
      if (selector.rest.length > 0) {
        data = parseJson(player, selector.rest, 0, selector.rest.length);
        if (!checkUnitData(player, unit, player.team(), data)) return;
      }

      Seq<Unit> transformed = new Seq<>(selector.selected.size);
      selector.execute((p, u) -> {
        if (player.dead()) {
          player.err("Unable to locate @'s unit.", p.getName());
          return;
        }
        transformed.add(u = transformPlayer(p, unit));
        if (u == null || player == p) return;
        p.warn("You have been transformed to " + article + " @ by @.", unit.name,player.getName());
      });
      int invalids = transformed.count(u -> u == null);
      selector.selected.set(transformed.removeAll(u -> u == null));
      player.ok(selector.formatColorMessage("Transformed") + "[green].");
      if (invalids > 0)
        player.err("Unable to transform @ @ to " + article + " @.  Unit cap reached?", invalids, "players", unit.name);

      if (data == null || transformed.isEmpty()) return;
      int count = 0, errors = 0;
      Throwable lastError = null;

      for (Unit u : transformed) {
        try {
          MindustryJson.get().readFields(u, data);
          count++;
        } catch (Exception e) {
          lastError = e;
          errors++;
        }
      }
      if (count > 0) player.ok("Succesfully applied custom data to @ units.", count);
      if (errors > 0)
        player.err("Failed to apply custom data to @ units: \n@", errors, Strings.neatError(lastError, false));
    });

    handler.addAdmin("kill", "[player|selector...] ", "Kill a player or a unit.", (args, player) -> {
      if (args.length == 0) {
        if (!player.dead()) {
          player.unit().kill();
          player.ok("Killed @.", "yourself");
        } else player.warn("Your already dead. =/");
        return;
      }

      SelectorParser selector = Modules.selector.parse(player, args);
      if (selector == null) return;

      selector.execute((p, u) -> {
        u.kill();
        if (p != null && p != player) p.warn("You have been killed by @.", player.getName());
      });
      player.ok(selector.formatColorMessage("Killed") + "[green].");
    });

    handler.addAdmin("clear-map", "[hard|y|n]", "Kill all units and blocks, except cores, on the map.",
    (args, player) -> {
      if (cleanupTriggerer != null) {
        if (cleanupTriggerer != player) {
          player.err("@ already triggered a cleanup. \nHe/She must to confirm/cancel it before auto cancellation.",
                     cleanupTriggerer.getName());
          return;
        } else if (args.length == 0) {
          player.err("Please confirm, or not, the operation. Use @ or @.", "/clear-map y", "/clear-map n");
          return;
        }

        // Manual check to only accept "yes" or "no" replies
        switch (args[0]) {
          case "y": case "yes":
            player.ok("Cleaning map.");
            confirmMapClean(true);
            return;
          case "n": case "no":
            cancelMapCleanConfirmation();
            return;
          default:
            player.err("Invalid argument! Must be '@' or '@'.", "y", "n");
            return;
        }
      }

      boolean hard = false;
      if (args.length == 1 && !(hard = args[0].equals("hard"))) {
        player.err("No cleaning is awaiting confirmation.");
        return;
      }

      scheludeMapCleanConfirmation(player, hard, 10); // 10 seconds before auto cancellation
      player.info("\nAre you sure to start a map cleanup? [orange]This can produce lot of lags[].\n"
                + "Use @ or @ to confirm or cancel the operation.", "/clear-map y", "/clear-map n");
      player.warn("The operation will be automatically canceled after @.", "10 seconds");
    });

    handler.addAdmin("weather", "[clear|weatherName] [intensity] [inf|duration]", "Control map weather.",
    (args, player) -> {
      if (args.length == 0) {
        StringBuilder builder = new StringBuilder();
        if (Groups.weather.isEmpty())
          builder.append("No current weathers");
        else
          builder.append("Current weathers are: ")
                 .append(Groups.weather.copy().toString(", ", w -> "[accent]" + w.weather.name + "[]"));

        builder.append("\nAvailable are: [accent]clear[], ")
               .append(Vars.content.weathers().toString(", ", w -> "[accent]" + w.name + "[]"));
        player.info(builder.toString());
        return;

      } else if (args[0].equals("clear")) {
        // Set life time to 1 and re-sync entities instead of simply clearing weathers.
        Groups.weather.each(w -> w.life(1f));
        // Weathers are duplicated for the client, we need to resync all entities
        sendWorld(); //sendEntitySnapshot();
        player.ok("Removed all weather status.");
        return;
      }

      Weather weather = Vars.content.weather(Strings.kebabize(args[0]));
      if (weather == null) {
        player.err("No weather named '@' found.", args[0]);
        return;
      }

      int intensity = 100;
      float duration = weather.duration / 60f;
      if (args.length > 1) {
        intensity = Strings.parseInt(args[1]);
        if (intensity < 0 || intensity > 100) {
          player.err("'@' must be a number between @ and @.", "intensity", "0", "100");
          return;
        }
      }

      if (args.length > 2) {
        duration = args[2].equals("inf") ? Float.POSITIVE_INFINITY : Strings.parseInt(args[2]);
        if (duration < 0) {
          player.err("'@' must be a positive number of seconds, or '@' for an infinite duration.", "duration", "inf");
          return;
        }
      }

      weather.create(intensity / 100f, duration * 60f);
      if (duration == Float.POSITIVE_INFINITY)
        player.ok("Weather @ created with @ of intensity @.", weather.name, intensity + "%", "forever");
      else
        player.ok("Weather @ created with @ of intensity for @.", weather.name, intensity + "%",
                  DurationFormatter.format((long)(duration * 1000)));
    });

    //IDEA: /time, /patch
  }
}
