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
import fr.zetamap.morecommands.modules.selector.Selector;
import fr.zetamap.morecommands.modules.selector.SelectorProperty;
import fr.zetamap.morecommands.util.StringReader;
import mindustry.gen.Player;
import mindustry.gen.Unit;


public class LimitProperty extends SelectorProperty {
  public LimitProperty() { super(Category.limiting); }

  @Override
  public Parsed read(StringReader reader) {
    int limit = reader.readNumeric(true);
    if (limit < 1) throw reader.error("Limit must be greater than 1");
    return new Parsed(limit);
  }


  public class Parsed extends SelectorProperty.Parsed {
    public final int limit;
    public Parsed(int limit) { this.limit = limit; }

    @Override
    public boolean passes(Selector selector, Player executor, Vec2 pos, Unit entity) {
      return true;
    }

    @Override
    public void update(Selector selector, Seq<Unit> entities, Player executor, Vec2 pos) {
      entities.truncate(limit);
    }
  }
}
