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

package fr.zetamap.morecommands.module;

import fr.zetamap.morecommands.util.Autosaver.Saveable;


public interface SaveableModule extends Module, Saveable {
  /** {@inheritDoc} */
  String name();

  /** @return whether the module's content has been modified. */
  boolean modified();

  /** Loads the settings. Can be called multiple times, e.g. to reload the configuration. */
  void load();

  /**
   * @return whether the module parameters have been loaded or not. <br>
   * This does not guarantee that the module has been initialized.
   */
  boolean loaded();

  /** Save the settings. */
  void save();

  /** Save all the settings now without checking for changes. */
  void forceSave();
}
