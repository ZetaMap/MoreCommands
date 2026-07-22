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

package fr.zetamap.morecommands.modules.selector;

import arc.math.geom.Vec2;
import arc.struct.Seq;

import mindustry.gen.Player;
import mindustry.gen.Unit;

import fr.zetamap.morecommands.util.StringReader;
import fr.zetamap.morecommands.util.Strings;


public abstract class SelectorProperty {
  public final String name;
  public final Category category;
  public final int id;

  /** Uses the class name to determine the property name. Class name without {@code "Property"}, in kebab case. */
  public SelectorProperty() { this(Category.other); }
  /** Uses the class name to determine the property name. Class name without {@code "Property"}, in kebab case. */
  public SelectorProperty(Category category) {
    this.name = Strings.kebabize(getClass().getSimpleName().replace("Property", ""));
    this.category = category;
    this.id = SelectorProperties.add(this);
  }
  
  public SelectorProperty(String name) { this(name, Category.other); }
  public SelectorProperty(String name, Category category) {
    this.name = name;
    this.category = category;
    this.id = SelectorProperties.add(this);
  }

  public abstract Parsed read(StringReader reader);

  
  /** Used to sort properties or override the default selector's behavior. */
  public static enum Category {
    // Order is important
    positioning, bounding, other, sorting, limiting;
  }
  
  
  public abstract class Parsed {
    public final SelectorProperty property = SelectorProperty.this;

    /** Note that {@code executor} can be {@code null} (e.g. when running from console). */
    public abstract boolean passes(Selector selector, Player executor, Vec2 pos, Unit entity);
    /** 
     * Called before or after entities selection, depending on {@link SelectorProperty#category}. <br>
     * Note that {@code entities} is an unordered list for optimization purposes, and {@code executor}
     * can be {@code null} (e.g. when running from console). <br>
     */
    public void update(Selector selector, Seq<Unit> entities, Player executor, Vec2 pos) {}
  }
}
