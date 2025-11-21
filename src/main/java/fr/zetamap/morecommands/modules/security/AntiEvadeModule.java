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

package fr.zetamap.morecommands.modules.security;

import arc.Events;
import arc.struct.ObjectMap;
import arc.util.Time;
import arc.util.Timer;
import arc.util.pooling.Pool;
import arc.util.pooling.Pools;

import mindustry.game.EventType;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.ServerCommandHandler;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractSaveableModule;
import fr.zetamap.morecommands.util.DurationFormatter;
import fr.zetamap.morecommands.util.JsonSettings;
import fr.zetamap.morecommands.util.Strings;


public class AntiEvadeModule extends AbstractSaveableModule {
  private Timer.Task cleaner;
  private float retainTime = 1 * 60; // in minutes
  
  public final ObjectMap<String, CacheEntry> quits = new ObjectMap<>();
  
  /** Clear the cache. */
  public void clearCache() {
    quits.clear();
  }
  
  /** Removes entries older than {@link #retainTime}. */
  public void cleanupCache() {
    ObjectMap.Values<CacheEntry> v = quits.values();
    long now = Time.millis(), retain = retainMillis();
    while (v.hasNext()) {
      CacheEntry e = v.next();
      if (now - e.time >= retain) v.remove();
    }
  }
  
  /** @return cache retain in minutes. */
  public float retain() {
    return retainTime;
  }
  
  public long retainMillis() {
    return (long)(retainTime * 60 * 1000);
  }
  
  public void setRetain(float minutes) {
    retainTime = Math.max(minutes, 1);
    setModified();
  }
  
  @Override
  protected void initImpl() {
    Events.on(EventType.PlayerJoin.class, e -> {
      if (isDisposed()) return;
      CacheEntry entry = quits.get(e.player.uuid());
      if (entry == null) return;
      String name = entry.name;
      Pools.free(quits.remove(e.player.uuid()));
      if (e.player.admin) return;
      PlayerData player = PlayerData.get(e.player);
      if (player.stripedName.equals(Strings.normalize(name))) return;
      PlayerData.each(p -> p != player, p -> 
        Players.warn(p, "[scarlet]Warning[]: the player @[orange] has changed his name. He was @[orange].", 
                     player.getName(), name));
    });
    
    Events.on(EventType.PlayerLeave.class, e -> {
      if (isDisposed() || e.player.admin) return; // ignore admins
      CacheEntry entry = Pools.obtain(CacheEntry.class, CacheEntry::new)
                              .set(e.player.uuid(), PlayerData.get(e.player).getName(), Time.millis());
      quits.put(e.player.uuid(), entry);
    });
    
    // Clean the cache every minutes
    cleaner = Timer.schedule(this::cleanupCache, 60, 60);
  }
  
  @Override
  protected void disposeImpl() {
    cleaner.cancel();
    quits.clear();
  }
  
  @Override
  protected void loadImpl(JsonSettings settings) {
    retainTime = settings.getFloat("retain", 1 * 60);
  }

  @Override
  protected void saveImpl(JsonSettings settings) {
    settings.put("retain", retainTime);
  }
  
  @Override
  public void registerServerCommands(ServerCommandHandler handler) {
    handler.add("anti-evade", "[clear|minutes]", "Control the anti evade system.", args -> {
      if (args.length == 0) {
        logger.info("Anti evade is a system that notify players when one quit and reconnect with another nickname.");
        logger.info("This system will ignore admin players and will retain nicknames @ after disconnection.",
                    DurationFormatter.format(retainMillis()));
      } else if (args[0].equals("clear")) {
        clearCache();
        logger.info("Cache cleared.");
      } else {
        int minutes = Strings.parseInt(args[0]);
        if (minutes == Integer.MIN_VALUE) {
          logger.err("Invalid argument! Must be 'clear' or a number of minutes.");
          return;
        } else if (minutes < 1) {
          logger.err("Should be greater than 1 minute.");
          return;
        }
        setRetain(minutes);
        logger.info("Nicknames will be retained for @ after disconnection.", DurationFormatter.format(retainMillis()));
      }
    });
  }
  
 
  public static class CacheEntry implements Pool.Poolable {
    public String uuid, name;
    public long time;
    
    CacheEntry set(String uuid, String name, long time) {
      this.uuid = uuid;
      this.name = name;
      this.time = time;
      return this;
    }
    
    public void reset() {
      uuid = name = null;
      time = 0;
    }
  }
}