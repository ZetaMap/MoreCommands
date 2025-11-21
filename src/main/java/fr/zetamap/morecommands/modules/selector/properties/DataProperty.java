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

import java.lang.reflect.Field;

import arc.math.geom.Vec2;
import arc.struct.ObjectMap;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;

import mindustry.gen.Player;
import mindustry.gen.Unit;

import fr.zetamap.morecommands.modules.selector.Selector;
import fr.zetamap.morecommands.modules.selector.SelectorProperty;
import fr.zetamap.morecommands.util.MindustryJson;
import fr.zetamap.morecommands.util.StringReader;


/** minecraft's nbt like property. WIP */
public class DataProperty extends SelectorProperty {
  private final JsonReader reader = new JsonReader();
  
  @Override
  public Parsed read(StringReader reader) {
    boolean negated = reader.isNegated();
    if (reader.peekNext() != '{') throw reader.expected("a map start", reader.peekNext());
    // TODO: Find a way to read json without error about leading comma or brace
    try { return new Parsed(this.reader.parse(reader.readRemaining()), negated); }
    catch (Exception e) { throw reader.error(e.getMessage()); }
  }

  
  public class Parsed extends SelectorProperty.Parsed {
    public final JsonValue data;
    public final boolean negated;
    protected Class<? extends Unit> readedType;
    protected ObjectMap<Field, Object> readed;
    
    public Parsed(JsonValue data, boolean negated) {
      this.data = data;
      this.negated = negated;
      // Should be an object
      if (!data.isObject()) throw new IllegalArgumentException("Should be a json object");
    }

    @Override
    public boolean passes(Selector selector, Player executor, Vec2 pos, Unit entity) {
      // Try to cache the result, better than nothing ¯\_(ツ)_/¯
      if (entity.getClass() != readedType) {
        readed = MindustryJson.get().readFields(entity.getClass(), data);
        readedType = entity.getClass();
      }
      
      // This is really inefficient (can hold lot of memory) but this is the only way
      for (ObjectMap.Entry<Field, Object> e : readed) {
        try {
          if (!e.key.get(entity).equals(e.value)) return negated;
        } catch (Exception ignored) {}
      }
      return !negated; 
    }
  }
}
