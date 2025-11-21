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

package fr.zetamap.morecommands.module;

import arc.util.serialization.Json;

import fr.zetamap.morecommands.util.JsonSettings;


public abstract class AbstractSaveableModule extends AbstractModule implements SaveableModule {
  private boolean modified, loaded;
 
  protected void setModified() {
    modified = true;
  }
  
  @Override
  public boolean modified() {
    return modified;
  }
  
  @Override
  public boolean loaded() {
    return loaded;
  }
  
  protected <T> void addSerializer(Class<T> clazz, Json.Serializer<T> serializer) {
    ModuleFactory.getSettings(this, true).getJson().setSerializer(clazz, serializer);
  }

  @Override
  public final void load() {
    if (!ModuleFactory.enabled(this)) return;
    boolean wasModified = modified; // Avoid to suppress modified status if sets while loading
    JsonSettings settings = ModuleFactory.getSettings(this, true);
    settings.load();
    loadImpl(settings);
    loaded = true;
    if (wasModified) modified = false;
  }

  @Override
  public final void save() {
    if (!modified() || !loaded) return;
    JsonSettings settings = ModuleFactory.getSettings(this, true);
    saveImpl(settings);
    settings.save();
    modified = false;
  }

  protected abstract void loadImpl(JsonSettings settings);
  protected abstract void saveImpl(JsonSettings settings);
}