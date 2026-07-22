/**
 * Minosoft
 * Copyright (C) 2020-2023 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package fr.zetamap.morecommands.util;

import arc.func.Cons;
import arc.func.Func;
import arc.func.Func2;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;


/**
 * From: https://github.com/Bixilon/Minosoft/blob/master/src/main/java/de/bixilon/minosoft/commands/util/StringReader.kt<br>
 * Converted with: https://codeconvert.ai<br>
 * Adapted for own use, with the original copyright.
 */
public class StringReader {
  public final String string;
  public final int length;
  public int pos = 0;
  protected StringBuilder builder;

  public StringReader(String string) {
    this.string = string;
    this.length = string.length();
    this.builder = new StringBuilder();
  }

  public int position() { return pos; }
  public void position(int pos) { this.pos = pos; }

  public void skip() { pos++; }
  public void skip(int n) { pos += n; }

  public boolean canPeek() { return canPeek(0, true); }
  public boolean canPeek(int offset) { return canPeek(offset, true); }
  public boolean canPeek(int offset, boolean ignoreWhitespaces) {
    if (pos + offset >= length) return false;
    int whitespaces = ignoreWhitespaces ? peekWhitespaces() : 0;
    return pos + offset + whitespaces < length;
  }

  public boolean canPeekNext() { return canPeekNext(0); }
  public boolean canPeekNext(int offset) {
    return pos + offset < length;
  }

  public int peekNext() { return peekNext(0); }
  public int peekNext(int offset) {
    return canPeekNext(offset) ? string.charAt(pos + offset) : -1;
  }

  public void checkPeekNext() { checkPeekNext(0); }
  public void checkPeekNext(int offset) {
    if (!canPeekNext(offset)) throw eoi();
  }

  public int readNext() {
    int next = peekNext();
    if (next == -1) return -1;
    pos++;
    return next;
  }

  public int unsafePeekNext() {
    int result = peekNext();
    if (result == -1) throw eoi();
    return result;
  }

  public int unsafeReadNext() {
    int result = readNext();
    if (result == -1) throw eoi();
    return result;
  }

  public int peekWhitespaces() {
    int count = 0, peek = peekNext();
    while (peek != -1 && Character.isWhitespace(peek)) peek = peekNext(++count);
    return count;
  }

  public int skipWhitespaces() { return skipWhitespaces(0); }
  public int skipWhitespaces(int minimum) {
    int count = peekWhitespaces();
    if (count < minimum) throw expected(minimum < 2 ? "a whitespace" : minimum + " whitespaces");
    pos += count;
    return count;
  }

  public int peek() { return peek(true); }
  public int peek(boolean ignoreWhitespaces) {
    if (ignoreWhitespaces) skipWhitespaces();
    return peekNext();
  }

  public int read() { return read(true); }
  public int read(boolean ignoreWhitespaces) {
    if (ignoreWhitespaces) skipWhitespaces();
    int peek = peekNext();
    if (peek == -1) return -1;
    pos++;
    return peek;
  }

  public int unsafePeek() { return unsafePeek(true); }
  public int unsafePeek(boolean ignoreWhitespaces) {
    if (ignoreWhitespaces) skipWhitespaces();
    return unsafePeekNext();
  }

  public int unsafeRead() { return unsafeRead(true); }
  public int unsafeRead(boolean ignoreWhitespaces) {
    int peek = unsafePeek(ignoreWhitespaces);
    pos++;
    return peek;
  }

  public int readQuote() { return readQuote(true); }
  public int readQuote(boolean ignoreWhitespaces) {
    int quote = peek(ignoreWhitespaces);
    if (!isQuote(quote)) return -1;
    pos++;
    return quote;
  }

  public int unsafeReadQuote() { return readQuote(true); }
  public int unsafeReadQuote(boolean ignoreWhitespaces) {
    int quote = unsafePeek(ignoreWhitespaces);
    if (!isQuote(quote)) return -1;
    pos++;
    return quote;
  }

  public String readQuotedString() { return readQuotedString(true); }
  public String readQuotedString(boolean ignoreWhitespaces) {
    if (ignoreWhitespaces) skipWhitespaces();
    if (!canPeekNext()) return null;
    int quote = unsafeReadQuote(false), read;
    if (quote == -1) throw expected("a quote");

    while ((read = readNext()) != -1 && read != quote) {
      if (read == '\\') {
        read = readNext();
        if (read != '\\' && read != quote) throw expected("\\ or " + (char)quote, read);
      }
      builder.append((char)read);
    }

    if (read != quote) throw expected("the end of quoted string", read);
    return buildResult();
  }

