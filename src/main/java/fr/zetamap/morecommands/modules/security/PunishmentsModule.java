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

package fr.zetamap.morecommands.modules.security;

import arc.Events;
import arc.math.geom.Vec2;
import arc.struct.*;
import arc.util.Structs;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;

import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.net.Administration;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.misc.Gatekeeper;
import fr.zetamap.morecommands.misc.MCEvents;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractSaveableModule;
import fr.zetamap.morecommands.util.DurationFormatter;
import fr.zetamap.morecommands.util.IntervalProv;
import fr.zetamap.morecommands.util.JsonSettings;
import fr.zetamap.morecommands.util.Strings;


public class PunishmentsModule extends AbstractSaveableModule {
  private final IntMap<Punishment> byId = new IntMap<>();
  private final ObjectMap<String, Seq<Punishment>> byPlayer = new ObjectMap<>(), byAddress = new ObjectMap<>();

  private final IntMap<IntervalProv> rateLimitCache = new IntMap<>();
  /** Reused instance */
  private final Punishment[] currentPunishment = new Punishment[Punishment.Type.all.length];
  private final OrderedMap<PlayerData, Vec2> currentlyFrozen = new OrderedMap<>();
  private final IntervalProv freezeTimer = new IntervalProv(2);
  private final float messageRateLimit = 3 * 60, freezePunishmentUpdateTime = 5 * 60, freezeUpdateTime = 60 / 30;
  private final int rates = 2;

  private String punishmentNote;

  private Punishment findLatest(Seq<Punishment> pp, Punishment.Type type) {
    Punishment result = null;
    for (int i=pp.size-1; i>=0; i--) {
      Punishment p = pp.get(i);
      if ((p.type == type || type == null) && !p.expired() && (result == null || p.creation >= result.creation))
        result = p;
    }
    return result;
  }

  private void findLatest(Seq<Punishment> pp, Punishment[] buf) {
    for (int i=pp.size-1; i>=0; i--) {
      Punishment p = pp.get(i), c = buf[p.type.ordinal()];
      if (!p.expired() && (c == null || p.creation >= c.creation))
        buf[p.type.ordinal()] = p;
    }
  }

  public Seq<Punishment> all() {
    return byId.values().toArray().sort(Structs.comparingLong(p -> p.creation));
  }

  @SuppressWarnings("unchecked")
  public Seq<Punishment>[] allType() {
    Seq<Punishment>[] types = new Seq[Punishment.Type.all.length];
    for (Punishment p : byId.values()) {
      Seq<Punishment> pl = types[p.type.ordinal()];
      if (pl == null)
        types[p.type.ordinal()] = pl = new Seq<>(byId.size / Punishment.Type.all.length);
      pl.add(p);
    }
    for (Seq<Punishment> pl : types) pl.sort(Structs.comparingLong(p -> p.creation));
    return types;
  }

  public ObjectMap<String, Seq<Punishment>> playerPunishments() {
    return byPlayer;
  }

  public ObjectMap<String, Seq<Punishment>> addressPunishments() {
    return byAddress;
  }

  /** Get all punishments of the specified type. Sorted by creation date. */
  public Seq<Punishment> get(Punishment.Type type) {
    Seq<Punishment> arr = new Seq<>(byId.size / Punishment.Type.all.length);
    for (Punishment p : byId.values()) {
      if (p.type == type) arr.add(p);
    }
    return arr.sort(Structs.comparingLong(p -> p.creation));
  }

  /** Get all punishments of an address of the specified type. */
  public ObjectMap<String, Seq<Punishment>> addressGet(Punishment.Type type) {
    ObjectMap<String, Seq<Punishment>> map = new ObjectMap<>(byAddress.size / Punishment.Type.all.length);
    byAddress.each((a, pl) -> pl.each(p -> p.type == type, p -> map.get(a, Seq::new).add(p)));
    return map;
  }

  /** Get a punishment by his {@code id}. */
  public Punishment get(int id) {
    return byId.get(id);
  }

