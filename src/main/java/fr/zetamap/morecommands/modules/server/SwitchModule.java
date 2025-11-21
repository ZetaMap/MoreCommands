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

import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Timer;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;

import mindustry.gen.Call;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.*;
import fr.zetamap.morecommands.misc.MCEvents;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractSaveableModule;
import fr.zetamap.morecommands.util.JsonSettings;
import fr.zetamap.morecommands.util.Strings;


public class SwitchModule extends AbstractSaveableModule {
  // Holds pings for 1 minute and re-updates them when /hub or /switch is called. 
  // This avoid a discovery timer, since the usage is only for these commands.
  private boolean discovering;
  private long lastDiscovery;
  
  public final OrderedMap<String, Server> servers = new OrderedMap<>();
  public final ObjectMap<String, Server> aliases = new ObjectMap<>();
  /** Special server name used by the /hub shortcut command. */
  public String hubServerName = "hub";
  /** Discovery cache holding time. (in seconds) */
  public float discoveryHoldTime = 60f;
  
  public boolean discovering() {
    return discovering;
  }
  
  /** Force a new servers discovery. Do nothing if one is already is running. */
  public void forceDiscovery() {
    if (discovering()) return;
    lastDiscovery = 0;
    discovery(null, null, null);
  }
  
  /** Force a new servers discovery. Do nothing if one is already is running. */
  public void forceDiscovery(Runnable done) {
    if (discovering()) return;
    lastDiscovery = 0;
    discovery(null, null, done);
  }
  
  /** Start a discovery if needed and calls {@code done} when finished or when a discovery is not needed. */
  public void discovery(Runnable done) {
    discovery(null, null, done);
  }

  protected void discovery(Runnable waiting, Runnable isDiscovering, Runnable done) {
    if (discovering()) {
      if (isDiscovering != null) isDiscovering.run();
      return;
      
    } else if (servers.isEmpty() || Time.timeSinceNanos(lastDiscovery) < discoveryHoldTime * 1_000_000_000) {
      if (done != null) done.run();
      return;
    }

    discovering = true;
    int[] counter = {servers.size};
    Timer.Task notifier = waiting != null ? Timer.schedule(waiting, 1) : null;
    Runnable finished = () -> {
      if (--counter[0] > 0) return;
      lastDiscovery = Time.nanos();
      discovering = false;
      if (notifier != null) notifier.cancel();
      if (done != null) done.run();
    };

    for (Server s : servers.values()) 
      s.ping(h -> finished.run(), e -> finished.run());
  }

  public void connectPlayer(PlayerData player, Server server) {
    server.connect(player.player, status -> {
      String message = status.toReason(server);
      StringBuilder builder = new StringBuilder();
      
      if (status.ok()) {
        builder.append(player.getName()).append("[accent] switched to [orange]");
        formattedServerName(builder, server);
        builder.append("[accent].");
        Call.sendMessage(builder.toString());
        Events.fire(new MCEvents.PlayerSwitchedEvent(player, server));
      } else {
        builder.append("[scarlet]Unable to connect you to [orange]");
        formattedServerName(builder, server);
        builder.append("[scarlet]:[white] \n").append(message);
        message = builder.toString();
      }

      Call.infoMessage(player.player.con, message);
    });
  }

  /** Ping the server (if needed) before connecting the player. */
  protected void safeConnect(PlayerData player, Server server) {
    discovery(
      () -> Players.info(player, "[orange]\ue86a Checking servers..."),
      () -> Players.info(player, "[orange]\ue837 A discovery is running, please wait..."), 
      () -> connectPlayer(player, server)
    );
  }
  
  protected void formattedServerName(StringBuilder builder, Server server) {
    if (server.displayName != null) builder.append(server.displayName).append(" [gray]([lightgray]");
    builder.append(server.name);
    if (server.alias != null) 
      builder.append(server.displayName != null ? "[], [lightgray]" : ", ").append(server.alias);
    if (server.displayName != null) builder.append("[])[]");
  }
  
  /** @return the server associated with the {@link #hubServerName} or {@code null} if not defined */
  public Server getHub() {
    return servers.get(hubServerName);
  }
  
  public Server get(String name) {
    return servers.get(name);
  }
  
  public Server getByAlias(String alias) {
    return aliases.get(alias);
  }
  
  public boolean isHubServer(Server server) {
    return isHubServer(server.name);
  }
  
