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

package fr.zetamap.morecommands.misc;

import arc.util.Nullable;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.modules.security.Punishment;
import fr.zetamap.morecommands.modules.server.Server;
import fr.zetamap.morecommands.modules.voting.PlayerVoteSession;
import fr.zetamap.morecommands.modules.voting.VoteSession.VoteType;


public class MCEvents {
  public static class PunishmentEvent {
    public final @Nullable PlayerData author, target;
    public final Punishment punishment;
    
    public PunishmentEvent(PlayerData author, PlayerData target, Punishment punishment) {
      this.author = author;
      this.target = target;
      this.punishment = punishment;
    }
  }
  public static class PunishmentPardonedEvent {
    public final @Nullable PlayerData author;
    public final Punishment punishment;
    public final Punishment.Pardon pardon;
    
    public PunishmentPardonedEvent(PlayerData author, Punishment punishment, Punishment.Pardon pardon) {
      this.author = author;
      this.punishment = punishment;
      this.pardon = pardon;
    }
  }
  /*
  /** Only fired for online players. *\/
  public static class PunishmentExpiredEvent {
    public final PlayerData player;
    public final Punishment punishment;
    
    public PunishmentExpiredEvent(PlayerData player, Punishment punishment) {
      this.player = player;
      this.punishment = punishment;
    }
  }
  */
  
  public static class GatekeeperProcessStartedEvent {
    public final Gatekeeper.Context context;

    public GatekeeperProcessStartedEvent(Gatekeeper.Context context) {
      this.context = context;
    }
  }
  /** Called after a processor finished. Instance is reused while processing a client, do not nest! */
  public static class GatekeeperProcessedEvent {
    public String name;
    public Gatekeeper.Priority priority;
    public Gatekeeper.Result result;
    public final Gatekeeper.Context context;
    
    public GatekeeperProcessedEvent(Gatekeeper.Context context) {
      this.context = context;
    }
    
    public GatekeeperProcessedEvent set(String name, Gatekeeper.Priority priority, Gatekeeper.Result result) {
      this.name = name;
      this.priority = priority;
      this.result = result;
      return this;
    }
  }
  
  public static class PlayerSwitchedEvent {
    public final PlayerData player;
    public final Server server;
    
    public PlayerSwitchedEvent(PlayerData player, Server server) {
      this.player = player;
      this.server = server;
    }
  }
  
  public static class VoteSessionStartedEvent {
    public final PlayerVoteSession<?> session;
    public final PlayerData author;
    
    public VoteSessionStartedEvent(PlayerVoteSession<?> session, PlayerData author) {
      this.session = session;
      this.author = author;
    }
  }
  public static class VoteSessionVotedEvent {
    public final PlayerVoteSession<?> session;
    public final PlayerVoteSession.VoteType type;
    public final PlayerData player;
    
    public VoteSessionVotedEvent(PlayerVoteSession<?> session, VoteType type, PlayerData player) {
      this.session = session;
      this.type = type;
      this.player = player;
    }
  }
  public static class VoteSessionClosedEvent {
    public final PlayerVoteSession<?> session;
    public final @Nullable PlayerData author;
    public final boolean passed;
    
    public VoteSessionClosedEvent(PlayerVoteSession<?> session, PlayerData author, boolean passed) {
      this.session = session;
      this.author = author;
      this.passed = passed;
    }
    
    public boolean passed() { return passed && author == null; }
    public boolean forced() { return passed && author != null; }
    public boolean failed() { return !passed && author == null; }
    public boolean canceled() { return !passed && author != null; }
  }
}
