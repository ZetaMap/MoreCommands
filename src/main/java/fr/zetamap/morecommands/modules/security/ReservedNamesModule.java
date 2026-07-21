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

package fr.zetamap.morecommands.modules.security;

import arc.struct.Seq;

import mindustry.Vars;
import mindustry.net.Packets.KickReason;

import fr.zetamap.morecommands.command.ServerCommandHandler;
import fr.zetamap.morecommands.misc.Gatekeeper;
import fr.zetamap.morecommands.module.AbstractSaveableModule;
import fr.zetamap.morecommands.util.JsonSettings;
import fr.zetamap.morecommands.util.Strings;


public class ReservedNamesModule extends AbstractSaveableModule {
  private boolean enabled;
  private String message;

  public final Seq<String> names = new Seq<>();

  public boolean add(String name) {
    setModified();
    return names.addUnique(clean(name));
  }

  public boolean remove(String name) {
    setModified();
    return names.remove(clean(name));
  }

  public boolean isReserved(String name) {
    name = clean(name);
    return names.contains(name::contains);
  }

  /** Removes colors, glyphs, and lower the case. */
  public String clean(String name) {
    return Strings.normalize(name).toLowerCase();
  }

  public boolean enabled() {
    return enabled;
  }

  public void enable() {
    enabled = true;
    setModified();
  }

  public void disable() {
    enabled = false;
    setModified();
  }

  public String kickMessage() {
    return message;
  }

  public void kickMessage(String message) {
    this.message = message;
    setModified();
  }

  @Override
  protected void initImpl() {
    Gatekeeper.add(internalName(), Gatekeeper.Priority.low, ctx ->
      enabled() && !Vars.netServer.admins.isAdmin(ctx.uuid, ctx.usid) && isReserved(ctx.strippedName) ?
        kickMessage() == null ? Gatekeeper.reject(KickReason.nameInUse) : Gatekeeper.reject(kickMessage()) :
      Gatekeeper.accept());
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void loadImpl(JsonSettings settings) {
    enabled = settings.getBool("enabled", true);
    message = settings.getString("message", "This nickname (or a part) is reserved, please choose another one.");
    names.clear();
    names.addAll(settings.getOrPut("names", Seq.class, String.class, Seq::new));
  }

  @Override
  protected void saveImpl(JsonSettings settings) {
    settings.put("enabled", enabled);
    settings.put("message", message);
    settings.put("names", String.class, names);
  }

  public void registerServerCommands(ServerCommandHandler handler) {
    handler.add("reserved-names", "[on|off|add|remove|message] [name|text...]", "Reserved nicknames can only be used by admins.",
    args -> {
      if (args.length == 0) {
        logger.info("&fiNote: The case, colors and glyphs are ignored and removed during the nickname check.");
        logger.info("Kick message: @", message == null ? "&fi(empty)" : message);
        if (names.isEmpty())
          logger.info("Reserved Nicknames: [@, @]", enabled ? "&fb&lgenabled&fr" : "&fb&lrdisabled&fr", "empty");
        else {
          logger.info("Reserved Nicknames: [@, total: @]", enabled ? "&fb&lgenabled&fr" : "&fb&lrdisabled&fr", names.size);
          names.each(n -> logger.info("&lk|&fr @", n));
        }

      } else if (args[0].equals("add")) {
        if (args.length == 1) logger.err("Missing 'name' argument.");
        else if (add(args[1])) logger.info("Nickname added to the list.");
        else logger.info("Nickname already in the list.");

      } else if (args[0].equals("remove")) {
        if (args.length == 1) logger.err("Missing 'name' argument.");
        else if (remove(args[1])) logger.info("Nickname removed from the list.");
        else logger.info("Nickname not in the list.");

      } else if (args[0].equals("message")) {
        if (args.length == 1) logger.err("Missing 'text' argument.");
        else if (args[1].equals("\"\"")) {
          kickMessage(null);
          logger.info("Kick message removed.");
        } else {
          kickMessage(args[1]);
          logger.info("Kick message modified.");
        }

      } else if (Strings.isTrue(args[0])) {
        enable();
        logger.info("Reserved nicknames list enabled.");

      } else if (Strings.isFalse(args[0])) {
        disable();
        logger.info("Reserved nicknames list disabled.");

      } else logger.err("Invalid argument! Must be 'on', 'off', 'add', 'remove' or 'message'.");
    });
  }
}
