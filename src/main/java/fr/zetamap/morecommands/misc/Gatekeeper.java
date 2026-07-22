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

package fr.zetamap.morecommands.misc;

import arc.Events;
import arc.graphics.Color;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Time;

import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.net.Administration.Config;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;

import fr.zetamap.morecommands.util.Logger;
import fr.zetamap.morecommands.util.Strings;
import fr.zetamap.morecommands.util.Timekeeper;


public class Gatekeeper {
  protected static final Logger logger = new Logger(Gatekeeper.class);
  /** Will avoid too many pipeline runs, for the same client, without creating an account for them. */
  protected static final ObjectMap<String, Long> kickCooldown = new ObjectMap<>();
  protected static final Timekeeper timer = Timekeeper.ofSeconds(60); // cleanup every minutes

  public static long defaultCooldownDuration = 10 * 1000;
  /** Whether to reject the client if all processors in the pipeline failed. */
  public static boolean rejectOnAllFailed = true;
  /** Config to change the {@link Gatekeeper} severity. It's just a server config field for {@link #rejectOnAllFailed}. */
  public static Config severity;


  public static void add(String name, Processor proc) {
    add(name, Priority.normal, proc);
  }

  /** Will replace any existing processor named same, and remove it from other priorities. */
  public static void add(String name, Priority priority, Processor proc) {
    if (name == null || name.isEmpty()) throw new NullPointerException("empty processor name");
    remove(name);
    priority.processors.put(name, proc);
  }

  public static void remove(String name) {
    for (Priority p : Priority.all) {
      if (p.processors.remove(name) != null) break;
    }
  }

  public static Processor get(String name) {
    for (Priority p : Priority.all) {
      Processor proc = get(name, p);
      if (proc != null) return proc;
    }
    return null;
  }

  public static Processor get(String name, Priority priority) {
    return priority.processors.get(name);
  }

  public static boolean has(Processor proc) {
    return priorityOf(proc) != null;
  }

  public static boolean has(Processor proc, Priority priority) {
    return priority.processors.orderedKeys().contains(n -> get(n, priority) == proc);
  }

  public static boolean has(String name) {
    return priorityOf(name) != null;
  }

  public static boolean has(String name, Priority priority) {
    return priority.processors.containsKey(name);
  }

  public static Priority priorityOf(Processor proc) {
    for (Priority p : Priority.all) {
      if (has(proc, p)) return p;
    }
    return null;
  }

  public static Priority priorityOf(String name) {
    for (Priority p : Priority.all) {
      if (has(name, p)) return p;
    }
    return null;
  }

  /** Process the {@link Gatekeeper} pipeline. */
  public static Result process(Context context) {
    Events.fire(new MCEvents.GatekeeperProcessStartedEvent(context));
    MCEvents.GatekeeperProcessedEvent processEvent = new MCEvents.GatekeeperProcessedEvent(context);
    int total = 0, failed = 0;

    for (Priority p : Priority.all) {
      for (OrderedMap.Entry<String, Processor> e : p.processors) {
        total++;
        try {
          Result result = e.value.process(context);
          Events.fire(processEvent.set(e.key, p, result));
          if (result instanceof Result.Rejected) return result;

        } catch (Exception ex) {
          failed++;
          logger.err("Processor @ failed to verify player '@' [@, @].", e.key, context.name, context.address,
                     context.uuid);
          logger.err(ex);
        }
      }
    }

    return rejectOnAllFailed && total > 0 && failed == total ?
             reject("[scarlet]Unable to connect you![]\nPlease try to reconnect.\n\n"
                  + "[lightgray]If this happens several times, please contact an administrator.[]") :
           accept();
  }

  /** Shortcut of {@link Result#accepted}. Always returns the same result instance. */
  public static Result accept() { return Result.accepted; }
  /** Shortcut of {@link Result.Rejected#set()}. Always returns the same result instance. */
  public static Result reject() { return Result.rejected.set(); }
  /** Shortcut of {@link Result.Rejected#set(long)}. Always returns the same result instance. */
  public static Result reject(long duration) { return Result.rejected.set(duration); }
  /** Shortcut of {@link Result.Rejected#set(String)}. Always returns the same result instance. */
  public static Result reject(String reason) { return Result.rejected.set(reason); }
  /** Shortcut of {@link Result.Rejected#set(String, long)}. Always returns the same result instance. */
  public static Result reject(String reason, long duration) { return Result.rejected.set(reason, duration); }
  /** Shortcut of {@link Result.Rejected#set(KickReason)}. Always returns the same result instance. */
  public static Result reject(KickReason reason) { return Result.rejected.set(reason); }
  /** Shortcut of {@link Result.Rejected#set(KickReason, long)}. Always returns the same result instance. */
  public static Result reject(KickReason reason, long duration) { return Result.rejected.set(reason, duration); }

