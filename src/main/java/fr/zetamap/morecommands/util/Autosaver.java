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

import arc.ApplicationListener;
import arc.Core;
import arc.func.Cons2;
import arc.struct.Seq;
import arc.util.Interval;


/** Save periodically the registered {@link Saveable}s and also when the application exit. */
public class Autosaver {
  protected final static Logger logger = new Logger(Autosaver.class);
  protected static ApplicationListener listener;
  protected static int spacing = 360 * 60; // in ticks
  /** 
   * Doesn't log a message when starting/stopping the {@link Autosaver} or when running an auto save. <br>
   * Errors will always be logged.
   */
  public static boolean silent;
  /** Called when a save fail. */
  public static Cons2<Saveable, Throwable> errorHandler;


  /** Adds to the {@code normal} priority. */
  public static void add(Saveable saveable) {
    add(saveable, SavePriority.normal);
  }
  
  public static void add(Saveable saveable, SavePriority priority) {
    remove(saveable);
    priority.saves.add(saveable);
  }
  
  public static void remove(Saveable saveable) {
    for (SavePriority p : SavePriority.all) {
      if (p.saves.remove(saveable)) break;
    }
  }
  
  public static void clear() {
    for (SavePriority p : SavePriority.all) clear(p);
  }
  
  public static void clear(SavePriority priority) {
    priority.saves.clear();
  }
  
  public static boolean has(Saveable saveable) {
    return priorityOf(saveable) != null;
  }
  
  public static boolean has(Saveable saveable, SavePriority priority) {
    return priority.saves.contains(saveable);
  }
  
  public static SavePriority priorityOf(Saveable saveable) {
    for (SavePriority p : SavePriority.all) {
      if (has(saveable, p)) return p;
    }
    return null;
  }
  
  public static boolean saveNeeded() {
    for (SavePriority p : SavePriority.all) {
      if (p.saves.contains(Saveable::modified)) return true;
    }
    return false;
  }
  
  /** Save all registered things now (only if modified). Errors are ignored and just printed. */
  public static boolean save() {
    if (!saveNeeded()) return false;
    if (!silent) logger.info("Running autosave...");
    for (SavePriority p : SavePriority.all) {
      p.saves.each(Saveable::modified, s -> {
        if (!silent) logger.debug("Saving @.", s.name());
        try { s.save(); } 
        catch (Throwable t) { 
          logger.err("Failed to save @", t, s.name()); 
          if (errorHandler != null) errorHandler.get(s, t);
        }
      });
    }
    if (!silent) logger.info("Autosave completed.");
    return true;
  }

  /** Start the auto saver, will also save on application exit. */
  public static boolean start() {
    if (isStarted()) return false;
    Core.app.addListener(listener = new ApplicationListener() {
      Interval timer = new Interval();
      
      @Override
      public void update() {
        if (timer.get(spacing)) save();
      }
      
      @Override
      public void dispose() { 
        save();
        stop();
      }
    });
    if (!silent) logger.info("Autosaver started!");
    return true;
  }
  
  public static boolean stop() {
    if (!isStarted()) return false;
    Core.app.removeListener(listener);
    listener = null;
    if (!silent) logger.info("Autosaver stopped!");
    return true;
  }

  public static boolean isStarted() {
    return listener != null;
  }
  
  public static int spacing() {
    return spacing / 60;
  }
  
  public static void spacing(int spacing) {
    if (spacing < 1) throw new IllegalArgumentException("spacing must be greater than 1 second");
    Autosaver.spacing = spacing * 60;
  }
  

  /** Defines a things that can be saved by the {@link Autosaver}. */
  public static interface Saveable {
    /** Used for logging. */
    String name();
    boolean modified();
    void save();
  }
  
  
  /** Defines the order to save things. */
  public static enum SavePriority {
    high, normal, low;
    
    static final SavePriority[] all = values();
    // More simple to store the saveable things here. 
    // Because the priority should not be modified after registration.
    final Seq<Saveable> saves = new Seq<>();
  }
}
