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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Time;

import mindustry.Vars;
import mindustry.net.Administration.PlayerInfo;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.*;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.module.ModuleRegistry;
import fr.zetamap.morecommands.modules.selector.SelectorParser;
import fr.zetamap.morecommands.modules.selector.Selectors;
import fr.zetamap.morecommands.util.DurationFormatter;
import fr.zetamap.morecommands.util.Strings;


/** Module adding punishment commands. */
public class ModerationModule extends AbstractModule {
  private static final Pattern quotes = Pattern.compile("'(.*?)'");
  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")
                                                                          .withZone(ZoneOffset.UTC);

  public long parseDuration(String str) throws IllegalArgumentException { return parseDuration(null, str); }
  public long parseDuration(Punishment.Type kind, String str) throws IllegalArgumentException {
    if (kind != null && str.equals("default")) return kind.defaultDuration.duration;
    PunishmentDuration duration = PunishmentDuration.of(str);
    if (duration != null) return duration.duration;
    return DurationFormatter.parse(str);
  }

  private long getDuration(Punishment.Type kind, String[] args, int index, PlayerData executor) {
    if (kind != Punishment.Type.warn && args.length > 1) {
      try { return parseDuration(kind, args[index]); }
      catch (Exception e) {
        String message = e.getMessage();
        message = message == null ? e.toString() : !message.endsWith(".") ? message+'.' : message;

        if (executor == null) logger.err(quotes.matcher(message).replaceAll("'&fb&lb$1&fr'"));
        else Players.err(executor, quotes.matcher(message).replaceAll("'[orange]$1[]'"));
        return -1;
      }
    }
    return -2;
  }

  private String getReason(Punishment.Type kind, String[] args, int index, PlayerData executor) {
    if (args.length > index + (kind != Punishment.Type.warn ? 1 : 0)) {
      String reason = Strings.join(" ", args, index + (kind != Punishment.Type.warn ? 1 : 0), args.length).strip();
      if (!reason.isEmpty()) return reason;
    }
    return null;
  }

  private boolean pardonPunishment(PlayerData executor, PlayerData target, Punishment punishment, String reason,
                                StringBuilder builder, Seq<Object> args, boolean address) {
    if (!Modules.punishments.pardon(executor, punishment, reason)) return false;

    builder.append(executor == null ? "Pardoned a @ (@) of " : "Pardoned a [accent]@[] [gray]([lightgray]@[])[] of ");
    args.add(punishment.type.name(), punishment.id);

    if (address) {
      builder.append(executor == null ? "IP '@'" : "IP '[accent]@[]'");
      args.add(punishment.address);
    } else {
      builder.append(executor == null ? "@ [@]" : "[accent]@[green] [gray][[[lightgray]@[]][]");
      if (target != null) args.add(target.stripedName, target.player.uuid());
      else args.add(Players.getLastName(reason, executor == null), punishment.target);
    }

    if (punishment.author != null) {
      builder.append(executor == null ? ", given by @ [@]" : ", given by [accent]@[green] [gray][[[lightgray]@[]][]");
      args.add(Players.getLastName(punishment.author, executor == null), punishment.author);
    }

    if (punishment.reason != null) {
      builder.append(punishment.author != null ? " for reason: " : "with punishment reason: '")
             .append(executor == null? "@'" : "[accent]@[green]'");
      args.add(punishment.reason);
    }

    builder.append('.');

    if (executor == null) logger.info(builder.toString(), args.toArray());
    else Players.ok(executor, builder.toString(), args.toArray());
    return true;
  }

