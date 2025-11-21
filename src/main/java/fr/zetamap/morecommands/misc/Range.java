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


public interface Range<T extends Comparable<T>> {
  T min();
  T max();
  
  default boolean contains(T value) {
    return value != null 
        && (min() == null || value.compareTo(min()) >= 0)
        && (max() == null || value.compareTo(max()) <= 0);
  }
  
  /** @return {@link #min()}{@code ..}{@link #max()} or {@link #min()} if they are equals. */
  default String asString() {
    T min = min(), max = max();
    return min.equals(max) ? String.valueOf(min) : min + ".." + max;
  }

  
  public static interface Fixed<T extends Comparable<T>> extends Range<T> {
    T value();
    
    default T min() { return value(); }
    default T max() { return value(); }
    default boolean contains(T v) { return v != null && (value() == null || v.compareTo(value()) == 0); }
    default String asString() { return String.valueOf(value()); }
  }
  
  
  /** A {@code null} {@code min} or {@code max} means no limit for the side (or both). */
  public static Range<Integer> of(Integer min, Integer max) {
    if (min != null && max != null && min > max) throw new IllegalArgumentException("min is greater than max");
    return new Range<Integer>() {
      public Integer min() { return min; }
      public Integer max() { return max; }
      public boolean contains(Integer value) {
        return value != null && (min == null || value >= min) && (max == null || value <= max);
      }
      public String toString() { return asString(); }
    };
  }
  
  public static Range.Fixed<Integer> fixed(int value) {
    return new Range.Fixed<Integer>() {
      public Integer value() { return value; }
      public boolean contains(Integer v) { return v != null && v == value; }
      public String toString() { return asString(); }
    };
  }
  
  /** A {@code null} {@code min} or {@code max} means no limit for the side (or both). */
  public static Range<Long> of(Long min, Long max) {
    if (min != null && max != null && min > max) throw new IllegalArgumentException("min is greater than max");
    return new Range<Long>() {
      public Long min() { return min; }
      public Long max() { return max; }
      public boolean contains(Long value) {
        return value != null && (min == null || value >= min) && (max == null || value <= max);
      }
      public String toString() { return asString(); }
    };
  }
  
  public static Range.Fixed<Long> fixed(long value) {
    return new Range.Fixed<Long>() {
      public Long value() { return value; }
      public boolean contains(Long v) { return v != null && v == value; }
      public String toString() { return asString(); }
    };
  }
  
  /** A {@code null} {@code min} or {@code max} means no limit for the side (or both). */
  public static Range<Float> of(Float min, Float max) {
    if (min != null && max != null && min > max) throw new IllegalArgumentException("min is greater than max");
    return new Range<Float>() {
      public Float min() { return min; }
      public Float max() { return max; }
      public boolean contains(Float value) {
        return value != null && (min == null || value >= min) && (max == null || value <= max);
      }
      public String toString() { return asString(); }
    };
  }
  
  public static Range.Fixed<Float> fixed(float value) {
    return new Range.Fixed<Float>() {
      public Float value() { return value; }
      public boolean contains(Float v) { return v != null && v == value; }
      public String toString() { return asString(); }
    };
  }
  
  /** A {@code null} {@code min} or {@code max} means no limit for the side (or both). */
  public static Range<Double> of(Double min, Double max) {
    if (min != null && max != null && min > max) throw new IllegalArgumentException("min is greater than max");
    return new Range<Double>() {
      public Double min() { return min; }
      public Double max() { return max; }
      public boolean contains(Double value) {
        return value != null && (min == null || value >= min) && (max == null || value <= max);
      }
      public String toString() { return asString(); }
    };
  }
  
  public static Range.Fixed<Double> fixed(double value) {
    return new Range.Fixed<Double>() {
      public Double value() { return value; }
      public boolean contains(Double v) { return v != null && v == value; }
      public String toString() { return asString(); }
    };
  }
}