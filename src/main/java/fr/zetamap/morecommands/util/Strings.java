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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.StringJoiner;

import arc.files.Fi;
import arc.func.Boolf;
import arc.func.Cons2;
import arc.func.Func;
import arc.func.Func2;
import arc.func.Intf;
import arc.graphics.Color;
import arc.struct.Seq;
import arc.util.Structs;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonWriter.OutputType;
import arc.util.serialization.SerializationException;


public class Strings extends arc.util.Strings {
  public static String rJust(String str, int length) { return rJust(str, length, " "); }
  /** Justify string to the right. E.g. "&emsp; right" */
  public static String rJust(String str, int length, String filler) {
    int sSize = str.length(), fSize = filler.length();
    
    if (fSize == 0 || sSize >= length) return str; 
    if (fSize == 1) return filler.repeat(length-sSize)+str;   
    int add = length-sSize;
    return filler.repeat(add/fSize)+filler.substring(0, add%fSize)+str;
  }
  public static Seq<String> rJust(Seq<String> list, int length) { return rJust(list, length, " "); }
  public static Seq<String> rJust(Seq<String> list, int length, String filler) {
    list.replace(str -> rJust(str, length, filler));
    return list;
  }

  public static String lJust(String str, int length) { return lJust(str, length, " "); }
  /** Justify string to the left. E.g. "left &emsp;" */
  public static String lJust(String str, int length, String filler) {
    int sSize = str.length(), fSize = filler.length();
    
    if (fSize == 0 || sSize >= length) return str;
    if (fSize == 1) return str+filler.repeat(length-sSize);
    int add = length-sSize;
    return str+filler.repeat(add/fSize)+filler.substring(0, add%fSize);
  }
  public static Seq<String> lJust(Seq<String> list, int length) { return lJust(list, length, " "); }
  public static Seq<String> lJust(Seq<String> list, int length, String filler) {
    list.replace(str -> lJust(str, length, filler));
    return list;
  }
  
  public static String cJust(String str, int length) { return cJust(str, length, " "); }
  /** Justify string to the center. E.g. "&emsp; center &emsp;". */
  public static String cJust(String str, int length, String filler) {
    int sSize = str.length(), fSize = filler.length();
    
    if (fSize == 0 || sSize >= length) return str;
    int add = length-sSize, left = add/2, right = add-add/2;
    if (fSize == 1) return filler.repeat(left)+str+filler.repeat(right);
    return filler.repeat(left/fSize)+filler.substring(0, left%fSize)+str+
           filler.repeat(right/fSize)+filler.substring(0, right%fSize);
  }
  public static Seq<String> cJust(Seq<String> list, int length) { return cJust(list, length, " "); }
  public static Seq<String> cJust(Seq<String> list, int length, String filler) {
    list.replace(str -> cJust(str, length, filler));
    return list;
  }

  public static String sJust(String left, String right, int length) { return sJust(left, right, length, " "); }
  /** Justify string to the sides. E.g. "left &emsp; right" */
  public static String sJust(String left, String right, int length, String filler) {
    int fSize = filler.length(), lSize = left.length(), rSize = right.length();
    
    if (fSize == 0 || lSize+rSize >= length) return left+right; 
    int add = length-lSize-rSize;
    if (fSize == 1) return left+filler.repeat(add)+right;
    return left+filler.repeat(add/fSize)+filler.substring(0, add%fSize)+right;
  }
  public static Seq<String> sJust(Seq<String> left, Seq<String> right, int length) { return sJust(left, right, length, " "); }
  public static Seq<String> sJust(Seq<String> left, Seq<String> right, int length, String filler) {
    Seq<String> arr = /*new Seq<>(Integer.max(left.size, right.size))*/left; // for optimization, the left side will be used
    int i = 0;

    for (; i<Integer.min(left.size, right.size); i++) arr.set(i, /*.add(*/sJust(left.get(i), right.get(i), length, filler));
    // Fill the rest
    for (; i<left.size; i++) arr.set(i, /*.add(*/lJust(left.get(i), length, filler));
    for (; i<right.size; i++) arr.add(rJust(right.get(i), length, filler));
    
    return arr;
  }
  