  private void punishmentCommand(Punishment.Type kind, String[] args, PlayerData executor) {
    long duration = kind.defaultDuration.duration;
    String reason = null, action = Strings.capitalize(kind.verb);

    if (Selectors.isSelector(args[0])) {
      SelectorParser selector;
      try {
        selector = Modules.selector.parse(executor, args, true);
        if (selector == null) return;
      } catch (Exception e) {
        logger.err(e.getMessage());
        return;
      }

      long d = getDuration(kind, selector.rest, 0, executor);
      if (d == -1) return;
      else if (d >= 0) duration = d;
      reason = getReason(kind, selector.rest, 0, executor);

      final long duration0 = duration;
      final String reason0 = reason;
      selector.execute((p, u) -> {
        if (executor == p) {
          Players.err(p, "You cannot punish yourself.");
          return;
        }
        Modules.punishments.punish(executor, p, kind, duration0, reason0);
        if (executor == null) logger.info("@ @.", action, p.stripedName);
        else Players.info(executor, "[accent]@[] @[white].", action, p.getName());
      });
      if (executor == null)
        logger.info("@ for @" + (reason != null ? " with reason: '@'." : '.'),
                    "&fr" + selector.formatMessage(action), DurationFormatter.format(duration), reason);
      else Players.ok(executor, "@[green] for [accent]@[]" + (reason != null ? " with reason: '[accent]@[green]'." : '.'),
                      selector.formatMessage(action, true), DurationFormatter.format(duration), reason);
      return;
    }

    Players.SearchResult result = Players.findByName(args);
    if (result.found) {
      if (executor == result.player) {
        Players.err(result.player, "You cannot punish yourself.");
        return;
      }

      long d = getDuration(kind, result.rest, 0, executor);
      if (d == -1) return;
      else if (d >= 0) duration = d;
      reason = getReason(kind, result.rest, 0, executor);

      Modules.punishments.punish(executor, result.player, kind, duration, reason);
      if (executor == null)
        logger.info("@ @ for @" + (reason != null ? " with reason: '@'." : '.'), action, result.player.stripedName,
                    DurationFormatter.format(duration), reason);
      else Players.ok(executor, "[accent]@[] @[green] for [accent]@[]" +
                                (reason != null ? " with reason: '[accent]@[green]'." : '.'),
                      action, result.player.getName(), DurationFormatter.format(duration), reason);
      return;
    }

    String target = null, address = null;
    PlayerInfo info = null;

    if (kind == Punishment.Type.ban && (args[0].indexOf('.') != -1 || args[0].indexOf(':') != -1)) {
      // Delay address punishment to parse rest of arguments after uuid search
      address = args[0];

    } else {
      info = Vars.netServer.admins.getInfoOptional(args[0]);
      if (info == null) {
        if (executor == null) logger.err("No player found with name or uuid '@'.", args[0]);
        else Players.err(executor, "No player found with name or uuid '[orange]@[]'.", args[0]);
        return;
      } else if (executor != null && executor.player.uuid().equals(info.id)) {
        Players.err(executor, "You cannot punish yourself.");
        return;
      }

      target = info.id;
      address = info.lastIP;
    }

    long d = getDuration(kind, args, 1, executor);
    if (d == -1) return;
    else if (d >= 0) duration = d;
    reason = getReason(kind, args, 1, executor);

    Modules.punishments.punish(executor, target, address, kind, duration, reason);
    if (info == null) {
      if (executor == null) logger.info("@ IP '@' for @" + (reason != null ? " with reason: '@'." : '.'), action, address,
                                        DurationFormatter.format(duration), reason);
      else Players.ok(executor, "[accent]@[] IP '[accent]@[]' for [accent]@[]" +
                                (reason != null ? " with reason: '[accent]@[green]'." : '.'),
                      action, address, DurationFormatter.format(duration), reason);

    } else if (executor == null)
      logger.info("@ @ [@, @] for @" + (reason != null ? " with reason: '@'." : '.'), action,
                  info.plainLastName(), target, address, DurationFormatter.format(duration), reason);
    else Players.ok(executor, "[accent]@ @[green] [gray][[[lightgray]@[], [lightgray]@[]][] for [accent]@[]" +
                              (reason != null ? " with reason: 'accent]@[green]'." : '.'),
                      action, info.lastName, target, address, DurationFormatter.format(duration), reason);
  }

