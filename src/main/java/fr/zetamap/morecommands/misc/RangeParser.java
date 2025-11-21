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


package fr.zetamap.morecommands.misc;

import fr.zetamap.morecommands.util.StringReader;


public class RangeParser {  
  public static Range<Integer> parseInt(String input, boolean notNegative) throws StringReader.ParseException {
    return parseInt(new StringReader(input), notNegative);
  }
  public static Range<Integer> parseInt(StringReader reader, boolean notNegative) throws StringReader.ParseException {
    reader.skipWhitespaces();
    if (!reader.canPeekNext()) throw reader.expected("an int or a range");
    Integer min = reader.readNumeric(notNegative, false);
    if (!reader.canPeekNext() || reader.peekNext() != '.') {
      if (min == null) throw reader.expected("an int or a range");
      return Range.fixed(min);
    }
    if (reader.peekNext(1) != '.') throw reader.error("Invalid int range");
    reader.skip(2);
    Integer max = reader.readNumeric(notNegative, false);
    if (min == null && max == null) throw reader.error("Invalid int range");
    if (min != null && max != null && min > max) throw reader.error("Min range greater than max");
    return Range.of(min, max);
  }
  
  public static Range<Float> parseFloat(String input, boolean notNegative) throws StringReader.ParseException {
    return parseFloat(new StringReader(input), notNegative);
  }
  public static Range<Float> parseFloat(StringReader reader, boolean notNegative) throws StringReader.ParseException {
    reader.skipWhitespaces();
    if (!reader.canPeekNext()) throw reader.expected("a float or a range");
    Float min = reader.readDecimal(notNegative, false);
    
    // The first dot can be consumed by the first decimal read
    if (reader.peekNext(-1) == '.') reader.skip(-1);
    if (!reader.canPeekNext() || reader.peekNext() != '.') {
      if (min == null) throw reader.expected("a float or a range");
      return Range.fixed(min);
    }
    if (reader.peekNext(1) != '.') throw reader.error("Invalid float range");
    reader.skip(2);
    if (reader.peekNext() == '.') throw reader.error("Invalid float range");
    
    Float max = reader.readDecimal(notNegative, false);
    if (min == null && max == null) throw reader.error("Invalid float range");
    if (min != null && max != null && min > max) throw reader.error("Min range greater than max");
    return Range.of(min, max);
  }
}
