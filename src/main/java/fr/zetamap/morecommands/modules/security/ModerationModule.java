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

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Time;

import mindustry.Vars;
import mindustry.net.Administration.PlayerInfo;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.ClientCommandHandler;
import fr.zetamap.morecommands.command.ServerCommandHandler;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.module.ModuleFactory;
import fr.zetamap.morecommands.util.DurationFormatter;
import fr.zetamap.morecommands.util.Strings;


/** Module adding punishment commands. */
public class ModerationModule extends AbstractModule {
  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");

  private void punishmentCommand(Punishment.Type kind, String[] args, PlayerData executor) {
    //warn: no time
    //unban: no selector and optional ip
    //TODO
    if (executor == null) logger.err("Not implemented yet.");
    else Players.err(executor, "Not implemented yet.");
  }
  
  private void pardonCommand(Punishment.Type kind, String[] args, PlayerData executor) {
    //use uuid or punishmentId if kind is null
    //TODO
    if (executor == null) logger.err("Not implemented yet.");
    else Players.err(executor, "Not implemented yet.");
  }
  
  @Override
  protected void initImpl() {
    if (ModuleFactory.enabled(Modules.punishments)) return;
    ModuleFactory.disable(this);
    logger.warn("This module cannot be used without the &frPunishments&fr module.");
    logger.warn("Please enable it with '@', to use the &fiModeration&fr module.", "morecommands enable punishments");
  }
  
  @Override
  public void registerServerCommands(ServerCommandHandler handler) {
    if (!ModuleFactory.enabled(Modules.punishments)) return;

    handler.add("bans", "[all]", "List all banned players and IPs.", args -> {
      boolean all;
      if (args.length == 1) {
        if (args[0].equals("all")) all = true;
        else {
          logger.err("Invalid argument! Must be 'all'.");
          return;
        }
      } else all = false;
      
      Seq<Punishment> punishments = Modules.punishments.get(Punishment.Type.ban);
      StringBuilder builder = new StringBuilder();
      
      if (punishments.isEmpty()) logger.info("Banned players: [@]", "empty");
      else logger.info("Banned players: [total: @, expired: @, pardonned: @]", punishments.size, 
                       punishments.count(Punishment::expired), punishments.count(Punishment::pardoned));
      punishments.each(p -> all || !p.expired(), p -> {
        PlayerInfo info = Vars.netServer.admins.getInfoOptional(p.target);
        String duration = p.permanant() ? "life" : DurationFormatter.format(p.duration());
        
        logger.info("&lk|&fr @ - '@' / @ / @:", p.id, info == null ? "<unknown>" : info.plainLastName(), p.target, 
                    p.address == null ? info.lastIP == null ? "<unknown>" : info.lastIP : p.address);
        builder.setLength(0);
        builder.append("&lk| |&fr Created at &fb&lb").append(dateFormatter.format(Instant.ofEpochMilli(p.creation)))
               .append("&fr");
        if (p.author != null) {
          info = Vars.netServer.admins.getInfoOptional(p.pardon.author);
          builder.append(" by '&fb&lb").append(info == null ? "<unknown>" : info.plainLastName()).append("&fr' [&fb&lb")
                 .append(p.author).append("&fr]");
        }
        if (p.reason != null) builder.append(" for reason: '&fb&lb").append(p.reason).append("&fr'");
        logger.info(builder.append('.').toString());
        
        if (p.pardoned()) {
          builder.setLength(0);
          builder.append("&lk| |&fr Duration: &fb&lb").append(duration).append("&fr. Pardonned at &fb&lb")
                 .append(dateFormatter.format(Instant.ofEpochMilli(p.pardon.when))).append("&fr");
          if (p.pardon.author != null) {
            if (!p.pardon.author.equals(info.id)) info = Vars.netServer.admins.getInfoOptional(p.pardon.author);
            builder.append(" by '&fb&lb").append(info == null ? "<unknown>" : info.plainLastName()).append("&fr' [&fb&lb")
                   .append(p.pardon.author).append("&fr]");
          }
          if (p.pardon.reason != null) builder.append(" for reason: '&fb&lb").append(p.pardon.reason).append("&fr'");
          logger.info(builder.append('.').toString());

        } else {
          long rest = p.expired() ? -p.remaining() : p.remaining();
          String expire = DurationFormatter.format(rest), date = dateFormatter.format(Instant.ofEpochMilli(rest));
          logger.info("&lk| |&fr Duration: @. " + (p.expired() ? "Expired since" : "Expires in") + " @ (@).", 
                      duration, expire, date);  
        }
        logger.info("&lk|&fr");
      });
      
      ObjectMap<String, Seq<Punishment>> ipPunishments = Modules.punishments.addressGet(Punishment.Type.ban);
      if (ipPunishments.isEmpty()) logger.info("Banned IPs: [@]", "empty");
      else {
        int[] count = {0, 0, 0};
        ipPunishments.each((a, pl) -> pl.each(p -> {
          count[0]++;
          if (p.expired()) count[1]++;
          if (p.pardoned()) count[2]++;
        }));
        logger.info("Banned IPs: [total: @, expired: @, pardonned: @]", count[0], count[1], count[2]);
      }
                       
      ipPunishments.each((a, pl) -> {
        if (!all && pl.allMatch(Punishment::expired)) return;
        logger.info("&lk|&fr @:", a);
        pl.each(p -> all || !p.expired(), p -> {
          PlayerInfo info = Vars.netServer.admins.getInfoOptional(p.target);
          logger.info("&lk| |&fr @ - '@' / @: Reason: '@'.", p.id, info == null ? "<unknown>" : info.plainLastName(), 
                      p.target, p.reason == null ? "<no reason>" : p.reason);
        });
        logger.info("&lk|&fr");
      });
    });

    handler.add("ban", "<player|uuid|ip|selector> [time] [reason...]", "Ban a player.", args -> 
                punishmentCommand(Punishment.Type.ban, args, null));
    
    handler.add("unban", "<uuid|ip> [reason...]", "Unban a player.", args -> 
                pardonCommand(Punishment.Type.ban, args, null));
    
    handler.add("kick", "<player|uuid|selector> [time] [reason...]", "Kick a player.", args -> 
                punishmentCommand(Punishment.Type.kick, args, null));
    
    handler.add("warn", "<player|uuid|selector> <reason...>", "Warn a player.", args -> 
                punishmentCommand(Punishment.Type.warn, args, null));
    
    handler.add("mute", "<player|uuid|selector> [time] [reason...]", "Mute a player.", args -> 
                punishmentCommand(Punishment.Type.mute, args, null));
    
    handler.add("unmute", "<player|uuid|selector> [reason...]", "Unmute a player.", args -> 
                pardonCommand(Punishment.Type.mute, args, null));
    
    handler.add("freeze", "<player|uuid|selector> [time] [reason...]", "Freeze a player.", args -> 
                punishmentCommand(Punishment.Type.freeze, args, null));
    
    handler.add("unfreeze", "<player|uuid|selector> [reason...]", "Unfreeze a player.", args -> 
                pardonCommand(Punishment.Type.freeze, args, null));
    
    handler.add("pardon", "<uuid|punishmentId> [reason...]", "Pardon a player or a punishment.", args -> 
                pardonCommand(null, args, null));
    
    handler.add("punishments", "[type|uuid|ip] [all|type]", "View all or a player punishments.", args -> {
      //TODO
      logger.err("Not implemented yet.");
    });
  }
  
  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    if (!ModuleFactory.enabled(Modules.punishments)) return;
    