  private void pardonCommand(Punishment.Type kind, String[] args, PlayerData executor) {
    String reason = args.length > 1 ? Strings.join(" ", args, 1, args.length).strip() : null;
    if (reason != null && reason.isEmpty()) reason = null;
    StringBuilder builder = new StringBuilder();
    Seq<Object> pargs = new Seq<>(7);
    Punishment p = null;
    boolean byAddress = false;

    if (kind == null) {
      int id = Strings.parseInt(args[0]);

      if (id != Integer.MIN_VALUE) {
        p = Modules.punishments.get(id);
        if (p == null) {
          if (executor == null) logger.err("No punishment with id '@' found.", id);
          else Players.err(executor, "No punishment with id '[orange]@[]' found.", id);
          return;
        } else if (p.pardoned()) {
          if (executor == null) logger.err("Punishment @ is already pardoned.", id);
          else Players.err(executor, "Punishment [orange]@[] is already pardoned.", id);
          return;
        }

        Modules.punishments.pardon(executor, p, reason);
        if (reason == null) {
          if (executor == null) logger.info("Punishment @ pardoned.", id);
          else Players.ok(executor, "Punishment [accent]@[] pardoned.", id);
        } else if (executor == null) logger.info("Punishment @ pardoned for reason: @.", id, reason);
        else Players.ok(executor, "Punishment [accent]@[] pardoned for reason: [accent]@[].", id, reason);

      } else {
        p = Modules.punishments.last(args[0], Punishment.Type.kick);
        if (p == null) p = Modules.punishments.last(args[0], Punishment.Type.votekick);
        if (p == null) {
          if (executor == null)
            logger.err("@ [@] is not currently @ or @.", Players.getLastName(args[0], true), args[0],
                       Punishment.Type.kick.verb, Punishment.Type.votekick.verb);
          else Players.err(executor, "[orange]@[scarlet] [gray][[[lightgray]@[]][] is not currently [orange]@[] or "
                                   + "[orange]@[].",
                           Players.getLastName(args[0]), args[0], Punishment.Type.kick.verb, Punishment.Type.votekick.verb);
          return;
        }

        pardonPunishment(executor, null, p, reason, builder, pargs, byAddress);
      }
      return;
    }

    if (kind != Punishment.Type.ban) {
      if (Selectors.isSelector(args[0])) {
        SelectorParser selector;
        try {
          selector = Modules.selector.parse(executor, args, true);
          if (selector == null) return;
        } catch (Exception e) {
          logger.err(e.getMessage());
          return;
        }
        reason = String.join(" ", selector.rest).strip();
        if (reason.isEmpty()) reason = null;

        final String reason0 = reason;
        selector.execute((t, u) -> {
          Punishment punishment = Modules.punishments.last(t, kind);
          if (punishment == null) return;
          builder.setLength(0);
          pargs.clear();
          pardonPunishment(executor, t, punishment, reason0, builder, pargs, false/*byAddress*/);
        });
        if (selector.noTargetFound()) {
          if (executor == null) logger.info(selector.formatMessage("Un" + kind.verb) + '.');
          else Players.ok(executor, selector.formatMessage("Un" + kind.verb, true) + "[green].");
        }
        return;
      }

      Players.SearchResult result = Players.findByName(args);
      if (result.found) {
        p = Modules.punishments.last(result.player, kind);
        if (p == null) {
          if (executor == null)
            logger.err("@ [@] is not currently @.", result.player.stripedName, result.player.player.uuid(), kind.verb);
          else Players.err(executor, "@[scarlet] [gray][[[lightgray]@[]][] is not currently [orange]@[].",
                           result.player.getName(), result.player.player.uuid(), kind.verb);
          return;
        }

        reason = String.join(" ", result.rest).strip();
        if (reason.isEmpty()) reason = null;

        pardonPunishment(executor, result.player, p, reason, builder, pargs, byAddress);
        return;
      }

    } else {
      p = Modules.punishments.addressLast(args[0], kind);
      byAddress = true;
    }

    if (p == null) {
      p = Modules.punishments.last(args[0], kind);
      if (p == null) {
        if (byAddress) {
          if (executor == null) logger.err("IP or UUID '@' is not currently @.", args[0], kind.verb);
          else Players.err(executor, "IP or UUID '[orange]@[]' is not currently [orange]@[].", args[0], kind.verb);
        } else if (executor == null) logger.err("Player not found!");
        else Players.errPlayerNotFound(executor);
        return;
      }
    }

    pardonPunishment(executor, null, p, reason, builder, pargs, byAddress);
  }

