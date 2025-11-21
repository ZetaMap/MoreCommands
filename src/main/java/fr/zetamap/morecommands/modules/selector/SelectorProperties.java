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

import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;

import fr.zetamap.morecommands.modules.selector.properties.*;


public class SelectorProperties {
  private static final ObjectMap<String, SelectorProperty> all = new ObjectMap<>();
  
  public static SelectorProperty
  // positioning
  positionXProperty, positionYProperty,
  // bounding
  volumeXProperty, volumeYProperty, distanceProperty, rotationProperty,
  // sorting
  sortProperty,
  // limiter
  limitProperty,
  // other
  unitProperty, typeProperty/*, familyProperty*/, teamProperty/*, dataProperty*/, hasItemProperty
  ;
  
  public static void init() {
    positionXProperty = new XProperty();
    positionYProperty = new YProperty();
    volumeXProperty = new VolumeXProperty();
    volumeYProperty = new VolumeYProperty();
    distanceProperty = new DistanceProperty();
    rotationProperty = new RotationProperty();
    sortProperty = new SortProperty();
    limitProperty = new LimitProperty();
    unitProperty = new UnitProperty();
    typeProperty = new TypeProperty();
    //familyProperty = new FamilyProperty();
    teamProperty = new TeamProperty();
    //dataProperty = new DataProperty();
    hasItemProperty = new HasitemProperty();
  }
  
  public static SelectorProperty get(String property) {
    return all.get(property);
  }
  
  /** @throws IllegalArgumentException if another selector property is registered with the same name. */
  public static int add(SelectorProperty property) {
    if (all.containsKey(property.name)) 
      throw new IllegalArgumentException("another selector property is named '"+property.name+"'");
    all.put(property.name, property);
    return all.size-1;
  }
  
  public static void each(Cons<SelectorProperty> consumer) {
    all.each((n, s) -> consumer.get(s));
  }
  
  public static Seq<SelectorProperty> all() {
    return all.values().toSeq();
  }
}
