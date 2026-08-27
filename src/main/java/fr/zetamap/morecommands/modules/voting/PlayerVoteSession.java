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

import arc.Events;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.misc.MCEvents;


/** Class that removes the "people" abstraction layer ({@link PlayerData}) and fires events of done actions. */
public abstract class PlayerVoteSession<O> extends VoteSession<PlayerData, O> {
  public PlayerVoteSession(float duration, float cooldown) {
    super(duration, cooldown);
  }

  @Override
  public boolean start(PlayerData player, O objective) {
    boolean started = super.start(player, objective);
    if (started) Events.fire(new MCEvents.VoteSessionStartedEvent(this, player));
    return started;
  }

  @Override
  protected boolean vote(PlayerData people, VoteType type, boolean silent) {
    boolean voted = super.vote(people, type, silent);
    if (voted && !silent) Events.fire(new MCEvents.VoteSessionVotedEvent(this, type, people));
    return voted;
  }

  @Override
  public boolean force() {
    boolean forced = super.force();
    if (forced) Events.fire(new MCEvents.VoteSessionClosedEvent(this, null, true));
    return forced;
  }

  @Override
  public boolean force(PlayerData player) {
    boolean forced = super.force(player);
    if (forced) Events.fire(new MCEvents.VoteSessionClosedEvent(this, player, true));
    return forced;
  }

  @Override
  public boolean cancel() {
    boolean canceled = super.cancel();
    if (canceled) Events.fire(new MCEvents.VoteSessionClosedEvent(this, null, false));
    return canceled;
  }

  @Override
  public boolean cancel(PlayerData player) {
    boolean canceled = super.cancel(player);
    if (canceled) Events.fire(new MCEvents.VoteSessionClosedEvent(this, player, false));
    return canceled;
  }
}