    handler.add("pinfo", "[uuid|nickname...]", "Get all player informations.", (args, player) -> {
      PlayerData target;
      PlayerInfo info;
      
      if (args.length > 0) {
        if (!player.admin()) {
          Players.errArgUseDenied(player);
          return;
        }
        
        //TODO: not working correctly
        String name = Strings.normalize(args[0]);
        ObjectSet<PlayerInfo> found = Vars.netServer.admins.searchNames(name);
        
        if (found.isEmpty()) {
          // Try with uuid
          info = Vars.netServer.admins.getInfoOptional(name);
          if (info == null) {
            Players.err(player, "No player containing name or with uuid '[orange]@[]' found.", name);
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
              if (n.toLowerCase().contains(name.toLowerCase()) || 
                  Strings.stripColors(n).trim().toLowerCase().contains(name)) {
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
        builder.append(" of player '[white]").append(target != null ? target.getName() : info.lastName).append("[gold]'");
      builder.append(":\n");
      builder.append("  - [white]Status: ").append(target != null ? "[green]online[]" : "[scarlet]offline[]");
      if (info.banned) builder.append(", [orange]banned[]");
      if (info.admin) builder.append(", [scarlet]admin[]");
      builder.append("[]\n");
      if (target != null) builder.append("  - [white]Country: [accent]").append(target.player.locale).append("[][]\n");
      builder.append("  - [white]UUID: [accent]").append(info.id).append("[][]\n")
             .append("  - [white]short UUID: [accent]").append(PlayerData.getShortUuid(info.id)).append("[][]\n")
             .append("  - [white]Names: ")
             .append(info.names.toString(", ", n -> "[accent]" + n.replace("[", "[[") + "[]" + 
                                                    (n.equals(info.lastName) ? " [gray]([lightgray]last[])[]" : "")))
             .append("[]\n")
             .append("  - [white]IPs: ")
             .append(info.ips.toString(", ", ip -> "[accent]" + ip+ "[]" + 
                                                    (ip.equals(info.lastIP) ? " [gray]([lightgray]last[])[]" : "")))
             .append("[]\n")
             .append("  - [white]Joins: [accent]").append(info.timesJoined).append("[][]\n")
             .append("  - [white]Kicks: [accent]").append(info.timesKicked).append("[][]\n");
      long time = Time.millis();
      if (info.lastKicked > time) 
        builder.append("  - [white]Kick expiration: [accent]in ").append(DurationFormatter.format(info.lastKicked-time))
               .append("[] [gray]([lightgray]").append(dateFormatter.format(Instant.ofEpochMilli(info.lastKicked)))
               .append("[])[][]\n");
      
      Players.info(player, builder.toString());
      builder.setLength(0);
      
      Seq<Punishment> punishments = Modules.punishments.get(info.id);
      if (punishments == null || punishments.isEmpty()) {
        Players.info(player, "[gold]@ was never punished.", info.id.equals(player.player.uuid()) ? "You" : "The player.");
        return;
      }
      
      builder.append("[gold]Punishments:\n");
      punishments.each(p -> {
        PlayerInfo pinfo;
        String duration = p.permanant() ? "life" : DurationFormatter.format(p.duration());

        builder.append("  [gold]- [blue]").append(dateFormatter.format(Instant.ofEpochMilli(p.creation))).append("[][white]");
        if (p.author != null) {
          pinfo = Vars.netServer.admins.getInfoOptional(p.pardon.author);
          builder.append(" by '[accent]").append(pinfo == null ? "<unknown>" : pinfo.lastName)
                 .append("[white]' [gray]([lightgray]").append(p.author).append("[])[]");
        }
        if (p.reason != null) builder.append(" for reason: '[accent]").append(p.reason).append("[white]'");
        builder.append(".\n     Duration: [accent]").append(duration).append("[]. ");
        
        if (p.pardoned()) {
          builder.append("Pardonned at [blue]").append(dateFormatter.format(Instant.ofEpochMilli(p.pardon.when))).append("[]");
          if (p.pardon.author != null) {
            pinfo = Vars.netServer.admins.getInfoOptional(p.pardon.author);
            builder.append(" by '[accent]").append(pinfo == null ? "<unknown>" : pinfo.plainLastName())
                   .append("[white]' [gray]([lightgray]").append(p.pardon.author).append("[])[]");
          }
          if (p.pardon.reason != null) builder.append(" for reason: '[accent]").append(p.pardon.reason).append("[white]'");
          builder.append(".[]\n");

        } else {
          long rest = p.expired() ? -p.remaining() : p.remaining();
          String expire = DurationFormatter.format(rest), date = dateFormatter.format(Instant.ofEpochMilli(rest));
          builder.append(p.expired() ? "Expired since" : "Expires in").append(" [accent]").append(expire)
                 .append("[] [gray]([lightgray]").append(date).append("[])[].[]\n");
        }
      });
      Players.info(player, builder.toString());
    });
    
    handler.addAdmin("ban", "<player|uuid|ip|selector> [time] [reason...]", "Ban a player.", (args, player) -> 
                     punishmentCommand(Punishment.Type.ban, args, player));
    
    handler.addAdmin("unban", "<uuid|ip> [reason...]", "Unban a player.", (args, player) -> 
                     pardonCommand(Punishment.Type.ban, args, player));
    
    handler.addAdmin("kick", "<player|uuid|selector> [time] [reason...]", "Kick a player.", (args, player) -> 
                     punishmentCommand(Punishment.Type.kick, args, player));
    
    handler.addAdmin("warn", "<player|uuid|selector> <reason...>", "Warn a player.", (args, player) -> 
                     punishmentCommand(Punishment.Type.warn, args, player));
    
    handler.addAdmin("mute", "<player|uuid|selector> [time] [reason...]", "Mute a player.", (args, player) -> 
                     punishmentCommand(Punishment.Type.mute, args, player));
    
    handler.addAdmin("unmute", "<player|uuid|selector> [reason...]", "Unmute a player.", (args, player) -> 
                     pardonCommand(Punishment.Type.mute, args, player));
    
    handler.addAdmin("freeze", "<player|uuid|selector> [time] [reason...]", "Freeze a player.", (args, player) -> 
                     punishmentCommand(Punishment.Type.freeze, args, player));
    
    handler.addAdmin("unfreeze", "<player|uuid|selector> [reason...]", "Unfreeze a player.", (args, player) -> 
                     pardonCommand(Punishment.Type.freeze, args, player));
    
    handler.addAdmin("pardon", "<uuid|punishmentId> [reason...]", "Pardon a player or a punishment.", (args, player) ->
                     pardonCommand(null, args, player));
  }
}
