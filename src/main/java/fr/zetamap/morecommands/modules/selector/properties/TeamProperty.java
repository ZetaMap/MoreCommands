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

package fr.zetamap.morecommands.modules.selector.properties;

import arc.math.geom.Vec2;
import arc.struct.ObjectSet;
import arc.util.Structs;

import mindustry.game.Team;
import mindustry.gen.Player;
import mindustry.gen.Unit;

import fr.zetamap.morecommands.modules.selector.Selector;
import fr.zetamap.morecommands.modules.selector.SelectorProperty;
import fr.zetamap.morecommands.util.StringReader;


/** 
 * Accepted formats: {@code team=}, {@code team=!}, {@code team=team}, {@code team=!team}, 
 * {@code team=[team1, ...]} or {@code team=![team1, ...]}. <br>
 * {@code team} value can be a named team from {@link Team#baseTeams} (e.g. {@code sharded}, {@code crux}),
 * an unnamed team from {@link Team#all} {@code team#7-255} (e.g. {@code team#7}, {@code team#46})
 * or a team id from {@link Team#all} {@code 0 -> 255} (e.g. {@code #1}, {@code #64})
 */
public class TeamProperty extends SelectorProperty {
  @Override
  public Parsed read(StringReader reader) {
    boolean negated = reader.isNegated();
    return !reader.isArray() ? new Parsed(readTeam(reader, true), negated) :
           new Parsed(reader.readSet(this::readTeam, "team name"), negated);
  }

  public Team readTeam(StringReader reader) { return readTeam(reader, false); }
  public Team readTeam(StringReader reader, boolean optional) {
    if (reader.peekNext() == '#') {
      reader.readNext();
      int id = reader.readNumeric(true);
      if (id < 0 || id > Team.all.length-1) throw reader.error("Invalid team id (range 0-255)");
      return Team.get(id);
    }
    String name = reader.readString();
    if (name == null) {
      if (optional) return null;
      throw reader.expected("a team name");
    }
    Team team = Structs.find(Team.all, t -> t.name.equals(name));
    if (team == null && !optional) throw reader.notFound("team", name);
    return team;
  }
  
  
  public class Parsed extends SelectorProperty.Parsed {
    /** Not {@code null} if multiple teams are specified. */
    public final ObjectSet<Team> teams;
    /** {@code null} if no or multiple teams are specified. */
    public final Team team;
    public final boolean negated;
    
    public Parsed(Team team, boolean negated) { 
      this.teams = null;
      this.team = team; 
      this.negated = negated;
    }
    
    public Parsed(ObjectSet<Team> teams, boolean negated) { 
      this.teams = teams;
      this.team = null; 
      this.negated = negated;
    }

    @Override
    public boolean passes(Selector selector, Player executor, Vec2 pos, Unit entity) {
      return negated ^ (teams != null ? teams.contains(entity.team) : 
                        team != null ? team == entity.team :
                        executor == null || executor.team() == entity.team);
    }
  }
}
