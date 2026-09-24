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

package fr.zetamap.morecommands.modules.selector;

import arc.func.Cons2;
import arc.struct.ObjectMap;
import arc.struct.Seq;

import mindustry.gen.Unit;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.util.StringReader;
import fr.zetamap.morecommands.util.Strings;


public class SelectorParser {
  public final PlayerData executor, target;
  public final Selector selector;
  public final boolean onlyPlayers, onlyOne, byPlayer;
  public final ObjectMap<String, SelectorProperty.Parsed> properties;
  public final Seq<Unit> selected;
  public final String[] rest;

  public SelectorParser(PlayerData executor, String[] args)
  throws IllegalArgumentException, StringReader.ParseException {
    this(executor, args, false, false);
  }

  public SelectorParser(PlayerData executor, String[] args, boolean onlyPlayers)
  throws IllegalArgumentException, StringReader.ParseException {
    this(executor, args, onlyPlayers, false);
  }

  public SelectorParser(PlayerData executor, String[] args, boolean onlyPlayers, boolean onlyOne)
  throws IllegalArgumentException, StringReader.ParseException {
    this(executor, args, 0, args.length, onlyPlayers, onlyOne);
  }

  public SelectorParser(PlayerData executor, String[] args, int from, int to, boolean onlyPlayers, boolean onlyOne)
  throws IllegalArgumentException, StringReader.ParseException {
    if (Strings.checkStringArray(args, from, to))
      throw new IllegalArgumentException("Missing player name/unitID/uuid or selector");

    this.executor = executor;
    this.onlyPlayers = onlyPlayers;
    this.onlyOne = onlyOne;

    if (!Selectors.isSelector(args[from])) {
      Players.SearchResult result = Players.find(args, from, to);
      byPlayer = true;
      selector = null;
      properties = null;
      selected = null;
      target = result.player;
      rest = result.rest;
      if (target == null) throw new IllegalArgumentException("Player not found");
      return;
    }

    target = null;
    byPlayer = false;
    StringReader reader = new StringReader(Strings.join(" ", args, from, to));
    if (reader.peekNext() == Selectors.prefix) reader.skip(); // skip prefix
    String selec = reader.readWord(false);
    if (selec == null) throw reader.expected("a selector name");
    selector = Selectors.get(selec);
    if (selector == null) throw reader.notFound("selector", selec);

    properties = reader.readArraySet(this::readKey, this::readValue, "property name");

    selected = selector.select(executor == null ? null : executor.player,
                               properties == null ? null : properties.values().toSeq());

    //TODO: use ParseException to show where it is. In case of multiple selectors being parsed
    if (onlyPlayers && !selected.allMatch(Unit::isPlayer))
      throw new IllegalArgumentException("The selector is targeting non-player units, but only players are expected");
    if (onlyOne && selected.size > 1)
      throw new IllegalArgumentException("The selector is targeting more than one player or unit");

    String r = reader.toString().strip();
    rest = r.isBlank() ? new String[0] : r.split(" ");
  }

  public boolean noTargetFound() {
    return target == null && (selector == null || selected.isEmpty());
  }

  public String formatMessage(String verb) { return format(executor, verb, null, null); }
  public String formatMessage(PlayerData src, String verb) { return format(src, verb, null, null); }
  public String formatColorMessage(String verb)  { return format(executor, verb, "[accent]", "[]"); }
  public String formatColorMessage(PlayerData src, String verb)  { return format(src, verb, "[accent]", "[]"); }
  public String format(PlayerData executor, String verb, String colorAdd, String colorClear) {
    StringBuilder builder = new StringBuilder();
    builder.append(verb).append(' ');

    if (target != null) {
      if (target == executor) {
        if (colorAdd != null) builder.append(colorAdd);
        builder.append("yourself");
        if (colorClear != null) builder.append(colorClear);
      } else builder.append(colorAdd != null ? target.getName() : target.stripedName);

    } else if (selected == null || selected.isEmpty()) {
      builder.setLength(0);
      builder.append("No ");
      if (!onlyPlayers) builder.append("units or ");
      builder.append("players was ").append(verb.toLowerCase());

    } else if (selected.size == 1) {
      Unit unit = selected.first();

      if (unit.isPlayer()) {
        PlayerData player = PlayerData.get(unit);
        if (player == null) // In case of
          builder.append(colorAdd != null ? unit.getPlayer().coloredName() : Strings.normalize(unit.getPlayer().name));
        else if (player != executor) builder.append(colorAdd != null ? player.getName() : player.stripedName);
        else {
          if (colorAdd != null) builder.append(colorAdd);
          builder.append("yourself");
          if (colorClear != null) builder.append(colorClear);
        }
      } else {
        builder.append(Strings.articleFor(unit.type.name)).append(' ');
        if (colorAdd != null) builder.append(colorAdd);
        builder.append(unit.type.name);
        if (colorClear != null) builder.append(colorClear);
      }

    } else {
      int players = selected.count(Unit::isPlayer), units = selected.size - players;
      if (players > 0) {
        if (colorAdd != null) builder.append(colorAdd);
        builder.append(players).append(" player");
        if (players > 1) builder.append('s');
        if (colorClear != null) builder.append(colorClear);
      }
      if (players > 0 && players < selected.size) builder.append(" and ");
      if (units > 0) {
        if (colorAdd != null) builder.append(colorAdd);
        builder.append(units).append(" unit");
        if (units > 1) builder.append('s');
        if (colorClear != null) builder.append(colorClear);
      }
    }

    return builder.toString();
  }

  public void execute(Cons2<PlayerData, Unit> consumer) {
    if (target != null) consumer.get(target, target.player.unit());
    else if (selected != null && selected.any())
      selected.each(u -> consumer.get(PlayerData.get(u), u));
  }

  protected String readKey(StringReader reader) {
    return (StringReader.isQuote(reader.peek()) ? reader.readQuotedString(false) : reader.readUntil('=')).strip();
  }

  protected SelectorProperty.Parsed readValue(StringReader reader, String key) {
    SelectorProperty property = SelectorProperties.get(key);
    if (property == null) throw reader.notFound("property", key);
    return property.read(reader);
  }
}
