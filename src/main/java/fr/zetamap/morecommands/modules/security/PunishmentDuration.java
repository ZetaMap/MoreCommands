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

import fr.zetamap.morecommands.util.DurationFormatter.DurationUnit;


/** Common punishments duration. */
public enum PunishmentDuration {
  thirtyMinutes(DurationUnit.minute, 30),
  oneHour(DurationUnit.hour),
  threeHours(DurationUnit.hour, 3),
  oneDay(DurationUnit.day),
  threeDays(DurationUnit.day, 3),
  oneWeek(DurationUnit.week),
  oneMonth(DurationUnit.month),
  threeMonths(DurationUnit.month, 3 + 1f/30f), //plus one day
  oneYear(DurationUnit.year.duration),
  permanant(-1);
  
  public static final PunishmentDuration[] all = values();
  
  public final DurationUnit unit;
  public final float multiplier;
  /** Total duration, after applied the {@link #multiplier}. */
  public final long duration;
  
  PunishmentDuration(long duration) {
    this.unit = null;
    this.multiplier = 1;
    this.duration = duration;
  }
  
  PunishmentDuration(DurationUnit unit) { this(unit, 1); }
  PunishmentDuration(DurationUnit unit, float multiplier) {
    this.unit = unit;
    this.multiplier = multiplier;
    this.duration = (long)(unit.duration * multiplier);
  }
  
  public static PunishmentDuration of(long duration) {
    for (PunishmentDuration u : all) {
      if (u.duration == duration) return u;
    }
    return null;
  }
}
