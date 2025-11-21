/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2021-2025  ZetaMap
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

package fr.zetamap.morecommands.modules.server;

import arc.Core;
import arc.func.Cons;
import arc.util.Nullable;

import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.net.Host;

import fr.zetamap.morecommands.util.Strings;


public class Server {
  //protected SwitchModule module;
  /** Internal server name, in kebab case without any colors and glyphs. */
  public final String name;
  protected String alias, displayName, ip;
  protected int port;
  protected boolean adminOnly, pingScheduled;
  public Host info;

  public Server(String name, String address) throws IllegalArgumentException {
    this(name, null, null, address, false);
  }
  
  public Server(String name, @Nullable String alias, @Nullable String displayName, String address, boolean admin) 
  throws IllegalArgumentException {
    this.name = name;
    this.alias = alias;
    this.displayName = displayName;
    this.adminOnly = admin;
    setAddress(address);
  }

  public Server(String name, String ip, int port) {
    this(name, null, null, ip, port, false);
  }
  
  public Server(String name, @Nullable String alias, @Nullable String displayName, String ip, int port, boolean admin) {
    this.name = name;
    this.alias = alias;
    this.displayName = displayName;
    this.ip = ip;
    this.port = port;
    this.adminOnly = admin;
  }
  
  public String alias() { return alias; }
  public String displayName() { return displayName; }
  public String ip() { return ip; }
  public int port() { return port; }
  public boolean adminOnly() { return adminOnly; }

  /** Parses ip:port format. */
  protected void setAddress(String address) {
    // From {@link mindustry.ui.dialogs.JoinDialog.Server#setIP(String)}.
    String i = address;
    int p = Vars.port;
    boolean isIpv6 = Strings.count(address, ':') > 1;
    
    if (isIpv6 && address.lastIndexOf("]:") != -1 && address.lastIndexOf("]:") != address.length()-1) {
      int idx = address.indexOf("]:");
      i = address.substring(1, idx);
      p = Strings.parseInt(address.substring(idx + 2));
    } else if (!isIpv6 && address.lastIndexOf(':') != -1 && address.lastIndexOf(':') != address.length()-1){ 
      int idx = address.lastIndexOf(':');
      i = address.substring(0, idx);
      p = Strings.parseInt(address.substring(idx+1));
    } else {
      i = address;
      p = Vars.port;
    }

    if (p == Integer.MIN_VALUE) {
      i = address;
      p = Vars.port;
    }
    
    this.ip = i;
    this.port = p;
  }
  
  /** @return {@link #ip}{@code :}{@link #port}. */
  public String address() {
    // From {@link mindustry.ui.dialogs.JoinDialog.Server#displayIP()}.
    return ip.indexOf(':') != -1 ? port != Vars.port ? '[' + ip + "]:" + port : ip : 
           port != Vars.port ? ip + ':' + port : ip;
  }
  
  public void ping() {
    ping(null, null);
  }

  public synchronized void ping(Cons<Host> valid, Cons<Exception> failed) {
    // Ignore if a ping is already scheduled
    if (pingScheduled) return;
    pingScheduled = true;
    
    Vars.net.pingHost(ip, port, h -> Core.app.post(() -> {
      info = h;
      pingScheduled = false;
      if (valid != null) valid.get(h);
    }), e -> Core.app.post(() -> {
      info = null;
      pingScheduled = false;
      if (failed != null) failed.get(e);
    }));
  }

  public void connect(Player player, Cons<PreConnect.Availability> status) {
    PreConnect.Availability pre = PreConnect.verify(player.admin, this);
    status.get(pre);
    if (pre.ok()) {
      // Prefer information from ping when possible
      if (info == null) Call.connect(player.con, ip, port);
      else Call.connect(player.con, info.address, info.port);
    }
  }
}