  /** Get all {@code type} punishments of a player. Or {@code null} if never punished. */
  public Seq<Punishment> get(String uuid, Punishment.Type type) {
     Seq<Punishment> pp = get(uuid);
     return pp == null ? null : pp.select(type::thisType);
  }

  /** Get all punishments of a player. Or {@code null} if never punished. */
  public Seq<Punishment> get(String uuid) {
    return byPlayer.get(uuid);
  }

  /** Get all punishments of a player. Or {@code null} if never punished. */
  public Seq<Punishment> get(PlayerData player) {
    return get(player.player.uuid());
  }

  /** Get all {@code type} punishments of an address. Or {@code null} if never punished. */
  public Seq<Punishment> addressGet(String address, Punishment.Type type) {
     Seq<Punishment> ap = addressGet(address);
     return ap == null ? null : ap.select(type::thisType);
  }

  /** Get all punishments of an address. Or {@code null} if never punished. */
  public Seq<Punishment> addressGet(String address) {
    return byAddress.get(address);
  }

  /** Get the last not pardoned and expired punishment of the specified type of a player. */
  public Punishment last(String uuid, Punishment.Type type) {
    Seq<Punishment> pp = get(uuid);
    return pp == null ? null : findLatest(pp, type);
  }

  /** Get the last not pardoned or expired punishment of the specified type of a player. */
  public Punishment last(PlayerData player, Punishment.Type type) {
    return last(player.player.uuid(), type);
  }

  /** Get the last not pardoned and expired punishment of a player. */
  public Punishment last(String uuid) {
    Seq<Punishment> pp = get(uuid);
    return pp == null ? null : findLatest(pp, (Punishment.Type)null);
  }

  /** Get the last not pardoned and expired punishment of a player. */
  public Punishment last(PlayerData player) {
    return last(player.player.uuid());
  }

  /** Get the last not pardoned and expired punishment of the specified type of an address. */
  public Punishment addressLast(String address, Punishment.Type type) {
    Seq<Punishment> ap = addressGet(address);
    return ap == null ? null : findLatest(ap, type);
  }

  /** Get the last not pardoned and expired punishment of an address. */
  public Punishment addressLast(String address) {
    Seq<Punishment> ap = addressGet(address);
    return ap == null ? null : findLatest(ap, (Punishment.Type)null);
  }

  /** @return whether the player is currently punished of something. */
  public boolean isPunished(String uuid) {
    return last(uuid) != null;
  }

  /** @return whether the player is currently punished of something. */
  public boolean isPunished(PlayerData player) {
    return isPunished(player.player.uuid());
  }

  /** @return whether the address is currently punished of something. */
  public boolean addressIsPunished(String address) {
    return addressLast(address) != null;
  }

  /** @return whether the player is currently punished of the specified type. */
  public boolean is(String uuid, Punishment.Type type) {
    return last(uuid, type) != null;
  }

  /** @return whether the player is currently punished of the specified type. */
  public boolean is(PlayerData player, Punishment.Type type) {
    return last(player, type) != null;
  }

  /** @return whether the the is currently punished of the specified type. */
  public boolean addressIs(String address, Punishment.Type type) {
    return addressLast(address, type) != null;
  }

  /** @return the latest active punishments of the player. (always returns the same array instance) */
  public Punishment[] current(String uuid) {
    Seq<Punishment> pp = get(uuid);
    for (int i=0; i<currentPunishment.length; i++) currentPunishment[i] = null;
    if (pp == null) return currentPunishment;
    findLatest(pp, currentPunishment);
    return currentPunishment;
  }

  /** @return the latest active punishments of the player. (always returns the same array instance) */
  public Punishment[] current(PlayerData player) {
    return current(player.player.uuid());
  }

  /** @return the latest active punishments of an address. (always returns the same array instance) */
  public Punishment[] addressCurrent(String address) {
    Seq<Punishment> pp = addressGet(address);
    for (int i=0; i<currentPunishment.length; i++) currentPunishment[i] = null;
    if (pp == null) return currentPunishment;
    findLatest(pp, currentPunishment);
    return currentPunishment;
  }