  @SafeVarargs
  public static Seq<String> columnify(Seq<String>... columns) {
    return columnify(1, columns);
  }

  @SafeVarargs
  public static Seq<String> columnify(int gap, Seq<String>... columns) {
    return columnify(gap, " ", columns);
  }
  
  @SafeVarargs
  public static Seq<String> columnify(String filler, Seq<String>... columns) {
    return columnify(1, filler, columns);
  }
  
  @SafeVarargs
  public static Seq<String> columnify(int gap, String filler, Seq<String>... columns) {
    int[] lengths = new int[columns.length];
    for (int c=0; c<columns.length; c++) lengths[c] = maxLength(columns[c]);
    return columnify(filler, columns, lengths);
  }

  public static Seq<String> columnify(String filler, Seq<String>[] columns, int[] lengths) {
    return columnify(1, filler, columns, lengths);
  }
  
  public static Seq<String> columnify(int gap, String filler, Seq<String>[] columns, int[] lengths) {
    if (columns.length == 0) return new Seq<>(0);
    if (lengths.length < columns.length) {
      int[] newLengths = new int[columns.length];
      System.arraycopy(lengths, 0, newLengths, 0, lengths.length);
      for (int c=lengths.length; c<columns.length; c++) newLengths[c] = maxLength(columns[c]);
      lengths = newLengths;
    }
    
    int max = max(columns, a -> a.size), fSize = filler.length();
    Seq<String> arr = new Seq<>(max);
    StringBuilder builder = new StringBuilder();
    String[] fillers = new String[columns.length];
    
    for (int i=0, c; i<max; i++) {
      for (c=0; c<columns.length; c++) {
        if (i < columns[c].size) builder.append(lJust(columns[c].get(i), lengths[c] + gap, filler));
        else if (fillers[c] != null) builder.append(fillers[c]);
        else if (fSize == 1) builder.append(fillers[c] = filler.repeat(lengths[c] + gap));
        else builder.append(fillers[c] = filler.repeat((lengths[c] + gap) / fSize) + 
                                         filler.substring(0, (lengths[c] + gap) % fSize));
      }
      arr.add(builder.toString());
      builder.setLength(0);
    }
    
    return arr;
  }
    
  public static Seq<String> tableify(Seq<String> lines, int width) {
    return tableify(lines, width, Strings::lJust);
  }
  
  public static Seq<String> tableify(Seq<String> lines, int width, int gap) {
    return tableify(lines, width, gap, Strings::lJust);
  }
  
  public static Seq<String> tableify(Seq<String> lines, int width, Func2<String, Integer, String> justifier) {
    return tableify(lines, width, 1, justifier);
  }
  
  /** 
   * Create a table with given {@code lines}.<br>
   * Columns number is automatic calculated with the table's {@code width}.
   */
  public static Seq<String> tableify(Seq<String> lines, int width, int gap, Func2<String, Integer, String> justifier) {
    int columns = Math.max(1, width / (maxLength(lines) + 2)); // Estimate the columns
    Seq<String> result = new Seq<>(lines.size / columns + 1);
    int[] bests = new int[columns];
    StringBuilder builder = new StringBuilder();
    
    // Calculate the best length for each columns
    for (int i=0, c=0, s=0; i<lines.size; i++) {
      s = lines.get(i).length();
      c = i % columns;
      if (s > bests[c]) bests[c] = s;
    }
    
    // Now justify lines
    for (int i=0, c; i<lines.size;) { 
      for (c=0; c<columns && i<lines.size; c++, i++) 
        builder.append(justifier.get(lines.get(i), bests[c] + gap));
      
      result.add(builder.toString());
      builder.setLength(0);
    }
    
    return result;
  }
  
  public static <T> int max(Iterable<T> list, Intf<T> intifier) {
    boolean first = true;
    int index = 0;
    
    for (T i : list) {
      int s = intifier.get(i);
      if (first) index = s;
      else if (s > index) index = s;
      first = false;
    }
    
    return index;
  }
  
  public static <T> int max(T[] list, Intf<T> intifier) {
    boolean first = true;
    int index = 0;
    
    for (T i : list) {
      int s = intifier.get(i);
      if (first) index = s;
      else if (s > index) index = s;
      first = false;
    }
    
    return index;
  }
  
