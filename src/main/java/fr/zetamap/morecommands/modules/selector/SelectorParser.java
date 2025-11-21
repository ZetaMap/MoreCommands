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

package fr.zetamap.morecommands.modules.selector;

import java.util.regex.Pattern;

import arc.func.Cons2;
import arc.struct.ObjectMap;
import arc.struct.Seq;

import mindustry.gen.Unit;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.util.StringReader;
import fr.zetamap.morecommands.util.Strings;


public class SelectorParser {
  private static final Pattern quotes = Pattern.compile("'(.*?)'");
  
  public final PlayerData executor, target;
  public final Selector selector;
  public final boolean onlyPlayers, byPlayer;
  public final ObjectMap<String, SelectorProperty.Parsed> properties;
  public final Seq<Unit> selected;
  public final String[] rest;

  public SelectorParser(PlayerData executor, String[] args, boolean onlyPlayers) 
  throws IllegalArgumentException, StringReader.ParseException { 
    this(executor, args, 0, args.length, onlyPlayers); 
  }
  
  //TODO: want one
  public SelectorParser(PlayerData executor, String[] args, int from, int to, boolean onlyPlayers) 
  throws IllegalArgumentException, StringReader.ParseException {
    if (args.length == 0 || from < 0 || to > args.length || from >= to || args[from].isEmpty()) 
      throw new IllegalArgumentException("Missing player name/uuid or selector");
    
    this.executor = executor;
    this.onlyPlayers = onlyPlayers;
    
    if (!Selectors.isSelector(args[from])) {
      Players.SearchResult result = Players.find(args, from, to);
      byPlayer = true;
      selector = null;
      properties = null;
      selected = null;
      if (result.found) {
        target = result.player;
        rest = result.rest;
      } else {
        target = null;
        rest = args;
      }
      return;
    }
    
    target = null;
    byPlayer = false;
    StringReader reader = new StringReader(Strings.join(" ", args, from, to));
    reader.readNext(); // skip prefix
    String selec = reader.readWord(false);
    if (selec == null) throw reader.expected("a selector name");
    selector = Selectors.get(selec);
    if (selector == null) throw reader.notFound("selector", selec);
    
    properties = reader.readArraySet(SelectorParser::readKey, SelectorParser::readValue, "property name");

    selected = selector.select(executor == null ? null : executor.player, 
                               properties == null ? null : properties.values().toSeq());
    if (onlyPlayers && !selected.allMatch(Unit::isPlayer)) 
      throw new IllegalArgumentException("The selector is targeting non-player units, but only players are expected");

    rest = reader.toString().strip().split(" ");
  }
  
  public boolean noTargetFound() {
    return target == null && (selector == null || selected.isEmpty());
  }
  
  public String formatMessage(String verb) { return formatMessage(verb, false); }
  public String formatMessage(String verb, boolean addColors) {
    StringBuilder builder = new StringBuilder();
    builder.append(verb).append(' ');
    
    if (target != null) {
      if (target == executor) {
        if (addColors) builder.append("[accent]");
        builder.append("yourself");
        if (addColors) builder.append("[]");
      } else builder.append(target.getName());

    } else if (selected.isEmpty()) {
      builder.setLength(0);
      builder.append("No ");
      if (!onlyPlayers) builder.append("units or ");
      builder.append("players was ").append(verb.toLowerCase());
      
    } else if (selected.size == 1) {
      Unit unit = selected.first();
      
      if (unit.isPlayer()) {
        PlayerData player = PlayerData.get(unit.getPlayer());
        if (player == null) builder.append(unit.getPlayer().name); // In case of
        else if (player != executor) builder.append(player.getName());
        else {
          if (addColors) builder.append("[accent]");
          builder.append("yourself");
          if (addColors) builder.append("[]");
        }        
      } else {
        builder.append(Strings.aOrAn(unit.type.name)).append(' ');
        if (addColors) builder.append("[accent]");
        builder.append(unit.type.name);
        if (addColors) builder.append("[]");
      }

    } else {
      int players = selected.count(Unit::isPlayer), units = selected.size - players;
      if (players > 0) {
        if (addColors) builder.append("[accent]");
        builder.append(players).append(" player");
        if (players > 1) builder.append('s');
        if (addColors) builder.append("[]");
      }
      if (players > 0 && players < selected.size) builder.append(" and ");
      if (units > 0) {
        if (addColors) builder.append("[accent]");
        builder.append(units).append(" unit");
        if (units > 1) builder.append('s');
        if (addColors) builder.append("[]");
      }      
    }

    return builder.toString();
  }
  
  public void execute(Cons2<PlayerData, Unit> consumer) {
    if (target != null) consumer.get(target, target.player.unit());
    else selected.each(u -> consumer.get(PlayerData.get(u.getPlayer()), u));
  }
  
  private static String readKey(StringReader reader) {
    return (StringReader.isQuote(reader.peek()) ? reader.readQuotedString(false) : reader.readUntil('=')).strip();
  }
  
  private static SelectorProperty.Parsed readValue(StringReader reader, String key) {
    SelectorProperty property = SelectorProperties.get(key);
    if (property == null) throw reader.notFound("property", key);
    return property.read(reader);
  }
  
  public static SelectorParser parse(PlayerData executor, String[] args) { 
    return parse(executor, args, 0, args.length, false); 
  }
  public static SelectorParser parse(PlayerData executor, String[] args, boolean onlyPlayers) { 
    return parse(executor, args, 0, args.length, onlyPlayers); 
  }
  public static SelectorParser parse(PlayerData executor, String[] args, int from, int to) {
    return parse(executor, args, from, to, false);
  }
  
  public static SelectorParser parse(PlayerData executor, String[] args, int from, int to, boolean onlyPlayers) {
    try { 
      SelectorParser p = new SelectorParser(executor, args, from, to, onlyPlayers);
      if (p.byPlayer && p.noTargetFound()) Players.errPlayerNotFound(executor);
      else return p;
    } catch (Exception e) {
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