  private Punishment punish(PlayerData author, String target, PlayerData player, String address, Punishment.Type type,
                            long duration,  String reason) {
    // Try to find a match in the server settings using the address, if target is not specified.
    if (target == null) {
      Seq<Administration.PlayerInfo> found = Vars.netServer.admins.findByIPs(address);
      // Only take if one result found
      if (found.size == 1) target = found.first().id;
    }
    Punishment p = new Punishment(author == null ? null : author.player.uuid(), target, address, type, duration, reason);
    byId.put(p.id, p);
    if (target != null) byPlayer.get(target, Seq::new).add(p);
    if (address != null) byAddress.get(address, Seq::new).add(p);
    setModified();
    Events.fire(new MCEvents.PunishmentEvent(author, player != null ? player : PlayerData.get(target), p));
    return p;
  }

  /** Punish a player of something. */
  public Punishment punish(PlayerData author, String target, String address, Punishment.Type type, String reason) {
    return punish(author, target, null, address, type, type.defaultDuration.duration, reason);
  }

  /** Punish a player of something. */
  public Punishment punish(PlayerData author, PlayerData target, Punishment.Type type, String reason) {
    return punish(author, target.player.uuid(), target, target.player.ip(), type, type.defaultDuration.duration, reason);
  }

  /** Punish a player of something. */
  public Punishment punish(PlayerData author, String target, String address, Punishment.Type type, long duration,
                           String reason) {
    return punish(author, target, null, address, type, duration, reason);
  }

  /** Punish a player of something. */
  public Punishment punish(PlayerData author, PlayerData target, Punishment.Type type, long duration, String reason) {
    return punish(author, target.player.uuid(), target, target.player.ip(), type, duration, reason);
  }

  /** Punish a player of something. */
  public Punishment punish(PlayerData author, String target, String address, Punishment.Type type,
                           PunishmentDuration duration, String reason) {
    return punish(author, target, address, type, duration.duration, reason);
  }

  /** Punish a player of something. */
  public Punishment punish(PlayerData author, PlayerData target, Punishment.Type type, PunishmentDuration duration,
                           String reason) {
    return punish(author, target.player.uuid(), target.player.ip(), type, duration.duration, reason);
  }

  /** @return {@code true} if pardoned, else {@code false} if already pardoned.*/
  public boolean pardon(PlayerData author, Punishment punishment, String reason) {
    if (punishment.pardoned()) return false;
    punishment.pardon = new Punishment.Pardon(author == null ? null : author.player.uuid(), reason);
    setModified();
    Events.fire(new MCEvents.PunishmentPardonedEvent(author, punishment, punishment.pardon));
    return true;
  }

  /** @return {@code true} if pardoned, else {@code false} if already pardoned or not found.*/
  public boolean pardon(PlayerData author, int punishment, String reason) {
    Punishment p = get(punishment);
    return p == null ? false : pardon(author, p, reason);
  }

  /** @return {@code true} if pardoned, else {@code false} if already pardoned or not found. */
  public boolean pardon(PlayerData author, int punishment) {
    return pardon(author, punishment, null);
  }

  /**
   * The note added at bottom of the message when kicking or banning a player. <br>
   * This can be used to specify a place to appeal the punishment.
   */
  public String getPunishmentNote() {
    return punishmentNote;
  }

  /**
   * The note added at bottom of the message when kicking or banning a player. <br>
   * This can be used to specify a place to appeal the punishment.
   */
  public void setPunishmentNote(String text) {
    punishmentNote = text;
    setModified();
  }

