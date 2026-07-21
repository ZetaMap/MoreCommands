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

package fr.zetamap.morecommands.modules.security;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;

import mindustry.Vars;
import mindustry.net.Administration;

import fr.zetamap.morecommands.module.AbstractSaveableModule;
import fr.zetamap.morecommands.module.ModuleRegistry;
import fr.zetamap.morecommands.util.JsonSettings;
import fr.zetamap.morecommands.util.Strings;


/** Modify some {@link Administration} methods to handle multiple admin usid. */
public class AdminUsidModule extends AbstractSaveableModule {
  /** Direct access to the modified {@link Administration}. {@code null} if not supported. */
  public WrappedAdministration admins;

  @Override
  protected void initImpl() {
    if (Vars.netServer.admins.getClass() != Administration.class) {
      logger.warn("Cannot override '@' because another mod/plugin replaced it.", "Vars.netServer.admins");
      ModuleRegistry.disableTemporary(this);
      logger.warn("The module has been disabled.");
      return;
    }

    Vars.netServer.admins.forceSave(); // to be sure not to lose data
    Vars.netServer.admins = admins = new WrappedAdministration(Vars.netServer.admins);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void loadImpl(JsonSettings settings) {
    if (admins == null) return;

    admins.usids.clear();
    for (String uuid : settings.keys())
      admins.usids.put(uuid, settings.getOrPut(uuid, ObjectSet.class, String.class, ObjectSet::new));

    // Add existing usids
    admins.getAdmins().each(admins::add);
  }

  @Override
  protected void saveImpl(JsonSettings settings) {
    if (admins == null) return;

    admins.usids.each((uuid, usids) -> settings.put(uuid, String.class, usids.toSeq()));
  }


  public class WrappedAdministration extends Administration {
    public final ObjectMap<String, ObjectSet<String>> usids = new ObjectMap<>();

    public WrappedAdministration(Administration old) {
      // Copy callbacks
      chatFilters.set(old.chatFilters);
      actionFilters.set(old.actionFilters);
    }

    public Seq<String> getUsids(String id) {
      ObjectSet<String> usids = get(id);
      return usids == null ? null : usids.toSeq();
    }

    protected void add(PlayerInfo info) {
      add(info.id, info.adminUsid);
    }

    protected boolean add(String id, String usid) {
      return usids.get(id, ObjectSet::new).add(usid);
    }

    protected boolean remove(String id, String usid) {
      ObjectSet<String> usids = get(id);
      return usids != null && usids.remove(usid);
    }

    protected boolean remove(String id) {
      return usids.remove(id) != null;
    }

    protected ObjectSet<String> get(String id) {
      return usids.get(id);
    }

    protected boolean contains(String id, String usid) {
      ObjectSet<String> usids = get(id);
      return usids != null && usids.contains(usid);
    }

    @Override
    public boolean adminPlayer(String id, String usid) {
      add(id, usid);
      setModified();
      return super.adminPlayer(id, usid);
    }

    @Override
    public boolean unAdminPlayer(String id) {
      remove(id);
      setModified();
      return super.unAdminPlayer(id);
    }

    @Override
    public boolean isAdmin(String id, String usid) {
      return super.isAdmin(id, usid) || contains(id, usid);
    }

    /**
     * Better {@link Administration#searchNames(String)} which also ignores glyphs. <br>
     * Used by the {@link ModerationModule}.
     */
    @Override
    public ObjectSet<PlayerInfo> searchNames(String name) {
      ObjectSet<PlayerInfo> result = new ObjectSet<>();
      final String name0 = name.toLowerCase();

      for (PlayerInfo info : playerInfo.values()) {
        if(info.names.contains(n -> Strings.normalize(n).equals(name0)))
          result.add(info);
      }

      return result;
    }
  }
}
