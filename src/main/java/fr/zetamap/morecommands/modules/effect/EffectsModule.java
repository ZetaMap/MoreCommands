/**
 * This file is part of MoreCommands. The plugin that adds a bunch of commands to your server.
 * Copyright (c) 2021-2026  ZetaMap
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

import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Structs;
import arc.util.Timer;
import arc.util.Tmp;

import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Call;
import mindustry.graphics.Pal;

import fr.zetamap.morecommands.Modules;
import fr.zetamap.morecommands.PlayerData;
import fr.zetamap.morecommands.command.*;
import fr.zetamap.morecommands.misc.Players;
import fr.zetamap.morecommands.module.AbstractSaveableModule;
import fr.zetamap.morecommands.modules.selector.SelectorParser;
import fr.zetamap.morecommands.util.JsonSettings;
import fr.zetamap.morecommands.util.Logger;
import fr.zetamap.morecommands.util.Strings;


public class EffectsModule extends AbstractSaveableModule {
  /**
   * <s>Effects that can cause a client-side crash if applied.</s> <br>
   * These effects does nothing now, but it's better to keep them disabled.
   */
  public static final String[] defaultDisabled = {
    "none", "blockCrash", "trailFade", "unitSpawn", "unitControl", "unitDespawn", "unitSpirit", "itemTransfer",
    "pointBeam", "lightning", "coreBuildBlock", "upgradeCore", "payloadDeposit", "fallSmoke", "unitWreck",
    "rocketSmoke", "rocketSmokeLarge", "unitAssemble", "dropItem", "regenSuppressSeek", "healBlockFull",
    "arcShieldBreak", "unitShieldBreak", "chainLightning", "chainEmp", "legDestroy", "debugLine", "debugRect",
  };

  /** Annoying effects reserved for admins. */
  public static final String[] defaultAdmin = {
    "unitCapKill", "unitEnvKill", "padLaunch", "titanExplosion", "titanExplosionLarge", "titanExplosionSmall",
    "titanExplosionFrag", "titanSmoke", "titanSmokeLarge", "titanSmokeSmall", "smokeAoeCloud", "missileTrailSmoke",
    "neoplasmSplat", "scatheExplosion", "scatheExplosionSmall", "scatheLight", "scatheLightSmall", "scatheSlash",
    "greenBomb", "greenLaserCharge", "healWave", "instBomb", "instShoot", "instHit", "fireRemove", "bigSockwave",
    "reactorExplosion", "impactReactorExplosion", "shootSmokeMissile", "shootSmokeMissileColor", "railShoot",
    "coreLaunchConstruct", "mineImpactWave", "launchAccelerator", "launch", "launchPod", "spawnShockwave",
    "dynamicExplosion", "overdriveBlockFull", "shieldWave", "padlaunch"
  };

  /** Effects that uses the rotation as a size. These will be capped to {@link #effectSizeCap}. */
  public static final String[] forcedResizement = {
    "coreBuildShockwave", "pointShockwave", "commandSend", "upgradeCoreBloom", "placeBlock", "coreLaunchConstruct",
    "tapBlock", "breakBlock", "breakProp", "unitLandSmall", "sparkExplosion", "dynamicSpikes", "healWaveDynamic",
    "dynamicWave", "artilleryTrail", "incendTrail", "missileTrail", "missileTrailShort", "colorTrail", "forceShrink",
    "spawnShockwave", "dynamicExplosion", "dooropen", "doorclose", "mineImpactWave", "ripple", "bubble",
    "healWaveMend", "overdriveWave", "healBlock", "rotateBlock", "lightBlock", "overdriveBlockFull", "shieldBreak",
  };

  private final Logger loaderLogger = new Logger(name() + " Loader");
  private Timer.Task effectsTask;

  /** Effect id is the key index. */
  public final OrderedMap<String, Effects> all = new OrderedMap<>();
  /** Effect 2 tiles behind the player. */
  public int effectOffset = 2 * Vars.tilesize;
  /** Size to use for {@link #forcedResizement} effects. */
  public float effectSizeCap = 2f;

  public Effects get(int id) {
    return id < 0 || id >= all.size ? null : get(all.orderedKeys().get(id));
  }

  public Effects get(Effect effect) {
    return get(all.orderedKeys().get(effect.id));
  }

  public Effects get(String name) {
    return all.get(name);
  }

  public boolean isEnabled(String name) {
    Effects e = get(name);
    return e != null && !e.disabled();
  }

  public boolean isDisabled(String name) {
    Effects e = get(name);
    return e != null && e.disabled();
  }

  public boolean isAdminOnly(String name) {
    Effects e = get(name);
    return e != null && e.adminOnly();
  }

  public Effects random(boolean withDisabled, boolean withAdmins) {
    Effects found;
    do { found = all.get(all.orderedKeys().random()); }
    while ((found.disabled && !withDisabled) || (found.adminOnly && !withAdmins));
    return found;
  }

  public Seq<Effects> copy(boolean withDisabled, boolean withAdmins) {
    if (withDisabled && withAdmins) return all.values().toSeq();
    Seq<Effects> result = new Seq<>(all.size/2);
    all.each((n, e) -> {
      if ((e.disabled && !withDisabled) || (e.adminOnly && !withAdmins)) return;
      result.add(e);
    });
    return result;
  }

  /** Some effects must be disabled to avoid a client-side crash or a visual flood to other players. */
  public void reset() {
    all.each((n, e) -> e.disabled = e.adminOnly = false);
    for (String name : defaultDisabled) {
      Effects ef = get(name);
      if (ef != null) ef.disabled = true;
    }
    for (String name : defaultAdmin) {
      Effects ef = get(name);
      if (ef != null) ef.adminOnly = true;
    }
    setModified();
  }

  /** Sort the registered effects by id. */
  public void sort() {
    all.orderedKeys().sort(n -> get(n).id);
  }

  /** Load particle effects from {@link Fx} using reflection (because the effect name is the field name). */
  public void loadFx() {
    all.clear();
    try {
      for (java.lang.reflect.Field f : Fx.class.getDeclaredFields()) {
        if (f.getType() != Effect.class) continue;
        String name = f.getName();
        try { all.put(name, new Effects(this, (Effect)f.get(null), name, Structs.contains(forcedResizement, name))); }
        catch (Throwable t) { loaderLogger.err("Failed to get Fx."+name+": @", t.toString()); }
      }
    } catch (Exception e) { loaderLogger.err("Failed to load particle effects", e); }

    // In case of
    sort();
  }

  void setModified0() {
    setModified();
  }

  @Override
  protected void initImpl() {
    loadFx();

    // Effects loop every 100ms
    effectsTask = Timer.schedule(() -> PlayerData.each(p -> {
      if (p.vanished() && !p.player.name.isEmpty()) {
        p.setName();
        return;
      }

      if (p.rainbowed) {
        p.updateRainbow();
        p.setName();

        // Beautiful rainbow trail =)
        if (p.player.dead()) return;
        float rotationBack = p.player.unit().rotation + 180,
              x = p.player.x + Angles.trnsx(rotationBack, effectOffset),
              y = p.player.y + Angles.trnsy(rotationBack, effectOffset);
        Tmp.c4.fromHsv(p.rainbowHue, 1f, 1f);
        Tmp.c4.a = 1f;
        for (int i=0; i<5; i++) Call.effect/*Reliable*/(Fx.bubble, x, y, 10, Tmp.c4);
      }

      if (p.effect != null && !p.player.dead()) {
        float rotationBack = p.player.unit().rotation + 180,
              x = p.player.x + Angles.trnsx(rotationBack, effectOffset),
              y = p.player.y + Angles.trnsy(rotationBack, effectOffset),
              size = p.effect.needsResizement ? effectSizeCap : rotationBack;
        Call.effect/*Reliable*/(p.effect.effect, x, y, size, Pal.accent);
      }
    }), 0.1f, 0.1f); //0.064f
  }

  @Override
  protected void disposeImpl() {
    if (effectsTask == null) return;
    effectsTask.cancel();
    effectsTask = null;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void loadImpl(JsonSettings settings) {
    settings.getOrPut("disabled", Seq.class, String.class, Seq::new).each(n -> {
      Effects e = get((String)n);
      if (e == null) loaderLogger.err("Unknown effect '@'", n);
      else e.disabled = true;
    });
    settings.getOrPut("admin-only", Seq.class, String.class, Seq::new).each(n -> {
      Effects e = get((String)n);
      if (e == null) loaderLogger.err("Unknown effect '@'", n);
      else e.adminOnly = true;
    });

    if (!settings.exists() && !settings.backupExists()) {
      loaderLogger.warn("Setting default enabled and admin only effects.");
      reset();
    }
  }

  @Override
  protected void saveImpl(JsonSettings settings) {
    settings.put("disabled", String.class, all.orderedKeys().select(this::isDisabled));
    settings.put("admin-only", String.class, all.orderedKeys().select(this::isAdminOnly));
  }

  @Override
  public void registerServerCommands(ServerCommandHandler handler) {
    handler.add("effect", "[reset|id|name] [on|off|admins|everyone]", "Manage the particles effects.",
    args -> {
      if (args.length == 0) {
        int length = Strings.max(all.entries(), e -> e.key.length() + Mathf.digits(e.value.id) + 14);
        logger.info("Effects list: [total: @, enabled: @, admins: @]", all.size,
                    all.orderedKeys().count(this::isEnabled),
                    all.orderedKeys().count(this::isAdminOnly));
        all.each((n, e) -> logger.info("&lk|&fr " + Strings.lJust(n + " (&fi&lb" + e.id + "&fr): ", length) +
                                       (e.disabled() ? "&fb&lrdisabled&fr, " : "&fb&lbenabled&fr,  ") +
                                       (e.adminOnly() ? "&fb&lyadmin-only&fr" : "&fb&lgeveryone&fr")));
        return;

      } else if (args[0].equals("reset")) {
        reset();
        logger.info("Effects set to default values");
        return;
      }

      Effects effect = get(args[0]);
      if (effect == null) {
        int id = Strings.parseInt(args[0]);
        effect = get(id);

        if (effect == null) {
          logger.err(id == Integer.MIN_VALUE ? "No effect named '@' found." : "No effect with id '@' found.", args[0]);
          return;
        }
      }

      if (args.length == 1) {
        logger.info("Effect @ (@) is currently @ and usable by @.", effect.name, effect.id,
                    effect.disabled() ? "disabled" : "enabled", effect.adminOnly() ? "administrators only" : "everyone");

      } else if (args[2].equals("admins")) {
        effect.adminOnly(true);
        logger.info("Effect @ (@) now usable only by administrators.");

      } else if (args[2].equals("everyone")) {
        effect.adminOnly(false);
        logger.info("Effect @ (@) now usable by everyone.");

      } else if (Strings.isTrue(args[2])) {
        effect.disabled(false);
        logger.info("Effect @ (@) enabled.", effect.name, effect.id);

      } else if (Strings.isFalse(args[2])) {
        effect.disabled(true);
        logger.info("Effect @ (@) disabled.", effect.name, effect.id);

      } else logger.err("Invalid argument! Must be 'on', 'off', 'admins' or 'everyone'.");
    });
  }

  @Override
  public void registerClientCommands(ClientCommandHandler handler) {
    handler.add("rainbow", "[on|off] [selector|player...]",
                "[#ff0000]R[#ff7f00]A[#ffff00]I[#00ff00]N[#0000ff]B[#2e2b5f]O[#8B00ff]W[#ff0000]![#ff7f00]!",
    (args, player) -> {
      if (args.length == 0) {
        Players.info(player, "Rainbow mode is currently [accent]@[].", player.rainbowed ? "enabled" : "disabled");
        return;
      }

      boolean enable, force = false;
      if (args[0].equals("me")) enable = force = true; // secret argument to force rainbow bubbles + particles
      else if (Strings.isTrue(args[0])) enable = true;
      else if (Strings.isFalse(args[0])) enable = false;
      else {
        Players.err(player, "Invalid argument! Must be 'on' or 'off'.");
        return;
      }

      if (args.length == 1) {
        if (player.vanished()) {
          Players.err(player.player, "Can't start rainbow mode in vanish mode!");
          return;
        }
        player.setRainbow(enable);
        if (!force) player.effect = null;
        Players.ok(player, "Rainbow effect [accent]@[].", player.rainbowed ? "enabled" : "disabled");
        return;

      } else if (!player.admin()) {
        Players.errArgUseDenied(player);
        return;
      }

      SelectorParser selector = Modules.selector.parse(player, args, 1, args.length, true);
      if (selector == null) return;
      selector.execute((p, u) -> {
        if (enable && p.vanished()) {
          Players.warn(player, "Can't start rainbow mode for @[scarlet] because he's in vanish mode!", p.getName());
          return;
        }
        p.setRainbow(enable);
        if (p == player) return;
        Players.ok(p, "Rainbow effect [accent]@[] by @[green].", p.rainbowed ? "enabled" : "disabled", player.getName());
      });
      if (selector.noTargetFound()) Players.ok(player, "No players was selected.");
      else Players.ok(player, "[accent]@[] rainbow effect @[green].", enable ? "Enabled" : "Disabled",
                      selector.formatMessage("for", true));
    });

    handler.add("effect", "[stop|list|search|name|id] [page|selector|player...]", "Gives you a particle effect.",
    (args, player) -> {
      boolean stop = args.length > 0 && args[0].equals("stop");

      if (args.length == 0 || (stop && args.length == 1)) {
        if (player.vanished()) {
          Players.err(player, "Can't start an effect in vanish mode!");
          return;
        } else if (stop && player.effect == null) {
          Players.err(player, "No started particle efffect.");
          return;
        }

        player.effect = stop || player.effect != null ? null : random(false, player.admin());
        if (player.effect == null) Players.ok(player, "Removed particle effect.");
        else {
          player.setRainbow(false); // Particles + rainbow bubbles are a little bit weird
          Players.ok(player, "Randomized particle effect to [accent]@[] ([accent]@[]).", player.effect.name, player.effect.id);
        }
        return;

      } else if (args[0].equals("list")) {
        int page = 1, perPage = 12, pages;
        Seq<Effects> everyoneEffects = null;
        if (args.length > 1) page = Strings.parseInt(args[1]);

        if (page == Integer.MIN_VALUE) {
          Players.err(player, "'[orange]page[]' must be a number.");
          return;
        }

        if (player.admin()) pages = Mathf.ceil((float)all.size / perPage);
        else {
          everyoneEffects = copy(false, false);
          pages = Mathf.ceil(everyoneEffects.size / perPage);
        }

        if (page < 1 || page > pages) {
          Players.err(player, "'[orange]page[]' must be between [orange]1[] and [orange]@[].", pages);
          return;
        }

        Players.info(player, "[orange]---- [gold]Effect list [lightgray]@[gray]/[]@ [gray]([]@[gray])[][][] ----",
                     page, pages, everyoneEffects == null ? all.size : everyoneEffects.size);
        StringBuilder builder = new StringBuilder();

        if (everyoneEffects == null) {
          for (int i=perPage*(page-1), n=Math.min(perPage*page, all.size); i<n; i++) {
            Effects e = get(i);
            builder.append("[lightgray]|[] ").append(e.name).append(" [gray]([lightgray]").append(e.id)
                   .append("[])[] [orange]/[] ").append(e.disabled() ? "[scarlet]disabled[], " : "[green]enabled[], ")
                   .append(e.adminOnly() ? "[scarlet]admin" : "[green]everyone").append("[][]\n");
          }

        } else {
          for (int i=perPage*(page-1), n=Math.min(perPage*page, everyoneEffects.size); i<n; i++) {
            Effects e = everyoneEffects.get(i);
            builder.append("[lightgray]|[] ").append(e.name).append(" [gray]([lightgray]").append(e.id).append("[])[]\n");
          }
        }

        Players.info(player, builder.toString());
        return;

      } else if (args[0].equals("search")) {
        if (args.length == 1) {
          Players.err(player, "Missing '[orange]name[]' argument.");
          return;
        }

        String name = args[1].toLowerCase();
        Seq<String> found = all.orderedKeys().select(n -> n.toLowerCase().contains(name));
        if (!player.admin()) found.remove(n -> get(n).adminOnly() || get(n).disabled());
        int perPage = 15, n = 0;
        StringBuilder builder = new StringBuilder();

        if (found.isEmpty()) {
          Players.info(player, "No match found.");
          return;
        }
        Players.info(player, "Found [accent]@[] matchs:", found.size);

        if (player.admin()) {
          for (int i=0; i<found.size; i++, n++) {
            Effects e = get(found.get(i));
            builder.append("[lightgray]|[] ").append(e.name).append(" [gray]([lightgray]").append(e.id)
                   .append("[])[] [orange]/[] ").append(e.disabled() ? "[scarlet]disabled[], " : "[green]enabled[], ")
                   .append(e.adminOnly() ? "[scarlet]admin" : "[green]everyone").append("[][]");
            if (n >= perPage) {
              Players.info(player, builder.toString());
              builder.setLength(0);
              n = 0;
            } else builder.append('\n');
          }

        } else {
          for (int i=0; i<found.size; i++, n++) {
            Effects e = get(found.get(i));
            builder.append("[lightgray]|[] ").append(e.name).append(" [gray]([lightgray]").append(e.id).append("[])[]");
            if (n >= perPage) {
              Players.info(player, builder.toString());
              builder.setLength(0);
              n = 0;
            } else builder.append('\n');
          }
        }
        if (n > 0) Players.info(player, builder.toString());
        return;
      }

      Effects effect;
      if (!stop) {
        Effects e = get(Strings.kebabToCamel(args[0]));
        if (e == null) {
          int id = Strings.parseInt(args[0]);
          e = get(id);

          if (e == null) {
            Players.err(player, id == Integer.MIN_VALUE ? "No effect named '[orange]@[]' found." :
                                                          "No effect with id '[orange]@[]' found.", args[0]);
            return;
          }
        }
        effect = e;

        if (effect.disabled()) {
          Players.err(player, "This particle effect is disabled.");
          return;
        } else if (effect.adminOnly() && !player.admin()) {
          Players.err(player, "This particle effect is only for admins.");
          return;

        } else if (args.length == 1) {
          if (player.vanished()) {
            Players.err(player, "Can't start an effect in vanish mode!");
            return;
          }

          player.setRainbow(false); // Effect + rainbow is a little bit weird
          player.effect = effect;
          Players.ok(player, "Starting particle effect [accent]@[] ([accent]@[]).", effect.name, effect.id);
          return;
        }
      } else effect = null;

      if (!player.admin()) {
        Players.errArgUseDenied(player);
        return;
      }

      SelectorParser selector = Modules.selector.parse(player, args, 1, args.length, true);
      if (selector == null) return;
      selector.execute((p, u) -> {
        if (!stop && p.vanished()) {
          Players.err(player, "Can't start an effect for @[scarlet] because he's in vanish mode!", p.getName());
          return;
        } else if (stop && p.effect == null) return;

        p.effect = stop/* || p.effect != null*/ ? null : effect;
        if (p.effect != null) p.rainbowed = false; // Effect + rainbow is a little bit weird
        if (p == player) return;

        if (p.effect == null) Players.ok(p, "Particle effect removed by @[green].", player.getName());
        else Players.ok(p, "Particle effect [accent]@[] ([accent]@[]) started by @[green].", p.effect.name,
                        p.effect.id, player.getName());
      });
      if (selector.noTargetFound()) Players.ok(player, "No player was selected.");
      else if (effect == null) Players.ok(player, "Removed particle effect @[green].",
                                          selector.formatMessage("from", true));
      else Players.ok(player, "Starting particle effect [accent]@[] ([accent]@[]) @[green].", effect.name, effect.id,
                      selector.formatMessage("for", true));
    });

    //IDEA: /sound
  }
}
