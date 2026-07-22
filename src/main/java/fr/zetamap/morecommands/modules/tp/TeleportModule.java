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

package fr.zetamap.morecommands.modules.tp;

import arc.math.geom.Position;

import mindustry.core.World;
import mindustry.gen.Call;
import mindustry.gen.Unit;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.ClientCommandHandler;
import fr.zetamap.morecommands.misc.CoordinatesParser;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.modules.selector.SelectorParser;
import fr.zetamap.morecommands.modules.selector.Selectors;


public class TeleportModule extends AbstractModule {
  public void teleport(PlayerData player, Position pos) {
    player.player.set(pos);
    player.player.snapInterpolation();
    if (!player.player.dead()) {
      player.player.unit().set(pos);
      player.player.unit().snapInterpolation();
    }
    Call.setPosition(player.player.con, pos.getX(), pos.getY());
  }

  public void teleport(Unit unit, Position pos) {
    unit.set(pos);
    unit.snapInterpolation();
    if (!unit.isPlayer()) return;
    unit.getPlayer().set(pos);
    unit.getPlayer().snapInterpolation();
    Call.setPosition(unit.getPlayer().con, pos.getX(), pos.getY());
  }

  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    handler.addAdmin("tp", "<player|selector|src-x,y> [player|dest-x,y...]", "Teleport to a location or player.",
    (args, player) -> {
      if (args[0].isEmpty()) Players.err(player, "Missing player, coordinates or selector.");

      else if (Selectors.isSelector(args[0])) {
        SelectorParser selector = Modules.selector.parse(player, args);
        if (selector == null) return; // Error already send to player
        CoordinatesParser dest = CoordinatesParser.parse(player, selector.rest);
        if (dest == null) return; // Error already send to player

        int tx = World.toTile(dest.pos.x), ty = World.toTile(dest.pos.y), x = (int)dest.pos.x, y = (int)dest.pos.y;
        selector.execute((p, u) -> {
          if (p != null) {
            teleport(p, dest.pos);
            if (p == player) return;
            if (dest.byCoordinates)
              Players.warn(p, "You have been teleported to [accent]@,@[] [gray]([lightgray]@[],[lightgray]@[])[] by @[orange].",
                           tx, ty, x, y, player.getName());
            else Players.warn(p, "You have been teleported to @[orange] by @[orange].", dest.target.getName(), player.getName());
          } else teleport(u, dest.pos);
        });
        if (dest.byCoordinates)
          Players.ok(player, "@[green] to [accent]@,@[] [gray]([lightgray]@[],[lightgray]@[])[].",
                     selector.formatMessage("Teleported", true), tx, ty, x, y);
        else Players.ok(player, "@[green] to @[green].", selector.formatMessage("Teleported", true), dest.target.getName());

      } else {
        CoordinatesParser src = CoordinatesParser.parse(player, args);
        if (src == null) return; // Error already send to player
        else if (src.byCoordinates && src.rest.length > 0)
          Players.err(player, "Too many arguments. "
                            + "Usage: [orange]/tp <player|x,y>[] or [orange]/tp <player|selector> <player|x,y>[].");

        else if (src.rest.length > 0) {
          CoordinatesParser dest = CoordinatesParser.parse(player, src.rest);
          if (dest == null) return; // Error already send to player

          teleport(src.target, dest.pos);
          if (dest.byCoordinates) {
            int tx = World.toTile(dest.pos.x), ty = World.toTile(dest.pos.y), x = (int)dest.pos.x, y = (int)dest.pos.y;
            Players.ok(player, "Teleported @[green] to [accent]@,@[] [gray]([lightgray]@[],[lightgray]@[])[].",
                       src.target.getName(), tx, ty, x, y);
            Players.warn(src.target, "You have been teleported to [accent]@,@[] [gray]([lightgray]@[],[lightgray]@[])[] by @[orange].",
                         tx, ty, x, y, player.getName());
          } else {
            Players.ok(player, "Teleported @[green] to @[green].", src.target.getName(), dest.target.getName());
            Players.warn(src.target, "You have been teleported to @[orange] by @[orange].", dest.target.getName(),
                         player.getName());
          }

        } else if (player.player.dead()) {
          Players.err(player, "Unable to find player position.");
        } else {
          teleport(player, src.pos);
          if (src.byCoordinates) {
            int tx = World.toTile(src.pos.x), ty = World.toTile(src.pos.y), x = (int)src.pos.x, y = (int)src.pos.y;
            Players.ok(player, "You teleported to @,@ [gray]([lightgray]@[],[lightgray]@[])[].", tx, ty, x, y);
          } else Players.ok(player, "You teleported to @[green].", src.target.getName());
        }
      }
    });

    //IDEA: /tpa, /tpahere, /tpaccept, /tpdeny.
  }
}
