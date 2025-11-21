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

package fr.zetamap.morecommands.modules.team;

import arc.util.Structs;

import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Unit;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.ClientCommandHandler;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.modules.selector.SelectorParser;
import fr.zetamap.morecommands.util.Strings;


public class TeamingModule extends AbstractModule {
  /** Search a team by his name ({@code crux}, {@code malis}, {@code team#50}) or his id ({@code #2}, {@code #255}). */
  public Team searchTeam(String nameOrId) {
    if (nameOrId.isEmpty()) return null;
    if (nameOrId.charAt(0) == '#') {
      int id = Strings.parseInt(nameOrId, 10, Integer.MIN_VALUE, 1, nameOrId.length());
      return id < 0 || id > Team.all.length-1 ? null : Team.get(id);
    }
    if (nameOrId.equals("purple")) return Team.malis; // old name
    return Structs.find(Team.all, t -> t.name.equals(nameOrId));
  }
  
  public Team getTeam(PlayerData executor, String tildNameOrId) {
    return tildNameOrId.equals("~") ? executor.player.team() : searchTeam(tildNameOrId);
  }
  
  /** 
   * Will change the team and do the associated things if it's the vanish team or if the player leaves it.
   * @return whether {@code player} has been {@link PlayerData#vanished()} or not.
   */
  public boolean setTeam(PlayerData player, Team team) {
    boolean wasVanished = player.player.team() == PlayerData.vanishTeam;
    if (!wasVanished) player.lastTeam = player.player.team();
    player.player.team(team);
    if (player.vanished()) {
      player.rainbowed = false;
      player.effect = null;
      player.setName();
      player.player.clearUnit(); // un-control the unit instead of killing it
      return true;
    } else if (wasVanished) {
      player.lastTeam = null;
      player.setName();
    }
    return false;
  }
  
  /** @return whether the {@code player} was {@link PlayerData#vanished()} or not. */
  public boolean removeVanish(PlayerData player) {
    if (!player.vanished()) return false;
    if (player.lastTeam == null) {
      Players.warn(player, "No last team registered, using default one.");
      player.lastTeam = Vars.state.rules.defaultTeam;
    }
    player.player.team(player.lastTeam);
    player.lastTeam = null;
    player.setName();
    return true;
  }
  
  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    //TODO: allow this command in pvp?
    handler.addAdmin("team", "[teamName|vanish|~] [player|selector...]", "Change team.", (args, player) -> {
      if (args.length == 0) {
        // Not a great idea to mix two behavior. 
        // But I don't want another argument or to run again '/team vanish' to get send back
        if (player.vanished()) {
          Players.info(player, "Transferring you back to your last team...");
          removeVanish(player);

        } else {
          Players.info(player, "Available teams: ");
          StringBuilder builder = new StringBuilder("  - [lightgray]vanish[] [scarlet](admin)[]\n");
          for (Team t : Team.all) {
            int cores = t.cores().size;
            builder.append("  - ").append(t.coloredName()).append(": ");
            if (cores == 0) builder.append("[accent]no cores[]");
            else builder.append("[accent]").append(cores).append("[]").append(cores == 1 ? " core" : " cores");
            builder.append('\n');
            if (t.id >= 10) {
              builder.append("... [accent]team#").append(t.id+1).append('-').append(Team.all.length-2).append("[]");
              break;
            }
          }
          Players.info(player, builder.toString());          
        }
        return;
      }
      
      Team team = args[0].equals("vanish") ? PlayerData.vanishTeam : getTeam(player, args[0]);
      if (team == null) {
        Players.err(player, "No team found for name or id '[orange]@[]'.", args[0]);
        return;
      }
      String teamName = team.coloredName();
      
      if (args.length == 1) {
        if (args[0].equals("~")) 
          Players.warn(player, "Makes no sense to get transferred to your current team =/. Use this with a selector instead.");
        //else if (team != PlayerData.vanishTeam && team.cores().isEmpty()) 
        //  Players.err(player, "This team has no cores. Cannot transfer you.");
        else {
          if (!setTeam(player, team)) Players.ok(player, "Transferred you to the [white]@[] team.", teamName);
          else Players.ok(player, "You are now in vanish mode. [lightgray]Use [gray]/team[] to disable it.");
        }
        return;
      }
      
      SelectorParser parsed = Modules.selector.parse(player, args, 1, args.length);
      if (parsed == null) return; // Error message has already been send to the player
      if (team == PlayerData.vanishTeam && parsed.selected != null && !parsed.selected.allMatch(Unit::isPlayer)) {
        Players.err(player, "Vanish team is reserved for players but some units was selected.");
        return;
      }
      
      parsed.execute((p, u) -> {
        if (p == null) {
          u.team(team);
          return;
        }
        boolean vanish = setTeam(p, team);
        if (p == player) return;
        if (vanish) Players.warn(p, "You have been vanished by @[orange]. @", player.getName(),
                                 p.admin() ? "[lightgray]Use [gray]/team[] to disable it." : "");
        else Players.warn(p, "You have been transferred to the [white]@[] team by @[orange].", teamName, player.getName());
      });
      if (team == PlayerData.vanishTeam) Players.ok(player, parsed.formatMessage("Vanished", true) + "[green].");
      else Players.ok(player, "@[green] to the [white]@[] team.", parsed.formatMessage("Transferred", true), teamName);
    });
  }
}
