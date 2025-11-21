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

import mindustry.Vars;
import mindustry.gen.Call;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.util.DurationFormatter;
import fr.zetamap.morecommands.util.Strings;


public class VoteNewWaveSession extends PlayerVoteSession<Integer> {
  public VoteNewWaveSession() {
    super(1* 60, 2 * 60);
  }

  public boolean canStart(PlayerData player, Integer wave) {
    if (PlayerData.size() < 3 && !player.admin()) {
      Players.err(player, "At least 3 players are required to start a vote.");
      return false;
    } else if (started()) {
      Players.err(player, "A vote to run [orange]@[] is already in progress!\n"
                        + "Type [orange]/vnw y[] or [orange]/vnw n[] to agree or not.", stringObjective());
      return false;
    } else if (waitRemaining() > 0) {
      Players.err(player, "You must wait [orange]@[] before able to restart a vote.", 
                  DurationFormatter.format(waitRemaining()));
      return false;
    } else if (wave < 1) {
      Players.err(player, "Invalid number of wave. Must be greater than [orange]1[].");
      return false;
    }
    return true;
  }
  
  /** Start a new vote session to skip one wave. */
  public boolean start(PlayerData player) {
    return start(player, 1);
  }
  
  public boolean canVote(PlayerData player) {
    if (!started()) {
      Players.err(player, "No vote session in progress.");
      return false;
    } else if (voted(player) != null) {
      Players.info(player, "You already voted to run [accent]@[].", stringObjective());
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

  /** 
   * Skip the waves. <br>
   * Note that doesn't just increases the wave number, it runs them.
   */
  public void skipWaves() {
    int waves = objective();
    while (waves-- > 0) Vars.logic.skipWave();
  }
  
  @Override
  protected void sessionStarted(PlayerData by) {
    int remaining = required() - votes();
    String vote = remaining == 1 ? "vote is" : "votes are";
    Call.sendMessage(
      Strings.format("[scarlet]VNW: @ [white] started a vote to run [accent]@[].\n"
                   + "[scarlet]VNW: [accent]@[white] more @ required [gray]([lightgray]@[]/[lightgray]@[])[]. "
                   + "Type [orange]/vnw y[] or [orange]/vnw n[] to agree or not.",
                     by.getName(), stringObjective(), remaining, vote, votes(), required()));
  }

  @Override
  protected void sessionPassed() {
    Call.sendMessage(Strings.format("[scarlet]VNW:[green] Vote passed, [accent]@[] will start soon.", stringObjective()));
    skipWaves();
  }

  @Override
  protected void sessionForced(PlayerData by) {
    Call.sendMessage(Strings.format("[scarlet]VNW:[green] Vote skipped by @[green], [accent]@[] will start soon.", 
                                    by.getName(), stringObjective()));
    skipWaves();
  }
  
  @Override
  protected void sessionFailed() {
    Call.sendMessage(Strings.format("[scarlet]VNW: Vote failed![] Not enough votes to run [accent]@[].", stringObjective()));
  }

  @Override
  protected void sessionCanceled(PlayerData by) {
    Call.sendMessage(Strings.format("[scarlet]VNW:[orange] Vote cancelled by @[orange].", by.getName()));
  }

  @Override
  protected void sessionVote(PlayerData who, VoteType type) {
    int remaining = required() - votes();
    String vote = remaining == 1 ? "vote is" : "votes are";
    Call.sendMessage(
      Strings.format("[scarlet]VNW: @ [white] voted to @run [accent]@[white].\n"
                   + "[scarlet]VNW: [accent]@[white] more @ required [gray]([lightgray]@[]/[lightgray]@[])[]."
                   + "Type [orange]/vnw y[] or [orange]/vnw n[] to agree or not with him.",
                     who.getName(), type.yes() ? "" : "not ", stringObjective(), remaining, vote, votes(), required()));
  }

  @Override
  protected void sessionVoteRemoved(PlayerData who) {
    int remaining = required() - votes();
    String vote = remaining == 1 ? "vote is" : "votes are";
    Call.sendMessage(Strings.format("[scarlet]VNW: @ [orange]left the game, [accent]@[white] more @ now required "
                                  + "[gray]([lightgray]@[]/[lightgray]@[])[].",
                                    who.getName(), remaining, vote, votes(), required()));
  }
  
  protected String stringObjective() {
    int waves = objective();
    return (waves == 1 ? "the" : waves+"") + ' ' + (waves == 1 ? "wave" : "waves");
  }
}
