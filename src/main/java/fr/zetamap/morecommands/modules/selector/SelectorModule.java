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

package fr.zetamap.morecommands.modules.selector;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.misc.CoordinatesParser;
import fr.zetamap.morecommands.misc.Gatekeeper;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.module.ModuleFactory;


/** 
 * Minecraft-like selectors. <br>
 * Nicknames must not start with {@code '@'} or {@code '~'} to avoid ambiguity with selector and coordinate prefixes.
 * <p>
 * To understand more about selectors and properties, please read: https://minecraft.wiki/w/Target_selectors
 */
public class SelectorModule extends AbstractModule {
  public boolean enabled() {
    return ModuleFactory.enabled(this);
  }
  
  public void enable() {
    ModuleFactory.enable(this);
  }
  
  public void disable() {
    ModuleFactory.disable(this);
  }
  
  public SelectorParser parse(PlayerData executor, String[] args) { 
    return parse(executor, args, 0, args.length, false); 
  }
  public SelectorParser parse(PlayerData executor, String[] args, boolean onlyPlayers) { 
    return parse(executor, args, 0, args.length, onlyPlayers); 
  }
  public SelectorParser parse(PlayerData executor, String[] args, int from, int to) {
    return parse(executor, args, from, to, false);
  }
  /** This method will notify the user if selectors are disabled. */
  public SelectorParser parse(PlayerData executor, String[] args, int from, int to, boolean onlyPlayers) {
    if (args.length == 0 || from < 0 || to > args.length || from >= to || args[from].isEmpty()) {
      Players.err(executor, "Missing player name/uuid or selector.");
      return null;
    } else if (Selectors.isSelector(args[from]) && !enabled()) {
      Players.err(executor, "Selectors are disabled, you cannot use them.");
      return null;
    }
    return SelectorParser.parse(executor, args, from, to, onlyPlayers);
  }
  
  @Override
  protected void initImpl() {
    Selectors.init();
    SelectorProperties.init();
    
    Gatekeeper.add(internalName(), ctx -> 
      !ctx.strippedName.isEmpty() && (ctx.strippedName.charAt(0) == Selectors.prefix || 
                                      ctx.strippedName.charAt(0) == CoordinatesParser.worldRelativePrefix) ?
        Gatekeeper.reject("Your nickname cannot start with '[orange]" + ctx.strippedName.charAt(0) + "[]'.") :
      Gatekeeper.accept());
  }
}
