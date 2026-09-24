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

import mindustry.gen.Call;
import mindustry.gen.Unit;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.ClientCommandHandler;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.modules.selector.*;
import fr.zetamap.morecommands.modules.selector.util.CoordinatesParser;
import fr.zetamap.morecommands.modules.selector.util.TargetResult;


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
    handler.addAdmin("tp", "<player|selector|src-x,y> [player|selector|dest-x,y...]",
                     "Teleport to a location or player.", (args, player) -> {
      // First, parse manually, to check arguments.
      // Coordinates cannot be at the first, and selector cannot target more than once at the second.
      boolean withSrc = false;
      try {
        boolean coords = false;
        String[] rest;
        if (Selectors.isSelector(args[0])) {
          if (!Modules.selector.enabled()) {
            player.err("Selectors are disabled, you cannot use them.");
            return;
          }
          rest = new SelectorParser(player, args).rest;
        } else {
          CoordinatesParser coord = new CoordinatesParser(player, args);
          rest = coord.rest;
          coords = coord.byCoordinates;
        }

        if (rest.length > 0) {
          if (coords) {
            player.err("Cannot use coordinates as a source. \nUsage: @ or @.",
                       "/tp <player|selector|x,y>", "/tp <player|selector> <player|selector|x,y>");
            return;
          }
          withSrc = true;
        }
      } catch (Exception e) {
        Modules.selector.error(player, e);
        return;
      }


      if (withSrc) {
        SelectorParser selector = Modules.selector.parse(player, args);
        if (selector == null) return;
        TargetResult dest = Modules.selector.parseOne(player, selector.rest);
        if (dest == null) return;
        selector.execute((p, u) -> {
          if (p != null) {
            teleport(p, dest.pos);
            if (p == player) return;
            p.warn("You have been teleported to @ by @.", dest.formatColors(p), player.getName());
          } else teleport(u, dest.pos);
        });
        player.ok("@ to @.", selector.formatColorMessage("Teleported"), dest.formatColors());

      } else {
        TargetResult dest = Modules.selector.parseOne(player, args);
        if (dest == null) return;
        if (!player.player.dead()) {
          teleport(player, dest.pos);
          player.ok("You teleported to @.", dest.formatColors());
        } else player.err("Unable to locate @ position.", "your");
      }
    });

    //IDEA: /tpa, /tpahere, /tpaccept, /tpdeny.
  }
}
