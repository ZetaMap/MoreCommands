/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2021-2025  ZetaMap
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

package fr.zetamap.morecommands.misc;

import java.util.regex.Pattern;

import arc.math.geom.Vec2;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.util.Strings;


/** 
 * Minecraft-like coordinates parser, but coordinates are separated with a comma instead of a space, 
 * and a single {@code '~'} can be used to specify both x and y axis.
 */
public class CoordinatesParser {
  private static final Pattern quotes = Pattern.compile("'(.*?)'");
  public static final char worldRelativePrefix = '~', separator = ',';
  
  public final PlayerData target;
  public final Vec2 pos = new Vec2();
  public final boolean byCoordinates;
  public final String[] rest;
  
  public CoordinatesParser(PlayerData executor, String[] args) throws IllegalArgumentException { 
    this(executor, args, 0, args.length); 
  }
  
  public CoordinatesParser(PlayerData executor, String[] args, int from, int to) throws IllegalArgumentException {
    if (args.length == 0 || from < 0 || to > args.length || from >= to || args[from].isEmpty()) 
      throw new IllegalArgumentException("Missing coordinates or player name/uuid");
    
    String coor = args[from];
    int length = coor.length();
    
    // Avoid to search a player if it's looks like coordinates
    if (!isRelativeCoordinate(coor, 0, length)) {
      Players.SearchResult result = Players.find(args, from, to);
      if (result.found) {
        if (result.player.player.dead())
          throw new IllegalArgumentException("Unable to find target player position");
        target = result.player;
        pos.set(result.player.player);
        byCoordinates = false;
        rest = result.rest;
        return;
      }
    }

    int comma = coor.indexOf(separator);
    float x, y;
    
    if (comma != -1) {
      x = parseCoordinate(executor, coor, 0, comma == -1 ? length : comma, executor.player.x);
      if (comma == length-1) throw new IllegalArgumentException("Missing 'y' axis after comma");
      y = parseCoordinate(executor, coor, comma+1, length, executor.player.y);
    } else if (isRelativeCoordinate(coor, 0, length)) {
      if (executor.player.dead()) throw new IllegalArgumentException("Unable to find player position");
      x = y = parseWorldCoordinate(coor, 1, length);
      x += executor.player.x;
      y += executor.player.y;
    } else {
      if (Strings.parseInt(coor, 10, Integer.MIN_VALUE, 0, comma) != Integer.MIN_VALUE)
           throw new IllegalArgumentException("Missing 'y' axis");
      else throw new IllegalArgumentException("Invalid coordinates or player not found");
    }
         
    
    target = executor;
    pos.set(x, y);
    byCoordinates = true;
    rest = java.util.Arrays.copyOfRange(args, from+1, to);
  }

  private static boolean isRelativeCoordinate(String arg, int from, int to) {
    return arg.charAt(from) == worldRelativePrefix;
  }
  
  private static float parseCoordinate(PlayerData executor, String arg, int from, int to, float base) {
    if (to <= from) throw new IllegalArgumentException("Invalid coordinates or player not found");
    if (isRelativeCoordinate(arg, from, to)) {
      // Check whether the player is dead, because relative coordinates will be wrong
      if (executor.player.dead()) throw new IllegalArgumentException("Unable to find player position");
      return base + parseWorldCoordinate(arg, from+1, to);
    }
    return parseWorldCoordinate(arg, from, to);
  }
  
  private static float parseWorldCoordinate(String arg, int from, int to) {
    if (to <= from) return 0f;
    int offset = Strings.parseInt(arg, 10, Integer.MIN_VALUE, from, to);
    if (offset == Integer.MIN_VALUE) throw new IllegalArgumentException("Invalid coordinates or player not found");
    return offset * mindustry.Vars.tilesize; // scale
  }
  
  public static CoordinatesParser parse(PlayerData executor, String[] args) { return parse(executor, args, 0, args.length); }
  public static CoordinatesParser parse(PlayerData executor, String[] args, int from, int to) {
    try { return new CoordinatesParser(executor, args, from, to); }
    catch (Exception e) {
      String message = e.getMessage();
      if (message == null || message.isEmpty()) message = e.getClass().getSimpleName();
      else {
        if (message.charAt(message.length()-1) != '.') message += '.';
        message = quotes.matcher(message).replaceAll("'[orange]$1[]'");
      }
      Players.err(executor, message); 
    }
    return null;
  }
}