  public boolean isHubServer(String name) {
    return name != null && name.equals(hubServerName);
  }
  
  /** Calls {@link #isAliasValid(String)} if the {@code server} as an alias. */
  public boolean isServerValid(Server server) {
    return server.alias == null 
        || !isHubServer(server.alias) && !has(server.alias) && getByAlias(server.alias) == server;
  }

  /** @return whether the alias is not already used or doesn't correspond to a server name. */
  protected boolean isAliasValid(String serverAlias) {
    return !isHubServer(serverAlias) && !hasAlias(serverAlias) && !has(serverAlias);
  }
  
  /** @return {@code false} if the alias is already used or correspond to a server name. */
  public boolean put(Server server) {
    if (server.alias != null) {
      if (!isAliasValid(server.alias)) return false;
      aliases.put(server.alias, server);
    }
    servers.put(server.name, server);
    setModified();
    return true;
  }
  
  public void remove(Server server) {
    servers.remove(server.name);
    if (server.alias != null) aliases.remove(server.alias);
    setModified();
  }
  
  public void remove(String name) {
    Server s = servers.remove(name);
    if (s == null) return;
    if (s.alias != null) aliases.remove(s.alias);
    setModified();
  }
  
  public boolean has(String name) {
    return servers.containsKey(name);
  }
  
  public boolean hasAlias(String serverAlias) {
    return aliases.containsKey(serverAlias);
  }
  
  public void clear() {
    servers.clear();
    setModified();
  }
  
  // region {@link Server} setters
  /** @return {@code false} if the alias is already used or correspond to a server name. */
  public boolean setAlias(Server server, String alias) {
    if (alias != null && !isAliasValid(alias)) return false;
    if (server.alias != null) aliases.remove(server.alias);
    server.alias = alias;
    if (alias != null) aliases.put(alias, server);
    setModified();
    return true;
  }
  
  public void setDisplayName(Server server, String displayName) {
    server.displayName = displayName;
    setModified();
  }

  public void setAddress(Server server, String address) {
    server.setAddress(address);
    setModified();
  }
  
  public void setIp(Server server, String ip) {
    server.ip = ip;
    setModified();
  }
  
  public void setPort(Server server, int port) {
    server.port = port;
    setModified();
  }
  
  public void setAdmin(Server server, boolean adminOnly) {
    server.adminOnly = adminOnly;
    setModified();
  }
  // end region
  
  void setModified0() {
    setModified();
  }
  
