/* Copyright (c) 2018 OpenJAX
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

package org.openjax.jsonx.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.regex.PatternSyntaxException;

import org.openjax.jsonx.runtime.ArrayValidator.Relation;
import org.openjax.jsonx.runtime.ArrayValidator.Relations;
import org.openjax.standard.json.JsonStrings;

class StringCodec extends PrimitiveCodec<String> {
  static String decodeArray(final String token) {
    return token.charAt(0) == '"' && token.charAt(token.length() - 1) == '"' ? StringCodec.decodeObject(token) : null;
  }

  static String decodeObject(final String json) {
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

    return unescaped.toString();
  }

  static Error encodeArray(final Annotation annotation, final String pattern, final Object object, final int index, final Relations relations, final boolean validate) {
    if (!(object instanceof String))
      return Error.CONTENT_NOT_EXPECTED(object);

    final String string = (String)object;
    if (validate) {
      if (pattern.length() != 0 && !string.matches(pattern))
        return Error.PATTERN_NOT_MATCHED(pattern, string);
    }

    relations.set(index, new Relation(false ? StringCodec.decodeObject("\"" + string + "\"") : object, annotation));
    return null;
  }

  static String encodeObject(final String string) throws EncodeException {
    return JsonStrings.escape(string).insert(0, '"').append('"').toString();
  }

  static Object encodeObject(final Annotation annotation, final String pattern, final String object, final boolean validate) throws EncodeException {
    if (validate) {
      final Error error = validate(annotation, object, pattern);
      if (error != null)
        return error;
    }

    return encodeObject(object);
  }

  private static Error validate(final Annotation annotation, final String object, final String pattern) {
    if (pattern.length() > 0 && !object.matches(pattern))
      return Error.PATTERN_NOT_MATCHED_ANNOTATION(annotation, object);

    return null;
  }

  private final String pattern;

  StringCodec(final StringProperty property, final Field field) {
    super(field, property.name(), property.nullable(), property.use());
    this.pattern = property.pattern().length() == 0 ? null : property.pattern();
  }

  @Override
  boolean test(final char firstChar) {
    return firstChar == '"';
  }

  @Override
  Error validate(final String token) {
    if ((token.charAt(0) != '"' || token.charAt(token.length() - 1) != '"') && (token.charAt(0) != '\'' || token.charAt(token.length() - 1) != '\''))
      return Error.NOT_A_STRING;

    final String value = token.substring(1, token.length() - 1);
    if (pattern != null) {
      try {
        if (!value.matches(pattern))
          return Error.PATTERN_NOT_MATCHED(pattern, token);
      }
      catch (final PatternSyntaxException e) {
        throw new ValidationException("Malformed pattern: " + pattern);
      }
    }

    return null;
  }

  @Override
  String parse(final String json) {
    return decodeObject(json);
  }

  @Override
  String elementName() {
    return "string";
  }
}