  private void listCommand(Punishment.Type kind, boolean all, PlayerData executor, String target) {
    Seq<Punishment> punishments = kind == null ?
                                    target == null ? Modules.punishments.all() : Modules.punishments.get(target) :
                                  target == null ? Modules.punishments.get(kind) : Modules.punishments.get(target, kind);
    StringBuilder builder = new StringBuilder();
    Seq<Object> args = new Seq<>(5);

    if (kind == null) {
      if (punishments == null || punishments.isEmpty()) {
        Players.info(executor, "[gold]@ was never punished.",
                     target != null && target.equals(executor.player.uuid()) ? "You" : "The player");
        return;
      }

      int perPage = 6, count = 0;
      builder.append("[gold]Punishments:\n");
      for (Punishment p : punishments) {
        String duration = p.permanant() ? "life" : DurationFormatter.format(p.duration());

        builder.append("  [gold]- [orange]").append(p.type).append("[]: [white]").append(p.id)
               .append("\n     [#1E90FF]").append(dateFormatter.format(Instant.ofEpochMilli(p.creation)))
               .append("[][white]");
        if (p.author != null)
          builder.append(" by '[accent]").append(Players.getLastName(p.author)).append("[white]' [gray]([lightgray]")
                 .append(p.author).append("[])[]");
        if (p.reason != null) builder.append(" for reason: '[accent]").append(p.reason).append("[white]'");
        builder.append('.');

        if (p.type != Punishment.Type.warn || p.pardoned()) {
          builder.append("\n     ");

          if (p.type != Punishment.Type.warn) builder.append("Duration: [accent]").append(duration).append("[]. ");
          if (p.pardoned()) {
            builder.append("Pardonned at [#1E90FF]").append(dateFormatter.format(Instant.ofEpochMilli(p.pardon.when)))
                   .append("[]");
            if (p.pardon.author != null)
              builder.append(" by '[accent]").append(Players.getLastName(p.author)).append("[white]' [gray]([lightgray]")
                     .append(p.pardon.author).append("[])[]");
            if (p.pardon.reason != null) builder.append(" for reason: '[accent]").append(p.pardon.reason).append("[white]'");
            builder.append(".[]");

          } else if (!p.permanant()) {
            long rest = p.expired() ? -p.remaining() : p.remaining();
            String expire = DurationFormatter.format(rest), date = dateFormatter.format(Instant.ofEpochMilli(p.expire));
            builder.append(p.expired() ? "Expired" : "Expires in").append(" [accent]").append(expire).append("[] ");
            if (p.expired()) builder.append("ago ");
            builder.append("[gray]([lightgray]").append(date).append("[])[].[]");
          }
        }

        if (count == perPage) {
          Players.info(executor, builder.toString());
          builder.setLength(0);
          count = 0;
        } else {
          builder.append('\n');
          count++;
        }
      }
      if (count > 0) Players.info(executor, builder.toString());
        return;
    }

    if (executor == null) {
      if (punishments.isEmpty()) logger.info(Strings.capitalize(kind.verb) + " players: [@]", "empty");
      else logger.info(Strings.capitalize(kind.verb) + " players: [total: @, expired: @, pardonned: @]", punishments.size,
                       punishments.count(Punishment::expired), punishments.count(Punishment::pardoned));

      punishments.each(p -> all || !p.expired(), p -> {
        PlayerInfo info = p.target == null ? null : Vars.netServer.admins.getInfoOptional(p.target);

        logger.info("&lk|&fr @ - '@' / @ / @:", p.id, info == null ? "<unknown>" : info.plainLastName(), p.target,
                    p.address == null ? (info == null || info.lastIP == null ? "<unknown>" : info.lastIP) : p.address);

        builder.setLength(0);
        args.clear();

        builder.append("&lk| |&fr Created at @");
        args.add(dateFormatter.format(Instant.ofEpochMilli(p.creation)));
        if (p.author != null) {
          builder.append(" by '@' [@]");
          args.add(Players.getLastName(p.author, true), p.author);
        }
        if (p.reason != null) {
          builder.append(" for reason: '@'");
          args.add(p.reason);
        }
        logger.info(builder.append('.').toString(), args.toArray());

        if (kind != Punishment.Type.warn || p.pardoned()) {
          builder.setLength(0);
          args.clear();

          builder.append("&lk| |&fr ");
          if (kind != Punishment.Type.warn) {
            builder.append("Duration: @.");
            args.add(p.permanant() ? "life" : DurationFormatter.format(p.duration()));
          }

          if (p.pardoned()) {
            builder.append("Pardonned at @");
            args.add(dateFormatter.format(Instant.ofEpochMilli(p.pardon.when)));
            if (p.pardon.author != null) {
              builder.append(" by '@' [@]");
              args.add(Players.getLastName(p.pardon.author, true), p.pardon.author);
            }
            if (p.pardon.reason != null) {
              builder.append(" for reason: '@'");
              args.add(p.pardon.reason);
            }
            builder.append('.');

          } else if (!p.permanant()) {
            builder.append(p.expired() ? "Expired" : "Expires in").append(p.expired() ?" @ ago" : " @").append(" (@).");
            args.add(DurationFormatter.format(p.expired() ? -p.remaining() : p.remaining()),
                     dateFormatter.format(Instant.ofEpochMilli(p.expire)));
          }
          logger.info(builder.toString(), args.toArray());
        }

        logger.info("&lk|&fr");
      });

      if (kind != Punishment.Type.ban) return;
      logger.ln();
      ObjectMap<String, Seq<Punishment>> ipPunishments = Modules.punishments.addressGet(kind);
      if (ipPunishments.isEmpty()) logger.info(Strings.capitalize(kind.verb) + " IPs: [@]", "empty");
      else {
        int[] count = {0, 0, 0};
        ipPunishments.each((a, pl) -> pl.each(p -> {
          count[0]++;
          if (p.expired()) count[1]++;
          if (p.pardoned()) count[2]++;
        }));
        logger.info(Strings.capitalize(kind.verb) + " IPs: [total: @, expired: @, pardoned: @]", count[0], count[1], count[2]);
      }

      ipPunishments.each((a, pl) -> {
        if (!all && pl.allMatch(Punishment::expired)) return;
        logger.info("&lk|&fr @:", a);
        pl.each(p -> all || !p.expired(), p ->
          logger.info("&lk| |&fr @ - '@' / @: Reason: '@'.", p.id, Players.getLastName(p.target, true), p.target,
                      p.reason == null ? "<no reason>" : p.reason)
        );
        logger.info("&lk|&fr");
      });
      return;
    }

    //TODO
  }

