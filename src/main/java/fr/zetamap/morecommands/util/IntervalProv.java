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

package fr.zetamap.morecommands.util;

import arc.func.Prov;
import arc.util.Interval;


/** Return the last {@link Prov} result until new check. */
public class IntervalProv extends Interval {
  protected Object[] results;
  protected boolean[] hasResults;
  
  public IntervalProv() { this(1); }
  public IntervalProv(int capacity) { 
    super(capacity); 
    results = new Object[capacity];
    hasResults = new boolean[capacity];
  }

  public <T> T get(float time, Prov<T> prov) {
    return get(0, time, prov);
  }
  
  @SuppressWarnings("unchecked")
  public <T> T get(int id, float time, Prov<T> prov) {
    if (get(id, time)) {
      T result = prov.get();
      results[id] = result;
      hasResults[id] = true;
      return result;
    }
    return (T)results[id];
  }
  
  @Override
  public void reset(int id, float time) {
    super.reset(id, time);
    results[id] = null;
    hasResults[id] = false;
  }
  
  @Override
  public void clear() {
    for (int i=0; i<results.length; i++) {
      getTimes()[i] = 0;
      results[i] = null;
      hasResults[i] = false;
    }
  }
  
  public boolean hasResult(int id) {
    return hasResults[id];
  }
  
  @SuppressWarnings("unchecked")
  public <T> T getResult(int id) {
    return (T)results[id];
  }
  
  public Object[] getResults() {
    return results;
  }
  
  public int size() {
    return results.length;
  }
}
