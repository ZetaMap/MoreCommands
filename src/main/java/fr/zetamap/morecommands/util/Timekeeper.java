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

import arc.util.Time;


/** Improved version of {@link arc.util.Timekeeper}. */
public class Timekeeper{
  private final long intervalms;
  private long time;

  public Timekeeper(long ms) {
    intervalms = ms;
  }
  
  public Timekeeper(float seconds) {
    intervalms = (long)(seconds * 1000);
  }
  
  public long interval() {
    return intervalms;
  }
  
  @Deprecated
  public boolean get() {
    return exceeded();
  }

  public boolean exceeded() {
    return elapsed() > intervalms;
  }
  
  public long elapsed() {
    return Time.timeSinceMillis(time);
  }
  
  public long remaining() {
    return Math.max(intervalms - elapsed(), 0);
  }

  public long last() {
    return time;
  }
  
  public void reset() {
    time = Time.millis();
  }
  
  public void zero() {
    time = 0;
  }
}
