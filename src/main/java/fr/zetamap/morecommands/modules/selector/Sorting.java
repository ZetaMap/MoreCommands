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

package fr.zetamap.morecommands.modules.selector;

import arc.func.Cons2;
import arc.math.geom.Position;
import arc.struct.Seq;

import mindustry.gen.Unit;


/** @see mindustry.entities.UnitSorts */
public enum Sorting {
  /** Sorts by nearest unit. */
  nearest((l, p) -> l.sort(u -> u.dst2(p))),
  /** Sorts by furthest unit. */
  furthest((l, p) -> l.sort(u -> -u.dst2(p))),
  /** Sort by nearest unit and with the highest maximum health. */
  strongest((l, p) -> l.sort(u -> -u.maxHealth + u.dst(p) / 6400f)),
  /** Sort by nearest unit and with the lowest maximum health. */
  weakest((l, p) -> l.sort(u -> u.maxHealth + u.dst(p) / 6400f)),
  /** Shuffle the list. */
  random((l, p) -> l.shuffle()),
  /** Sort by id. */
  arbitrary((l, p) -> l.sort(u -> u.id)), //implicitly sorted by id?
//  /** No sorting. */
//  none((l, p) -> {}),
  ;
  
  final Cons2<Seq<Unit>, Position> sorter;
  Sorting(Cons2<Seq<Unit>, Position> sorter) { this.sorter = sorter; }
  
  public void sort(Seq<Unit> units, Position pos) {
    sorter.get(units, pos);
  }
}
