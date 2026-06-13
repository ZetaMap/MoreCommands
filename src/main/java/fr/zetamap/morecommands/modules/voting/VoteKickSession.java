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
import mindustry.core.NetServer;
import mindustry.gen.Call;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.modules.security.Punishment;
import fr.zetamap.morecommands.util.DurationFormatter;
import fr.zetamap.morecommands.util.Strings;


public class VoteKickSession extends PlayerVoteSession<VoteKickSession.Context> {
  public VoteKickSession() {
    super(1 * 60, 2 * 60);
  }
  
  public boolean canStart(PlayerData player, Context reason) {
    if (PlayerData.size() < 3 && !player.admin()) {
      Players.err(player, "At least 3 players are needed to start a votekick.");
      return false;
    } else if (started()) {
      Players.err(player, "A vote to kick @ [scarlet]is already in progress!\n"
                        + "[scarlet]Type [orange]/vote y[] or [orange]/vote n[] to agree or not.", objective().target.getName());
      return false;
    
    } else if (reason != null) {
      if (player == reason.target) {
        Players.err(player, "You can't vote to kick yourself.");
        return false;
      } else if (reason.target.admin()) {
        Players.err(player, "Did you really expect to be able to kick an admin?");
        return false;
      } else if (player.player.team() != reason.target.player.team()) {
        Players.err(player, "Only players on your team can be kicked.");
        return false;
      } else if (!Vars.state.rules.pvp && 
                 PlayerData.count(p -> p.player.team() == reason.target.player.team()) < 3) {
        Players.err(player, "At least 3 players from your own team are needed to start a votekick.");
        return false;
      }
    }
    if (waitRemaining() > 0) {
      Players.err(player, "You must wait [orange]@[] before able to restart a vote.", 
                  DurationFormatter.format(waitRemaining()));
      return false;
    }
    return true;
  }
  
  public boolean canVote(PlayerData player) {
    if (!started()) {
      Players.err(player, "No vote session in progress.");
      return false;
    } else if (voted(player) != null) {
      Players.info(player, "You already voted to kick @[white].", objective().target.getName());
      return false;
    } else if (objective().target == player) {
      Players.err(player, "You can't vote on your own trial.");
      return false;
    } else if (objective().target.player.team() != player.player.team()) {
      Players.err(player, "You can't vote for other teams.");
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
  
  /** Vote cannot be forced by an admin or other. */
  public void force(PlayerData by) {
    Players.err(by, "The vote cannot be forced, kick the player yourself instead.");
  }

  @Override
  public int required() {
    // from https://github.com/xpdustry/imperium/blob/master/imperium-mindustry/src/main/kotlin/com/xpdustry/imperium/mindustry/security/VoteKickCommand.kt#L238
    int players = PlayerData.size();
    return players < 4 ? 2 : players < 5 ? 3 : players < 21 ? (int)Math.ceil(players/2f) : 10;
  }
  
  public void punishTarget() {
    Context o = objective();
    Modules.punishments.punish(o.by, o.target, Punishment.Type.votekick, NetServer.kickDuration * 1000, o.reason);
  }

  @Override
  protected void sessionStarted(PlayerData by) {
    int remaining = required() - votes();
    String vote = remaining == 1 ? "vote is" : "votes are";
    Call.sendMessage(
      Strings.format("@[lightgray] started a vote to kick @[lightgray].\n"
                   + "[accent]@[white] more @ required [gray]([lightgray]@[]/[lightgray]@[])[]."
                   + "Type [orange]/vote y[] or [orange]/vote n[] to agree or not.\n"
                   + "[lightgray]Reason: [orange]@[lightgray].", 
                     by.getName(), objective().target.getName(), remaining, vote, votes(), required(), objective().reason));
  }

  @Override
  protected void sessionPassed() {
    Call.sendMessage(Strings.format("[orange]Vote passed. @[orange] will be kicked from the server.\n"
                                  + "[lightgray]Reason: [orange]@[lightgray].", 
                                    objective().target.getName(), objective().reason));
    punishTarget();
  }

  @Override
  protected void sessionForced(PlayerData by) {
    Call.sendMessage(Strings.format("[orange]Vote skipped by @. @[orange] will be kicked from the server.\n"
                                  + "[lightgray]Reason: [orange]@[lightgray].", 
                                    by.getName(), objective().target.getName(), objective().reason));
    punishTarget();
  }
  
  @Override
  protected void sessionFailed() {
    Call.sendMessage(Strings.format("[lightgray]Vote failed! Not enough votes to kick[orange] @[lightgray].", 
                                    objective().target.getName()));
    Players.info(objective().target, "[sky]You are no longer involved in a vote kick, you have been unfrozen.");
  }

  @Override
  protected void sessionCanceled(PlayerData by) {
    Call.sendMessage(Strings.format("[scarlet]VoteKick: [orange] Vote cancelled by @[orange].", by.getName()));
    Players.info(objective().target, "[sky]You are no longer involved in a vote kick, you have been unfrozen.");
  }

  @Override
  protected void sessionVote(PlayerData who, VoteType type) {
    int remaining = required() - votes();
    String vote = remaining == 1 ? "vote is" : "votes are";
    Call.sendMessage(
      Strings.format("[lightgray]@[lightgray] voted to @kick @[lightgray].\n"
                   + "[accent]@[white] more @ required [gray]([lightgray]@[]/[lightgray]@[])[]."
                   + "Type [orange]/vote y[] or [orange]/vote n[] to agree or not with him.", 
                     who.getName(), type.yes() ? "" : "not ", objective().target.getName(), remaining, vote, votes(), 
                     required()));
  }

  @Override
  protected void sessionVoteRemoved(PlayerData who) {
    int remaining = required() - votes();
    String vote = remaining == 1 ? "vote is" : "votes are";
    Call.sendMessage(Strings.format("[scarlet]VoteKick: @ [orange]left the game, [accent]@[white] more @ now required "
                                  + "[gray]([lightgray]@[]/[lightgray]@[])[].", 
                                    who.getName(), remaining, vote, votes(), required()));
  }

  
  public static class Context {
    public final PlayerData target, by;
    public final String reason;
    
    public Context(PlayerData by, PlayerData target, String reason) {
      this.by = by;
      this.target = target;
      this.reason = reason;
    }
  }
}
