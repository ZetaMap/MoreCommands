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

package fr.zetamap.morecommands.modules.voting;

import arc.Events;

import mindustry.Vars;
import mindustry.game.EventType.GameOverEvent;
import mindustry.gen.Call;
import mindustry.maps.Map;
import mindustry.server.ServerControl;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.util.DurationFormatter;
import fr.zetamap.morecommands.util.Strings;


public class RockTheVoteSession extends PlayerVoteSession<Map> {
  public RockTheVoteSession() {
    super(2 * 60, 3 * 60);
  }
  
  public boolean canStart(PlayerData player, Map map) {
    if (PlayerData.size() < 2 && !player.admin()) {
      Players.err(player, "At least 2 players are required to start a vote.");
      return false;
    } else if (started()) {
      Players.err(player, "A vote to change the map is already in progress! [lightgray](selected: [accent]@[lightgray])\n"
                        + "[scarlet]Type [orange]/rtv y[] or [orange]/rtv n[] to agree or not.", objective().name());
      return false;
    } else if (waitRemaining() > 0) {
      Players.err(player, "You must wait [orange]@[] before able to restart a vote.", 
                  DurationFormatter.format(waitRemaining()));
      return false;
    }
    return true;
  }
  
  /** Start a new vote session with a random map. */
  public boolean start(PlayerData player) {
    if (!canStart(player, null)) return false; // null objective can be used safely.
    Map map = Vars.maps.getNextMap(ServerControl.instance.lastMode, Vars.state.map);
    Players.info(player, "Randomized to [accent]@[white].", map.name());
    return start(player, map);
  }
  
  /** Start a new vote session using the map name. */
  public boolean start(PlayerData player, String mapName) {
    if (!canStart(player, null)) return false; // null objective can be used safely.
    String name = Strings.stripColors(mapName).replace('_', ' ');
    Map map = Vars.maps.all().find(m -> m.plainName().replace('_', ' ').equalsIgnoreCase(name));

    if (map == null) {
      Players.err(player.player, "No map named '@' found.", mapName);
      return false;
    }
    return start(player, map);
  }
  
  public boolean canVote(PlayerData player) {
    if (!started()) {
      Players.err(player, "No vote session in progress.");
      return false;
    } else if (voted(player) != null) {
      Players.info(player, "You already voted for the map [accent]@[white].", objective().name());
      return false;
    }
    return true;
  }
  
  public boolean canStop(PlayerData player) {
    if (!started()) {
      Players.err(player, "No vote session in progress.");
      return false;
    } else if (!player.admin()) {
      Players.errArgUseDenied(player);
      return false;
    }
    return true;
  }

  @Override
  public int required() {
    return PlayerData.size() / 2 + 1;
  }

  /** Runs a gameover and skips the current map to the selected one. */
  public void changeMap() {
    Vars.maps.setNextMapOverride(objective());
    ServerControl.instance.inGameOverWait = false;
    Events.fire(new GameOverEvent(Vars.state.rules.waveTeam));
  }
  
  @Override
  protected void sessionStarted(PlayerData by) {
    int remaining = required() - votes();
    Call.sendMessage(
      Strings.format("[scarlet]RTV: @[white] started a vote to change the map to [accent]@[white].\n"
                   + "[scarlet]RTV: [accent]@[white] more vote(s) are required [gray]([lightgray]@[]/[lightgray]@[])[]."
                   + "Type [orange]/rtv y[] or [orange]/rtv n[] to agree or not.", 
                     by.getName(), objective().name(), remaining, votes(), required()));
  }

  @Override
  protected void sessionPassed() {
    Call.sendMessage(Strings.format("[scarlet]RTV:[green] Vote passed, map changed to [accent]@[green].", objective().name()));
    changeMap();
  }

  @Override
  protected void sessionForced(PlayerData by) {
    Call.sendMessage(Strings.format("[scarlet]RTV:[green] Vote skipped by @[green], map changed to [accent]@[green].",
                                    by.getName(), objective().name()));
    changeMap();
  }
  
  @Override
  protected void sessionFailed() {
    Call.sendMessage(Strings.format("[scarlet]RTV: Vote failed![] Not enough votes to change the map to [accent]@[white].", 
                                    objective().name()));
  }

  @Override
  protected void sessionCanceled(PlayerData by) {
    Call.sendMessage(Strings.format("[scarlet]RTV:[orange] Vote cancelled by @[orange].", by.getName()));
  }

  @Override
  protected void sessionVote(PlayerData who, VoteType type) {
    int remaining = required() - votes();
    Call.sendMessage(
      Strings.format("[scarlet]RTV: @ [white] voted to @change the map to [accent]@[white].\n"
                   + "[scarlet]RTV: [accent]@[white] more vote(s) are required [gray]([lightgray]@[]/[lightgray]@[])[]."
                   + "Type [orange]/rtv y[] or [orange]/rtv n[] to agree or not with him.", 
                     who.getName(), type.yes() ? "" : "not ", objective().name(), remaining, votes(), required()));
  }

  @Override
  protected void sessionVoteRemoved(PlayerData who) {
    int remaining = required() - votes();
    Call.sendMessage(Strings.format("[scarlet]RTV: @ [orange]left the game, [accent]@[white] more vote(s) are now required "
                                  + "[gray]([lightgray]@[]/[lightgray]@[])[].", 
                                    who.getName(), remaining, votes(), required()));
  }
}