  public String peekWord() { return peekWord(true); }
  public String peekWord(boolean ignoreWhitespaces) {
    if (ignoreWhitespaces) skipWhitespaces();
    if (!canPeekNext()) return null;
    int peek, plus = 0;
    while ((peek = peekNext(plus)) != -1 && isWord(peek)) {
      builder.append((char)peek);
      plus++;
    }
    return builder.length() == 0 ? null : buildResult();
  }

  public String readWord() { return readWord(true); }
  public String readWord(boolean ignoreWhitespaces) {
    if (!canPeekNext()) return null;
    String word = peekWord(ignoreWhitespaces);
    if (word != null) pos += word.length();
    return word;
  }

  public String readString() { return readString(true); }
  public String readString(boolean ignoreWhitespaces) {
    int start = peek(ignoreWhitespaces);
    return start == -1 ? null : isQuote(start) ? readQuotedString(false) : readWord(false);
  }

  public String peekRemaining() {
    return pos >= length ? null : string.substring(pos, length);
  }

  public String readRemaining() {
    String str = peekRemaining();
    if (str == null) return null;
    pos = length;
    return str;
  }

  public String readUntil(char ch) { return readUntil(false, ch); }
  public String readUntil(boolean required, char ch) {
    if (!canPeekNext()) return null;
    int peek;
    while ((peek = peekNext()) != -1 && peek != ch) {
      builder.append((char)peek);
      pos++;
    }
    if (peek == -1 && required) throw expected("'" + ch + "'");
    return buildResult();
  }

  public int readNumeric() { return readNumeric(false, true); }
  public int readNumeric(boolean notNegative) { return readNumeric(notNegative, true); }
  /** @return the parsed number or {@code null} if no one found and {@code required} id {@code false}, else throw an error. */
  public Integer readNumeric(boolean notNegative, boolean required) {
    int peek = peek();
    if (peek == -1) {
      if (required) throw expected(notNegative ? "a positive number" : "a number");
      return null;
    }

    // Validate format
    int start = pos;
    if (peek == '-') {
      pos++;
      if (notNegative) throw error("Negative number not allowed here");
    }
    boolean foundDigit = false;
    while ((peek = peekNext()) != -1 && isDigit(peek)) {
      pos++;
      foundDigit = true;
    }
    if (!foundDigit) {
      if (required) throw expected(notNegative ? "a positive number" : "a number");
      return null;
    }
    if (peek == 'e' || peek == 'E' || peek == 'f' || peek == 'F') {
      pos++;
      throw error("Decimal number not allowed here");
    }

    int parsed = Strings.parseInt(string, 10, Integer.MIN_VALUE, start, pos);
    if (parsed == Integer.MIN_VALUE) throw error(notNegative ? "Invalid positive number" : "Invalid number");
    return parsed;
  }

  public float readDecimal() { return readDecimal(false); }
  public float readDecimal(boolean notNegative) { return readDecimal(notNegative, true); }
  /** @return the parsed number or {@code null} if no one found and {@code required} id {@code false}, else throw an error. */
  public Float readDecimal(boolean notNegative, boolean required) {
    int peek = peek();
    if (peek == -1) {
      if (required) throw expected(notNegative ? "a positive decimal number" : "a decimal number");
      return null;
    }

    // Validate format
    if (peek == '-') {
      pos++;
      if (notNegative) throw error("Negative decimal number not allowed here");
      builder.append((char)peek);
    }
    boolean foundDigit = false, foundDot = false, foundExponent = false;
    while ((peek = peekNext()) != -1) {
      if (peek == '.') {
        if (foundDot) break;//break instead  //throw error("Found another dot");
        else foundDot = true;
      } else if (peek == 'e' || peek == 'E') {
        if (foundExponent) {
          pos++;
          throw error("Found another exponent");
        }
        else foundExponent = true;
      } else if (!isDigit(peek) && peek != 'f' && peek != 'F') break;
      else foundDigit = true;
      builder.append((char)peek);
      pos++;
    }
    if (!foundDigit) {
      if (required) throw expected(notNegative ? "a positive decimal number" : "a decimal number");
      return null;
    }

    // Use double parsing
    double parsed = Strings.parseDouble(buildResult(), Double.MAX_VALUE);
    if (parsed <= -Float.MAX_VALUE || parsed >= Float.MAX_VALUE)
      throw error(notNegative ? "Invalid positive decimal number" : "Invalid decimal number");
    return (float)parsed;
  }

  public boolean isNegated() { return isNegated(false); }
  public boolean isNegated(boolean notAllowed) {
    int peek = peek();
    if (peek != '!') return false;
    pos++;
    if (notAllowed) throw error("Negateable value not allowed here");
    return true;
  }

  public boolean isArray() {
    return peekNext() == '[';
  }

