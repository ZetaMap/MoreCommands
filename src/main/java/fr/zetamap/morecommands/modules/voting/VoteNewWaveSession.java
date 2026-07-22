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
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.util.DurationFormatter;


public class VoteNewWaveSession extends PlayerVoteSession<Integer> {
  public VoteNewWaveSession() {
    super(1* 60, 2 * 60);
  }

  @Override
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

  @Override
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

  @Override
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

  /** Skip the waves. This doesn't just increases the wave number, it runs them. */
  public void skipWaves() {
    int waves = objective();
    while (waves-- > 0) mindustry.Vars.logic.skipWave();
  }

  @Override
  protected void sessionStarted(PlayerData by) {
    String vote = remaining() == 1 ? "vote is" : "votes are";
    Modules.messaging.serverInfo("VNW", """
      @ started a vote to run @.
      @ more @ required [gray]([lightgray]@[gray]/[lightgray]@[gray])[white]. \
      Type [orange]/vnw y[] or [orange]/vnw n[] to agree or not.""",
      by.getName(), stringObjective(), remaining(), "[]"+vote, "[]"+votes(), "[]"+required()
    );
  }

  @Override
  protected void sessionPassed() {
    Modules.messaging.serverOk("VNW", "Vote passed, @ will start soon.", stringObjective());
    skipWaves();
  }

  @Override
  protected void sessionForced(PlayerData by) {
    Modules.messaging.serverOk("VNW", "Vote skipped by @, @ will start soon.", by.getName(), stringObjective());
    skipWaves();
  }

  @Override
  protected void sessionFailed() {
    Modules.messaging.serverInfo("VNW", "[scarlet]Vote failed![] Not enough votes to run @.", stringObjective());
  }

  @Override
  protected void sessionCanceled(PlayerData by) {
    Modules.messaging.serverWarn("VNW", "Vote cancelled by @.", by.getName());
  }

  @Override
  protected void sessionVote(PlayerData who, VoteType type) {
    String vote = remaining() == 1 ? "vote is" : "votes are";
    Modules.messaging.serverInfo("VNW", """
      @ voted to @run @.
      @ more @ required [gray]([lightgray]@[gray]/[lightgray]@[gray])[white]. \
      Type [orange]/vnw y[] or [orange]/vnw n[] to agree or not with him.""",
      who.getName(), type.yes() ? "[]" : "[]not ", stringObjective(), "[]"+vote, "[]"+votes(), "[]"+required()
    );
  }

  @Override
  protected void sessionVoteRemoved(PlayerData who) {
    String vote = remaining() == 1 ? "vote is" : "votes are";
    Modules.messaging.serverInfo("VNW",
      "@ [orange]left the game[], @ more @ now required [gray]([lightgray]@[gray]/[lightgray]@[gray])[white].",
      who.getName(), remaining(), "[]"+vote, "[]"+votes(), "[]"+required()
    );
  }

  protected String stringObjective() {
    int waves = objective();
    return (waves == 1 ? "the" : waves+"") + ' ' + (waves == 1 ? "wave" : "waves");
  }
}
