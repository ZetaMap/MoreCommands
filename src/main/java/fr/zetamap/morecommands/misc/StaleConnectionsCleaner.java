/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2025-2026  ZetaMap
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

import arc.net.*;
import arc.struct.ObjectSet;
import arc.util.Reflect;

import mindustry.Vars;
import mindustry.net.ArcNetProvider;
import mindustry.net.Packets.ConnectPacket;;

/**
 * Kick stale connections. <br>
 * New connections will be kicked after x seconds if one of the specified packets is not received before the time.
 */
public class StaleConnectionsCleaner implements NetListener {
  /** Init cleaner with defaults values for the Mindustry server. */
  public static void init() {
    init(Reflect.get(Reflect.<ArcNetProvider>get(Vars.net, "provider"), "server"), 5 * 1000L, ConnectPacket.class);
  }

  /**
   * Will add a new listener to the server to be able to kick stale connections. <br>
   * This assumes that the server receives enough packets from other clients to be able to periodically update the list. <br>
   * The worst scenario of this system is when no connection sends packets, the list will never be updated. <br>
   * But normally the server automatically handle this case. By default, {@code 12 seconds} between the last read.
   *
   * @param server The server on which to add the listener.
   * @param timeout Time before kick if the {@link waitingPacket} is not received, in ms.
   * @param waitingPackets Packets classes to wait for.
   */
   public static void init(Server server, long timeout, Class<?>... waitingPackets) {
    server.addListener(new StaleConnectionsCleaner(timeout, waitingPackets));
  }


  protected final LinkedHashMap<Connection, Long> connecting = new LinkedHashMap<>();
  // Avoid to use a set if there is only one packet to wait for
  public final Class<?> waitingc;
  public final ObjectSet<Class<?>> waitingl;
  public final long ntimeout;
  protected long nextRefresh;
  protected boolean refreshing;

  public StaleConnectionsCleaner(long timeout, Class<?>... waitingPackets) {
    waitingc = waitingPackets.length == 1 ? waitingPackets[0] : null;
    waitingl = waitingPackets.length == 1 ? null : ObjectSet.with(waitingPackets);
    ntimeout = timeout * 1_000_000L;
  }

  @Override
  public void connected(Connection connection) {
    long deadline = System.nanoTime() + ntimeout;
    if (connecting.isEmpty()) nextRefresh = deadline;
    connecting.put(connection, deadline);
  }

  @Override
  public void disconnected(Connection connection, DcReason reason) {
    if (!refreshing) connecting.remove(connection); // Refresher remove faster
  }

  @Override
  public void received(Connection connection, Object object) {
    if (waitingc != null ? waitingc == object.getClass() : waitingl.contains(object.getClass()))
      connecting.remove(connection);
    cleanConnections();
  }

  public boolean refreshing() {
    return refreshing;
  }

  public void cleanConnections() {
    long now = System.nanoTime();
    if (now < nextRefresh) return; // No deadline due yet
    refreshing = true;
    long soonest = Long.MAX_VALUE;
    Iterator<Map.Entry<Connection, Long>> it = connecting.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Connection, Long> e = it.next();
      if (e.getValue() - now > 0) { // Rest are younger
        soonest = e.getValue();
        break;
      }
      e.getKey().close(DcReason.timeout);
      it.remove();
    }
    nextRefresh = soonest;
    refreshing = false;
  }
}