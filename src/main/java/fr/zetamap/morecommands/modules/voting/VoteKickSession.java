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

package fr.zetamap.morecommands.modules.voting;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.modules.security.Punishment;
import fr.zetamap.morecommands.util.DurationFormatter;


public class VoteKickSession extends PlayerVoteSession<VoteKickSession.Context> {
  public VoteKickSession() {
    super(1 * 60, 2 * 60);
  }

  public boolean start(PlayerData by, PlayerData target, String reason) {
    return start(by, new Context(by, target, reason));
  }

  @Override
  public boolean canStart(PlayerData player, Context reason) {
    if (PlayerData.size() < 3 && !player.admin()) {
      player.err("At least @ are needed to start a votekick.", "3 players");
      return false;
    } else if (started()) {
      player.err("A vote to kick @ is already in progress! \nType @ or @ to agree or not.",
                 objective().target.getName(), "/vote y", "/vote n");
      return false;

    } else if (reason != null) { // Can be null when checking for availability
      if (player == reason.target) {
        player.err("You can't vote to kick yourself.");
        return false;
      } else if (reason.target.admin()) {
        player.err("Did you really expect to be able to kick an admin?");
        return false;
      } else if (player.player.team() != reason.target.player.team()) {
        player.err("Only players on your team can be kicked.");
        return false;
      } else if (!mindustry.Vars.state.rules.pvp &&
                 PlayerData.count(p -> p.player.team() == reason.target.player.team()) < 3) {
        player.err("At least 3 players from your own team are needed to start a votekick.");
        return false;
      }
    }
    if (waitRemaining() > 0) {
      player.err("You must wait @ before able to restart a vote.", DurationFormatter.format(waitRemaining()));
      return false;
    }
    return true;
  }

  @Override
  public boolean canVote(PlayerData player) {
    if (!started()) {
      player.err("No vote session in progress.");
      return false;
    } else if (voted(player) != null) {
      player.info("You already voted to kick @.", objective().target.getName());
      return false;
    } else if (objective().target == player) {
      player.err("You can't vote for yourself.");
      return false;
    } else if (objective().target.player.team() != player.player.team()) {
      player.err("You can't vote for other teams.");
      return false;
    }
    return true;
  }

  @Override
  public boolean canStop(PlayerData player) {
    if (!started()) {
      player.err("No vote session in progress.");
      return false;
    } else if (!player.admin()) {
      player.errArgUseDenied();
      return false;
    }
    return true;
  }

  /** Vote cannot be forced. */
  @Override
  public boolean force(PlayerData by) {
    by.err("The vote cannot be forced, kick the player yourself instead.");
    return false;
  }

  @Override
  public int required() {
    // from https://github.com/xpdustry/imperium/blob/master/imperium-mindustry/src/main/kotlin/com/xpdustry/imperium/mindustry/security/VoteKickCommand.kt#L238
    int players = PlayerData.count(p ->p.player.team() == objective().target.player.team());
    return players < 4 ? 2 : players < 5 ? 3 : players < 21 ? (int)Math.ceil(players/2f) : 10;
  }

  public void punishTarget() {
    Context o = objective();
    Modules.punishments.punish(o.by, o.target, Punishment.Type.votekick, o.kickDuration, o.reason);
  }

  @Override
  protected void sessionStarted(PlayerData by) {
    String vote = remaining() == 1 ? "vote is" : "votes are";
    Modules.messaging.serverInfo("VoteKick", """
      @ [lightgray]started a vote to kick @[lightgray].
      @ more @ required [gray]([lightgray]@[gray]/[lightgray]@[gray])[white]. \
      Type [orange]/vote y[] or [orange]/vote n[] to agree or not.
      [lightgray]Reason: [orange]@[lightgray].""",
      by.getName(), objective().target.getName(), remaining(), "[]"+vote, "[]"+votes(), "[]"+required(),
      "[]"+objective().reason
    );
  }

  @Override
  protected void sessionPassed() {
    Modules.messaging.serverWarn("VoteKick", "Vote passed. @ will be kicked from the server.\n"
                                           + "[lightgray]Reason: [orange]@[lightgray].",
                                 objective().target.getName(), "[]"+objective().reason);
    punishTarget();
  }

  @Override
  protected void sessionForced(PlayerData by) {
    Modules.messaging.serverWarn("VoteKick", "Vote skipped by @. @ will be kicked from the server.\n"
                                           + "[lightgray]Reason: [orange]@[lightgray].",
                                 by.getName(), objective().target.getName(), "[]"+objective().reason);
    punishTarget();
  }

  @Override
  protected void sessionFailed() {
    Modules.messaging.serverInfo("VoteKick", "[lightgray]Vote failed! Not enough votes to kick [orange]@[lightgray].",
                                 "[]"+objective().target.getName());
    objective().target.info("[sky]You are no longer involved in a vote kick, you have been unfrozen.");
  }

  @Override
  protected void sessionCanceled(PlayerData by) {
    Modules.messaging.serverWarn("VoteKick", "Vote cancelled by @.", by.getName());
    objective().target.info("[sky]You are no longer involved in a vote kick, you have been unfrozen.");
  }

  @Override
  protected void sessionVote(PlayerData who, VoteType type) {
    String vote = remaining() == 1 ? "vote is" : "votes are";
    Modules.messaging.serverInfo("VoteKick", """
      @ [lightgray]voted to @[lightgray]kick @[lightgray].
      @ more @ required [gray]([lightgray]@[gray]/[lightgray]@[gray])[white]. \
      Type [orange]/vote y[] or [orange]/vote n[] to agree or not with him.""",
      who.getName(), type.yes() ? "[]" : "[]not ", objective().target.getName(), remaining(), "[]"+vote,
      "[]"+votes(), "[]"+required()
    );
  }

  @Override
  protected void sessionVoteRemoved(PlayerData who) {
    String vote = remaining() == 1 ? "vote is" : "votes are";
    Modules.messaging.serverInfo("VoteKick", "@ [orange]left the game[], @ more @ now required "
                                           + "[gray]([lightgray]@[gray]/[lightgray]@[gray])[white].",
                                 who.getName(), remaining(), "[]"+vote, "[]"+votes(), "[]"+required());
  }


  public static class Context {
    public long kickDuration = Punishment.Type.kick.defaultDuration.duration;
    public final PlayerData by;
    /** Not final in case of votekick escape. */
    public PlayerData target;
    public final String reason;

    public Context(PlayerData by, PlayerData target, String reason) {
      this.by = by;
      this.target = target;
      this.reason = reason;
    }
  }
}