  public static <T> int min(Iterable<T> list, Intf<T> intifier) {
    boolean first = true;
    int index = 0;
    
    for (T i : list) {
      int s = intifier.get(i);
      if (first) index = s;
      else if (s < index) index = s;
      first = false;
    }
    
    return index;
  }
  
  public static <T> int min(T[] list, Intf<T> intifier) {
    boolean first = true;
    int index = 0;
    
    for (T i : list) {
      int s = intifier.get(i);
      if (first) index = s;
      else if (s < index) index = s;
      first = false;
    }
    
    return index;
  }
  
  public static int maxLength(Iterable<? extends String> list) {
    return max(list, str -> str.length());
  }
  
  public static int maxLength(String... list) {
    return max(list, str -> str.length());
  }

  //TODO: need to merge these three methods in one StringBuilder
  public static String normalize(String str) {
    return stripGlyphs(stripColors(str)).strip();
  }

  public static long bits2int(boolean... list) {
    int out = 0;
    for (int i=0; i<list.length; i++) {
      out |= list[i] ? 1 : 0;
      out <<= 1;
    }
    return out >> 1;
  }

  public static boolean[] int2bits(long number) { return int2bits(number, 0); }
  public static boolean[] int2bits(long number, int bits) {
    // Check value because 0 have a negative size  
    if (number == 0) return new boolean[bits == 0 ? 1 : bits];
      
    int size = bits < 1 ? (int)(Math.log(number)/Math.log(2)+1) : bits;
    boolean[] out = new boolean[size];
    while (size-- > 0) {
      out[size] = (number & 1) != 0;
      number >>= 1;
    }
    return out;
  }
  
  public static String hueToColorTag(int hue) {
    StringBuilder builder = new StringBuilder(9);
    hueToColorTag(builder, hue);
    return builder.toString();
  }
  
  /** Re-implementation of {@link Color#fromHsv(float, float, float)} that handles only the hue. */
  public static void hueToColorTag(StringBuilder builder, int hue) {
    int r, g, b;

    float x = (hue / 60f + 6) % 6;
    float f = x - (int)x;
    float q = 1 - f;
    switch ((int)x) {
      case 0:
        r = 255;
        g = (int)(f * 255);
        b = 0;
        break;
      case 1:
        r = (int)(q * 255);
        g = 255;
        b = 0;
        break;
      case 2:
        r = 0;
        g = 255;
        b = (int)(f * 255);
        break;
      case 3:
        r = 0;
        g = (int)(q * 255);;
        b = 255;
        break;
      case 4:
        r = (int)(f * 255);
        g = 0;
        b = 255;
        break;
      default:
        r = 255;
        g = 0;
        b = (int)(q * 255);
    }
    
    builder.append("[#");
    if (r < 16) builder.append('0');
    builder.append(Integer.toHexString(r));
    if (g > 0 || b > 0) {
      if (g < 16) builder.append('0');
      builder.append(Integer.toHexString(g));  
      if (b > 0) {
        if (b < 16) builder.append('0');
        builder.append(Integer.toHexString(b));  
      }
    }
    builder.append(']');
  }

  public static String rainbowify(String src, int startHue, int addedHue) {
    StringBuilder builder = new StringBuilder(src.length() * 9);

    for (int i=0, n=src.length(); i<n; i++) {
      hueToColorTag(builder, startHue);
      
      char c = src.charAt(i);
      if (c == '[') builder.append('[');
      builder.append(c).append("[]");
      
      startHue += addedHue;
      startHue %= 360;
    }

    return builder.toString();
  }
  
  /** @return whether the specified string mean true */
  public static boolean isTrue(String str) {
    return isTrue(str, true);
  }
  
  /** @return whether the specified string mean true */
  public static boolean isTrue(String str, boolean isValue) {
    if (isValue) {
      switch (str.toLowerCase()) {
        case "1": case "true": case "on": 
        case "enable": case "activate": case "yes":
                 return true;
        default: return false;
      }  
    } else {
      switch (str.toLowerCase()) {
        case "on": case "enable": case "activate":
                 return true;
        default: return false;
      }
    }
  }
  
