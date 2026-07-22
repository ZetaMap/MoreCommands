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
import arc.struct.Seq;

import mindustry.gen.Player;
import mindustry.gen.Unit;

import fr.zetamap.morecommands.modules.selector.Selector;
import fr.zetamap.morecommands.modules.selector.SelectorProperty;
import fr.zetamap.morecommands.modules.selector.util.UnitCategory;
import fr.zetamap.morecommands.util.StringReader;


// Doesn't allow empty category because determination is not reliable.
/** 
 * Accepted formats: {@code type=category}, {@code type=!category}, 
 * {@code type=[category1, ...]} or {@code type=![category1, ...]}. 
 */
public class TypeProperty extends SelectorProperty {
  @Override
  public Parsed read(StringReader reader) {
    boolean negated = reader.isNegated();
    return !reader.isArray() ? new Parsed(readCategory(reader), negated) :
           new Parsed(reader.readSet(this::readCategory, "unit category").toSeq(), negated);
  }

  public UnitCategory readCategory(StringReader reader) {
    String name = reader.readString();
    if (name == null) throw reader.expected("a unit category");
    UnitCategory category = UnitCategory.of(name = name.toLowerCase());
    if (category == null) throw reader.notFound("unit category", name);
    return category;
  }
  
  
  public class Parsed extends SelectorProperty.Parsed {
    /** Not {@code null} if multiple types are specified. */
    public final Seq<UnitCategory> types;
    /** {@code null} if multiple types are specified. */
    public final UnitCategory type;
    public final boolean negated;

    public Parsed(UnitCategory type, boolean negated) { 
      this.types = null;
      this.type = type; 
      this.negated = negated;
    }
    
    public Parsed(Seq<UnitCategory> types, boolean negated) { 
      this.types = types;
      this.type = null; 
      this.negated = negated;
    }

    @Override
    public boolean passes(Selector selector, Player executor, Vec2 pos, Unit entity) {
      // Quite slow because i have no other choice to determine the unit category
      return negated ^ (types != null ? types.contains(c -> c.sameCategory(entity)) : type.sameCategory(entity));
    }
  }
}
