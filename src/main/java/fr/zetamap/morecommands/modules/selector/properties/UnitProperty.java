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

package fr.zetamap.morecommands.modules.selector.properties;

import arc.math.geom.Vec2;
import arc.struct.ObjectSet;

import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

import fr.zetamap.morecommands.modules.selector.Selector;
import fr.zetamap.morecommands.modules.selector.SelectorProperty;
import fr.zetamap.morecommands.util.StringReader;


/** 
 * Accepted formats: {@code unit=}, {@code unit=!}, {@code unit=unit}, {@code unit=!unit}, 
 * {@code unit=[unit1, ...]} or {@code unit=![unit1, ...]}. 
 */
public class UnitProperty extends SelectorProperty {
  @Override
  public Parsed read(StringReader reader) {
    boolean negated = reader.isNegated();
    return !reader.isArray() ? new Parsed(readUnit(reader, true), negated) :
           new Parsed(reader.readSet(this::readUnit, "unit name"), negated);
  }
  
  public UnitType readUnit(StringReader reader) { return readUnit(reader, false); }
  public UnitType readUnit(StringReader reader, boolean optional) {
    String name = reader.readString();
    if (name == null) {
      if (optional) return null;
      throw reader.expected("a unit name");
    }
    UnitType unit = Vars.content.unit(name);
    if (unit == null && !optional) throw reader.notFound("unit", name);
    return unit;
  }

  
  public class Parsed extends SelectorProperty.Parsed {
    /** Not {@code null} if multiple units are specified. */
    public final ObjectSet<UnitType> units;
    /** {@code null} if no or multiple units are specified. */
    public final UnitType unit;
    public final boolean negated;
    
    public Parsed(UnitType unit, boolean negated) { 
      this.units = null;
      this.unit = unit; 
      this.negated = negated;
    }
    
    public Parsed(ObjectSet<UnitType> units, boolean negated) { 
      this.units = units;
      this.unit = null; 
      this.negated = negated;
    }

    @Override
    public boolean passes(Selector selector, Player executor, Vec2 pos, Unit entity) {
      return negated ^ (units != null ? units.contains(entity.type) : 
                        unit != null ? unit == entity.type : 
                        // always false if the player is dead or if it's the console
                        executor == null || executor.dead() ? negated : 
                        executor.unit().type == entity.type);
    }
  }
}
