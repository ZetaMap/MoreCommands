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
import arc.struct.ObjectMap;

import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.Item;

import fr.zetamap.morecommands.modules.selector.Selector;
import fr.zetamap.morecommands.modules.selector.SelectorProperty;
import fr.zetamap.morecommands.modules.selector.util.Range;
import fr.zetamap.morecommands.modules.selector.util.RangeParser;
import fr.zetamap.morecommands.util.StringReader;


/** Accepted formats: {@code hasitem=[item1, !item2, item2=quantity, !item3=..max, item4=min.., item5=min..max]} */
public class HasitemProperty extends SelectorProperty {
  @Override
  public Parsed read(StringReader reader) {
    reader.skipWhitespaces();
    if (!reader.isArray()) throw reader.expected("an array");
    ObjectMap<Item, Range<Integer>> items = new ObjectMap<>();
    ObjectMap<Item, Boolean> negated = new ObjectMap<>();
    reader.readArray(r -> {
      boolean negate = r.isNegated();
      String name = r.readString();
      if (name == null || name.isEmpty()) throw r.expected("an item name");
      Item item = Vars.content.item(name);
      if (item == null) throw r.notFound("item", name);
      Range<Integer> range = null;
      if (r.peek() == '=') {
        r.skip();
        range = RangeParser.parseInt(reader, false);
      }
      items.put(item, range);
      negated.put(item, negate);
    });
    return new Parsed(items, negated); 
  }

  
  public class Parsed extends SelectorProperty.Parsed {
    public final ObjectMap<Item, Range<Integer>> items;
    public final ObjectMap<Item, Boolean> negated;
    
    public Parsed(ObjectMap<Item, Range<Integer>> items, ObjectMap<Item, Boolean> negated) {
      this.items = items;
      this.negated = negated;
    }
    
    @Override
    public boolean passes(Selector selector, Player executor, Vec2 pos, Unit entity) {
      if (!entity.hasItem() || !items.containsKey(entity.item())) return false;
      Range<Integer> range = items.get(entity.item());
      Boolean negate = negated.get(entity.item());
      return (range == null || range.contains(entity.stack().amount)) ^ (negate != null && negate);
    }
  }
}
