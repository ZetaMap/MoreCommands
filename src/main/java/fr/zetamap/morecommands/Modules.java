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

package fr.zetamap.morecommands;

import mindustry.mod.Mod;

import fr.zetamap.morecommands.modules.command.CommandsModule;
import fr.zetamap.morecommands.modules.effect.EffectsModule;
import fr.zetamap.morecommands.modules.godmode.GodmodeModule;
import fr.zetamap.morecommands.modules.help.HelpModule;
import fr.zetamap.morecommands.modules.manager.ManagerModule;
import fr.zetamap.morecommands.modules.messaging.MessagingModule;
import fr.zetamap.morecommands.modules.misc.MiscModule;
import fr.zetamap.morecommands.modules.security.AdminUsidModule;
import fr.zetamap.morecommands.modules.security.AntiEvadeModule;
import fr.zetamap.morecommands.modules.security.CrackedClientsModule;
import fr.zetamap.morecommands.modules.security.ModerationModule;
import fr.zetamap.morecommands.modules.security.PunishmentsModule;
import fr.zetamap.morecommands.modules.security.ReservedNamesModule;
import fr.zetamap.morecommands.modules.selector.SelectorModule;
import fr.zetamap.morecommands.modules.server.SwitchModule;
import fr.zetamap.morecommands.modules.tag.TagsModule;
import fr.zetamap.morecommands.modules.team.TeamingModule;
import fr.zetamap.morecommands.modules.tp.TeleportModule;
import fr.zetamap.morecommands.modules.voting.VotingModule;
import fr.zetamap.morecommands.modules.world.WorldEditModule;


/** Static access to the MoreCommands modules. */
public class Modules {
  public static ManagerModule manager;
  public static CommandsModule commands;
  public static HelpModule help;
  public static AdminUsidModule usid;
  public static MessagingModule messaging;
  public static VotingModule voting;
  public static TagsModule tags;
  public static EffectsModule effects;
  public static SwitchModule switcher;
  public static TeamingModule team;
  public static TeleportModule teleport;
  public static WorldEditModule worldEdit;
  public static MiscModule misc;
  public static GodmodeModule godmode;
  public static PunishmentsModule punishments;
  public static ModerationModule moderation;
  public static SelectorModule selector;
  public static CrackedClientsModule cracked;
  public static ReservedNamesModule reserved;
  public static AntiEvadeModule antiEvade;

  /** Creates and registers the MoreCommands modules. */
  public static void register(Mod context) {
    manager = new ManagerModule().register(context);
    commands = new CommandsModule().register(context);
    help = new HelpModule().register(context);
    usid = new AdminUsidModule().register(context);
    messaging = new MessagingModule().register(context);
    voting = new VotingModule().register(context);
    tags = new TagsModule().register(context);
    effects = new EffectsModule().register(context);
    switcher = new SwitchModule().register(context);
    team = new TeamingModule().register(context);
    teleport = new TeleportModule().register(context);
    worldEdit = new WorldEditModule().register(context);
    misc = new MiscModule().register(context);
    godmode = new GodmodeModule().register(context);
    punishments = new PunishmentsModule().register(context);
    moderation = new ModerationModule().register(context);
    selector = new SelectorModule().register(context);
    cracked = new CrackedClientsModule().register(context);
    reserved = new ReservedNamesModule().register(context);
    antiEvade = new AntiEvadeModule().register(context);
  }
}
