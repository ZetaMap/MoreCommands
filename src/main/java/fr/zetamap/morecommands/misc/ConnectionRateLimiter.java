/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2026  ZetaMap
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

package fr.zetamap.morecommands.misc;

import java.util.*;

import arc.net.Server.ServerConnectFilter;
import arc.struct.ObjectMap;

import mindustry.Vars;


//TODO: move to AVS
/**
 * Per-IP connection rate limiter. <br>
 * Bucket are stored in an access-ordered {@link LinkedHashMap}. <br>
 * A bucket idle for a full window is guaranteed fully refilled. <br>
 * {@link #removeEldestEntry} is a hard cap fallback in case of a IP flood.
 */
public class ConnectionRateLimiter implements ServerConnectFilter {
  public static final int MAX_TRACKED_IPS = 20_000;

  public static void init() {
    init(3, 10 * 1000L);
  }

  /** @param capacity Max burst of connections allowed per IP. @param windowMs Time to fully refill the bucket, in ms. */
  public static void init(int capacity, long windowMs) {
    ServerConnectFilter previous = Vars.net.getConnectFilter();
    Vars.net.setConnectFilter(new ConnectionRateLimiter(capacity, windowMs, previous));
  }


  protected final LinkedHashMap<String, ObjectMap.Entry<Long, Long>> buckets = new LinkedHashMap<>(64, 0.8f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, ObjectMap.Entry<Long, Long>> eldest) {
      return size() > MAX_TRACKED_IPS;
    }
  };
  protected final long capacity, refillInterval, windowNanos;
  protected final ServerConnectFilter delegate;

  public ConnectionRateLimiter(int capacity, long windowMs, ServerConnectFilter delegate) {
    this.capacity = capacity;
    this.windowNanos = windowMs * 1_000_000L;
    this.refillInterval = windowNanos / capacity;
    this.delegate = delegate;
  }

  @Override
  public boolean accept(String address) {
    if (delegate != null && !delegate.accept(address)) return false;

    long now = System.nanoTime();
    boolean allowed;
    ObjectMap.Entry<Long, Long> bucket = buckets.get(address);
    if (bucket == null) {
      bucket = new ObjectMap.Entry<>();
      bucket.key = capacity - 1;
      bucket.value = now;
      buckets.put(address, bucket);
      allowed = true;
    } else {
      long refilled = (now - bucket.value) / refillInterval;
      if (refilled > 0) {
        bucket.key = Math.min(capacity, bucket.key + refilled);
        bucket.value += refilled * refillInterval;
      }
      allowed = bucket.key > 0;
      if (allowed) bucket.key--;
    }

    evictStale(now);
    return allowed;
  }

  protected void evictStale(long now) {
    Iterator<Map.Entry<String, ObjectMap.Entry<Long, Long>>> it = buckets.entrySet().iterator();
    while (it.hasNext()) {
      if (now - it.next().getValue().value < windowNanos) break; // LRU order
      it.remove();
    }
  }
}