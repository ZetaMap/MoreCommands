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

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;

import mindustry.Vars;
import mindustry.net.Administration;

import fr.zetamap.morecommands.module.AbstractSaveableModule;
import fr.zetamap.morecommands.util.JsonSettings;


/** Modify some {@link Administration} methods to handle multiple usid. */
public class AdminUsidModule extends AbstractSaveableModule {
  /** Direct access to the modified {@link Administration}. */
  public WrappedAdministration admins;
  
  @Override
  protected void initImpl() {
    Vars.netServer.admins.forceSave(); // in case of
    Vars.netServer.admins = admins = new WrappedAdministration(Vars.netServer.admins);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void loadImpl(JsonSettings settings) {
    if (admins == null) return;

    admins.usids.clear();
    for (String uuid : settings.keys())
      admins.usids.put(uuid, settings.getOrPut(uuid, ObjectSet.class, String.class, ObjectSet::new));
    
    // Add missing admin usids
    admins.getAdmins().each(p -> admins.usids.get(p.id, ObjectSet::new).add(p.adminUsid));
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
      return usids.get(id).toSeq();
    }
    
    @Override
    public boolean adminPlayer(String id, String usid) {
      usids.get(id, ObjectSet::new).add(usid);
      setModified();
      return super.adminPlayer(id, usid);
    }
    
    @Override
    public boolean unAdminPlayer(String id) {
      usids.remove(id);
      setModified();
      return super.unAdminPlayer(id);
    }
    
    @Override
    public boolean isAdmin(String id, String usid) {
      ObjectSet<String> u;
      return super.isAdmin(id, usid) || (u = usids.get(id)) != null && u.contains(usid);
    }
  }
}
