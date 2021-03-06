/* Copyright (c) 2018 JSONx
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.jsonx;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jsonx.ArrayValidator.Relation;
import org.jsonx.ArrayValidator.Relations;
import org.libj.lang.Classes;
import org.libj.util.Patterns;
import org.openjax.json.JsonReader;
import org.openjax.json.JsonUtil;

class StringCodec extends PrimitiveCodec {
  static Object decodeObject(final Class<?> type, final Executable decode, final String json) {
    final StringBuilder unescaped = new StringBuilder(json.length() - 2);
    for (int i = 1, len = json.length() - 1; i < len; ++i) {
      char ch = json.charAt(i);
      if (ch == '\\') {
        ch = json.charAt(++i);
        if (ch != '"' && ch != '\\')
          unescaped.append('\\');
      }

      unescaped.append(ch);
    }

    final String string = unescaped.toString();
    final Object value;
    if (decode != null) {
      try {
        value = JsdUtil.invoke(decode, string);
      }
      catch (final Exception e) {
        return Error.CONTENT_NOT_EXPECTED(json, null);
      }
    }
    else {
      value = string;
    }

    try {
      return value == null || Classes.isInstance(type, value) ? value : Classes.newInstance(type, value);
    }
    catch (final IllegalAccessException | InstantiationException e) {
      throw new RuntimeException(e);
    }
    catch (final InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException)
        throw (RuntimeException)e.getCause();

      throw new RuntimeException(e.getCause());
    }
  }

  static Object decodeArray(final Class<?> type, final String decode, final String token) {
    return token.charAt(0) == '"' && token.charAt(token.length() - 1) == '"' ? StringCodec.decodeObject(type, getMethod(decodeToMethod, decode, String.class), token) : null;
  }

  static Error encodeArray(final Annotation annotation, final String pattern, final Class<?> type, final String encode, Object object, final int index, final Relations relations, final boolean validate) {
    if (!Classes.isInstance(type, object))
      return Error.CONTENT_NOT_EXPECTED(object, null);

    final Executable method = getMethod(encodeToMethod, encode, type);
    if (method != null)
      object = JsdUtil.invoke(method, object);

    final String string = object.toString();
    if (validate && !pattern.isEmpty() && !Patterns.compile(pattern).matcher(string).matches())
      return Error.PATTERN_NOT_MATCHED(pattern, string, null);

    relations.set(index, new Relation(encodeObject(string), annotation));
    return null;
  }

  static String encodeObject(final String value) throws EncodeException {
    return JsonUtil.escape(value).insert(0, '"').append('"').toString();
  }

  static Object encodeObject(final Annotation annotation, final Method getMethod, final String pattern, final Class<?> type, final String encode, Object object, final boolean validate) throws EncodeException {
    if (!Classes.isInstance(type, object))
      return Error.CONTENT_NOT_EXPECTED(object, null);

    final Executable method = getMethod(encodeToMethod, encode, object.getClass());
    if (method != null)
      object = JsdUtil.invoke(method, object);

    final String str = object == null ? null : object.toString();
    if (validate) {
      final Error error = validate(annotation, getMethod, str, pattern);
      if (error != null)
        return error;
    }

    return encodeObject(str);
  }

  private static Error validate(final Annotation annotation, final Method getMethod, final String object, final String pattern) {
    return pattern.isEmpty() || Patterns.compile(pattern).matcher(object).matches() ? null : Error.PATTERN_NOT_MATCHED_ANNOTATION(annotation, getMethod, object);
  }

  private final Pattern pattern;

  StringCodec(final StringProperty property, final Method getMethod, final Method setMethod) {
    super(getMethod, setMethod, property.name(), property.nullable(), property.use(), property.decode());
    try {
      this.pattern = property.pattern().isEmpty() ? null : Patterns.compile(property.pattern());
    }
    catch (final PatternSyntaxException e) {
      throw new ValidationException("Malformed pattern: " + property.pattern());
    }
  }

  @Override
  boolean test(final char firstChar) {
    return firstChar == '"';
  }

  @Override
  Error validate(final String json, final JsonReader reader) {
    if ((json.charAt(0) != '"' || json.charAt(json.length() - 1) != '"') && (json.charAt(0) != '\'' || json.charAt(json.length() - 1) != '\''))
      return Error.NOT_A_STRING();

    final String value = json.substring(1, json.length() - 1);
    if (pattern != null && !pattern.matcher(value).matches())
      return Error.PATTERN_NOT_MATCHED(pattern.pattern(), value, reader);

    return null;
  }

  @Override
  Object parseValue(final String json) {
    return decodeObject(type(), decode(), json);
  }

  @Override
  String elementName() {
    return "string";
  }
}