  @SuppressWarnings("rawtypes")
  @Override
  protected void initImpl() {
    addSerializer(Punishment.class, new Json.Serializer<Punishment>() {
      @Override
      public void write(Json json, Punishment object, Class knownType) {
        json.writeObjectStart();
        json.writeValue("id", object.id);
        if (object.author != null) json.writeValue("author", object.author);
        json.writeValue("target", object.target);
        if (object.address != null) json.writeValue("address", object.address);
        json.writeValue("type", object.type);
        json.writeValue("creation", object.creation);
        json.writeValue("expire", object.expire);
        if (object.reason != null) json.writeValue("reason", object.reason);
        if (object.pardon != null) json.writeValue("pardon", object.pardon);
        json.writeObjectEnd();
      }

      @Override
      public Punishment read(Json json, JsonValue jsonData, Class type) {
        return new Punishment(
          jsonData.getInt("id"),
          jsonData.getString("author", null),
          jsonData.getString("target"),
          jsonData.getString("address", null),
          json.readValue("type", Punishment.Type.class, jsonData),
          jsonData.getLong("creation"),
          jsonData.getLong("expire"),
          jsonData.getString("reason", null),
          json.readValue("pardon", Punishment.Pardon.class, jsonData)
        );
      }
    });

    addSerializer(Punishment.Pardon.class, new Json.Serializer<Punishment.Pardon>() {
      @Override
      public void write(Json json, Punishment.Pardon object, Class knownType) {
        json.writeObjectStart();
        if (object.author != null) json.writeValue("author", object.author);
        json.writeValue("when", object.when);
        if (object.reason != null) json.writeValue("reason", object.reason);
        json.writeObjectEnd();
      }

      @Override
      public Punishment.Pardon read(Json json, JsonValue jsonData, Class type) {
        return new Punishment.Pardon(
          jsonData.getString("author", null),
          jsonData.getLong("when"),
          jsonData.getString("reason", null)
        );
      }
    });

    //TODO: re-apply freeze at reconnection

    Events.on(EventType.PlayerLeave.class, e -> {
      rateLimitCache.remove(e.player.id);
      currentlyFrozen.remove(PlayerData.get(e.player));
    });

    // Avoid to block players out of map bounds
    Events.on(EventType.WorldLoadEvent.class, e -> {
      currentlyFrozen.each((p, v) -> {
        if (p.player.core() != null) v.set(p.player.closestCore());
        else v.set(Vars.world.unitWidth()/2, Vars.world.unitHeight()/2);
      });
    });

    Events.on(MCEvents.PunishmentEvent.class, e -> {
      // Ban and kick also in the server
      if (e.punishment.type.impliesKick) {
        Administration.PlayerInfo info = null;
        boolean searched = false;
        String address = null;

        if (e.punishment.address != null) address = e.punishment.address;
        else if (e.target != null) address = e.target.player.ip();
        else if (e.punishment.target != null) {
          info = Vars.netServer.admins.getInfoOptional(e.punishment.target);
          searched = true;
          if (info != null) address = info.lastIP;
        }

        // Just run the events; doesn't save the action on the server, it will be a problem later on.
        if (e.punishment.type == Punishment.Type.ban) {
          if (e.punishment.target != null) {
            if (!searched) info = Vars.netServer.admins.getInfoOptional(e.punishment.target);
            if (info != null) {
              PlayerData player = PlayerData.get(info.id);
              Events.fire(new EventType.PlayerBanEvent(player == null ? null : player.player, info.id));
            }
            //if (info != null) Vars.netServer.admins.banPlayerID(e.punishment.target);
          }
          if (address != null) Events.fire(new EventType.PlayerIpBanEvent(address));
          //if (address != null) Vars.netServer.admins.banPlayerIP(address);
        } //else if (address != null)
          //Vars.netServer.admins.handleKicked(e.punishment.target, address, e.punishment.duration());
      }

      if (e.target == null) return;

      String reason = getReason(true, false, e.author == null ? null : e.author.getName(), e.punishment);
      if (e.punishment.type.impliesKick) e.target.player.kick(reason);
      else Call.infoMessage(e.target.player.con, reason);

      if (e.punishment.type == Punishment.Type.freeze) currentlyFrozen.put(e.target, new Vec2().set(e.target.player));
      else notifyPunishment(e.target, e.author, e.punishment);
    });

    Events.on(MCEvents.PunishmentPardonedEvent.class, e -> {
      if (!e.punishment.type.impliesKick) return;
      Administration.PlayerInfo info = e.punishment.target == null ? null :
                                       Vars.netServer.admins.getInfoOptional(e.punishment.target);
      String address = null;

      if (e.punishment.address != null) address = e.punishment.address;
      else if (info != null) address = info.lastIP;

      // Removes punishment from the server, to avoid possible issues.
      if (e.punishment.type == Punishment.Type.ban) {
        if (info != null) {
          info.banned = true; // fake the state
          Vars.netServer.admins.unbanPlayerID(e.punishment.target);
        }
        if (address != null) Vars.netServer.admins.unbanPlayerIP(address);
      } else {
        if (address != null) Vars.netServer.admins.kickedIPs.remove(address);
        if (info != null) info.lastKicked = 0l;
      }
    });

    Events.run(EventType.Trigger.beforeGameUpdate, () -> {
      //TODO: make a global thread that updates punishments?
      // Update freeze punishment every 5 seconds
      if (freezeTimer.get(0, freezePunishmentUpdateTime)) {
        OrderedMap.Entries<PlayerData, Vec2> it = currentlyFrozen.entries();
        while (it.hasNext()) {
          OrderedMap.Entry<PlayerData, Vec2> e = it.next();
          if (!is(e.key.player.uuid(), Punishment.Type.freeze)) it.remove();
        }
      }

      // Block movements when frozen
      if (freezeTimer.get(1, freezeUpdateTime))
        currentlyFrozen.each(Modules.teleport::teleport);
    });

    Vars.netServer.admins.addChatFilter((p, m) ->
      getrate(p).get(0, messageRateLimit, () -> {
        Punishment mute = last(p.uuid(), Punishment.Type.mute);
        if (mute != null) {
          Players.err(p, "You're muted, you can't speak! [gray](ends in [lightgray]@[])",
                      DurationFormatter.format(mute.remaining()));
          return null;
        }
        return m;
      })
    );

    Vars.netServer.admins.addActionFilter(a ->
      getrate(a.player).get(1, messageRateLimit, () -> {
        if (is(a.player.uuid(), Punishment.Type.freeze)) {
          Players.err(a.player, "You are frozen, you can no longer move or interact with the game elements.");
          return false;
        }
        return true;
      })
    );

    // IP block
    /*// Disabled because this overrides message of the punishment target
    Events.on(EventType.ConnectionEvent.class, e -> {
      Punishment[] current = addressCurrent(e.connection.address);
      for (Punishment.Type t : Punishment.Type.all) {
        Punishment p = current[t.ordinal()];
        if (!t.impliesKick || p == null) continue;
        Administration.PlayerInfo author = p.author == null ? null : Vars.netServer.admins.getInfoOptional(p.author);
        e.connection.kick(getReason(false, true, author != null ? author.lastName : null, p), 0);
        return;
      }
    });
    */

    Gatekeeper.add(internalName(), Gatekeeper.Priority.high, ctx -> {
      Punishment[] current = current(ctx.uuid);
      for (Punishment.Type t : Punishment.Type.all) {
        Punishment p = current[t.ordinal()];
        if (!t.impliesKick || p == null) continue;
        Administration.PlayerInfo author = p.author == null ? null : Vars.netServer.admins.getInfoOptional(p.author);
        return Gatekeeper.reject(getReason(false, false, author != null ? author.lastName : null, p));
      }
      current = addressCurrent(ctx.address);
      for (Punishment.Type t : Punishment.Type.all) {
        Punishment p = current[t.ordinal()];
        if (!t.impliesKick || p == null) continue;
        Administration.PlayerInfo author = p.author == null ? null : Vars.netServer.admins.getInfoOptional(p.author);
        return Gatekeeper.reject(getReason(false, true, author != null ? author.lastName : null, p));
      }
      return Gatekeeper.accept();
    });
  }

