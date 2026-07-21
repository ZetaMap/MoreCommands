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

import arc.util.serialization.Json.Serializer;

import fr.zetamap.morecommands.util.JsonSettings;


public abstract class AbstractSaveableModule extends AbstractModule implements SaveableModule {
  private boolean modified, loaded;
  /** Only used at first call of {@link #save()}, {@link #load()} or {@link #addSerializer()}. */
  protected boolean backuped = true;

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

  protected <T> void addSerializer(Class<T> clazz, Serializer<T> serializer) {
    ModuleRegistry.getSettings(this, backuped).addSerializer(clazz, serializer);
  }

  @Override
  public final void load() {
    if (!ModuleRegistry.enabled(this)) return;
    JsonSettings settings = ModuleRegistry.getSettings(this, backuped);
    boolean success = settings.load();
    loadImpl(settings);
    if (success) settings.backup();
    loaded = true;
    modified = false;
  }

  @Override
  public final void save() {
    if (!modified() || !loaded()) return;
    forceSave();
  }

  @Override
  public final void forceSave() {
    if (!loaded()) return;
    JsonSettings settings = ModuleRegistry.getSettings(this, backuped);
    saveImpl(settings);
    settings.save();
    modified = false;
  }

  protected abstract void loadImpl(JsonSettings settings);
  protected abstract void saveImpl(JsonSettings settings);
}