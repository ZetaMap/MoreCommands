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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import arc.func.Func2;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Reflect;
import arc.util.Strings;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import arc.util.serialization.SerializationException;

import mindustry.Vars;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.units.StatusEntry;
import mindustry.gen.*;
import mindustry.io.JsonIO;
import mindustry.mod.ContentParser;
import mindustry.type.*;
import mindustry.world.Tile;
import mindustry.world.consumers.*;
import mindustry.world.modules.*;


/** 
 * Extended class that allow to get the read fields from {@link #readField} and {@link #readFields} methods, 
 * instead of setting them on the object. Allows to read transient fields, use default mindustry serializers
 * from {@link JsonIO#json} and {@link ContentParser#parser}, and have a static instance. 
 */
@SuppressWarnings("rawtypes")
public class MindustryJson extends Json {
  protected static final MindustryJson instance = new MindustryJson();
  
  static {
    applyMindustrySerializers(instance);
  }
  
  public static MindustryJson get() {
    return instance;
  }
  
  // region mindustry
  
  protected static ContentType[] toSearch;
  protected static ObjectMap<Class<?>, ContentType> contentTypes;
  
  /** Same as {@link Reflect#get(Class, Object, String)} but search in the entire class hierarchy. */
  @SuppressWarnings("unchecked")
  private static <T> T Reflect_get(Class<?> type, Object object, String name) {
    try {
      Exception first = null;
      Field field = null;
      do {
        try { 
          field = type.getDeclaredField(name); 
          break;
        } catch (NoSuchFieldException e) { 
          if (first == null) first = e;
          type = type.getSuperclass(); 
        }
      } while (type != Object.class);
      if (field == null) throw first;
      field.setAccessible(true);
      return (T)field.get(object);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static <T> T Reflect_get(Object object, String name) { 
    return Reflect_get(object.getClass(), object, name); 
  }

  
  /** This is completely a mess but i have no choice, everything is private >=( */
  @SuppressWarnings("unchecked")
  public static void applyMindustrySerializers(Json json) {
    MindustryJson.<ObjectMap<Class, Serializer>>Reflect_get(JsonIO.json, "classToSerializer").each(json::setSerializer);
    ContentParser modParser = Reflect.get(Vars.mods, "parser");
    MindustryJson.<ObjectMap<Class, Serializer>>Reflect_get(Reflect.<Json>get(modParser, "parser"), 
                                                            "classToSerializer").each(json::setSerializer);
    
    // Convert parsers from mod parser to Serializer
    // Inferred types cannot be used and invocation via reflection is required, because ContentParser.FieldParser is private
    ObjectMap parsers = Reflect.get(modParser, "classParsers");
    parsers.each((clazz, fieldParser) -> {
      Method method;
      try { method = fieldParser.getClass().getDeclaredMethod("parse", Class.class, JsonValue.class); } 
      catch (Exception ignored) { return; } // Should not happen
      method.setAccessible(true);
      Serializer existing = json.getSerializer((Class)clazz);
      
      json.setSerializer((Class)clazz, new Serializer(){
        /** Not used */
        @Override
        public void write(Json json, Object object, Class knownType) {
          if (existing != null) existing.write(json, object, knownType);
          else {
            Class<?> actual = object.getClass();
            while (actual.isAnonymousClass() && actual != Object.class) actual = actual.getSuperclass();
            json.writeObjectStart(actual, knownType);
            json.writeFields(object);
            json.writeObjectEnd();
          }
        }
        
        @Override
        public Object read(Json json, JsonValue jsonData, Class type) {
          Throwable last;
          try { return method.invoke(fieldParser, type, jsonData); } 
          catch (InvocationTargetException e) { last = e.getTargetException(); }
          catch (Exception e) { throw new RuntimeException(e); } // Should not happen
          if (existing != null) return existing.read(json, jsonData, type);
          throw new SerializationException(last);
        }
      });
    });
    
    // Add hard-written parsers
    ObjectMap<Class<?>, Func2<String, Integer, ?>> stacks = new ObjectMap<>();
    stacks.put(ItemStack.class, (n, a) -> {
      Item item = Vars.content.item(n);
      return new ItemStack(item == null ? Items.copper : item, a);
    });
    stacks.put(PayloadStack.class, (n, a) -> {
      UnlockableContent content = Vars.content.unit(n);
      if (content == null) content = Vars.content.block(n);
      return new PayloadStack(content == null ? Blocks.router : content, a);
    });
    stacks.put(LiquidStack.class, (n, a) -> {
      Liquid liquid = Vars.content.liquid(n);
      return new LiquidStack(liquid == null ? Liquids.water : liquid, a);
    });
    stacks.put(ConsumeLiquid.class, (n, a) -> {
      Liquid liquid = Vars.content.liquid(n);
      return new ConsumeLiquid(liquid == null ? Liquids.water : liquid, a);
    });
    
    stacks.each((c, s) -> {
      Serializer existing = json.getSerializer(c);
      json.setSerializer(c, new Serializer() {
        @Override
        public void write(Json json, Object object, Class knownType) {
          if (existing != null) existing.write(json, object, knownType);
          else {
            Class<?> actual = object.getClass();
            while (actual.isAnonymousClass() && actual != Object.class) actual = actual.getSuperclass();
            json.writeObjectStart(actual, knownType);
            json.writeFields(object);
            json.writeObjectEnd();
          }
        }
  
        @Override
        public Object read(Json json, JsonValue jsonData, Class type) { 
          if (jsonData.isString() && jsonData.asString().contains("/")) {
            String[] split = jsonData.asString().split("/");
            return s.get(split[0], Strings.parseInt(split[1], 1));
          }
          if (existing != null) return existing.read(json, jsonData, type);
          Object o = instance.newInstance(type); // =/
          json.readFields(o, jsonData);
          return o;
        }
      });  
    });

    json.setSerializer(Rect.class, new Serializer<Rect>() {
      @Override
      public void write(Json json, Rect object, Class knownType) {
        json.writeArrayStart();
        json.writeValue(object.x);
        json.writeValue(object.y);
        json.writeValue(object.width);
        json.writeValue(object.height);
        json.writeArrayEnd();
      }

      @Override
      public Rect read(Json json, JsonValue jsonData, Class type) { 
        if (!jsonData.isArray() || jsonData.size != 4) throw new IllegalArgumentException("Not a Rect");
        return new Rect(jsonData.get(0).asFloat(), jsonData.get(1).asFloat(), 
                        jsonData.get(2).asFloat(), jsonData.get(3).asFloat());
      }
    });
    
    toSearch = Reflect.get(modParser, "typesToSearch");
    contentTypes = Reflect.get(modParser, "contentTypes");
    
    // Custom serializers, mainly used by Building, Unit and subclasses
    json.setSerializer(Vec2.class, new Serializer<Vec2>() {
      @Override
      public void write(Json json, Vec2 object, Class knownType) {
        json.writeObjectStart();
        json.writeValue("x", object.x);
        json.writeValue("y", object.y);
        json.writeObjectEnd();
      }

      @Override
      public Vec2 read(Json json, JsonValue jsonData, Class type) { 
        if (jsonData.isArray()) {
          float[] arr = jsonData.asFloatArray();
          new Vec2(arr[0], arr[1]);
        }
        return new Vec2(jsonData.getFloat("x", 0f), jsonData.getFloat("y", 0f));
      }
    });
    
    json.setSerializer(Tile.class, new Serializer<Tile>() {
      @Override
      public void write(Json json, Tile object, Class knownType) {
        json.writeValue(object.x + ',' + object.y);
      }

      @Override
      public Tile read(Json json, JsonValue jsonData, Class type) {
        if (jsonData.isString()) {
          String value = jsonData.asString();
          int comma = value.indexOf(',');
          if (comma == -1) throw new IllegalArgumentException("Missing comma (',') in coordinates");
          return Vars.world.tile(Integer.parseInt(value.substring(0, comma).trim()), 
                                 Integer.parseInt(value.substring(comma+1).trim()));
        } else if (jsonData.isLong()) 
          return Vars.world.tile(jsonData.asInt());
        return Vars.world.tile(jsonData.getInt("x"), jsonData.getInt("y"));
      }
    });
    
    json.setSerializer(Interval.class, new Serializer<Interval>() {
      @Override
      public void write(Json json, Interval object, Class knownType) {
        json.writeValue(object.getTimes().length);
      }

      @Override
      public Interval read(Json json, JsonValue jsonData, Class type) {
        int times = jsonData.asInt();
        if (times < 1) throw new IllegalArgumentException("'times' must be greater than 1.");
        return new Interval(times); 
      }
    });
    
    json.setSerializer(StatusEntry.class, new Serializer<StatusEntry>() {
      @Override
      public void write(Json json, StatusEntry object, Class knownType) {
        json.writeObjectStart();
        json.writeValue("effect", object.effect);
        json.writeValue("time", object.time);
        json.writeObjectEnd();
      }

      @Override
      public StatusEntry read(Json json, JsonValue jsonData, Class type) { 
        // Do not use pool to avoid a never freed object
        return new StatusEntry().set(json.readValue("effect", StatusEffect.class, jsonData), 
                                     jsonData.getFloat("time"));
      }
    });
    
    json.setSerializer(ItemModule.class, new Serializer<ItemModule>() {
      @Override
      public void write(Json json, ItemModule object, Class knownType) {
        json.writeObjectStart();
        object.each((i, a) -> {
          json.writeValue("item", i.name);
          json.writeValue("amount", a);
        });
        json.writeObjectEnd();
      }

      @Override
      public ItemModule read(Json json, JsonValue jsonData, Class type) { 
        ItemModule module = new ItemModule();
        for (JsonValue entry=jsonData.child; entry!=null; entry=entry.next) {
          ItemStack stack = json.readValue(ItemStack.class, entry);
          module.set(stack.item, stack.amount);
        }
        return module; 
      } 
    });
    json.setSerializer(LiquidModule.class, new Serializer<LiquidModule>() {
      @Override
      public void write(Json json, LiquidModule object, Class knownType) {
        json.writeObjectStart();
        object.each((l, a) -> {
          json.writeValue("liquid", l.name);
          json.writeValue("amount", a);
        });
        json.writeObjectEnd();
      }

      @Override
      public LiquidModule read(Json json, JsonValue jsonData, Class type) { 
        LiquidModule module = new LiquidModule();
        for (JsonValue entry=jsonData.child; entry!=null; entry=entry.next) {
          LiquidStack stack = json.readValue(LiquidStack.class, entry);
          module.set(stack.liquid, stack.amount);
        }
        return module; 
      } 
    });
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public <T> T readValue(Class<T> type, Class elementType, JsonValue jsonData, Class keyType) {
    if (type == UnlockableContent.class) {
      if (toSearch != null) {
        for (ContentType c : toSearch) {
          UnlockableContent found = Vars.content.getByName(c, jsonData.asString());
          if (found != null) { return (T)found; }
        }
        throw new IllegalArgumentException("\"" + jsonData.name + "\": No content found with name '" + 
                                           jsonData.asString() + "'.");
      }
      
    } else if (MappableContent.class.isAssignableFrom(type) && contentTypes != null) {
      ContentType ctype = contentTypes.getThrow(type, () -> 
        new IllegalArgumentException("No content type for class: " + type.getSimpleName()));
      T content = (T)Vars.content.getByName(ctype, jsonData.asString());
      if (content != null) return content;
      throw new IllegalArgumentException("\"" + jsonData.name + "\": No " + ctype + " found with name '" + 
                                         jsonData.asString()  + "'.\nMake sure '" + jsonData.asString() + 
                                         "' is spelled correctly, and that it really exists!");
      
    } else if (Entityc.class.isAssignableFrom(type)) {
      Entityc entity = Groups.all.getByID(jsonData.asInt());
      if (entity == null) throw new IllegalArgumentException("No entity found with id " + jsonData.asInt());
      return (T)entity;
    }

    return super.readValue(type, elementType, jsonData, keyType);
  }
  
  @Override
  public void writeValue(Object value, Class knownType, Class elementType) {
    if (value instanceof MappableContent) {
      try { getWriter().value(((MappableContent)value).name); } 
      catch (Exception e) { throw new RuntimeException(e); }
    } else if (value instanceof Entityc) {
      try { getWriter().value(((Entityc)value).id()); } 
      catch (Exception e) { throw new RuntimeException(e); }
    } else super.writeValue(value, knownType, elementType);
  }

  @Override
  protected String convertToString(Object object) {
    if (object instanceof MappableContent) 
      return ((MappableContent)object).name;
    if (object instanceof Entityc)
      return Integer.toString(((Entityc)object).id());
    return super.convertToString(object);
  }
  
  // end region
  // region redefinition
  // Redefinition of some fields as they are all private instead of protected...
  
  protected final ObjectMap<Class, OrderedMap<String, FieldMetadata>> typeToFields = new ObjectMap<>();
  public boolean ignoreTransiant/* = true*/, ignoreDeprecated, readDeprecated;
  public String typeName = "class";
  
  public void setTypeName(String typeName) { 
    this.typeName = typeName; 
    super.setTypeName(typeName);
  }

  public void setIgnoreDeprecated(boolean ignoreDeprecated) {
    this.ignoreDeprecated = ignoreDeprecated;
    super.setIgnoreDeprecated(ignoreDeprecated);
  }

  public void setReadDeprecated(boolean readDeprecated) {
    this.readDeprecated = readDeprecated;
    super.setReadDeprecated(readDeprecated);
  }
  
  // end region
  
  public <T> T readField(Class type, String name, JsonValue jsonData) {
    return readField(type, name, name, null, jsonData);
  }

  public <T> T readField(Class type, String name, Class elementType, JsonValue jsonData) {
    return readField(type, name, name, elementType, jsonData);
  }

  public <T> T readField(Class type, String fieldName, String jsonName, JsonValue jsonData) {
    return readField(type, fieldName, jsonName, null, jsonData);
  }

  /** @param elementType May be null if the type is unknown. */
  public <T> T readField(Class type, String fieldName, String jsonName, Class elementType, JsonValue jsonMap) {
    ObjectMap<String, FieldMetadata> fields = getFields(type);
    FieldMetadata metadata = fields.get(fieldName);
    if(metadata == null) throw new SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")");
    if(elementType == null) elementType = metadata.elementType;
    return readField(metadata.field, jsonName, elementType, jsonMap);
  }

  /**
   * @param object May be null if the field is static.
   * @param elementType May be null if the type is unknown.
   */
  @SuppressWarnings("unchecked")
  public <T> T readField(Field field, String jsonName, Class elementType, JsonValue jsonMap) {
    JsonValue jsonValue = jsonMap.get(jsonName);
    if(jsonValue == null) return null;
    try {
      return (T)readValue(field.getType(), elementType, jsonValue);
    } catch (SerializationException e) {
      e.addTrace(field.getName() + " (" + field.getDeclaringClass().getName() + ")");
      throw e;
    } catch (RuntimeException re) {
      SerializationException e = new SerializationException(re);
      e.addTrace(jsonValue.trace());
      e.addTrace(field.getName() + " (" + field.getDeclaringClass().getName() + ")");
      throw e;
    }
  }

  public ObjectMap<Field, Object> readFields(Class type, JsonValue jsonMap) {
    ObjectMap<String, FieldMetadata> fields = getFields(type);
    ObjectMap<Field, Object> reads = new ObjectMap<>(fields.size); //i think ordered is useless
    for (JsonValue child=jsonMap.child; child!=null; child=child.next) {
      FieldMetadata metadata = fields.get(child.name().replace(" ", "_"));
      if (metadata == null) {
        if (child.name.equals(typeName)) continue;
        if (getIgnoreUnknownFields() || ignoreUnknownField(type, child.name)) continue;
        SerializationException e = new SerializationException("Field not found: " + child.name + " (" + type.getName() + ")");
        e.addTrace(child.trace());
        throw e;
      }
      
      Field field = metadata.field;
      try {
        reads.put(field, readValue(field.getType(), metadata.elementType, child, metadata.keyType));
      } catch (SerializationException e) {
        e.addTrace(field.getName() + " (" + type.getName() + ")");
        throw e;
      } catch (RuntimeException re) {
        SerializationException e = new SerializationException(re);
        e.addTrace(child.trace());
        e.addTrace(field.getName() + " (" + type.getName() + ")");
        throw e;
      }
    }
    return reads;
  }

  /** Same as {@link super#getFields(Class)} but can read {@code transient} fields. */
  @SuppressWarnings("deprecation")
  public OrderedMap<String, FieldMetadata> getFields(Class type) {
    OrderedMap<String, FieldMetadata> fields = typeToFields.get(type);
    if (fields != null) return fields;
    
    Seq<Class> classHierarchy = new Seq<>();
    Class nextClass = type;
    while (nextClass != Object.class) {
      classHierarchy.add(nextClass);
      nextClass = nextClass.getSuperclass();
    }
    Seq<Field> allFields = new Seq<>();
    for (int i = classHierarchy.size - 1; i >= 0; i--) 
      allFields.addAll(classHierarchy.get(i).getDeclaredFields());
    
    OrderedMap<String, FieldMetadata> nameToField = new OrderedMap<>(allFields.size);
    for (Field field : allFields) {
      if (ignoreTransiant && Modifier.isTransient(field.getModifiers())) continue;
      if (Modifier.isStatic(field.getModifiers())) continue;
      if (field.isSynthetic() || type.isEnum() || Reflect.isWrapper(type)) continue;
      
      // this is deprecated, but I know what I'm doing
      if (!field.isAccessible()) {
        try {
          field.setAccessible(true);
        } catch (Exception ex) {
          continue;
        }
      }
      
      if (ignoreDeprecated && !readDeprecated && field.isAnnotationPresent(Deprecated.class)) continue;
      nameToField.put(field.getName(), new FieldMetadata(field));
    }
    
    typeToFields.put(type, nameToField);
    return nameToField;
  }
}