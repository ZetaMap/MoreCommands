/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024-2025 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fr.zetamap.morecommands.util;

import arc.util.Http;

import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods;


public class VersionChecker {
  private static final Logger logger = new Logger("Updater");
  
  public static String keyToFind = "tag_name";
  public static String repoLinkFormat = "https://github.com/@/releases/latest";
  public static String repoApiLinkFormat = mindustry.Vars.ghApi + "/repos/@/releases/latest";
  
  public static <T extends Mod> UpdateState checkFor(T mod) { return checkFor(mod, true); }
  
  public static <T extends Mod> UpdateState checkFor(T mod, boolean promptStatus) {
    Mods.LoadedMod load = Vars.mods.getMod(mod.getClass());
    if(load == null) throw new IllegalArgumentException("Mod is not loaded yet (or missing)!");
    return checkFor(load.meta);
  }
  
  public static UpdateState checkFor(Mods.ModMeta mod) { return checkFor(mod, true); }
  /** 
   * Check for update using the "version" and "repo" properties 
   * in the mod/plugin definition (&ltplugin/mod&gt.[h]json).
   * <p>
   * The github repo must be formatted like that "{@code <username>/<repo-name>}".<br>
   * The version must be formatted like that "{@code 146.2}" and can starts with "{@code v}", 
   * but must not contains letters, like "{@code beta}" or "{@code -dev}".
   * 
   * @return the update state of the mod
   */
  public static UpdateState checkFor(Mods.ModMeta mod, boolean promptStatus) {
    if (promptStatus) logger.info("Checking for updates...");
    
    if (mod.repo == null || mod.repo.isEmpty() || mod.repo.indexOf('/') == -1) {
      if (promptStatus) logger.warn("No repo found for an update.");
      return UpdateState.missing;
    } else if (mod.version == null || mod.version.isEmpty()) {
      if (promptStatus) logger.warn("No current version found for an update.");
      return UpdateState.missing;
    }
    
    UpdateState[] status = {UpdateState.error};
    Http.get(Strings.format(repoApiLinkFormat, mod.repo))
    .timeout(5000)
    .error(failure -> {
      if (promptStatus) logger.err("Unable to check for updates: @", failure.getLocalizedMessage());
    }).block(success -> {
      String content = success.getResultAsString();
      if (content.isBlank()) {
        if (promptStatus) logger.err("Unable to check for updates: no content received.");
        return;
      }
      
      // Extract the version
      String tagName;
      try { tagName = new arc.util.serialization.JsonReader().parse(content).getString(keyToFind); } 
      catch (Exception e) {
        if (promptStatus) {
          logger.err("Unable to check for updates: invalid Json or missing key 'tag_name'.");
          logger.err("Error: @", e.getLocalizedMessage());
        }
        return;
      }  
      
      // Compare the version
      if (promptStatus) logger.info("Found version: @. Current version: @", tagName, mod.version);
      if (Strings.isVersionAtLeast(mod.version, tagName)) {
        if (promptStatus) logger.info("Check out this link to upgrade @: @", mod.displayName, 
                                      Strings.format(repoLinkFormat, mod.repo));
        status[0] = UpdateState.outdated;
      } else {
        if (promptStatus) logger.info("Already up-to-date, no need to update.");
        status[0] = UpdateState.uptodate;
      }  
    });
    
    return status[0];
  }
  
  
  public static enum UpdateState {
    /** "version" or/and "repo" properties are missing in the mod/plugin definition. */
    missing,
    /** Error while checking for updates. */
    error, 
    /** No new updates found, it's the latest version. */
    uptodate,
    /** An update was found, the mod/plugin needs to be upgraded. */
    outdated
  }
}