  private IntervalProv getrate(Player player) {
    return rateLimitCache.get(player.id, () -> new IntervalProv(rates));
  }

  public void notifyPunishment(PlayerData target, PlayerData author, Punishment punishment) {
    StringBuilder builder = new StringBuilder();
    Seq<Object> args = new Seq<>(4);

    builder.append('@');
    args.add(target.getName());
    if (author != null && punishment.type != Punishment.Type.votekick) {
      builder.append(" by @");
      args.add(author.getName());
    }
    if (punishment.type != Punishment.Type.warn) {
      builder.append(" for @");
      args.add(punishment.permanant() ? "life" : DurationFormatter.format(punishment.duration()));
    }
    if (punishment.reason != null) {
      builder.append(": @");
      args.add(punishment.reason);
    } else if (punishment.type != Punishment.Type.freeze)
      builder.append(": [lightgray](no reason)[]");
    builder.append('.');

    Modules.messaging.serverWarn(Strings.capitalize(punishment.type.name()), builder.toString(), args.toArray());
  }

  public String getReason(boolean now, boolean isAddress, String authorName, Punishment punishment) {
    StringBuilder builder = new StringBuilder();

    if (isAddress && punishment.address != null)
      builder.append("Your IP address [gray]([lightgray]").append(punishment.address).append("[])[] ")
             .append(now ? "has been" : "is");
    else builder.append(now ? "You have been" : "You are");

    builder.append(" [accent]").append(punishment.type.verb).append("[]");
    if (punishment.type.impliesKick) builder.append(" from this server");
    //TODO: sentence seems not correct if now is false

    // Avoid to display the people who started the votekick, to avoid potential consequences from players...
    if (authorName != null && punishment.type != Punishment.Type.votekick)
      builder.append(" by [accent]").append(authorName).append("[white]");

    // A punishment duration for a warn makes no sense.
    if (punishment.type != Punishment.Type.warn)
      builder.append(" for [accent]").append(DurationFormatter.format(now ? punishment.duration() : punishment.remaining()))
             .append("[]");

    if (punishment.reason != null) builder.append(".\nReason: [accent]").append(punishment.reason).append("[white].");
    else if (punishment.type != Punishment.Type.freeze) builder.append(".\nReason: [lightgray](no reason)[].");
    if (punishment.type == Punishment.Type.warn) return builder.toString();

    builder.append("\n\n[lightgray]");
    if (punishment.type == Punishment.Type.mute)
      builder.append("You can no longer speak in the chat.");
    else if (punishment.type == Punishment.Type.freeze)
      builder.append("You can no longer move or interact with the game elements.");
    else if (punishment.type.impliesKick) {
      builder.append("If you think this is a mistake, please report it to an administrator.");
      if (getPunishmentNote() != null) builder.append('\n').append(getPunishmentNote());
      builder.append("\n\n[gray]Punishment ID: [lightgray]").append(punishment.id);
    }

    return builder.toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void loadImpl(JsonSettings settings) {
    punishmentNote = settings.getString("note", null);
    Punishment.lastId = settings.getInt("last-id", 0);

    byId.clear();
    byPlayer.clear();
    byAddress.clear();
    settings.getOrPut("users", Seq.class, Punishment.class, Seq::new).each(p -> {
      Punishment punish = (Punishment)p;
      byId.put(punish.id, punish);
      if (punish.target != null) byPlayer.get(punish.target, Seq::new).add(punish);
      if (punish.address != null) byAddress.get(punish.address, Seq::new).add(punish);
    });

    // Check id
    int max = Strings.max(byId, i -> i.key); // idk why i can't use #keys()
    if (!byId.isEmpty() && Punishment.lastId <= max) {
      logger.warn("Invalid 'last-id' value! The field has been modified to the highest punishment id.");
      Punishment.lastId = max+1;
    }

    // Sort player punishments by creation date
    byPlayer.each((i, pp) -> pp.sort(Structs.comparingLong(p -> p.creation)));
    byAddress.each((a, pp) -> pp.sort(Structs.comparingLong(p -> p.creation)));
  }

  @Override
  protected void saveImpl(JsonSettings settings) {
    settings.put("note", punishmentNote);
    settings.put("last-id", Punishment.lastId);
    settings.put("users", Punishment.class, byId.values().toArray());
  }
}
