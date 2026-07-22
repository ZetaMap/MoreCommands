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

import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;

import mindustry.gen.Groups;

import fr.zetamap.morecommands.util.StringReader;
import fr.zetamap.morecommands.util.Strings;


public class Selectors {
  public static final char prefix = '@';
  private static final ObjectMap<String, Selector> all = new ObjectMap<>();

  /** Default selectors. */
  public static Selector
  // Player related
  nearestPlayer, randomPlayer, allPlayers, teamPlayers, self,
  // Unit related
  nearestUnit, randomUnit, allUnits, teamUnits,
  // Both related
  nearestEntity, randomEntity, allEntities, teamEntities;


  public static void init() {
    nearestPlayer = new Selector("p", 1, Sorting.nearest, Selector.playerExtractor);
    randomPlayer = new Selector("r", 1, Sorting.random, Selector.playerExtractor);
    allPlayers = new Selector("a", Selector.playerExtractor);
    teamPlayers = new Selector("t", (l, e) -> Groups.player.each(p -> e == null || p.team() == e.team(), p -> l.add(p.unit())));
    self = new Selector("s", 1, null, (l, e) -> {
      if (e == null) throw new IllegalArgumentException("Unavailable selector");
      l.add(e.unit());
    });

    nearestUnit = new Selector("nu", 1, Sorting.nearest, Selector.unitExtractor);
    randomUnit = new Selector("ru", 1, Sorting.random, Selector.unitExtractor);
    allUnits = new Selector("u", Selector.unitExtractor);
    teamUnits = new Selector("tu", (l, e) -> Groups.unit.each(p -> !p.isPlayer() && e == null || p.team() == e.team(), l::add));

    nearestEntity = new Selector("n", 1, Sorting.nearest, Selector.entityExtractor);
    randomEntity = new Selector("re", 1, Sorting.random, Selector.entityExtractor);
    allEntities = new Selector("e", Selector.entityExtractor);
    teamEntities = new Selector("te", (l, e) -> Groups.unit.each(p -> e == null || p.team() == e.team(), l::add));
  }

  public static Selector get(String name) {
    return name.isEmpty() ? null : all.get(name.charAt(0) == prefix ? name.substring(1) : name);
  }

  public static boolean isSelector(String arg) {
    return !arg.isEmpty() && arg.charAt(0) == prefix;
  }

  /** @throws IllegalArgumentException if another selector is registered with the same name. */
  public static int add(Selector selector) {
    if (all.containsKey(selector.name))
      throw new IllegalArgumentException("another selector is named '"+selector.name+"'");
    // Validate the name depending on what StringReader can accept
    if (Strings.contains(selector.name, c -> !StringReader.isAlpha(c)))
      throw new IllegalArgumentException("invalid selector name");
    all.put(selector.name, selector);
    return all.size-1;
  }

  public static void each(Cons<Selector> consumer) {
    all.each((n, s) -> consumer.get(s));
  }

  public static Seq<Selector> all() {
    return all.values().toSeq();
  }
}
