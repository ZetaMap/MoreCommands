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

package fr.zetamap.morecommands.modules.godmode;

import arc.Events;

import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Unitc;
import mindustry.net.Administration.ActionType;
import mindustry.world.blocks.ConstructBlock;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.ClientCommandHandler;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.modules.selector.SelectorParser;
import fr.zetamap.morecommands.util.Strings;


public class GodmodeModule extends AbstractModule {
  public void setGodmode(PlayerData player, boolean enabled) {
    player.inGodmode = enabled;
    if (player.player.dead()) return;
    player.player.unit().health = enabled ? Float.POSITIVE_INFINITY : player.player.unit().maxHealth;
  }

  /** @return the new godmode status. */
  public boolean toggleGodmode(PlayerData player) {
    setGodmode(player, !player.inGodmode);
    return player.inGodmode;
  }

  @Override
  protected void initImpl() {
    // Instant build
    Vars.netServer.admins.addActionFilter(a -> {
      PlayerData p = PlayerData.get(a.player);
      if (p != null && p.inGodmode) {
        if (a.type == ActionType.placeBlock)
          ConstructBlock.constructed(a.tile, a.block, p.unit(), (byte)a.rotation, p.team(), a.config);
        else if (a.type == ActionType.breakBlock)
          Call.deconstructFinish(a.tile, a.block, p.unit());
      }
      return true;
    });

    // Infinite health
    Events.on(EventType.UnitChangeEvent.class, e -> {
      PlayerData player = PlayerData.get(e.player);
      if (player == null) return;
      if (player.lastUnit != null && player.lastUnit.health == Float.POSITIVE_INFINITY) player.lastUnit.clampHealth();
      if (player.inGodmode && e.unit != null) e.unit.health = Float.POSITIVE_INFINITY;
      player.lastUnit = e.unit;
    });

    // Instant unit kill
    Events.on(EventType.UnitDamageEvent.class, e -> {
      if (e.unit.dead || !(e.bullet.owner instanceof Unitc u) || u == e.unit) return;
      PlayerData player = PlayerData.get(u);
      if (player == null || !player.inGodmode) return;
      e.unit.kill();
    });

    // Instant block destroy
    Events.on(EventType.BuildDamageEvent.class, e -> {
      if (e.build.dead || !(e.source.shooter instanceof Unitc u)) return;
      PlayerData player = PlayerData.get(u);
      if (player == null || !player.inGodmode) return;
      e.build.kill();
    });
  }

  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    handler.addAdmin("godmode", "[on|off] [player|selector...]", "[coral][[[scarlet]God[]]: [gold]I'm divine!",
    (args, player) -> {
      if (args.length == 0) {
        player.info("Godmode is currently @.", player.inGodmode ? "enabled" : "disabled");
        return;
      }

      boolean enable;
      if (Strings.isTrue(args[0])) enable = true;
      else if (Strings.isFalse(args[0])) enable = false;
      else {
        player.err("Invalid argument! Must be '@' or '@'.", "on", "off");
        return;
      }

      if (args.length == 1) {
        if (enable == player.inGodmode)
          player.err("Godmode already @.", enable ? "enabled" : "disabled");
        else {
          setGodmode(player, enable);
          player.ok("Godmode @.", enable ? "enabled" : "disabled");
        }
        return;
      }

      SelectorParser selector = Modules.selector.parse(player, args, 1, args.length, true);
      if (selector == null) return;
      int[] count = {0};
      selector.execute((p, u) -> {
        if (enable == p.inGodmode)
          player.warn("Godmode already @ for @.", enable ? "enabled" : "disabled", p.getName());
        else {
          count[0]++;
          setGodmode(p, enable);
          if (p == player) return;
          p.warn("Godmode @ by @.", enable ? "enabled" : "disabled", player.getName());
        }
      });
      player.ok("@ godmode for @ players.", enable ? "Enabled" : "Disabled", count[0]);
    });
  }
}