  @Override
  protected void initImpl() {
    if (ModuleRegistry.enabled(Modules.punishments)) return;
    ModuleRegistry.disableTemporary(this);
    logger.warn("This module cannot be used without the @ module. Please enable it with '@'.",
                "&fi" + Modules.punishments.name() + "&fr", "morecommands enable punishments");
  }

  @Override
  public void registerServerCommands(ServerCommandHandler handler) {
    if (!ModuleRegistry.enabled(Modules.punishments)) return;

    handler.add("bans", "[all]", "List all banexecutorayers and IPs.", args -> {
      // Shortcut of 'punishments ban'
      boolean all = false;
      if (args.length == 1) {
        if (args[0].equals("all")) all = true;
        else {
          logger.err("Invalid argument! Must be '@'.", "all");
          return;
        }
      }

      listCommand(Punishment.Type.ban, all, null, null);
    });

    handler.add("punishments", "[type|uuid|ip] [all|type]", "List all punishments or ones of a player.", args -> {
      //TODO
      logger.err("Not implemented yet.");
    });

    handler.add("ban", "<player|uuid|ip|selector> [time|default] [reason...]", "Ban a player.", args ->
                punishmentCommand(Punishment.Type.ban, args, null));

    handler.add("unban", "<uuid|ip> [reason...]", "Unban a player.", args ->
                pardonCommand(Punishment.Type.ban, args, null));

    handler.add("kick", "<player|uuid|selector> [time|default] [reason...]", "Kick a player.", args ->
                punishmentCommand(Punishment.Type.kick, args, null));

    handler.add("warn", "<player|uuid|selector> <reason...>", "Warn a player.", args ->
                punishmentCommand(Punishment.Type.warn, args, null));

    handler.add("mute", "<player|uuid|selector> [time|default] [reason...]", "Mute a player.", args ->
                punishmentCommand(Punishment.Type.mute, args, null));

    handler.add("unmute", "<player|uuid|selector> [reason...]", "Unmute a player.", args ->
                pardonCommand(Punishment.Type.mute, args, null));

    handler.add("freeze", "<player|uuid|selector> [time|default] [reason...]", "Freeze a player.", args ->
                punishmentCommand(Punishment.Type.freeze, args, null));

    handler.add("unfreeze", "<player|uuid|selector> [reason...]", "Unfreeze a player.", args ->
                pardonCommand(Punishment.Type.freeze, args, null));

    handler.add("pardon", "<uuid|punishmentId> [reason...]", "Pardon a player or a punishment.", args ->
                pardonCommand(null, args, null));
  }

  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    if (!ModuleRegistry.enabled(Modules.punishments)) return;

