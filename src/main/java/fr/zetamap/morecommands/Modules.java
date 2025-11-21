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

package fr.zetamap.morecommands;

import mindustry.mod.Mod;

import fr.zetamap.morecommands.module.ModuleFactory;
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
    manager = new ManagerModule();          ModuleFactory.add(context, manager);
    commands = new CommandsModule();        ModuleFactory.add(context, commands);    
    help = new HelpModule();                ModuleFactory.add(context, help);
    usid = new AdminUsidModule();           ModuleFactory.add(context, usid);
    messaging = new MessagingModule();      ModuleFactory.add(context, messaging);
    voting = new VotingModule();            ModuleFactory.add(context, voting);
    tags = new TagsModule();                ModuleFactory.add(context, tags);
    effects = new EffectsModule();          ModuleFactory.add(context, effects);
    switcher = new SwitchModule();          ModuleFactory.add(context, switcher);
    team = new TeamingModule();             ModuleFactory.add(context, team);
    teleport = new TeleportModule();        ModuleFactory.add(context, teleport);
    worldEdit = new WorldEditModule();      ModuleFactory.add(context, worldEdit);
    misc = new MiscModule();                ModuleFactory.add(context, misc);
    godmode = new GodmodeModule();          ModuleFactory.add(context, godmode);
    punishments = new PunishmentsModule();  ModuleFactory.add(context, punishments);
    moderation = new ModerationModule();    ModuleFactory.add(context, moderation);
    selector = new SelectorModule();        ModuleFactory.add(context, selector);
    cracked = new CrackedClientsModule();   ModuleFactory.add(context, cracked);
    reserved = new ReservedNamesModule();   ModuleFactory.add(context, reserved);
    antiEvade = new AntiEvadeModule();      ModuleFactory.add(context, antiEvade);
  }
}
