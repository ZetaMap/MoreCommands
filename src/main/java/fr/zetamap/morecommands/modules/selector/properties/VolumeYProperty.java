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
import fr.zetamap.morecommands.util.StringReader;


/** Accepted formats: {@code dy=10} (normal volume y) or {@code dy=10w} (world scaled volume y: {@code 10*8=80}). */
public class VolumeYProperty extends SelectorProperty {
  public VolumeYProperty() { super("dy", Category.bounding); }
  
  @Override
  public Parsed read(StringReader reader) {
    int y = reader.readNumeric();
    if (reader.peekNext() != 'w') return new Parsed(y * Vars.tilesize);
    reader.readNext();
    return new Parsed(y);
  }

  
  public class Parsed extends SelectorProperty.Parsed {
    public final float dy;
    public Parsed(float dy) { this.dy = dy; }

    @Override
    public boolean passes(Selector selector, Player executor, Vec2 pos, Unit entity) {
      float min = Math.min(pos.y, this.dy), max = Math.max(pos.y, this.dy),
            y = entity.getY(), r = entity.hitSize / 2f;
      return y + r >= min && y - r <= max;
    }
  }
}