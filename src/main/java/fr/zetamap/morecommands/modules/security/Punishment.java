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

package fr.zetamap.morecommands.modules.security;

import arc.util.Nullable;
import arc.util.Time;


public class Punishment {
  static int lastId;
  
  public final int id;
  public @Nullable String author; 
  public final String target;
  public @Nullable String address;
  public final Type type;
  public final long creation;
  public long expire;
  public @Nullable String reason;
  public @Nullable Pardon pardon;

  Punishment(int id, String author, String target, String address, Type type, long creation, long expire, String reason, 
             Pardon pardon) {
    this.id = id;
    this.author = author;
    this.target = target;
    this.address = address;
    this.type = type;
    this.creation = creation;
    this.expire = Math.max(expire, -1);
    this.reason = reason;
    this.pardon = pardon;
  }
  
  /** A negative {@code duration} can be used to specify a forever punishment. */
  public Punishment(String author, String target, String address, Type type, long duration, String reason) {
    this.id = lastId++;
    this.author = author;
    this.target = target;
    this.address = address;
    this.type = type;
    this.creation = Time.millis();
    this.expire = duration < 0 ? -1 : creation + duration;
    this.reason = reason;
  }
  
  public boolean expired() {
    return pardoned() || !permanant() && Time.millis() > expire;
  }
  
  public boolean permanant() {
    return expire < 0;
  }
  
  public boolean pardoned() {
    return pardon != null;
  }
  
  public long duration() {
    return expire - creation;
  }
  
  public long remaining() {
    return permanant() ? Long.MAX_VALUE : Time.millis() - expire;
  }
  
  public void setDuration(long duration) {
    expire = creation + duration;
  }
  
  
  public static enum Type {
    ban("banned", true, PunishmentDuration.oneMonth), 
    kick("kicked", true, PunishmentDuration.thirtyMinutes), 
    votekick("vote kicked", true, PunishmentDuration.oneHour), 
    warn("warned", false, PunishmentDuration.permanant), 
    mute("muted", false, PunishmentDuration.threeHours), 
    freeze("frozen", false, PunishmentDuration.oneHour);
    
    public static final Type[] all = values();
    
    public final String verb;
    /** Whether this kind of punishment implies a kick of the target. */
    public final boolean impliesKick;
    public final PunishmentDuration defaultDuration;
    
    Type(String verb, boolean impliesKick, PunishmentDuration defaultDuration) {
      this.verb = verb;
      this.impliesKick = impliesKick;
      this.defaultDuration = defaultDuration;
    }
  }
  
  public static class Pardon {
    public @Nullable String author;
    public final long when;
    public @Nullable String reason;
    
    public Pardon() { this(null, null); }
    public Pardon(String author) { this(author, null); }
    public Pardon(String author, String reason) { this(author, Time.millis(), reason); }
    public Pardon(String author, long when, String reason) {
      this.author = author;
      this.when = when;
      this.reason = reason;
    } 
  }
}
