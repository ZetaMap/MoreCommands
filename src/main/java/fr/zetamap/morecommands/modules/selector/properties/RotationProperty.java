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

import mindustry.gen.Player;
import mindustry.gen.Unit;

import fr.zetamap.morecommands.misc.Range;
import fr.zetamap.morecommands.misc.RangeParser;
import fr.zetamap.morecommands.modules.selector.Selector;
import fr.zetamap.morecommands.modules.selector.SelectorProperty;
import fr.zetamap.morecommands.util.StringReader;


/** Accepted formats: {@code rotation=}, {@code rotation=90}, {@code rotation=..90}, {@code rotation=45..} or
 * {@code rotation=45..90}. */
public class RotationProperty extends SelectorProperty {
  public RotationProperty() { super(Category.bounding); }
  
  @Override
  public Parsed read(StringReader reader) {
    return new Parsed(reader.peekWord() == null ? null : RangeParser.parseFloat(reader, false));
  }

  
  public class Parsed extends SelectorProperty.Parsed {
    /** {@code null} means the executor's rotation. */
    public final Range<Float> range;
    public Parsed(Range<Float> range) { this.range = range; }

    @Override
    public boolean passes(Selector selector, Player executor, Vec2 pos, Unit entity) { 
      return range != null ? range.contains(entity.rotation) :
             executor == null || executor.dead() || 
             // loses precision because the rotations are never completely identical
             (int)executor.unit().rotation == (int)entity.rotation;
    }
  }
}