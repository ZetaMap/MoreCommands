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

import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.gen.Unit;

import fr.zetamap.morecommands.modules.selector.Selector;
import fr.zetamap.morecommands.modules.selector.SelectorProperty;
import fr.zetamap.morecommands.util.StringReader;


/** Accepted formats: {@code x=5} (normal x) or {@code x=5w} (world scaled x: {@code 5*8=40}). */
public class XProperty extends SelectorProperty {
  public XProperty() { super(Category.positioning); }
  
  @Override
  public Parsed read(StringReader reader) {
    int x = reader.readNumeric();
    if (reader.peekNext() != 'w') return new Parsed(x * Vars.tilesize);
    reader.readNext();
    return new Parsed(x);
  }

  
  public class Parsed extends SelectorProperty.Parsed {
    public final float x;
    public Parsed(float x) { this.x = x; }

    @Override
    public boolean passes(Selector selector, Player executor, Vec2 pos, Unit entity) { 
      return true; 
    }
    
    @Override
    public void update(Selector selector, Seq<Unit> entities, Player executor, Vec2 pos) {
      pos.x = x;
    }
  }
}