    handler.addAdmin("pinfo", "[player|uuid...]", "Get all player information.", (args, player) -> {
      PlayerData target;
      PlayerInfo info;

      if (args.length > 0) {
        if (!player.admin()) {
          Players.errArgUseDenied(player);
          return;
        }

        String name = Strings.normalize(args[0]), lowerName = name.toLowerCase();
        ObjectSet<PlayerInfo> found = Vars.netServer.admins.searchNames(lowerName);

        if (found.isEmpty()) {
          // Try with uuid
          info = Vars.netServer.admins.getInfoOptional(name);
          if (info == null) {
            Players.err(player, "No player found containing name or with uuid '[orange]@[]'.", name);
            return;
          }
        } else if (found.size == 1) {
          info = found.first();
        } else {
          StringBuilder builder = new StringBuilder();
          builder.append("[gold]Found [accent]").append(found.size).append("[] players: ");
          found.each(i -> {
            builder.append("\n  [gold]-[] [white]").append(i.id).append(":[] ");
            // Only display matching nicknames
            boolean first = true;
            for (String n : i.names) {
              if (n.toLowerCase().contains(lowerName) ||
                  Strings.stripColors(n).trim().toLowerCase().contains(lowerName)) {
                if (!first) builder.append("[white],[] ");
                builder.append("[accent]").append(n).append("[]");
                first = false;
              }
            }
          });
          Players.info(player, builder.toString());
          return;
        }

        target = PlayerData.get(info.id);

      } else {
        target = player;
        info = target.player.getInfo();
      }

      StringBuilder builder = new StringBuilder();

      builder.append("[gold]Trace info");
      if (info != player.player.getInfo())
        builder.append(" of '[white]").append(target != null ? target.getName() : info.lastName).append("[gold]'");
      builder.append(":\n");
      builder.append("  - [white]Status: ").append(target != null ? "[green]online[]" : "[scarlet]offline[]");
      if (info.banned) builder.append(", [orange]banned[]");
      if (info.admin) builder.append(", [scarlet]admin[]");
      builder.append("[]\n");
      if (target != null) builder.append("  - [white]Country: [accent]").append(target.player.locale).append("[][]\n");
      builder.append("  - [white]UUID: [accent]").append(info.id).append("[][]\n")
             .append("  - [white]short UUID: [accent]").append(PlayerData.getShortUuid(info.id)).append("[][]\n")
             .append("  - [white]Names: ").append(info.names.toString(", ", n ->
               (n.equals(info.lastName) ? "[orange]" : "[accent]") + n.replace("[", "[[") + "[]" +
               (n.equals(info.lastName) ? " [gray]([lightgray]last[])[]" : ""))).append("[]\n")
             .append("  - [white]IPs: ").append(info.ips.toString(", ", ip ->
               (ip.equals(info.lastIP) ? "[orange]" : "[accent]") + ip+ "[]" +
               (ip.equals(info.lastIP) ? " [gray]([lightgray]last[])[]" : ""))).append("[]\n")
             .append("  - [white]Joins: [accent]").append(info.timesJoined).append("[][]\n")
             .append("  - [white]Total kicks: [accent]").append(info.timesKicked).append("[][]\n");
      long time = Time.millis();
      if (info.lastKicked > time)
        builder.append("  - [white]Kick expiration: [accent]in ").append(DurationFormatter.format(info.lastKicked-time))
               .append("[] [gray]([lightgray]").append(dateFormatter.format(Instant.ofEpochMilli(info.lastKicked)))
               .append("[])[][]\n");

      // Safe check
      if (!ModuleRegistry.enabled(Modules.punishments)) {
        builder.append("\n[scarlet]Error: '[orange]").append(Modules.punishments.internalName())
               .append("[]' module is disabled.");
        Players.info(player, builder.toString());
        return;
      }

      Seq<Punishment> punishments = Modules.punishments.get(info.id);
      if (punishments != null && punishments.any()) {
        int[] counts = new int[Punishment.Type.all.length * 2];
        punishments.each(p -> {
          counts[p.type.ordinal()]++;
          if (!p.expired()) counts[Punishment.Type.all.length + p.type.ordinal()]++;
        });
        builder.append(" - [white]Punishments: ");
        for (Punishment.Type t : Punishment.Type.all) {
          if (t.ordinal() > 0) builder.append(", ");
          builder.append("[accent]").append(counts[t.ordinal()]).append("[] [lightgray]").append(t.name());
          if (counts[t.ordinal()] > 1) builder.append('s');
          builder.append("[]");
        }
        builder.append("[]\n");
        builder.append(" - [white]In progress: ");
        for (Punishment.Type t : Punishment.Type.all) {
          if (t.ordinal() > 0) builder.append(", ");
          builder.append("[accent]").append(counts[Punishment.Type.all.length + t.ordinal()]).append("[] [lightgray]")
                 .append(t.name());
          if (counts[Punishment.Type.all.length + t.ordinal()] > 1) builder.append('s');
          builder.append("[]");
        }
        builder.append("[]\n");
      }

      Players.info(player, builder.toString());
    });

