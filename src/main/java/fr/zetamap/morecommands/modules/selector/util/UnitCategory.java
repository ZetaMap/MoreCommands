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

package fr.zetamap.morecommands.modules.selector.util;

import arc.func.Boolf;

import mindustry.Vars;
import mindustry.gen.*;
import mindustry.type.unit.NeoplasmUnitType;
import mindustry.world.blocks.storage.CoreBlock;


/** Map of all kind of units, as defined in {@link mindustry.content.UnitTypes} and 
 * {@link mindustry.mod.ContentParser#unitType(JsonValue)}. */
public enum UnitCategory {
  player(u -> u.isPlayer()),
  mech(u -> u instanceof Mechc),
  legs(u -> u instanceof Legsc),
  air(u -> u.type().flying || u.type().hovering && !u.type().allowLegStep), // if hovering and has no legs, consider flying
  payload(u -> u instanceof Payloadc),
  naval(u -> u.type().naval),
  tank(u -> u instanceof Tankc),
  //missile(u -> u instanceof TimedKillc),
  tether(u -> u instanceof BuildingTetherc),
  //crawl(u -> u instanceof Crawlc),
  neoplasm(u -> u.type() instanceof NeoplasmUnitType),
  //core(u -> Vars.content.blocks().contains(b -> b instanceof CoreBlock && u.type() == ((CoreBlock)b).unitType)),
  core(u -> Vars.state.teams.getActive().contains(t -> t.cores.contains(b -> u.type() == ((CoreBlock)b.block).unitType))),
  other(u -> true),
  ;
  
  public static final UnitCategory[] all = values();

  final Boolf<Unitc> checker;
  UnitCategory(Boolf<Unitc> checker) { this.checker = checker; }
  
  
  public boolean sameCategory(Unitc unit) {
    for (UnitCategory c : all) {
      if (c.checker.get(unit) && c == this) return true;
    }
    return false;
  }
  
  public static UnitCategory of(String name) {
    for (UnitCategory c : all) {
      if (c.name().equals(name)) return c;
    }
    return null;
  }
  
  /** 
   * Attempts to dynamically determine the unit's category.
   * <p>
   * This is unreliable, as a unit can belong to multiple categories. <br>
   * Consider using a comparison, with {@link #sameCategory(Unitc)}, instead.
   */
  public static UnitCategory of(Unitc unit) {
    for (UnitCategory c : all) {
      if (c.checker.get(unit)) return c;
    }
    return null; // never happen
  }
}
