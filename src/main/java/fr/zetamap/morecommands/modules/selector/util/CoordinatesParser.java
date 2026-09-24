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

package fr.zetamap.morecommands.modules.selector.util;

import java.util.Arrays;

import arc.math.geom.*;

import mindustry.Vars;
import mindustry.core.World;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.util.Strings;


/**
 * Minecraft-like coordinates parser, but coordinates are separated with a comma instead of a space,
 * and a single {@code '~'} can be used to specify both x and y axis.
 */
public class CoordinatesParser {
  public static final char worldRelativePrefix = '~', separator = ',';

  public final PlayerData executor, target;
  public final Vec2 pos;
  public final Point2 wpos;
  public final boolean byCoordinates;
  public final String[] rest;

  public CoordinatesParser(PlayerData executor, String[] args) throws IllegalArgumentException {
    this(executor, args, 0, args.length);
  }

  public CoordinatesParser(PlayerData executor, String[] args, int from, int to) throws IllegalArgumentException {
    if (Strings.checkStringArray(args, from, to))
      throw new IllegalArgumentException("Missing coordinates or player name/uuid");

    this.executor = executor;
    String coor = args[from];
    int length = coor.length();

    // Avoid to search a player if it's looks like coordinates
    if (!isRelativeCoordinate(coor, 0, length)) {
      Players.SearchResult result = Players.find(args, from, to);
      if (result.found) {
        if (result.player.player.dead())
          throw new IllegalArgumentException("Unable to find target player position");
        target = result.player;
        pos = new Vec2().set(result.player.player);
        wpos = toWorld(pos);
        byCoordinates = false;
        rest = result.rest;
        return;
      }
    } else if (executor == null) throw new IllegalArgumentException("Unable to find player position");

    int comma = coor.indexOf(separator);
    float x, y;

    if (comma != -1) {
      x = parseCoordinate(executor, coor, 0, comma == -1 ? length : comma, false);
      if (comma == length-1) throw new IllegalArgumentException("Missing 'y' axis after comma");
      y = parseCoordinate(executor, coor, comma+1, length, true);
    } else if (isRelativeCoordinate(coor, 0, length)) {
      if (executor.player.dead()) throw new IllegalArgumentException("Unable to find player position");
      x = y = parseWorldCoordinate(coor, 1, length);
      x += executor.player.x;
      y += executor.player.y;
    } else if (Strings.parseInt(coor, 10, Integer.MIN_VALUE, 0, comma) != Integer.MIN_VALUE)
         throw new IllegalArgumentException("Missing 'y' axis");
    else throw new IllegalArgumentException("Invalid coordinates or player not found");


    target = executor;
    pos = new Vec2(x, y);
    wpos = toWorld(pos);
    byCoordinates = true;
    rest = Arrays.copyOfRange(args, from+1, to);
  }

  private static boolean isRelativeCoordinate(String arg, int from, int to) {
    return arg.charAt(from) == worldRelativePrefix;
  }

  private static float parseCoordinate(PlayerData executor, String arg, int from, int to, boolean isY) {
    if (to <= from) throw new IllegalArgumentException("Invalid coordinates or player not found");
    if (isRelativeCoordinate(arg, from, to)) {
      // Check whether the player is dead, because relative coordinates will be wrong
      if (executor == null || executor.player.dead())
        throw new IllegalArgumentException("Unable to find player position");
      return (isY ? executor.player.y : executor.player.x) + parseWorldCoordinate(arg, from+1, to);
    }
    return parseWorldCoordinate(arg, from, to);
  }

  private static int parseWorldCoordinate(String arg, int from, int to) {
    if (to <= from) return 0;
    int offset = Strings.parseInt(arg, 10, Integer.MIN_VALUE, from, to);
    if (offset == Integer.MIN_VALUE) throw new IllegalArgumentException("Invalid coordinates or player not found");
    return offset * Vars.tilesize; // scale
  }

  public static Point2 toWorld(Position pos) {
    return new Point2(World.toTile(pos.getX()), World.toTile(pos.getY()));
  }
}