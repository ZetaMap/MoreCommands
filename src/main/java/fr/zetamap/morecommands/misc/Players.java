/*
 * This file is part of Player Follow. The plugin that allow players to follow each others.
 *
 * MIT License
 *
 * Copyright (c) 2024-2025 ZetaMap
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fr.zetamap.morecommands.misc;

import mindustry.gen.Player;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.util.Strings;


public class Players {
  public static void errPlayerNotFound(PlayerData player) { errPlayerNotFound(player.player); }
  public static void errPlayerNotFound(Player player) { err(player, "Player not found!"); }
  public static void errArgUseDenied(PlayerData player) { errArgUseDenied(player.player); }
  public static void errArgUseDenied(Player player) { err(player, "You are not allowed to use this argument!"); }
  public static void errCommandUseDenied(PlayerData player) { errCommandUseDenied(player.player); }
  public static void errCommandUseDenied(Player player) { err(player, "You are not allowed to use this command!"); }

  public static void err(PlayerData player, String message) { err(player.player, message); }
  public static void err(PlayerData player, String message, Object... args) { err(player.player, message, args); }
  public static void err(Player player, String message) { player.sendMessage("[scarlet]" + message); }
  public static void err(Player player, String message, Object... args) { player.sendMessage("[scarlet]" + Strings.format(message, args)); }
  
  public static void info(PlayerData player, String message) { info(player.player, message); }
  public static void info(PlayerData player, String message, Object... args) { info(player.player, message, args); }
  public static void info(Player player, String message) { player.sendMessage(message); }
  public static void info(Player player, String message, Object... args) { player.sendMessage(Strings.format(message, args)); }
  
  public static void warn(PlayerData player, String message) { warn(player.player, message); }
  public static void warn(PlayerData player, String message, Object... args) { warn(player.player, message, args); }
  public static void warn(Player player, String message) { player.sendMessage("[orange]" + message); }
  public static void warn(Player player, String message, Object... args) { player.sendMessage("[orange]" + Strings.format(message, args)); }
  
  public static void ok(PlayerData player, String message) { ok(player.player, message); }
  public static void ok(PlayerData player, String message, Object... args) { ok(player.player, message, args); }
  public static void ok(Player player, String message) { player.sendMessage("[green]" + message); }
  public static void ok(Player player, String message, Object... args) { player.sendMessage("[green]" + Strings.format(message, args)); }
  
  public static SearchResult findByName(String[] args) { return findByName(String.join(" ", args)); }
  public static SearchResult findByName(String[] args, int from, int to) { return findByName(Strings.join(" ", args, from, to)); }
  /** 
   * Tries to find a player by their name. (sorted by most larger name first to avoid non-targatable players) <br>
   * Non-targatable players are players that includes a command argument or information of another player
   * at end of his nickname, to not be targeted by commands.
   */
  public static SearchResult findByName(String arg) {
    String args = Strings.normalize(arg) + " ";
    // Sort descending
    PlayerData.sort((p1, p2) -> Integer.compare(p2.stripedName.length(), p1.stripedName.length()));
    PlayerData target = PlayerData.find(p -> args.startsWith(p.stripedName + " "));

    //TODO: avoid to removes colors to the rest of arguments
    return new SearchResult(target, (target == null ? arg : args.substring(target.stripedName.length()).strip()).split(" "));
  }
  
  public static SearchResult findByID(String arg) { return findByID(arg.split(" ")); }
  public static SearchResult findByID(String[] args) { return findByID(args, 0, args.length); }
  /** Tries to find a player by their unitID (a unitID looks like this: #000001) */
  public static SearchResult findByID(String[] args, int from, int to) { 
    if (args.length == 0 || from < 0 || to > args.length || from >= to) return new SearchResult(null, args);
    if (args[from].length() < 2 || args[from].charAt(0) != '#') return new SearchResult(null, copyIfNeeded(args, from, to));
    int id = Strings.parseInt(args[from], 10, Integer.MIN_VALUE, 1, args[from].length());
    return id == Integer.MIN_VALUE ? new SearchResult(null, copyIfNeeded(args, from, to)) :
           new SearchResult(PlayerData.id(id), copyIfNeeded(args, from+1, to)); 
  }
  
  public static SearchResult findByUUID(String arg) { return findByUUID(arg.split(" ")); }
  public static SearchResult findByUUID(String[] args) { return findByUUID(args, 0, args.length); }
  /** Tries to find a player by their UUID or shortUUID */
  public static SearchResult findByUUID(String[] args, int from, int to) { 
    if (args.length == 0 || from < 0 || to > args.length || from >= to) return new SearchResult(null, args);
    if (args[from].length() < 10 || args[from].length() > 25) return new SearchResult(null, copyIfNeeded(args, from, to));
    return new SearchResult(PlayerData.find(p -> p.player.uuid().equals(args[from]) || p.shortUuid.equals(args[from])), 
                            copyIfNeeded(args, from+1, to)); 
  }
  
  public static SearchResult find(String arg) { return find(arg.split(" ")); }
  public static SearchResult find(String[] args) { return find(args, 0, args.length); }
  /** 
   * General function to find a player. <br>
   * First, will try to find by name, to avoid non-targatable players. 
   * (more infos in {@link #findByName(String)} <br>
   * After, by the unitID (like #012345). <br>
   * And finally, by the player UUID.
   */
  public static SearchResult find(String[] args, int from, int to) { 
    args = copyIfNeeded(args, from, to);// avoid multiple copy
    SearchResult target = Players.findByName(args); 
    return target.found ? target : (target = Players.findByID(args)).found ? target : Players.findByUUID(args);
  }
  
  protected static String[] copyIfNeeded(String[] original, int from, int to) {
    return from != 0 || to != original.length ? java.util.Arrays.copyOfRange(original, from, to) : original;
  }

  
  public static class SearchResult {
    public final PlayerData player;
    public final String[] rest;
    public final boolean found;
    
    public SearchResult(PlayerData player, String[] rest) {
      this.player = player;
      // In case of
      this.rest = rest.length == 1 && rest[0].isEmpty() ? new String[0] : rest;
      this.found = player != null;
    }
  }
}