  @SuppressWarnings("rawtypes")   
  @Override
  protected void initImpl() {
    addSerializer(Server.class, new Json.Serializer<Server>() {
      @Override
      public void write(Json json, Server object, Class knownType) {
        json.writeObjectStart();
        json.writeValue("name", object.name);
        if (object.alias != null) json.writeValue("alias", object.alias);
        if (object.displayName != null) json.writeValue("display", object.displayName);
        json.writeValue("address", object.address());
        if (object.adminOnly) json.writeValue("private", object.adminOnly);
        json.writeObjectEnd();
      }

      @Override
      public Server read(Json json, JsonValue jsonData, Class type) {
        return new Server(
          jsonData.getString("name"), 
          jsonData.getString("alias", null),
          jsonData.getString("display", null),
          jsonData.getString("address"),
          jsonData.getBoolean("private", false)
        );   
      }
    });
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void loadImpl(JsonSettings settings) {
    servers.clear();
    settings.getOrPut("servers", Seq.class, Server.class, Seq::new)
            .each(s -> servers.put(((Server)s).name, (Server)s));

    // Check for aliases duplication
    servers.each((n, s) -> {
      if (s.alias == null) return;
      Server found = aliases.get(s.alias);
      if (found != null) 
        logger.warn("Duplicated alias '@' for server '@': '@' is already using it.", s.alias, s.name, found.name);
      else if (isHubServer(s.alias))
        logger.warn("Invalid alias '@' for server '@': this is a reserved name, so it cannot be used as an alias.",
                    s.alias, s.name);
      else if (has(s.alias))
        logger.warn("Invalid alias '@' for server '@': another server is named same as the alias.", s.alias, s.name);
      else aliases.put(s.alias, s);
    });
    
    forceDiscovery();
  }

  @Override
  protected void saveImpl(JsonSettings settings) {
    settings.put("servers", Server.class, servers.values().toSeq());
  }

  @Override
  public void registerServerCommands(ServerCommandHandler handler) {
    handler.add("switch", "[help|arg0] [args...]", "Configures the switch system. Use '&fiswitch help&fr&lw' for usage.", 
    args -> {
      if (args.length == 0) {
        if (servers.isEmpty()) {
          logger.info("Server list: [@]", "empty");
          return;
        }
        
        int[] privates = {0};
        StringBuilder builder = new StringBuilder();
        Seq<String> lines = servers.orderedKeys().map(n -> {
          Server s = servers.get(n);
          if (s.adminOnly()) privates[0]++;
          
          builder.setLength(0);
          builder.append("&lk|&fr &fb&lb").append(s.name).append("&fr");
          if (s.alias() != null) builder.append(", &fb&lb").append(s.alias()).append("&fr");
          if (s.displayName() != null) builder.append(" / '&fb&lb").append(s.displayName()).append("'&fr");
          builder.append(": &fb&lb").append(s.address()).append("&fr");
          if (isHubServer(s)) builder.append(", &fb&lghub server&fr");
          if (s.adminOnly()) builder.append(", &fb&lradminOnly&fr");
          if (!isServerValid(s)) builder.append(", &fb&lrINVALID&fr");
          
          return builder.toString();
        });
        
        logger.info("Server list: [total: @, private: @]", servers.size, privates[0]);
        lines.each(logger::info);
        return;
      }

      String[] rest = args.length == 1 ? new String[0] : args[1].split(" ");
      switch (args[0]) {
        case "help":
          logger.info("Usage: switch help\n"
                    + "   or: switch refresh\n"
                    + "   or: switch add <name> <ip[:port]>\n"
                    + "   or: switch remove <name>\n"
                    + "   or: switch clear\n"
                    + "   or: switch set <name> <property> <value...>\n\n"
                    + "Properties:\n"
                    + "  - alias: The server's short name, usually one or two letters.\n"
                    + "  - display-name: The formatted server name, with colors, glyphs, spaces or capital letters.\n"
                    + "                  Only used to display servers in &fi/switch&fr.\n"
                    + "  - admin-only: Whether only administrators from THIS server can connect to the other server.\n"
                    + "                This DOESN'T guarantee that only administrators from the OTHER server can connect, \n"
                    + "                e.g. using the server ip instead of the command. \n"
                    + "                So PLEASE set a whitelist on the OTHER server to ensure its security. \n\n"
                    + "Notes:\n"
                    + "  - The name '@' is special and used by the '&fi/hub&fr' shortcut command.\n"
                    + "  - The server @ must be in lowercase kebab, without colors, glyphs, spaces, etc.\n"
                    + "    The same goes for the @ property.\n"
                    + "  - The '@' value can be used to reset a property.\n"
                    + "  - A server is marked as '@' if it's alias is the same as another one or correspond "
                    + "to a server name.", hubServerName, "name", "alias", "null", "&fb&lrINVALID");
          break;
          
        case "refresh":
          if (discovering()) logger.err("A servers discovery is already running.");
          else {
            logger.info("Started servers discovery.");
            forceDiscovery(() -> logger.info("Discovery finished."));
          }
          break;

        case "add":
          if (rest.length < 2) {
            logger.err("Missing 'name' and/or 'ip' argument(s)!");
            return;
          } else if (has(rest[0])) {
            logger.err("Server '@' already in the list. Use '@' to modify it's properties.", 
                       rest[0], "switch set " + rest[0] + " ...");
            return;
          } else if (args[1].isBlank()) {
            logger.err("Empty server address.");
            return;
          }
          
          try { 
            put(new Server(rest[0], rest[1].strip())); 
            logger.info("Server added to the list.");
          } catch (Exception e) {
            logger.err("Invalid server address: @", e.getMessage());
          }
          break;

        case "remove":
          if (rest.length < 1) {
            logger.err("Missing 'name' argument!");
            return;
          } else if (!has(rest[0])) {
            logger.err("Server '@' not in the list. ", rest[0]);
            return;
          }
          
          remove(rest[0]);
          logger.info("Server removed from the list.");
          break;

        case "clear":
          clear();
          logger.info("Removed all servers from the list.");
          break;
          
        case "set":
          if (rest.length < 2) {
            logger.err("Missing 'name' and/or 'property' argument(s)!");
            return;
          } else if (!has(rest[0])) {
            logger.err("Server '@' not in the list. ", rest[0]);
            return;
          }
          
          Server server = get(rest[0]);
          boolean remove = rest.length > 2 && rest[2].equals("null");
          
          switch (rest[1]) {
            case "alias":
              if (rest.length < 3) {
                logger.info("The alias of server '@' is currently: @.", server.name, server.alias);
              } else if (remove) {
                setAlias(server, null);
                logger.info("Server alias @.", "removed");
              } else if (rest[2].isBlank()) {
                logger.err("Empty server alias.");
              } else if (isHubServer(rest[2])) {
                logger.err("This is a reserved name, so it cannot be used as an alias.");
              } else if (hasAlias(rest[2])) {
                logger.err("The alias '@' is already used by the server '@'.", rest[2], getByAlias(rest[2]).name);
              } else if (has(rest[2])) {
                logger.err("Cannot use a server name an alias.");
              } else if (rest.length > 2) {
                String old = server.alias();
                setAlias(server, rest[2]);
                logger.info("Server alias @.", old == null ? "added" : "modified");
              }
              break;
              
            case "display-name":
              if (remove) {
                setDisplayName(server, null);
                logger.info("Server display name @.", "removed");
              } else if (rest.length > 2) {
                String old = server.displayName();
                setDisplayName(server, Strings.join(" ", rest, 2, rest.length));
                logger.info("Server display name @.", old == null ? "added" : "modified");
              } else logger.info("The display name of server '@' is currently: @.", server.name, server.displayName);
              break;
              
            case "admin-only":
              if (rest.length > 2) {
                if (remove || Strings.isFalse(rest[2])) {
                  setAdmin(server, false);
                  logger.info("Server now available for everyone.");
                } else if (Strings.isTrue(rest[2])) {
                  setAdmin(server, true);
                  logger.info("Server now only available for administrators.");
                } else {
                  logger.err("Invalid property value! Must be 'true', 'false' or 'null'.");
                  return;
                }
              } else logger.info("The server '@' is currently available for @.", server.name, 
                                 server.adminOnly ? "administrators only" : "everyone");
              if (server.adminOnly) 
                logger.warn("This doesn't guarantee that only administrators from the server can connect. "
                          + "Don't forget to set a whitelist on the other server to ensure its security.");
              break;
              
            default:
              logger.err("Invalid property name! Must be 'alias', 'display-name' or 'admin-only'.");
          }
          break;
          
        default: 
          logger.err("Invalid argument! Must be 'help', 'refresh', 'add', 'remove', 'clear' or 'set'.");
      }
    });
  }
  
  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    handler.add("hub", "Connect you to the hub server. [gray]Shortcut of '/switch hub'.[]", (args, player) -> {
      Server s = getHub();
      if (s == null) Players.err(player, "No hub server defined.");
      else safeConnect(player, s);
    });

    handler.add("switch", "[name|alias...]", "Connect you to another server.", (args, player) -> {
      if (servers.isEmpty()) {
        Players.err(player, "No server defined.");
        return;

      } else if (args.length == 0) {
        discovery(
          () -> Players.info(player, "[orange]\ue86a Checking servers..."),
          () -> Players.info(player, "[orange]\ue837 A discovery is running, please wait..."), 
          () -> {
            //TODO: a popup instead?
            Players.info(player, "Available servers:");
            StringBuilder builder = new StringBuilder();
            servers.each((n, s) -> {
              if (!isServerValid(s) || s.adminOnly && !player.admin()) return;
              builder.append(" [gray]- [orange]");
              formattedServerName(builder, s);
              builder.append("[white]: ");
              if (s.info == null) {
                builder.append("[scarlet]Offline\n");
                return;
              } 
              builder.append("[accent]").append(s.info.players);
              if (s.info.playerLimit != 0) builder.append("[white]/[accent]").append(s.info.playerLimit);
              builder.append("[white] player");
              if (s.info.players > 1) builder.append('s');
              builder.append(", ").append(s.info.mapname).append('\n');
            });
            Players.info(player, builder.toString());
          }
        );
        return;
      }
      
      String name = Strings.kebabize(args[0]); // In case of the player typed the name with spaces or capital letters.
      Server server = get(name);
      if (server == null) server = getByAlias(name);
      
      if (server != null && isServerValid(server)) safeConnect(player, server);
      else Players.err(player, "No server named '[orange]@[]' found.", name);
    });
  }
}