  /** @return whether the specified string mean false */
  public static boolean isFalse(String str) {
    return isFalse(str, true);
  }
  
  /** @return whether the specified string mean false */
  public static boolean isFalse(String str, boolean isValue) {
    if (isValue) {
      switch (str.toLowerCase()) {
        case "0": case "false": case "off": 
        case "disable": case "desactivate": case "no":
                 return true;
        default: return false;
      }  
    } else {
      switch (str.toLowerCase()) {
        case "off": case "disable": case "desactivate":
                 return true;
        default: return false;
      }
    }
  }
  
  /** 
   * @return whether {@code newVersion} is greater than {@code currentVersion}. (e.g. {@code "v146" > "124.1"})
   * @apiNote can handle dots and dashes in the version and makes very fast comparison. <br>
   *          Also ignores non-int parts. (e.g. {@code "v1.2-rc36"}, the {@code "rc36"} part will be ignored)
   */
  public static boolean isVersionAtLeast(String currentVersion, String newVersion) {
    if (currentVersion == null || newVersion == null || 
        currentVersion.isEmpty() || newVersion.isEmpty()) return false;
    
    int last1 = currentVersion.charAt(0) == 'v' ? 1 : 0, last2 = newVersion.charAt(0) == 'v' ? 1 : 0, 
        len1 = currentVersion.length(), len2 = newVersion.length(),
        dot1 = 0, dot2 = 0, dash1 = 0, dash2 = 0, 
        part1 = 0, part2 = 0;
    
    while ((dot1 != -1  && dot2 != -1) && (last1 < len1 && last2 < len2)) {
      dot1 = currentVersion.indexOf('.', last1);
      dash1 = currentVersion.indexOf('-', last1);
      dot2 = newVersion.indexOf('.', last2);
      dash2 = newVersion.indexOf('-', last2);
      if (dot1 == -1) dot1 = dash1;
      if (dash1 != -1) dot1 = Math.min(dot1, dash1);
      if (dot1 == -1) dot1 = len1;
      if (dot2 == -1) dot2 = dash2;
      if (dash2 != -1) dot2 = Math.min(dot2, dash2);
      if (dot2 == -1) dot2 = len2;
      
      part1 = parseInt(currentVersion, 10, 0, last1, dot1);
      part2 = parseInt(newVersion, 10, 0, last2, dot2);
      last1 = dot1+1;
      last2 = dot2+1;

      if (part1 != part2) return part2 > part1;
    }

    // Continue iteration on newVersion to see if it's just leading zeros.
    while (dot2 != -1 && last2 < len2) {
      dot2 = newVersion.indexOf('.', last2);
      dash2 = newVersion.indexOf('-', last2);
      if (dot2 == -1) dot2 = dash2;
      if (dash2 != -1) dot2 = Math.min(dot2, dash2);
      if (dot2 == -1) dot2 = len2;
      
      part2 = parseInt(newVersion, 10, 0, last2, dot2);
      last2 = dot2+1;
      
      if (part2 > 0) return true;
    }
    
    return false;
  }
  
  public static String kebabize(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    boolean sep = true;
    
    for (int i=0, n=s.length(); i<n; i++) {
      char c = s.charAt(i);
      if (c == '_' || c == '-' || c == ' ') {
        if (!sep) sb.append('-');
        sep = true;
      } else if (Character.isUpperCase(c)) {
        if (!sep) sb.append('-');
        sb.append(Character.toLowerCase(c));
        sep = false;
      } else {
        sb.append(c);
        sep = false;  
      }
    }
    
    return sb.toString();
  }
  
  public static String jsonPrettyPrint(JsonValue object, OutputType outputType) {
    StringWriter out = new StringWriter();
    try { jsonPrettyPrint(object, out, outputType, 0); } 
    catch (IOException ignored) { return ""; }
    return out.toString();
  }
  
  public static void jsonPrettyPrint(JsonValue object, Writer writer, OutputType outputType) throws IOException {
    jsonPrettyPrint(object, writer, outputType, 0);
  }
  
