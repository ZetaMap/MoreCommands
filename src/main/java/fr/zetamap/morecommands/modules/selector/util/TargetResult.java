/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2026  ZetaMap
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

package fr.zetamap.morecommands.modules.selector.util;

import arc.math.geom.Point2;
import arc.math.geom.Position;

import mindustry.gen.Unit;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.modules.selector.SelectorModule;
import fr.zetamap.morecommands.util.Strings;


//TODO: idk how to name it.
/** Only used by {@link SelectorModule#parseOne()}. */
public class TargetResult {
  public final PlayerData executor, target;
  public final Unit unit;
  public final Position pos;
  public final Point2 wpos;
  public final boolean byPos, bySelector, byPlayer;
  public final String[] rest;
  public TargetResult(PlayerData executor, PlayerData target, Unit unit, Position pos, Point2 wpos, boolean byPos,
                      boolean bySelector, boolean byPlayer, String[] rest) {
    this.executor = executor;
    this.target = target;
    this.unit = unit;
    this.pos = pos;
    this.wpos = wpos;
    this.byPos = byPos;
    this.bySelector = bySelector;
    this.byPlayer = byPlayer;
    this.rest = rest;
  }

  //TODO: return selected unit coordinates instead of it's name?
  public String format() { return format(executor); }
  public String format(PlayerData executor) {
    return byPos ? wpos.x + "," + wpos.y + " (" + (int)pos.getX() + "," + (int)pos.getY() + ")"
         : bySelector ?
           unit.isPlayer() ?
             PlayerData.get(unit) == executor ? "yourself"
             : PlayerData.get(unit).stripedName
           : Strings.articleFor(unit.type.name) + " " + unit.type.name
         : byPlayer ?
             target == executor ? "yourself"
           : target.stripedName
         :"<unknown>";
  }

  public String formatColors() { return formatColors(executor); }
  public String formatColors(PlayerData executor) {
    return byPos ? "[accent]" + wpos.x + "," + wpos.y + "[] [gray]([lightgray]" +
                   (int)pos.getX() + "[],[lightgray]" + (int)pos.getY() + "[])[]"
         : bySelector ?
             target != null ? target.getName()
           : unit.isPlayer() ?
             PlayerData.get(unit) == executor ? "[accent]yourself[]"
             : PlayerData.get(unit).getName()
           : Strings.articleFor(unit.type.name) + " [accent]" + unit.type.name + "[]"
         : byPlayer ?
             target == executor ? "[accent]yourself[]"
           : target.getName()
         : "[lightgray]<unknown>[]";
  }
}
