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
import arc.math.Mathf;
import arc.struct.IntMap;

import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.net.Administration.Config;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.*;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.util.IntervalProv;
import fr.zetamap.morecommands.util.Strings;


public class VotingModule extends AbstractModule {
  private final IntMap<IntervalProv> rateLimitCache = new IntMap<>();
  private final float messageRateLimit = 3 * 60;
  
  public final RockTheVoteSession rtvSession = new RockTheVoteSession();
  public final VoteNewWaveSession vnwSession = new VoteNewWaveSession();
  public final VoteKickSession vkSession = new VoteKickSession();
  public boolean canVote;
  
  @Override
  protected void initImpl() {
    Events.on(EventType.GameOverEvent.class, e -> {
      // Disable votes
      canVote = false; 
      // Stop votes
      rtvSession.cancel();
      vnwSession.cancel();
    });

    Events.on(EventType.WorldLoadEvent.class, e -> {
      // Enable votes
      canVote = true;
    });
    
    Events.on(EventType.PlayerLeave.class, e -> {
      PlayerData player = PlayerData.get(e.player);
      rtvSession.remove(player);
      vnwSession.remove(player);
      vkSession.remove(player); // remove the vote?
      rateLimitCache.remove(player.player.id);
    });
    
    // Players who are currently being voted on can no longer interact, to prevent griefing.
    Vars.netServer.admins.addActionFilter(a -> {
      return getrate(a.player).get(messageRateLimit, () -> {
        if (vkSession.started() && vkSession.objective().target.player == a.player) {
          Players.err(a.player, "You are currently being voted in. \n"
                              + "You can no longer interact with the game elements until the vote ends.");
          return false;
        }
        return true;  
      });
    });
  }

  private IntervalProv getrate(Player player) {
    return rateLimitCache.get(player.id, IntervalProv::new);
  }
  
  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    handler.add("votekick", "[player] [reason...]", "Vote to kick a player with a valid reason.", (args, player) -> {
      //TODO: make popup instead?
      if (!Config.enableVotekick.bool()) {
        Players.err(player, "Vote-kick is disabled on this server.");
        return;
      } else if (!canVote) {
        Players.err(player, "Votes are disabled for now, please wait.");
        return;
        
      } else if (args.length == 0) {
        if (PlayerData.size() == 1) {
          Players.info(player, "No player to votekick.");
          return;
        }
        
        StringBuilder builder = new StringBuilder("[orange]Players to kick: \n");
        PlayerData.each(p -> !p.admin() && p.player.con != null && p != player, p -> {
            builder.append(" [orange]- ").append(p.getName());
            if (player.admin()) builder.append(" [orange]/ [lightgray]").append(p.player.uuid());
            builder.append(" [accent](#").append(p.player.id()).append(")[]\n");
        });
        Players.info(player, builder.toString());
        return;
      } else if (!vkSession.canStart(player, null)) return;
      
      Players.SearchResult result = Players.find(args);
      if (!result.found) Players.errPlayerNotFound(player);
      else if (result.rest.length == 0) 
        Players.warn(player, "You need a valid reason to kick the player.[] Add a reason after the player name.");
      else vkSession.start(player, new VoteKickSession.Context(player, result.player, Strings.join(" ", result.rest)));
    });
    
    handler.add("vote", "<y|n|c>", "Vote to kick the current player. Admins can cancel the vote with 'c'.", (args, player) -> {
      if (!canVote) {
        Players.err(player, "Votes are disabled for now, please wait.");
        return;
      } 
      
      switch (args[0].toLowerCase()) {
        case "y": case "yes": vkSession.yes(player); break;
        case "n": case "no": vkSession.no(player); break;
        case "c": case "cancel": vkSession.cancel(player); break;
        default: Players.err(player.player, "Vote either 'y' (yes)@ 'n' (no)@.", player.admin() ? ", " : " or", 
                             player.admin() ? " or 'c' (cancel)" : "");
      }
    });
    
    handler.add("maps", "[page]", "List all maps of the server.", (args, player) -> {
      if (args.length == 1 && !Strings.canParseInt(args[0])) {
        Players.err(player, "'[orange]page[]' must be a number.");
        return;
      }

      StringBuilder builder = new StringBuilder();
      int page = args.length == 1 ? Strings.parseInt(args[0]) : 1, 
          perPage = 12, 
          pages = Mathf.ceil((float)Vars.maps.all().size / perPage);

      if (page > pages || page < 1) {
        Players.err(player, "'[orange]page[]' must be a number between [orange]1[] and [orange]" + pages + "[].");
        return;
      }

      builder.append("[accent]Current map:[] ").append(Vars.state.map.name()).append("[white]\n")
             .append("[orange]---- [gold]Maps page [lightgray]").append(page).append("[gray]/[]").append(pages)
             .append("[orange] ----\n");
      for (int i=(page-1)*perPage; i < Math.min(perPage*page, Vars.maps.all().size); i++) {
        Map map = Vars.maps.all().get(i);
        builder.append("[orange]  - [green]").append(map.workshop ? '\ue822' : map.custom ? '\ue81d' : '\ue811')
               .append(" [accent]").append(map.name()).append(" [gray]([lightgray]").append(map.width).append("[]x[lightgray]")
               .append(map.height).append("[])[]").append(" [white]by [sky]").append(map.author()).append('\n');
      }
      builder.append("[orange]-----------------------");
      
      Players.info(player, builder.toString());
    });

    handler.add("vnw", "[y|n|c|f|number]", "Vote for sending a new wave.", (args, player) -> {
      if (!canVote) {
        Players.err(player, "Votes are disabled for now, please wait.");
        return;
        
      } else if (args.length == 0) {
        vnwSession.start(player);
        return;
      }
   
      switch (args[0].toLowerCase()) {
        case "y": case "yes": vnwSession.yes(player); break;
        case "n": case "no": vnwSession.no(player); break;
        case "c": case "cancel": vnwSession.cancel(player); break;
        case "f": case "force": 
          if (vnwSession.started()) vnwSession.force(player);
          else if (!player.admin()) Players.errArgUseDenied(player);
          else {
            vnwSession.skipCooldown();
            Players.ok(player, "Cooldown skipped.");
          }
          break;
        default:
          int waves = Strings.parseInt(args[0]);
          if (waves == Integer.MIN_VALUE) {
            if (!player.admin()) Players.err(player.player, "Vote either 'y' (yes) or 'n' (no).");
            else Players.err(player.player, "Vote either 'y' (yes), 'n' (no), 'c' (cancel) or 'f' (force).@",
                             vnwSession.started() ? "\nOr a number of waves to send." : "");
          } else if (!player.admin()) Players.errArgUseDenied(player);
          else vnwSession.start(player, waves);
      }
    });

    handler.add("rtv", "[y|n|c|f|mapName...]", "Vote to change the map.", (args, player) -> {
      if (!canVote) {
        Players.err(player, "Votes are disabled for now, please wait.");
        return;
        
      } else if (args.length == 0) {
        rtvSession.start(player);
        return;
      }
   
      switch (args[0].toLowerCase()) {
        case "y": case "yes": rtvSession.yes(player); break;
        case "n": case "no": rtvSession.no(player); break;
        case "c": case "cancel": rtvSession.cancel(player); break;
        case "f": case "force": 
          if (rtvSession.started()) rtvSession.force(player);
          else if (!player.admin()) Players.errArgUseDenied(player);
          else {
            rtvSession.skipCooldown();
            Players.ok(player, "Cooldown skipped.");
          }
          break;
        default: rtvSession.start(player, args[0]);
      }
    });
  }
}
