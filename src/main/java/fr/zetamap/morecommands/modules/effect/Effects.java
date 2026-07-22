/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2021-2025  ZetaMap
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

package fr.zetamap.morecommands.modules.effect;

import mindustry.entities.Effect;


/** Class that adds some properties to an effect */
public class Effects {
  public final EffectsModule module;
  public final Effect effect;
  public final String name;
  public final int id;
  public final boolean needsResizement;
  protected boolean disabled, adminOnly;

  public Effects(EffectsModule module, Effect effect, String name, boolean needsResizement) {
    this.module = module;
    this.effect = effect;
    this.name = name;
    this.id = effect.id;
    this.needsResizement = needsResizement;
  }

  public boolean disabled() { return disabled; }
  public void disabled(boolean disabled) {
    this.disabled = disabled;
    this.module.setModified0();
  }
  public boolean adminOnly() { return adminOnly; }
  public void adminOnly(boolean adminOnly) {
    this.adminOnly = adminOnly;
    this.module.setModified0();
  }
}