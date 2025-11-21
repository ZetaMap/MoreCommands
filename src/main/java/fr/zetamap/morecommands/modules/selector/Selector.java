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

import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;


public class Selector {
  /** Common extractors. */
  public static final Extractor
    /** Only players. */
    playerExtractor = (l, e) -> Groups.player.each(p -> l.add(p.unit())),
    /** Only units. */
    unitExtractor = (l, e) -> Groups.unit.each(u -> !u.isPlayer(), l::add),
    /** Unit and players. */
    entityExtractor = (l, e) -> Groups.unit.copy(l);

  public final String name;
  /** Less than {@code 0} means no limit. */
  public final int limit;
  public final Sorting sorting;
  public final Extractor extractor;
  public final int id;
  
  public Selector(String name, Extractor extractor) { this(name, 0, Sorting.arbitrary, extractor); }
  public Selector(String name, int limit, Extractor extractor) { this(name, limit, Sorting.arbitrary, extractor); }
  public Selector(String name, Sorting sorting, Extractor extractor) { this(name, 0, sorting, extractor); }
  /**
   * @param name the selector's name/alias.
   * @param one whether this selector will select only one entity.
   * @param sorting sorting to do after selected the units. {@code null} for no sorting. 
   * @param extractor the entity extractor
   */
  public Selector(String name, int limit, Sorting sorting, Extractor extractor) {
    this.name = name;
    this.limit = limit;
    this.sorting = sorting;
    this.extractor = extractor;
    this.id = Selectors.add(this);
  }

  public Seq<Unit> select(Player executor) { return select(executor, null); }
  /** 
   * If {@code executor} is {@code null} or {@link Player#dead()}, a zero position is used. <br>
   * And, in this case, if {@code bounding} properties are presents, {@code positioning} properties must also be presents, 
   * else an {@link IllegalArgumentException} is thrown.
   */
  public Seq<Unit> select(Player executor, Seq<SelectorProperty.Parsed> properties) {
    Seq<Unit> selected = new Seq<>(false); // First unordered for optimization purposes
    Vec2 pos = new Vec2();
    boolean hasProps = properties != null && properties.any(), 
            needSorting = sorting != null,
            needLimiting = limit > 0;

    if (hasProps) properties.sort(p -> p.property.category.ordinal());

    if (executor != null && !executor.dead()) pos.set(executor);
    else if (hasProps && 
             properties.contains(p -> p.property.category == SelectorProperty.Category.bounding) &&
             !properties.contains(p -> p.property.category == SelectorProperty.Category.positioning))
      throw new IllegalArgumentException("Unable to find executor position. Please add positioning properties.");
    
    extractor.select(selected, executor);
    // Removes null units. Can happen when a selector target dead players
    selected.removeAll(u -> u == null);

    if (hasProps) {
      int resume = -1;
      // Apply properties updates and stop at sorting and limiting properties
      for (int i=0; i<properties.size; i++) {
        SelectorProperty.Parsed p = properties.get(i);
        if (p.property.category.ordinal() > SelectorProperty.Category.other.ordinal()) {
          resume = i;
          break;
        }
        p.update(this, selected, executor, pos);
      }
      // Apply filtering
      selected.removeAll(e -> !properties.allMatch(p -> p.passes(this, executor, pos, e)));
      // Resume iteration for sorting and limiting properties
      for (int i=resume; i>=0 && i<properties.size; i++) {
        SelectorProperty.Parsed p = properties.get(i);
        if (p.property.category == SelectorProperty.Category.sorting) needSorting = false;
        else if (p.property.category == SelectorProperty.Category.limiting) needLimiting = false;
        p.update(this, selected, executor, pos);
      }
    }
    
    if (needSorting) sorting.sort(selected, pos);
    if (needLimiting) selected.truncate(limit);

    selected.ordered = true; // Now make it ordered for the caller's use.
    return selected;
  }
  
  public String toString() {
    return Selectors.prefix + name;
  }

  
  public static interface Extractor {
    /** Note that {@code executor} can be {@code null}, e.g. when running on console. */
    void select(Seq<Unit> out, Player executor);
  }
}
