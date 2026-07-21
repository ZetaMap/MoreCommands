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

import arc.struct.ObjectSet;

import fr.zetamap.morecommands.misc.Gatekeeper;
import fr.zetamap.morecommands.module.AbstractModule;


/** Go figure why but some people are using cracked clients (of the Steam version) on a free game... Incredible. */
public class CrackedClientsModule extends AbstractModule {
  /** List of all known cracked clients in lower case. */
  public final ObjectSet<String> crackedClientUsernames = ObjectSet.with(
      "valve", "tuttop", "codex", "igggames", "igg-games.com", "igruhaorg", "freetp.org", "goldberg", "rog"
  );
  public String crackedClientsKickMessage = """
    [green]Mindustry is a free and open source game.[]
    [white]It is available on: [royal]https://anuke.itch.io/mindustry[].[]

    [red]Please, get a legit copy of the game.[]""";

  public boolean isCrackedClient(String username) {
    return crackedClientUsernames.contains(username.toLowerCase());
  }

  @Override
  protected void initImpl() {
    Gatekeeper.add(internalName(), Gatekeeper.Priority.low,
      ctx -> isCrackedClient(ctx.strippedName) ? Gatekeeper.reject(crackedClientsKickMessage) : Gatekeeper.accept());
  }
}