  /** 
   * Re-implementation of {@link JsonValue#prettyPrint(OutputType, Writer)}, 
   * because the indent isn't correct and the max object size before new line, is too big.
   */
  public static void jsonPrettyPrint(JsonValue object, Writer writer, OutputType outputType, int indent) throws IOException {
    switch (object.type()) {
      case object:
        if (object.child == null) writer.write("{}");
        else {
          indent++;
          boolean newLines = needNewLine(object, 1);
          writer.write(newLines ? "{\n" : "{ ");
          for (JsonValue child = object.child; child != null; child = child.next) {
            if(newLines) writer.write("  ".repeat(indent));
            writer.write(outputType.quoteName(child.name));
            writer.write(": ");
            jsonPrettyPrint(child, writer, outputType, indent);
            if((!newLines || outputType != OutputType.minimal) && child.next != null) writer.append(',');
            writer.write(newLines ? '\n' : ' ');
          }
          if(newLines) writer.write("  ".repeat(indent - 1));
          writer.write('}');
        }
        break;
      case array:
        if (object.child == null) writer.write("[]");
        else {
          indent++;
          boolean newLines = needNewLine(object, 1);
          writer.write(newLines ? "[\n" : "[ ");
          for (JsonValue child = object.child; child != null; child = child.next) {
            if (newLines) writer.write("  ".repeat(indent));
            jsonPrettyPrint(child, writer, outputType, indent);
            if ((!newLines || outputType != OutputType.minimal) && child.next != null) writer.append(',');
            writer.write(newLines ? '\n' : ' ');
          }
          if (newLines) writer.append("  ".repeat(indent - 1));
          writer.write(']');
        }
        break;
      case stringValue: writer.write(outputType.quoteValue(object.asString())); break;
      case doubleValue: writer.write(Double.toString(object.asDouble())); break;
      case longValue: writer.write(Long.toString(object.asLong())); break;
      case booleanValue: writer.write(Boolean.toString(object.asBoolean())); break;
      case nullValue: writer.write("null"); break;
      default: throw new SerializationException("Unknown object type: " + object);
    }
  }
  
  public static String toJson(JsonValue object, OutputType outputType) {
    StringWriter out = new StringWriter();
    try { toJson(object, out, outputType); } 
    catch (IOException ignored) { return ""; }
    return out.toString();
  }
  
  /** 
   * Re-implementation of {@link JsonValue#json(JsonValue, StringBuilder, OutputType)}, 
   * with the ability write directly to a {@link Writer} instead of a {@link StringBuilder}. 
   */
  public static void toJson(JsonValue object, Writer writer, OutputType outputType) throws IOException {
    switch (object.type()) {
      case object:
        if (object.child == null) writer.write("{}");
        else {
          writer.write('{');
          for (JsonValue child = object.child; child != null; child = child.next) {
            writer.write(outputType.quoteName(child.name));
            writer.write(':');
            toJson(child, writer, outputType);
            if (child.next != null) writer.write(',');
          }
          writer.write('}');
        }
        break;
      case array:
        if (object.child == null) writer.write("[]");
        else {
          writer.write('[');
          for (JsonValue child = object.child; child != null; child = child.next) {
            toJson(child, writer, outputType);
            if (child.next != null) writer.write(',');
          }
          writer.write(']');
        }
        break;
      case stringValue: writer.write(outputType.quoteValue(object.asString())); break;
      case doubleValue: writer.write(Double.toString(object.asDouble())); break;
      case longValue: writer.write(Long.toString(object.asLong())); break;
      case booleanValue: writer.write(Boolean.toString(object.asBoolean())); break;
      case nullValue: writer.write("null"); break;
      default: throw new SerializationException("Unknown object type: " + object);
    }
  }
  
  private static boolean needNewLine(JsonValue object, int maxChildren){
    for (JsonValue child = object.child; child != null; child = child.next) 
      if (child.isObject() || child.isArray() || --maxChildren < 0) return true;
    return false;
  }
  
  
  public static Fi getFiChild(Fi parent, String path) {
    if (parent == null) throw new NullPointerException("parent cannot be null");
    if (path == null || path.isEmpty()) return parent;
    Fi folder = new Fi(path);
    return folder.file().isAbsolute() ? folder : parent.child(path);
  }
  
