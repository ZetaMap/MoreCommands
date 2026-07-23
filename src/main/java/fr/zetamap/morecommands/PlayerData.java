/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2021-2026  ZetaMap
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

package fr.zetamap.morecommands;

import java.security.MessageDigest;
import java.util.Comparator;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.func.Cons;
import arc.struct.IntMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.serialization.Base64Coder;

import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.net.Packets.KickReason;

import fr.zetamap.morecommands.misc.Gatekeeper;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.modules.effect.Effects;
import fr.zetamap.morecommands.util.Strings;


/** Class that stores additional data to {@link Player}. */
public class PlayerData {
  /** In fact there is no dedicated "vanish" team, so just transfer to {@code team#255}. */
  public static final Team vanishTeam = Team.get(Team.all.length-1);

  protected static final IntMap<PlayerData> map = new IntMap<>();
  protected static final Seq<PlayerData> array = new Seq<>(false);

  public final Player player;
  /** The player uuid without the checksum part. So on 8 bytes instead of 16. */
  public final String shortUuid;

  /** Used to properly dereference the player whispers. Nulled to know when the player never whispered. */
  protected ObjectSet<PlayerData> whispers;
  public PlayerData whisperTarget;
  public Unit lastUnit;
  public Team lastTeam;
  public Effects effect;
  public String rainbowName, realName, uncoloredName, stripedName, tag, uncoloredTag;
  public int rainbowHue;
  public boolean rainbowed, inGodmode;

  private PlayerData(Player player) {
    this.player = player;
    shortUuid = getShortUuid(player.uuid());
    realName = player.name;
    uncoloredName = Strings.stripColors(player.name).strip();
    stripedName = Strings.stripGlyphs(uncoloredName).strip();
  }

  /** @return whether the current player's team is the {@link #vanishTeam}. */
  public boolean vanished() {
    return player.team() == vanishTeam;
  }

  public void setTag() { setTag(true); }
  public void setTag(boolean apply) {
    uncoloredTag = tag = null;
    if (!Modules.tags.enabled()) {
      if (apply) setName();
      return;
    }

    String t = Modules.tags.get(this);
    if (t != null) {
      tag = "[gold][[[white]" + t + "[gold]]";
      uncoloredTag = "[" + Strings.stripColors(t).strip() + "]";
    } else if (admin()) {
      tag = "[gold][[[scarlet]<Admin>[]][]";
      uncoloredTag = "[<Admin>]";
    }
    if (apply) setName();
  }

  /** Sets the player's name, with a tag, if they are enabled and there is one for the player or it's an admin. */
  public void setName() {
    if (vanished()) player.name = "";
    else if (tag == null) player.name = getName();
    else player.name = tag + " " + getName();
  }

  public void setRainbow(boolean enabled) {
    rainbowed = enabled;
    setName();
  }

  public void updateRainbow() {
    if (rainbowed) {
      rainbowHue += 5;
      rainbowHue %= 360;
      rainbowName = Strings.rainbowify(uncoloredName, rainbowHue, 5);
    } else {
      rainbowHue = 0;
      rainbowName = null;
    }
  }

  /**
   * @return nothing if {@link #vanished} is {@code true} else the {@link #rainbowName} if {@link #rainbowed} is {@code true},
   *         else the player's colored name.
   */
  public String getName() {
    return vanished() ? "" : rainbowed ? rainbowName : "[#" + player.color.toString() + "]" + realName + "[white]";
  }

  public boolean hasWhispered() {
    return whispers != null;
  }

  public void setWhisper(PlayerData other) {
    whisperTarget = other;
    if (whispers == null) whispers = new ObjectSet<>();
    whispers.add(other);

    other.whisperTarget = this;
    if (other.whispers == null) other.whispers = new ObjectSet<>();
    other.whispers.add(this);
  }

  public void removeWhisper() {
    if (whisperTarget != null && whisperTarget.whisperTarget == this)
      whisperTarget.whisperTarget = null;
    whisperTarget = null;

    if (whispers == null) return;
    whispers.each(p -> {
      if (p.whispers.remove(this) && p.whisperTarget == this) p.whisperTarget = null;
    });
    whispers.clear();
    whispers = null;
  }

  public boolean admin() {
    return player.admin;
  }

