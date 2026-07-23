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
import arc.math.geom.Position;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Timer;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.TechTree;
import mindustry.core.World;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.game.EventType.TileChangeEvent;
import mindustry.game.EventType.TilePreChangeEvent;
import mindustry.gen.*;
import mindustry.type.UnitType;
import mindustry.type.Weather;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.BlockFlag;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.ClientCommandHandler;
import fr.zetamap.morecommands.misc.CoordinatesParser;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.modules.selector.SelectorParser;
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
    Position p = player.player.dead() ? player.player.closestCore() : player.player;
    Unit u = unit.spawn(player.player.team(), p);
    if (!u.isValid()) return null;
    if (!player.player.dead()) u.rotation = player.player.unit().rotation;
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
    else logger.warn("'@' [@] has triggered a map cleanup!", executor.stripedName, executor.player.uuid());

    Runnable task = () -> {
      int units = Groups.unit.size(), blocks = 0;

      if (hardClean) {
        Groups.unit.clear();
        for (Tile t : Vars.world.tiles) {
          if (t != null && t.build != null && !(t.block() instanceof CoreBlock)) {
            t.build.dead = true;
            // Fire event manually
            Events.fire(new EventType.BlockDestroyEvent(t));
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
    if (cleanupTriggerer != null) Players.warn(cleanupTriggerer, "Cleanup confirmation cancelled.");
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

  public void sendWorld(Player player) {
    Call.worldDataBegin(player.con);
    Vars.netServer.sendWorldData(player);
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

  public void sendEntitySnapshot(Player player) {
    try {
      Vars.netServer.writeStateSnapshot(); // This will send to all players but this is not important
      Vars.netServer.writeEntitySnapshotsTeam(player.team(), Seq.with(player));
      if (player.con.localEntities.size > 0)
        Vars.netServer.writeCustomEntitySnapshot(player, player.con.localEntities);
    } catch (Exception e) {
      logger.err("Failed to send entity snapshot to player '@'. Resending world data...", player.uuid());
      logger.err(e);
      sendWorld(player);
    }
  }

  protected JsonValue parseJson(PlayerData player, String[] args, int from, int to) {
    try {
      return new JsonReader().parse(Strings.join(" ", args, from, to));
    } catch (Exception e) {
      Players.err(player, Strings.neatError(e, false));
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
        Players.err(player, Strings.neatError(e, false));
        return false;
      }
    } else Players.warn(player, "Ignored custom building data because [accent]@[] doesn't create one.", block.name);
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
      Players.err(player, Strings.neatError(e, false));
      return false;
    }
  }

  protected static final TileChangeEvent tileChange = new TileChangeEvent();
  protected static final TilePreChangeEvent preChange = new TilePreChangeEvent();

  /** Apply custom data to building and run tile change events. */
  protected void updateBuilding(Tile tile, JsonValue data) throws Exception {
    if (!Vars.world.isGenerating()) Events.fire(preChange.set(tile));
    //TODO: use new patch system?
    MindustryJson.get().readFields(tile.build, data);
    if (tile.build != null) Vars.indexer.getFlagged(tile.team(), BlockFlag.synced).add(tile.build);
    if (!Vars.world.isGenerating()) {
      tile.build.updateProximity();
      Events.fire(tileChange.set(tile));
    }
    tile.block().blockChanged(tile);
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
            if (n.content instanceof CoreBlock) {
              cores.add((CoreBlock) n.content);
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
    //TODO: allow selectors with only one target for /place, /fill, /core, /tp, /spawn
    handler.addAdmin("place", "<blockName> [player|x,y] [teamName|~] [buildData...]", "Place a block.",
    (args, player) -> {
      Block block = Vars.content.block(Strings.kebabize(args[0]));
      if (block == null) {
        Players.err(player, "No block named '[orange]@[]' found.", args[0]);
        return;
      } else if (block instanceof CoreBlock && player.vanished()) {
        Players.err(player, "You can't build a core in vanish mode!");
        return;
      }

      Tile tile = player.player.tileOn();
      Team team = player.player.team();
      JsonValue data = null;
      if (args.length > 1) {
        CoordinatesParser dest = CoordinatesParser.parse(player, args, 1, args.length);
        if (dest == null) return; // Error already send to player
        args = dest.rest;

        tile = Vars.world.tileWorld(dest.pos.x, dest.pos.y);

        if (args.length > 0 && (team = Modules.team.getTeam(player, args[0])) == null) {
          Players.err(player, "Team not found. [lightgray]Use [gray]/team[] to list them.");
          return;
        }

        if (args.length > 1) {
          if (block.hasBuilding()) data = parseJson(player, args, 1, args.length);
          if (!checkBuildingData(player, block, team, data)) return;
        }
      } else if (player.player.dead()) {
        Players.err(player, "Unable to find player position.");
        return;
      }

      if (tile == null) {
        Players.err(player, "Coordinates out of map bounds.");
        return;
      }
      Building last = tile.build;
      if (block == Blocks.air) Call.deconstructFinish(tile, block, player.player.unit()); //tile.removeNet()
      else ConstructBlock.constructed(tile, block, player.player.unit(), (byte)0, team, null);

      String pronoun = Strings.aOrAn(block.name);
      if (block == Blocks.air)
        Players.ok(player, "Removed @ at [accent]@,@[].",
                   last != null ? Strings.aOrAn(last.block.name) + " [accent]" + last.block.name + "[]" : "nothing",
                   tile.x, tile.y);
      else if (block.hasBuilding())
        Players.ok(player, "Built @ [accent]@[] at [accent]@,@[] for the [white]@[] team.", pronoun, block.name,
                   tile.x, tile.y, team.coloredName());
      else
        Players.ok(player, "Placed @ [accent]@[] block at [accent]@,@[].", pronoun, block.name, tile.x, tile.y);

      if (block.hasBuilding() && data != null && tile.build != null && tile.build.isValid()) {
        try {
          updateBuilding(tile, data);
          Players.ok(player, "Succesfully applied custom building data.");
          sendBlockSnapshot();
          //sendEntitySnapshot();
        } catch (Exception e) {
          Players.err(player, "Failed to apply custom building data: \n@", Strings.neatError(e, false));
        }
      }
    });

    handler.addAdmin("fill", "<blockName> <player|src-x,y> <player|dest-x,y> [teamName|~] [buildData...]", "Fill a zone.",
    (args, player) -> {
      Block block = Vars.content.block(Strings.kebabize(args[0]));
      if (block == null) {
        Players.err(player, "No block named '[orange]@[]' found.", args[0]);
        return;
      } else if (block instanceof CoreBlock && player.vanished()) {
        Players.err(player, "You can't build a core in vanish mode!");
        return;
      }

      CoordinatesParser src = CoordinatesParser.parse(player, args, 1, args.length);
      if (src == null) return;
      CoordinatesParser dest = CoordinatesParser.parse(player, src.rest);
      if (dest == null) return;
      args = dest.rest;

      Team team = args.length == 0 ? player.player.team() : Modules.team.getTeam(player, args[0]);
      if (team == null) {
        Players.err(player, "Team not found. [lightgray]Use [gray]/team[] to list them.");
        return;
      }

      JsonValue data = null;
      if (args.length > 1) {
        if (block.hasBuilding()) data = parseJson(player, args, 1, args.length);
        if (!checkBuildingData(player, block, team, data)) return;
      }

      int count = 0, srcx = World.toTile(src.pos.x), srcy = World.toTile(src.pos.y),
          destx = World.toTile(dest.pos.x), desty = World.toTile(dest.pos.y),
          x = Math.min(srcx, destx), xn = Math.max(srcx, destx),
          y = Math.min(srcy, desty), yn = Math.max(srcy, desty);
      Tile tile;

      if (block == Blocks.air) {
        for (int xx=x, yy; xx<xn; xx+=block.size) {
          for (yy=y; yy<yn; yy+=block.size) {
            tile = Vars.world.tile(xx, yy);
            if (tile == null) continue;
            if (tile.build != null) count++;
            //tile.removeNet();
            Call.deconstructFinish(tile, block, player.player.unit());
          }
        }
        Players.ok(player, "Removed [accent]@[] blocks from [accent]@,@[] to [accent]@,@[].", count, x, y, xn, yn);

      } else {
        for (int xx=x, yy; xx<xn; xx+=block.size) {
          for (yy=y; yy<yn; yy+=block.size) {
            tile = Vars.world.tile(xx, yy);
            if (tile == null) continue;
            ConstructBlock.constructed(tile, block, player.player.unit(), (byte)0, team, null);
            if (!block.hasBuilding() ||
                tile.block() == block && tile.build != null && tile.build.isValid()) count++;
          }
        }
        if (block.hasBuilding())
          Players.ok(player, "Built [accent]@ @[] from [accent]@,@[] to [accent]@,@[] for the [white]@[] team.",
                     count, block.name, x, y, xn, yn, team.coloredName());
        else
          Players.ok(player, "Placed [accent]@ @[] blocks from [accent]@,@[] to [accent]@,@[].", count, block.name,
                     x, y, xn, yn);
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
      if (count > 0) Players.ok(player, "Succesfully applied custom data to [accent]@[] buildings.", count);
      if (errors > 0) Players.err(player, "Failed to apply custom data to [accent]@[] buildings: \n@", errors,
                                  Strings.neatError(lastError, false));
      if (count > 0) {
        sendBlockSnapshot();
        //sendEntitySnapshot();
      }
    });

    handler.addAdmin("core", "<small|medium|big|coreName> [player|x,y] [teamName|~...]", "Build a core.",
    (args, player) -> {
      if (player.vanished()) {
        Players.err(player, "You can't build a core in vanish mode!");
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
            Players.err(player, "No core named '@' found.", args[0]);
            return;
          }
      }
      if (core == null) {
        Players.err(player, "Unable to find a core evolution for '[orange]@[]' based on the current map.", args[0]);
        return;
      }

      Tile tile = player.player.tileOn();
      Team team = player.player.team();
      if (args.length > 1) {
        CoordinatesParser dest = CoordinatesParser.parse(player, args, 1, args.length);
        if (dest == null) return; // Error already send to player
        args = dest.rest;

        tile = Vars.world.tileWorld(dest.pos.x, dest.pos.y);

        if (args.length > 0 && (team = Modules.team.getTeam(player, args[0])) == null) {
          Players.err(player, "Team not found. [lightgray]Use [gray]/team[] to list them.");
          return;
        }
      } else if (player.player.dead()) {
        Players.err(player, "Unable to find player position.");
        return;
      }

      if (tile == null) {
        Players.err(player, "Coordinates out of map bounds.");
        return;
      }
      ConstructBlock.constructed(tile, core, player.player.unit(), (byte)0, team, null);
      Players.ok(player, "Built a [accent]@[] at [accent]@,@[] for the [white]@[] team.", core.name, tile.x, tile.y,
                 team.coloredName());
    });

    handler.addAdmin("spawn", "<unit> [count] [player|x,y] [teamName|~] [unitData...]", "Spawn a unit.",
    (args, player) -> {
      UnitType unit = Vars.content.unit(Strings.kebabize(args[0]));
      if (unit == null) {
        Players.err(player, "No unit named '[orange]@[]' found.", args[0]);
        return;
      }

      int count = 1;
      if (args.length > 1) {
        count = Strings.parseInt(args[1]);
        if (count < 1) {
          Players.err(player, "'[orange]count[]' must be a number greater than [orange]1[].");
          return;
        }
      }

      Position pos = player.player;
      Team team = player.player.team();
      JsonValue data = null;
      if (args.length > 2) {
        CoordinatesParser dest = CoordinatesParser.parse(player, args, 2, args.length);
        if (dest == null) return; // Error already send to player
        args = dest.rest;

        pos = dest.pos;

        if (args.length > 0 && (team = Modules.team.getTeam(player, args[0])) == null) {
          Players.err(player, "Team not found. [lightgray]Use [gray]/team[] to list them.");
          return;
        }

        if (args.length > 1) {
          data = parseJson(player, args, 1, args.length);
          if (!checkUnitData(player, unit, team, data)) return;
        }
      } else if (player.player.dead()) {
        Players.err(player, "Unable to find player position.");
        return;
      }

      if (team.cores().isEmpty()) {
        Players.err(player, "No core available in the [white]@[] team.", team.coloredName());
        return;
      }

      Seq<Unit> spawned = new Seq<>(Math.min(count, Units.getCap(team)));
      while (count-- > 0) {
        Unit u = unit.spawn(pos, team);
        if (u.isValid()) spawned.add(u);
      }
      Players.ok(player, "Spawned [accent]@ @[] at [accent]@,@[] [gray]([lightgray]@[],[lightgray]@[])[] "
                       + "for the [white]@[] team.",
                 spawned.size, unit.name, World.toTile(pos.getX()), World.toTile(pos.getY()), (int)pos.getX(),
                 (int)pos.getY(), team.coloredName());

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
      if (count > 0) Players.ok(player, "Succesfully applied custom data to [accent]@[] units.", count);
      if (errors > 0) Players.err(player, "Failed to apply custom data to [accent]@[] units: \n@", errors,
                                  Strings.neatError(lastError, false));
    });

    handler.addAdmin("transform", "<unit> [player|selector] [unitData...]", "Transform a player unit.",
    (args, player) -> {
      UnitType unit = Vars.content.unit(Strings.kebabize(args[0]));
      if (unit == null) {
        Players.err(player, "No unit named '[orange]@[]' found.", args[0]);
        return;
      } else if (player.player.team().cores().isEmpty()) {
        Players.err(player, "No core available in the [white]@[] team.", player.player.team().coloredName());
        return;
      }

      String pronoun = Strings.aOrAn(unit.name);

      if (args.length == 1) {
        if (transformPlayer(player, unit) != null)
          Players.ok(player, "Transformed to @ [accent]@[].", pronoun, unit.name);
        else Players.err(player, "Unable to transform you to @ [orange]@[]. Unit cap reached?", pronoun, unit.name);
        return;
      }

      SelectorParser selector = Modules.selector.parse(player, args, 1, args.length, true);
      if (selector == null) return;

      JsonValue data = null;
      if (selector.rest.length > 0) {
        data = parseJson(player, selector.rest, 0, selector.rest.length);
        if (!checkUnitData(player, unit, player.player.team(), data)) return;
      }

      Seq<Unit> transformed = new Seq<>(selector.selected.size);
      selector.execute((p, u) -> {
        transformed.add(u = transformPlayer(p, unit));
        if (u == null || player == p) return;
        Players.warn(p, "You have been transformed to @ [accent]@[] by @[orange].", pronoun, unit.name,
                     player.getName());
      });
      int invalids = transformed.count(u -> u == null);
      selector.selected.set(transformed.removeAll(u -> u == null));
      Players.ok(player, selector.formatMessage("Transformed", true) + "[green].");
      if (invalids > 0)
        Players.err(player, "Unable to transform [orange]@ players[] to @ [orange]@[].  Unit cap reached?", invalids,
                    pronoun, unit.name);

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
      if (count > 0) Players.ok(player, "Succesfully applied custom data to [accent]@[] units.", count);
      if (errors > 0) Players.err(player, "Failed to apply custom data to [accent]@[] units: \n@", errors,
                                  Strings.neatError(lastError, false));
    });

    handler.addAdmin("kill", "[player|selector...] ", "Kill a player or a unit.", (args, player) -> {
      if (args.length == 0) {
        if (!player.player.dead()) player.player.unit().kill();
        Players.ok(player, "Killed [accent]yourself[].");
        return;
      }

      SelectorParser selector = Modules.selector.parse(player, args);
      if (selector == null) return;

      selector.execute((p, u) -> {
        u.kill();
        if (p != null && p != player) Players.warn(p, "You have been killed by @[orange].", player.getName());
      });
      Players.ok(player, selector.formatMessage("Killed", true) + "[green].");
    });

    handler.addAdmin("clear-map", "[hard|y|n]", "Kill all units and blocks, except cores, on the map.",
    (args, player) -> {
      if (cleanupTriggerer != null) {
        if (cleanupTriggerer != player) {
          Players.err(player, "@[scarlet] already triggered a cleanup. \n"
                            + "He/She must to confirm/cancel it or wait a little before auto cancellation.",
                      cleanupTriggerer.getName());
          return;
        } else if (args.length == 0) {
          Players.err(player, "Please confirm, or not, the operation. "
                            + "Use [orange]/clear-map y[] or [orange]/clear-map n[].");
          return;
        }

        // Manual check to only accept "yes" or "no" replies
        switch (args[0]) {
          case "y": case "yes":
            Players.ok(player, "Cleaning map.");
            confirmMapClean(true);
            return;
          case "n": case "no":
            cancelMapCleanConfirmation();
            return;
          default:
            Players.err(player, "Invalid argument! Must be 'y' or 'n'.");
            return;
        }
      }

      boolean hard = false;
      if (args.length == 1 && !(hard = args[0].equals("hard"))) {
        Players.err(player, "No cleaning is awaiting confirmation.");
        return;
      }

      scheludeMapCleanConfirmation(player, hard, 10); // 10 seconds before auto cancellation
      Players.info(player, "\nAre you sure to start a map cleanup? [orange]This can produce lot of lags[].\n"
                         + "Use [accent]/clear-map y[] or [accent]/clear-map n[] to confirm or cancel the operation.");
      Players.warn(player, "The operation will be automatically canceled after 10 seconds.");
    });

    handler.addAdmin("weather", "[clear|weatherName] [intensity] [inf|duration]", "Control map weather.",
    (args, player) -> {
      if (args.length == 0) {
        StringBuilder builder = new StringBuilder();
        if (Groups.weather.isEmpty()) {
          builder.append("No current weathers");
        } else {
          Seq<Weather> weathers = new Seq<>();
          Groups.weather.each(w -> weathers.add/*Unique*/(w.weather));
          builder.append("Current weathers are: ").append(weathers.toString(", ", w -> "[accent]" + w.name + "[]"));
        }
        builder.append("\nAvailable are: [accent]clear[], ")
               .append(Vars.content.weathers().toString(", ", w -> "[accent]" + w.name + "[]"));
        Players.info(player, builder.toString());
        return;

      } else if (args[0].equals("clear")) {
        // Set life time to 1 and re-sync entities instead of simply clearing weathers.
        Groups.weather.each(w -> w.life(1f));
        // Weathers are duplicated for the client, we need to resync all entities
        sendWorld(); //sendEntitySnapshot();
        Players.ok(player, "Removed all weather status.");
        return;
      }

      Weather weather = Vars.content.weather(Strings.kebabize(args[0]));
      if (weather == null) {
        Players.err(player, "No weather named '[orange]@[]' found.", args[0]);
        return;
      }

      int intensity = 100;
      float duration = weather.duration / 60f;
      if (args.length > 1) {
        intensity = Strings.parseInt(args[1]);
        if (intensity < 0 || intensity > 100) {
          Players.err(player, "'[orange]intensity[]' must be a number between [orange]0[] and [orange]100[].");
          return;
        }
      }

      if (args.length > 2) {
        duration = args[2].equals("inf") ? Float.POSITIVE_INFINITY : Strings.parseInt(args[2]);
        if (duration < 0) {
          Players.err(player, "'[orange]duration[]' must be a positive number of seconds, "
                            + "or '[orange]inf[]' for an infinite duration.");
          return;
        }
      }

      weather.create(intensity / 100f, duration * 60f);
      if (duration == Float.POSITIVE_INFINITY)
        Players.ok(player, "Weather [accent]@[] created with [accent]@%[] of intensity [accent]forever[].",
                   weather.name, intensity);
      else
        Players.ok(player, "Weather [accent]@[] created with [accent]@%[] of intensity for [accent]@[].",
                   weather.name, intensity, DurationFormatter.format((long)(duration * 1000)));
    });

    //IDEA: /time, /patch
  }
}