    handler.addAdmin("punishments", "<player|uuid> [page|type...]", "View player punishments.", (args, player) -> {
      //TODO
      Players.err(player, "Not implemented yet.");
    });

    handler.addAdmin("ban", "<player|uuid|ip|selector> [time|default] [reason...]", "Ban a player.", (args, player) ->
                     punishmentCommand(Punishment.Type.ban, args, player));

    handler.addAdmin("unban", "<uuid|ip> [reason...]", "Unban a player.", (args, player) ->
                     pardonCommand(Punishment.Type.ban, args, player));

    handler.addAdmin("kick", "<player|uuid|selector> [time|default] [reason...]", "Kick a player.", (args, player) ->
                     punishmentCommand(Punishment.Type.kick, args, player));

    handler.addAdmin("warn", "<player|uuid|selector> <reason...>", "Warn a player.", (args, player) ->
                     punishmentCommand(Punishment.Type.warn, args, player));

    handler.addAdmin("mute", "<player|uuid|selector> [time|default] [reason...]", "Mute a player.", (args, player) ->
                     punishmentCommand(Punishment.Type.mute, args, player));

    handler.addAdmin("unmute", "<player|uuid|selector> [reason...]", "Unmute a player.", (args, player) ->
                     pardonCommand(Punishment.Type.mute, args, player));

    handler.addAdmin("freeze", "<player|uuid|selector> [time|default] [reason...]", "Freeze a player.", (args, player) ->
                     punishmentCommand(Punishment.Type.freeze, args, player));

    handler.addAdmin("unfreeze", "<player|uuid|selector> [reason...]", "Unfreeze a player.", (args, player) ->
                     pardonCommand(Punishment.Type.freeze, args, player));

    handler.addAdmin("pardon", "<uuid|punishmentId> [reason...]", "Pardon a player or a punishment.", (args, player) ->
                     pardonCommand(null, args, player));
  }
}