  /** Like {@link arc.util.Strings#format(String, Object...)} but you can specify the {@link StringBuilder} */
  public static void format(StringBuilder out, String text, Object... args){
    if (args.length > 0) {
      for (int i=0, argi=0, n=text.length(); i<n; i++) {
        char c = text.charAt(i);
        if (c == '@' && argi < args.length) out.append(args[argi++]);
        else out.append(c);
      }
    } else out.append(text);
  }
  
  public static String join(CharSequence delimiter, CharSequence[] elements, int start, int end) {
    if (elements == null || delimiter == null) return null;
    if (elements.length == 0 || start < 0 || end > elements.length || start >= end) return "";

    StringJoiner joiner = new StringJoiner(delimiter);
    for (int i=start; i<end; i++) joiner.add(elements[i]);
    return joiner.toString();
  }
  
  /** Converts a list to a human readable sentence. E.g. [1, 2, 3, 4, 5] -> "1, 2, 3, 4 and 5" */
  public static <T> String toSentence(Iterable<T> list, Func<T, String> stringifier) {
    StringBuilder builder = new StringBuilder();
    toSentence(builder, list, (b, e) -> b.append(stringifier.get(e)));
    return builder.toString();
  }
  
  /** Converts a list to a human readable sentence. E.g. [1, 2, 3, 4, 5] -> "1, 2, 3, 4 and 5" */
  public static <T> String toSentence(Iterable<T> list, Func<T, String> stringifier, String or, String and) {
    StringBuilder builder = new StringBuilder();
    toSentence(builder, list, (b, e) -> b.append(stringifier.get(e)), or, and);
    return builder.toString();
  }
  
  /** Converts a list to a human readable sentence. E.g. [1, 2, 3, 4, 5] -> "1, 2, 3, 4 and 5" */
  public static <T> void toSentence(StringBuilder builder, Iterable<T> list, Cons2<StringBuilder, T> stringifier) {
    toSentence(builder, list, stringifier, ", ", " and ");
  }
  /** Converts a list to a human readable sentence. E.g. [1, 2, 3, 4, 5] -> "1, 2, 3, 4 and 5" */
  public static <T> void toSentence(StringBuilder builder, Iterable<T> list, Cons2<StringBuilder, T> stringifier, 
                                    String or, String and) {
    java.util.Iterator<T> iter = list.iterator();
    if (!iter.hasNext()) return;

    stringifier.get(builder, iter.next());
    while (iter.hasNext()) {
      T tmp = iter.next();
      builder.append(iter.hasNext() ? or : and);
      stringifier.get(builder, tmp);
    }
  }
  
  /** {@link String#contains(CharSequence)} but with a predicate. */
  public static boolean contains(String src, Boolf<Character> predicate) {
    for (int i=0, n=src.length(); i<n; i++) {
      if (predicate.get(src.charAt(i))) return true;
    }
    return false;
  }

  // Why not ¯\_(ツ)_/¯
  private static final String vowels = "aeiou"/*y"*/;
  private static final String[] anExceptions = {"hei", "hon", "hou"},
                                aExceptions = {"uni", "use", "one", "ut", "eu"};
  
  /** This is not totally great, but it do the job most of the time. */
  public static String aOrAn(String adjective) {
    if (adjective == null || adjective.isEmpty()) return "";
    // exceptions
    if (Structs.contains(anExceptions, adjective::startsWith)) return "an";
    if (Structs.contains(aExceptions, adjective::startsWith)) return "a";
    int len = adjective.length();
    char letter = Character.toLowerCase(adjective.charAt(0));
    boolean reversed = false;
    // "you" sound
    if (letter == 'u') {
      if (len == 1) return "an";
      letter = Character.toLowerCase(adjective.charAt(1));
      reversed = true; 
    } else if (len > 1 && letter == 'e' && Character.toLowerCase(adjective.charAt(1)) == 'u') {
      if (len == 2) return "an";
      letter = Character.toLowerCase(adjective.charAt(2));
      reversed = true;
    }
    return vowels.indexOf(letter) != -1 ^ reversed ? "an" : "a";
  }
}