  protected static void cleanupCache() {
    if (!timer.exceeded()) return;
    long now = Time.millis();
    ObjectMap.Entries<String, Long> it = kickCooldown.iterator();
    ObjectMap.Entry<String, Long> e;
    while (it.hasNext()) {
      e = it.next();
      if (e.value <= now) it.remove();
    }
  }

  public static void init() {
    Events.on(EventType.ConnectPacketEvent.class, e -> {
      // Ignore invalid packets
      if (e.connection.kicked) return;
      if (e.packet.uuid == null || e.packet.usid == null || e.connection.hasBegunConnecting) {
        e.connection.kick(KickReason.idInUse, 0);
        return;
      }

      // Recent kick
      Long lastKick = kickCooldown.get(e.packet.uuid);
      if (lastKick != null) {
        if (lastKick > Time.millis()) {
          e.connection.kick(KickReason.recentKick, 0);
          return;
        } else kickCooldown.remove(e.packet.uuid);
      }

      cleanupCache();
      Context.instance.set(e.packet.version, e.packet.versionType, e.packet.mods, e.packet.name,
                           e.packet.locale, e.packet.uuid, e.packet.usid, e.connection.address,
                           e.packet.mobile, e.packet.color);

      Result result = process(Context.instance);
      if (!(result instanceof Result.Rejected reject)) return;

      if (reject.reason != null) e.connection.kick(reject.reason, 0);
      else if (reject.kickReason != null) e.connection.kick(reject.kickReason, 0);
      else e.connection.kick(KickReason.idInUse, 0); //idk what reason to use.
      kickCooldown.put(e.packet.uuid, Time.millis() + reject.duration);
      reject.reset();
    });

    severity = new Config("rejectOnFailed", "Whether to reject the client if the MoreCommands Gatekeeper pipeline failed.",
                          rejectOnAllFailed, () -> rejectOnAllFailed = severity.bool());
  }


  public interface Result {
    Accepted accepted = new Accepted();
    Rejected rejected = new Rejected();

    public static class Accepted implements Result {}

    /** Instance is reused, do not nest. */
    public static class Rejected implements Result {
      public String reason;
      public KickReason kickReason;
      /** in ms. */
      public long duration;

      public Rejected set() { return set(null, null, defaultCooldownDuration); }
      public Rejected set(long duration) { return set(null, null, duration); }
      public Rejected set(String reason) { return set(reason, defaultCooldownDuration); }
      public Rejected set(String reason, long duration) { return set(reason, null, duration); }
      public Rejected set(KickReason reason) { return set(reason, defaultCooldownDuration); }
      public Rejected set(KickReason reason, long duration) { return set(null, reason, duration); }

      Rejected set(String reason, KickReason kickReason, long duration) {
        this.reason = reason;
        this.kickReason = kickReason;
        this.duration = duration;
        return this;
      }

      void reset() {
        this.reason = null;
        this.kickReason = null;
        this.duration = 0;
      }
    }
  }

  public interface Processor {
    /** {@code null} can be used to specify {@link Result#accepted}. */
    Result process(Context context);
  }

  /** Instance is reused, do not nest or modify fields while processing the {@link Gatekeeper}. */
  public static class Context {
    static final Context instance = new Context();

    public int version;
    public String versionType;
    public Seq<String> mods;
    public String name, strippedName, locale, uuid, usid, address;
    public boolean mobile;
    public Color color = Color.white.cpy();
    /** The player's info. Can be {@code null} if the player is new on the server. */
    public PlayerInfo info;

    Context set(int version, String versionType, Seq<String> mods, String name, String locale, String uuid,
                String usid, String address, boolean mobile, int color) {
      this.version = version;
      this.versionType = versionType;
      this.mods = mods;
      this.name = name;
      this.strippedName = name == null ? null : Strings.normalize(name);
      this.locale = locale == null ? "en" : locale;
      this.uuid = uuid;
      this.usid = usid;
      this.address = address;
      this.mobile = mobile;
      this.color.set(color).a(1);
      this.info = uuid == null ? null : Vars.netServer.admins.getInfoOptional(uuid);
      return this;
    }
  }

  public enum Priority {
    high, normal, low;

    static final Priority[] all = values();
    /** More simple to store processors here. */
    final OrderedMap<String, Processor> processors = new OrderedMap<>(8);
  }
}