  // Players messaging shortcuts
  public void errPlayerNotFound() { Players.errPlayerNotFound(this); }
  public void errArgUseDenied() { Players.errArgUseDenied(this); }
  public void errCommandUseDenied() { Players.errCommandUseDenied(this); }
  public void err(String message) { Players.err(this, message); }
  public void err(String message, Object... args) { Players.err(this, message, args); }
  public void info(String message) { Players.info(this, message); }
  public void info(String message, Object... args) { Players.info(this, message, args); }
  public void warn(String message) { Players.warn(this, message); }
  public void warn(String message, Object... args) { Players.warn(this, message, args); }
  public void ok(String message) { Players.ok(this, message); }
  public void ok(String message, Object... args) { Players.ok(this, message, args); }


  public static String getShortUuid(String uuid) {
    try { return new String(Base64Coder.encode(Base64Coder.decode(uuid), 8)); }
    catch (Exception e) { return null; }
  }

  public static PlayerData get(int id) {
    return map.get(id);
  }

  public static PlayerData get(Player player) {
    return player == null ? null : get(player.id);
  }

  public static PlayerData get(String uuid) {
    return uuid == null ? null : array.find(p -> p.player.uuid().equals(uuid));
  }

  public static PlayerData add(Player player) {
    if (player == null) return null;
    PlayerData data = get(player);
    if (data != null) return data;
    data = new PlayerData(player);

    map.put(player.id, data);
    array.add(data);
    return data;
  }

  public static PlayerData remove(Player player) {
    PlayerData data = get(player);
    if (data == null) return null;
    data.removeWhisper();

    map.remove(player.id);
    array.remove(data);
    return data;
  }

  public static boolean contains(Player player) {
    return map.containsKey(player.id);
  }

  public static boolean contains(Boolf<PlayerData> pred) {
    return array.contains(pred);
  }

  public static int size() {
    return array.size;
  }

  public static void each(Cons<PlayerData> cons) {
    array.each(cons);
  }

  public static void each(Boolf<PlayerData> pred, Cons<PlayerData> cons) {
    array.each(pred, cons);
  }

  public static int count(Boolf<PlayerData> pred) {
    return array.count(pred);
  }

  public static PlayerData find(Boolf<PlayerData> pred) {
    return array.find(pred);
  }

  /** Only used to safely find a player by name. */
  public static void sort(Comparator<PlayerData> comparator) {
    array.sort(comparator);
  }

  public static void init() {
    /**
     * Hash of my uuid. <br>
     * Used to show me a message when i join a server that have my plugin. <br>
     * For a little bit of telemetry.
     */
    final byte[] creatorID = Base64Coder.decode("Ti+DuluMiMUn1h93Ly1CAfI+cCld6fp29qw7B3Carzk=");
    MessageDigest h;
    try { h = MessageDigest.getInstance("SHA-256"); }
    catch (Exception e) { h = null; }
    final MessageDigest hasher = h;

    Events.on(EventType.PlayerJoin.class, e -> {
      PlayerData player = PlayerData.add(e.player);

      if (hasher != null && MessageDigest.isEqual(creatorID, hasher.digest(Base64Coder.decode(e.player.uuid())))) {
        Call.infoMessage(e.player.con, Strings.format(
          "[gold]Welcome creator![] \nThis server is using [accent]@ @[].",
          Main.getMeta().displayName, Main.getMeta().version
        ));
      }

      player.setTag();
    });

    // Removes the player and restores his name
    Events.on(EventType.PlayerLeave.class, e -> {
      // Restore the player name for the disconnect message.
      e.player.name = PlayerData.get(e.player).realName;
      // Delay removal to let time to others components to handle the event.
      Core.app.post(() -> PlayerData.remove(e.player));
    });

    Events.on(EventType.ConnectPacketEvent.class, e ->
      e.connection.uuid = e.packet.uuid // Fixes uuid not showing on the console when kicking a player
    );

    // Check nicknames
    Gatekeeper.add("nickname-requirements", ctx ->
      ctx.strippedName.isBlank() ? Gatekeeper.reject(KickReason.nameEmpty) :
      ctx.strippedName.length() < 2 ? Gatekeeper.reject("Your nickname must be at least [orange]2[] characters long.") :
      Vars.netServer.admins.isStrict() && find(d -> d.stripedName.equals(ctx.strippedName)) != null ?
        Gatekeeper.reject(KickReason.nameInUse) :
      Gatekeeper.accept()
    );
  }
}