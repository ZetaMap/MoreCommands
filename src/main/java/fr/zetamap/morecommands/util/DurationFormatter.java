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

package fr.zetamap.morecommands.util;

import arc.struct.EnumSet;
import arc.struct.ObjectMap;


/** Utility to format duration. */
public class DurationFormatter {
  /** Format with the compact mode and classic units. */
  public static String formatCompact(long millis) {
    return format(millis, DurationFormatMode.compact, DurationUnit.classic, Integer.MAX_VALUE);
  }

  /** Format with the narrow mode and classic units. */
  public static String formatNarrow(long millis) {
    return format(millis, DurationFormatMode.narrow, DurationUnit.classic, Integer.MAX_VALUE);
  }

  /** Format with the natural mode and the classic units. */
  public static String format(long millis) {
    return format(millis, DurationFormatMode.natural, DurationUnit.classic);
  }

  public static String format(long millis, int depth) {
    return format(millis, DurationFormatMode.natural, DurationUnit.classic, depth);
  }

  /** Format with the natural mode. */
  public static String format(long millis, EnumSet<DurationUnit> units) {
    return format(millis, DurationFormatMode.natural, units);
  }

  public static String format(long millis, EnumSet<DurationUnit> units, int depth) {
    return format(millis, DurationFormatMode.natural, units, depth);
  }

  /** Format with the classic units. */
  public static String format(long millis, DurationFormatMode mode) {
    return format(millis, mode, DurationUnit.classic);
  }

  public static String format(long millis, DurationFormatMode mode, int depth) {
    return format(millis, mode, DurationUnit.classic, depth);
  }

  public static String format(long millis, DurationFormatMode mode, EnumSet<DurationUnit> units) {
    return format(millis, mode, units, 2/*3*/);
  }

  /**
   * Convert {@code millis} duration to a human readable format.
   * (e.g. {@code 123456 } -> {@code "2 minutes and 3 seconds"})
   *
   * @param millis milliseconds duration to convert.
   * @param mode the formatting mode to use.
   * @param units the unit list to use. Must be sorted by ordinal value, else the result will be weird.
   * @param depth maximum units to format, {@code Integer.MAX_VALUE} can be used for no limit.
   *
   * @see DurationFormatMode
   * @see DurationUnit
   */
  public static String format(long millis, DurationFormatMode mode, EnumSet<DurationUnit> units, int depth) {
    if (millis <= 0 || units.size == 0 || depth <= 0) return "";
    Integer[] parts = new Integer[units.size];
    int end = 0;
    StringBuilder builder = new StringBuilder();
    boolean first = true;

    for (int i=0; i<parts.length && depth>0; i++) {
      if (units.array[i].longer(millis)) {
        parts[end = i] = (int)units.array[i].get(millis);
        millis = units.array[i].rest(millis);
        depth--;
      }
    }

    for (int i=0; i<=end; i++) {
      if (parts[i] == null) continue;
      if (!first) mode.joiner.join(builder, i == end);
      mode.formatter.format(builder, parts[i], units.array[i]);
      first = false;
    }

    return builder.toString();
  }

  public static long parse(String src) throws IllegalArgumentException {
    return parse(src, 0, src.length());
  }

  /**
   * Parse a human-readable duration into milliseconds.
   * @throws IllegalArgumentException if the string cannot be parsed.
   */
  public static long parse(String src, int from, int to) throws IllegalArgumentException {
    if (src == null) return 0L;
    from = Math.max(from, 0);
    to = Math.min(to, src.length());
    if (to - from <= 0) return 0L;

    long result = 0L, add;
    int mark, count;
    char ch;
    String name;
    DurationUnit unit;

    while (from < to) {
      ch = src.charAt(from);
      mark = from;

      // Skip spaces, commas, semicolons
      if (Character.isWhitespace(ch) || ch == ',' || ch == ';') {
        from++;
        continue;
      }

      // Skip joiner word "and"
      if (Character.isLetter(ch)) {
        while (from < to && Character.isLetter(src.charAt(from))) from++;
        if ("and".regionMatches(true, 0, src, mark, from - mark)) continue;
        ch = src.charAt(from);
      }

      // Parse number
      if (from >= to || !Character.isDigit(ch))
        throw new IllegalArgumentException("Expected a number at " + from + " of '" + src + "'");
      while (from < to && Character.isDigit(src.charAt(from))) from++;
      count = Strings.parseInt(src, 10, Integer.MIN_VALUE, mark, from);
      if (count < 0) throw new IllegalArgumentException("Invalid unit count at " + from + " of '" + src + "'");

      // Skip whitespace between number and unit
      while (from < to && Character.isWhitespace(src.charAt(from))) from++;

      // Read unit
      mark = from;
      while (from < to && Character.isLetter(src.charAt(from))) from++;
      name = mark < from ? src.substring(mark, from).toLowerCase() : null;
      if (name == null || name.isEmpty())
        throw new IllegalArgumentException("Excepted a duration unit at " + mark + " of '" + src + "'");
      unit = DurationUnit.get(name);
      if (unit == null)
        throw new IllegalArgumentException("Unknown duration unit '" + name + "' at " + mark + " of '" + src + "'");

      // Add result
      if (count > 0 && result < Long.MAX_VALUE) {
        // Saturate on overflow
        if (unit.duration != 0 && count > Long.MAX_VALUE / unit.duration) {
          result = Long.MAX_VALUE;
          continue; // Continue parsing even it's overflow
        }
        add = count * unit.duration;
        if (Long.MAX_VALUE - result < add) result = Long.MAX_VALUE;
        else result += add;
      }
    }

    return Math.max(0L, result); // In case of
  }

