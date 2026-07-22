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

import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.gen.Unit;

import fr.zetamap.morecommands.modules.selector.Selector;
import fr.zetamap.morecommands.modules.selector.SelectorProperty;
import fr.zetamap.morecommands.modules.selector.util.Range;
import fr.zetamap.morecommands.modules.selector.util.RangeParser;
import fr.zetamap.morecommands.util.StringReader;


/** 
 * Accepted formats: {@code distance=10}, {@code distance=..10}, {@code distance=10..}, {@code distance=5..10}
 * (normal distance). <br>
 * Or {@code distance=10w}, {@code distance=..10w}, {@code distance=10..w}, {@code distance=5..10w} 
 * (world scaled distance: {@code 5*8=40}{@code ..}{@code 10*8=80}).
 */
public class DistanceProperty extends SelectorProperty {
  public DistanceProperty() { super(Category.bounding); }

  @Override
  public Parsed read(StringReader reader) { 
    Range<Float> range = RangeParser.parseFloat(reader, true);
    boolean scaled = reader.peekNext() == 'w';
    if (scaled) reader.readNext();
    return new Parsed(range, scaled); 
  }

  
  public class Parsed extends SelectorProperty.Parsed {
    public final Range<Float> range;
    public final boolean worldScaled;
    
    public Parsed(Range<Float> range, boolean worldScaled) { 
      this.range = range; 
      this.worldScaled = worldScaled;
    }

    @Override
    public boolean passes(Selector selector, Player executor, Vec2 pos, Unit entity) { 
      return range.contains(worldScaled ? entity.dst(pos) : entity.dst(pos) / Vars.tilesize);
    }
  }
}