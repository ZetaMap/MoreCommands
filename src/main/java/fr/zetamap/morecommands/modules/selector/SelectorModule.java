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

package fr.zetamap.morecommands.modules.selector;

import java.util.regex.Pattern;

import arc.math.geom.Position;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.misc.Gatekeeper;
import fr.zetamap.morecommands.module.AbstractModule;
import fr.zetamap.morecommands.module.ModuleRegistry;
import fr.zetamap.morecommands.modules.selector.util.CoordinatesParser;
import fr.zetamap.morecommands.modules.selector.util.TargetResult;
import fr.zetamap.morecommands.util.Strings;


/**
 * Minecraft-like selectors. <br>
 * Nicknames must not start with {@code '@'} or {@code '~'} to avoid ambiguity with selector and coordinate prefixes.
 * <p>
 * To understand more about selectors and properties, please read: https://minecraft.wiki/w/Target_selectors
 */
public class SelectorModule extends AbstractModule {
  protected static final Pattern quotes = Pattern.compile("'(.*?)'");

  // Shortcuts
  public boolean enabled() {
    return ModuleRegistry.enabled(this);
  }
  public void enabled(boolean enable) {
    if (enable) ModuleRegistry.enable(this);
    else ModuleRegistry.disable(this);
  }

  public SelectorParser parse(PlayerData executor, String[] args) {
    return parse(executor, args, 0, args.length, false, false);
  }
  public SelectorParser parse(PlayerData executor, String[] args, boolean onlyPlayers) {
    return parse(executor, args, 0, args.length, onlyPlayers, false);
  }
  public SelectorParser parse(PlayerData executor, String[] args, boolean onlyPlayers, boolean onlyOne) {
    return parse(executor, args, 0, args.length, onlyPlayers, onlyOne);
  }
  public SelectorParser parse(PlayerData executor, String[] args, int from, int to) {
    return parse(executor, args, from, to, false, false);
  }
  public SelectorParser parse(PlayerData executor, String[] args, int from, int to, boolean onlyPlayers) {
    return parse(executor, args, from, to, onlyPlayers, false);
  }
  /**
   * Can parse {@code @selector[properties]} or {@code player username/unitID/UUID} syntaxes.
   * <p>
   * This method will notify the user if selectors are disabled. <br>
   * Exceptions are only thrown if {@code executor} is {@code null}, otherwise they will be sent to the player.
   */
  public SelectorParser parse(PlayerData executor, String[] args, int from, int to, boolean onlyPlayers,
                              boolean onlyOne) {
    if (Strings.checkStringArray(args, from, to)) {
      if (executor == null) throw new IllegalArgumentException("Missing player name/unitID/uuid or selector.");
      else executor.err("Missing player name/unitID/uuid or selector.");
      return null;
    } else if (Selectors.isSelector(args[from]) && !enabled()) {
      if (executor == null) throw new IllegalArgumentException("Selectors are disabled, you cannot use them.");
      else executor.err("Selectors are disabled, you cannot use them.");
      return null;
    }

    try { return new SelectorParser(executor, args, from, to, onlyPlayers, onlyOne); }
    catch (Exception e) { error(executor, e); }
    return null;
  }

  public CoordinatesParser parseCoord(PlayerData executor, String[] args) {
    return parseCoord(executor, args, 0, args.length);
  }
  /**
   * Can parse {@code [~]x[,[~]y]} (relative or absolute coordinates) or {@code player username/unitID/UUID} syntaxes.
   * <p>
   * Exceptions are only thrown if {@code executor} is {@code null}, otherwise they will be sent to the player.
   */
  public CoordinatesParser parseCoord(PlayerData executor, String[] args, int from, int to) {
    try { return new CoordinatesParser(executor, args, from, to); }
    catch (Exception e) { error(executor, e); }
    return null;
  }

  public TargetResult parseOne(PlayerData executor, String[] args) {
    return parseOne(executor, args, 0, args.length);
  }
  /** Can parse coordinates, player or selectors (but only one target). */
  public TargetResult parseOne(PlayerData executor, String[] args, int from, int to) {
    if (Strings.checkStringArray(args, from, to)) {
      if (executor == null)
        throw new IllegalArgumentException("Missing coordinates, player name/unitID/uuid or selector.");
      else executor.err("Missing coordinates, player name/unitID/uuid or selector.");
      return null;
    }

    if (Selectors.isSelector(args[from])) {
      SelectorParser selector = parse(executor, args, from, to, false, true);
      if (selector == null) return null;
      if (selector.noTargetFound()) {
        if (executor == null) throw new IllegalArgumentException("Nothing was selected.");
        else executor.err("Nothing was selected.");
        return null;
      }

      Position pos = selector.byPlayer ? selector.target.player : selector.selected.first();
      return new TargetResult(
        selector.executor,
        selector.target,
        selector.selected.first(),
        pos,
        CoordinatesParser.toWorld(pos),
        false,
        !selector.byPlayer,
        selector.byPlayer,
        selector.rest
      );

    } else {
      CoordinatesParser coord = parseCoord(executor, args, from, to);
      if (coord == null) return null;
      return new TargetResult(
        coord.executor,
        coord.target,
        null,
        coord.pos,
        coord.wpos,
        coord.byCoordinates,
        false,
        !coord.byCoordinates,
        coord.rest
      );
    }
  }

  public void error(PlayerData executor, Exception error) {
    String message = error.getMessage();
    if (message == null || message.isEmpty()) message = error.getClass().getSimpleName();
    else {
      if (executor != null && "Player not found".equals(message)) {
        executor.errPlayerNotFound();
        return;
      }
      if (message.charAt(message.length()-1) != '.') message += '.';
      message = quotes.matcher(message).replaceAll(executor == null ? "'&fr&lb$1&fr'" : "'[orange]$1[]'");
    }
    if (executor == null) throw new IllegalArgumentException(message);
    else executor.err(message);
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