  public enum DurationFormatMode {
    /** Natural duration formatting in a readable human sentence. E.g. {@code "1 hour, 2 minutes and 3 seconds"}. */
    natural((b, l) -> b.append(l ? " and " : ", "), (b, c, u) -> b.append(c).append(' ').append(c > 1 ? u.plurial : u.singular)),
    /** Normal duration formatting. E.g. {@code "1 hour 2 minutes 3 seconds"}. */
    normal((b, l) -> b.append(' '), (b, c, u) -> b.append(c).append(' ').append(c > 1 ? u.plurial : u.singular)),
    /** Shorthand duration formatting in a readable human sentence. E.g. {@code "1h, 2min and 3s"}. */
    narrowNatural((b, l) -> b.append(l ? " and " : ", "), (b, c, u) -> b.append(c).append(u.narrow)),
    /** Classic shorthand duration formatting. E.g. {@code "1h 2min 3s"}. */
    narrow((b, l) -> b.append(' '), (b, c, u) -> b.append(c).append(u.narrow)),
    /** Compact shorthand formatting. E.g. {@code "1h2min3s"}. */
    compact((b, l) -> {}, (b, c, u) -> b.append(c).append(u.narrow));

    public final PartJoiner joiner;
    public final PartFormatter formatter;

    DurationFormatMode(PartJoiner joiner, PartFormatter formatter) {
      this.joiner = joiner;
      this.formatter = formatter;
    }
  }

  public interface PartJoiner {
    void join(StringBuilder builder, boolean isLast);
  }

  public interface PartFormatter {
    void format(StringBuilder builder, int count, DurationUnit unit);
  }

  public enum DurationUnit {
    // Sorted by longest duration
      year("year",        "y",  1000L * 60 * 60 * 24 * 365),
     month("month",       "mo", 1000L * 60 * 60 * 24 * 30),
      week("week",        "w",  1000L * 60 * 60 * 24 * 7),
       day("day",         "d",  1000L * 60 * 60 * 24),
      hour("hour",        "h",  1000L * 60 * 60),
    minute("minute",      "m",  1000L * 60),
    second("second",      "s",  1000L),
    millis("millisecond", "ms", 1L);

    public static final EnumSet<DurationUnit>
      /** All duration units. */
      all = EnumSet.of(values()),
      /** All duration units without {@link #week} and {@link #millis} as they are generally unnecessary. */
      classic = EnumSet.of(year, month, day, hour, minute, second),
      /** All duration units without {@link #week}. */
      withoutWeek = EnumSet.of(year, month, day, hour, minute, second, millis),
      /** All duration units without {@link #millis}. */
      withoutMillis = EnumSet.of(year, month, week, day, hour, minute, second);

    /** Map of {@link #singular}, {@link #plurial} and {@link #narrow} names to their units. */
    private static final ObjectMap<String, DurationUnit> nameToUnit = new ObjectMap<>();

    static {
      for (DurationUnit u : all.array) {
        add(u.singular, u);
        add(u.plurial, u);
        add(u.narrow, u);
      }
      add(millis.name(), millis);
    }

    private static void add(String name, DurationUnit unit) {
      if (nameToUnit.put(name, unit) == null) return;
      throw new IllegalArgumentException("Duplicate unit name '" + name + "'");
    }

    public static DurationUnit get(String name) {
      return nameToUnit.get(name);
    }


    public final String singular, plurial, narrow;
    public final long duration;

    /** {@link #plurial} will be {@link #singular}{@code +'s'}. */
    DurationUnit(String singular, String narrow, long duration) { this(singular, singular+'s', narrow, duration); }
    DurationUnit(String singular, String plurial, String narrow, long duration) {
      this.singular = singular;
      this.plurial = plurial;
      this.narrow = narrow;
      this.duration = duration;
    }

    public boolean longer(long millis) { return millis >= duration; }
    public long get(long millis) { return millis / duration; }
    public long rest(long millis) { return millis % duration; }
  }
}
