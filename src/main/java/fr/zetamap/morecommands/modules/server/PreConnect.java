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

package fr.zetamap.morecommands.modules.server;

import mindustry.core.Version;


/** Try to anticipate a refused connection by using the last server discovery information. */
public class PreConnect {
  /** Check for connection availability using {@link Server#info} provided by the last {@link Server#ping()} call. */
  public static Availability verify(boolean adminPlayer, Server server) {
    return
      server.info == null ? Availability.serverClosed :
      !adminPlayer && server.adminOnly ? Availability.adminOnly :
      server.info.playerLimit > 0 && server.info.players >= server.info.playerLimit && !adminPlayer ? Availability.playerLimit :
      !server.info.versionType.equals(Version.type) ? Availability.typeMismatch :
      Version.build == -1 && server.info.version != -1 ? Availability.customClient :
      server.info.version != Version.build && Version.build != -1 && server.info.version != -1 ?
        Version.build > server.info.version ? Availability.serverOutdated : Availability.clientOutdated :
      Availability.ok;
  }


  /** A part come from {@link mindustry.net.Packets.KickReason}. */
  public enum Availability {
    ok, serverOutdated, clientOutdated, typeMismatch, customClient, playerLimit, adminOnly, serverClosed;

    public boolean ok() {
      return this == ok;
    }

    public String toReason(Server server) {
      return switch (this) {
        case ok ->
          "[green]Connection success.";
        case adminOnly ->
          "This server is only for admins.";
        case serverOutdated ->
          "The server is in a lower version of Mindustry. \n" +
          "[gray]Current: [lightgray]v" + Version.build + "[], Required: [lightgray]v" + server.info.version;
        case clientOutdated ->
          "The server is in a newer version of Mindustry. \n" +
          "[gray]Current: [lightgray]v" + Version.build + "[], Required: [lightgray]v" + server.info.version;
        case typeMismatch ->
          "The server is not compatible with this Mindustry version. \n" +
          "[gray]Current: [lightgray]" + Version.type + "[], Required: [lightgray]" + server.info.versionType;
        case customClient ->
          "The server does not accept custom Mindustry versions.";
        case playerLimit ->
          "Server full. [gray]([lightgray]" + server.info.players + "[]/[lightgray]" + server.info.playerLimit + "[])";
        case serverClosed ->
          "The server is not responding. [gray]([lightgray]timeout[])";
        default ->
          "[lightgray]<unknown reason>[]";
      };
    }
  }
}
