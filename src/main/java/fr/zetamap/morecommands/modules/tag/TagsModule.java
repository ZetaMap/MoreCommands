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

package fr.zetamap.morecommands.modules.tag;

import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.StringMap;

import mindustry.Vars;
import mindustry.net.Administration.PlayerInfo;

import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.*;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractSaveableModule;
import fr.zetamap.morecommands.util.JsonSettings;
import fr.zetamap.morecommands.util.Strings;


public class TagsModule extends AbstractSaveableModule {
  private boolean enabled = true;
  
  public final StringMap tags = new StringMap();
  
  public String get(String uuid) {
    return tags.get(uuid);
  }
  
  public String get(PlayerData player) {
    return get(player.player.uuid());
  }
  
  public void put(String uuid, String tag) {
    tags.put(uuid, tag);
    setModified();
  }
  
  public void put(PlayerData player, String tag) {
    put(player.player.uuid(), tag);
  }
  
  public void remove(String uuid) {
    tags.remove(uuid);
    setModified();
  }
  
  public void remove(PlayerData player) {
    remove(player.player.uuid());
  }
  
  public boolean has(String uuid) {
    return tags.containsKey(uuid);
  }
  
  public boolean has(PlayerData player) {
    return has(player.player.uuid());
  }
  
  public boolean enabled() {
    return enabled;
  }
  
  public void enable() {
    if (enabled) return;
    enabled = true;
    setModified();
    PlayerData.each(PlayerData::setTag);
  }
  
  public void disable() {
    if (!enabled) return;
    enabled = false;
    setModified();
    PlayerData.each(PlayerData::setTag);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void loadImpl(JsonSettings settings) {
    enabled = settings.getBool("enabled", true);
    tags.clear();
    tags.putAll(settings.getOrPut("tags", ObjectMap.class, String.class, String.class, ObjectMap::new));
  }

  @Override
  protected void saveImpl(JsonSettings settings) {
    settings.put("enabled", enabled);
    settings.put("tags", String.class, String.class, tags);
  }

  @Override
  public void registerServerCommands(ServerCommandHandler handler) {
    handler.add("tag", "[on|off|set|remove] [UUID] [tag...]", "Configure the tag system.", args -> {
      if (args.length == 0) {
        if (!tags.isEmpty()) {
          logger.info("Player tags: [@, total: @]", enabled ? "&fb&lgenabled&fr" : "&fb&lrdisabled&fr", tags.size);
          tags.each((p, t) -> {
            PlayerInfo info = Vars.netServer.admins.getInfoOptional(p);
            if (info == null) logger.info("&lk|&fr @ / <unknown>: @", p, t);
            else logger.info("&lk|&fr @ / '@': @", p, info.lastName, t);
          });
        } else logger.info("Player tags: [@, @]", enabled ? "&fb&lgenabled&fr" : "&fb&lrdisabled&fr", "empty");

      } else if (args[0].equals("set")) {
        if (args.length < 3) {
          logger.err(args.length == 1 ? "The 'UUID' and 'tag' arguments are missing!" : "The 'tag' argument is missing!");
          return;
        }
        
        if (Vars.netServer.admins.getInfoOptional(args[1]) == null)
          logger.warn("No player found with the UUID '@'.", args[1]);
        logger.info(tags.put(args[1], args[2]) == null ? "Tag added." : "Tag replaced.");
        setModified();
        
        PlayerData p = PlayerData.get(args[1]);
        if (p != null) {
          p.setTag();
          logger.info("Player online, the tag has been added to him.");
        }
        
      } else if (args[0].equals("remove")) {
        if (args.length == 1) {
          logger.err("The 'UUID' argument is missing!");
          return;
        } else if (tags.remove(args[1]) == null) {
          logger.err("No tag associated with this player UUID.");
          return;
        } else {
          logger.info("Tag removed.");
          setModified();
        }
        
        PlayerData p = PlayerData.get(args[1]);
        if (p != null) {
          p.setTag();
          logger.info("Player online, the tag has been removed from him.");
        }
        
      } else if (Strings.isTrue(args[0])) {
        enable();
        logger.info("Tags enabled.");
        
      } else if (Strings.isFalse(args[0])) {
        disable();
        logger.info("Tags disabled.");
        
      } else logger.err("Invalid argument! Must be 'on', 'off', 'set' or 'remove'");
    });
  }
  
  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    handler.addAdmin("tag", "[page|set|remove] [UUID] [tag...]", "Configure the tag system.", (args, player) -> {
      if (args.length > 0) {
        if (args[0].equals("set")) {
          if (args.length < 3) {
            Players.err(player, args.length == 1 ? "The '[orange]UUID[]' and '[orange]tag[]' arguments are missing!" : 
                                                   "The '[orange]tag[]' argument is missing!");
            return;
          }
          
          if (Vars.netServer.admins.getInfoOptional(args[1]) == null)
            Players.warn(player, "No player found with the UUID '@'.", args[1]);
          Players.ok(player, tags.put(args[1], args[2]) == null ? "Tag added." : "Tag replaced.");
          setModified();
          
          PlayerData p = PlayerData.get(args[1]);
          if (p != null) {
            p.setTag();
            Players.info(player, "Player online, the tag has been added to him.");
          }
          return;
          
        } else if (args[0].equals("remove")) {
          if (args.length == 1) {
            Players.err(player, "The 'UUID' argument is missing!");
            return;
          } else if (tags.remove(args[1]) == null) {
            Players.err(player, "No tag associated with this player UUID.");
            return;
          } else {
            Players.ok(player, "Tag removed.");
            setModified();
          }
          
          PlayerData p = PlayerData.get(args[1]);
          if (p != null) {
            p.setTag();
            p.setName();
            Players.info(player, "Player online, the tag has been removed from him.");
          }
          return;
        }
      }
      
      int page = 1, perPage = 12, pages = Mathf.ceil((float)tags.size / perPage);
      if (args.length > 0) page = Strings.parseInt(args[0]);
      
      if (page == Integer.MIN_VALUE) {
        Players.err(player, "Invalid argument! Must be '[orange]set[]' or '[orange]remove[]' or [orange]a page number[].");
        return;
      } else if (tags.isEmpty()) {
        Players.info(player, "Player tags: [[@, [gray]empty[]]", 
                     enabled ? "[green]enabled[]" : "[scarlet]disabled[]");
        return;
      } else if (page < 1 || page > pages) {
        Players.err(player, "'[orange]page[]' must be between [orange]1[] and [orange]@[].", pages);
        return;
      }
      
      Players.info(player, "Player tags: [[[lightgray]@[gray]/[]@[], @, total: [lightgray]@[]]", page, pages, 
                   enabled ? "[green]enabled[]" : "[scarlet]disabled[]", tags.size);
      StringBuilder builder = new StringBuilder();
      
      // Since this is an unordered map, the paging system will be by skipping the n first elements.
      int i = 0, n = perPage*(page-1);
      ObjectMap.Entries<String, String> it = tags.entries();
      while (i < n && it.hasNext()) {
        it.next();
        i++;
      }

      for (n=Math.min(perPage*page, tags.size); i<n; i++) {
        ObjectMap.Entry<String, String> e = it.next();
        PlayerInfo info = Vars.netServer.admins.getInfoOptional(e.key);
        builder.append("[gray]|[] [white]").append(e.key);
        if (info == null) builder.append(" [gray]/[] [accent]<unknown>[]: ");
        else builder.append(" [gray]/[] '[accent]").append(info.plainLastName()).append("[white]': ");
        builder.append(e.value).append('\n');
      }

      Players.info(player, builder.toString());
    });
  }
}