  public <K, V> ObjectMap<K, V> readArraySet(Func<StringReader, K> keyReader, Func2<StringReader, K, V> valueReader) {
    return readArraySet(keyReader, valueReader, "map key");
  }
  public <K, V> ObjectMap<K, V> readArraySet(Func<StringReader, K> keyReader, Func2<StringReader, K, V> valueReader,
                                             String keyKind) {
    if (!isArray()) return null;
    ObjectMap<K, V> map = new ObjectMap<>();
    String pronoun = Strings.aOrAn(keyKind);
    readArray(r -> {
      K key = keyReader.get(r);
      if (key == null) throw expected(pronoun + ' ' + keyKind);
      if (map.containsKey(key)) throw error("Duplicated " + keyKind);
      int assign = read();
      if (assign != '=') throw expected("a value assignment ('=')", assign);
      skipWhitespaces();
      V value = valueReader.get(r, key);
      map.put(key, value);
    }, "map");
    return map;
  }

  public <E> Seq<E> readArray(Func<StringReader, E> elementReader) { return readArray(elementReader, "array element"); }
  public <E> Seq<E> readArray(Func<StringReader, E> elementReader, String elementKind) {
    if (!isArray()) return null;
    Seq<E> seq = new Seq<>();
    String pronoun = Strings.aOrAn(elementKind);
    readArray(r -> {
      E element = elementReader.get(r);
      if (element == null) throw expected(pronoun + ' ' + elementKind);
      seq.add(element);
    });
    return seq;
  }

  // Reads same as an array but duplicate elements are not allowed
  public <E> ObjectSet<E> readSet(Func<StringReader, E> elementReader) { return readSet(elementReader, "array element"); }
  public <E> ObjectSet<E> readSet(Func<StringReader, E> elementReader, String elementKind) {
    if (!isArray()) return null;
    ObjectSet<E> set = new ObjectSet<>();
    String pronoun = Strings.aOrAn(elementKind);
    readArray(r -> {
      E element = elementReader.get(r);
      if (element == null) throw expected(pronoun + ' ' + elementKind);
      if (!set.add(element)) throw error("Duplicated " + elementKind);
    });
    return set;
  }

  public boolean readArray(Cons<StringReader> reader) { return readArray(reader, "array"); }
  public boolean readArray(Cons<StringReader> reader, String kind) {
    if (!isArray()) return false;
    pos++; // '['
    if (peek() == ']') {
      pos++;
      return true;
    }

    String pronoun = Strings.aOrAn(kind);
    while (canPeekNext()) {
      reader.get(this);
      int read = read();
      if (read == ']') return true;
      if (read != ',') throw expected(pronoun + ' ' + kind + " separator (',') or " +
                                      pronoun + ' ' + kind + " end (']')", read);
      if (peek() == ']') {
        pos++;
        return true;
      }
    }
    throw expected(pronoun + ' ' + kind + " end (']')");
  }

  @Override
  public String toString() {
    String str = peekRemaining();
    return str == null ? "" : str;
  }

  protected String buildResult() {
    String result = builder.toString();
    builder.setLength(0);
    return result;
  }

  public ParseException eoi() { return new ParseException("Unexpected end of input"); }
  public ParseException error(String message) { return new ParseException(message); }
  public ParseException expected(String what) { return new ParseException(what, -1); }
  public ParseException expected(String what, int got) { return new ParseException(what, got); }
  public ParseException notFound(String kind, String got) { return new ParseException(kind, got); }

  public static boolean isQuote(int ch) { return ch == '"' || ch == '\''; }
  public static boolean isAlpha(int ch) { return ch != -1 && isDigit(ch) || ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z'; }
  public static boolean isWord(int ch) { return ch != -1 && isAlpha(ch) || ch == '.' || ch == '_' || ch == '-' || ch == ':'; }
  public static boolean isDigit(int ch) { return ch >= '0' && ch <= '9'; }


  @SuppressWarnings("serial")
  public class ParseException extends RuntimeException {
    public static final int maxNearCharacters = 16;

    public final int pos = StringReader.this.pos;
    public final String near = getNear(pos);
    protected final String message;

    // Reset builder in case of
    { builder.setLength(0); }

    public ParseException(String message) {
      this.message = message + formatNear();
    }

    public ParseException(String expected, int got) {
      this.message = "Expected " + expected + formatNear() + (got == -1 ? "" : ", but got '" + (char)got + "'");
    }

    public ParseException(String kind, String got) {
      this.message = "No " + kind + " named '" + got + "' found" + formatNear();
    }

    protected String getNear(int pos) {
      return string.substring(Math.max(0, pos - maxNearCharacters), pos);
    }

    protected String formatNear() {
      return " near '" + near + "<--HERE'";
    }

    @Override
    public String getMessage() {
      return message;
    }
  }
}
