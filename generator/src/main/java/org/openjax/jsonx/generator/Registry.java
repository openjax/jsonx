/* Copyright (c) 2017 OpenJAX
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

package org.openjax.jsonx.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.fastjax.util.Classes;
import org.fastjax.util.Strings;
import org.openjax.jsonx.jsonx_0_9_8.xL3gluGCXYYJc.$Object;
import org.openjax.jsonx.jsonx_0_9_8.xL3gluGCXYYJc.Jsonx;

class Registry {
  final class Value {
    private final String name;

    private Value(final String name) {
      if (refToModel.containsKey(name))
        throw new IllegalArgumentException("Value name=\"" + name + "\" already registered");

      refToModel.put(name, null);
      this.name = name;
    }

    <T extends Model>T value(final T model, final Referrer<?> referrer) {
      refToModel.put(name, model);
      return reference(model, referrer);
    }
  }

  enum Kind {
    CLASS("class"),
    ANNOTATION("@interface");

    private final String value;

    Kind(final String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  final class Type {
    private final Kind kind;
    private final String packageName;
    private final String canonicalPackageName;
    private final String simpleName;
    private final String compoundName;
    private final String canonicalCompoundName;
    private final String name;
    private final String canonicalName;
    private final Type superType;
    private final Type genericType;

    private Type(final Kind kind, final String packageName, final String compoundName, final Type superType, final Type genericType) {
      this.kind = kind;
      final boolean defaultPackage = packageName.length() == 0;
      final int dot = compoundName.lastIndexOf('.');
      this.packageName = packageName;
      this.canonicalPackageName = dot == -1 ? packageName : defaultPackage ? compoundName.substring(0, dot) : packageName + "." + compoundName.substring(0, dot);
      this.compoundName = compoundName;
      this.name = defaultPackage ? compoundName : packageName + "." + compoundName;
      this.canonicalCompoundName = Classes.toCanonicalClassName(compoundName);
      this.simpleName = canonicalCompoundName.substring(canonicalCompoundName.lastIndexOf('.') + 1);
      this.canonicalName = defaultPackage ? canonicalCompoundName : packageName + "." + canonicalCompoundName;
      this.superType = superType;
      this.genericType = genericType;
      qualifiedNameToType.put(toCanonicalString(), this);
    }

    private Type(final Class<?> cls, final Type genericType) {
      this(cls.isAnnotation() ? Kind.ANNOTATION : Kind.CLASS, cls.getPackage().getName(), cls.getSimpleName(), cls.getSuperclass() == null ? null : cls.getSuperclass() == Object.class ? null : new Type(cls.getSuperclass(), null), genericType);
    }

    Type getSuperType() {
      return superType;
    }

    Type getDeclaringType() {
      final String declaringClassName = Classes.getDeclaringClassName(compoundName);
      return compoundName.length() == declaringClassName.length() ? null : getType(Kind.CLASS, packageName, declaringClassName, null, null, null);
    }

    Type getGreatestCommonSuperType(final Type type) {
      Type a = this;
      do {
        Type b = type;
        do {
          if (a.name.equals(b.name))
            return a;

          b = b.superType;
        }
        while (b != null);
        a = a.superType;
      }
      while (a != null);
      return OBJECT;
    }

    Kind getKind() {
      return this.kind;
    }

    String getPackage() {
      return packageName;
    }

    String getCanonicalPackage() {
      return canonicalPackageName;
    }

    String getName() {
      return name;
    }

    String getSimpleName() {
      return simpleName;
    }

    String getCanonicalName() {
      return canonicalName;
    }

    String getCompoundName() {
      return compoundName;
    }

    String getCanonicalCompoundName() {
      return canonicalCompoundName;
    }

    String getRelativeName(final String packageName) {
      return packageName.length() == 0 ? name : name.substring(packageName.length() + 1);
    }

    String getSubName(final String superName) {
      return Registry.getSubName(name, superName);
    }

    String toCanonicalString() {
      final StringBuilder builder = new StringBuilder(canonicalName);
      if (genericType != null)
        builder.append('<').append(genericType.toCanonicalString()).append('>');

      return builder.toString();
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this)
        return true;

      if (!(obj instanceof Type))
        return false;

      return toString().equals(obj.toString());
    }

    @Override
    public int hashCode() {
      return toString().hashCode();
    }

    @Override
    public String toString() {
      final StringBuilder builder = new StringBuilder(name);
      if (genericType != null)
        builder.append('<').append(genericType.toString()).append('>');

      return builder.toString();
    }
  }

  static String getSubName(final String name, final String superName) {
    return superName != null && name.startsWith(superName) ? name.substring(superName.length() + 1) : name;
  }

  Type getType(final Kind kind, final String packageName, final String compoundName) {
    return getType(kind, packageName, compoundName, null, null, null);
  }

  Type getType(final String packageName, final String compoundName, final String superCompoundName) {
    return getType(Kind.CLASS, packageName, compoundName, packageName, superCompoundName, (Type)null);
  }

  Type getType(final Kind kind, final String packageName, final String compoundName, final String superPackageName, final String superCompoundName, final Type genericType) {
    final StringBuilder className = new StringBuilder();
    if (packageName.length() > 0)
      className.append(packageName).append(".");

    className.append(compoundName);
    if (genericType != null)
      className.append('<').append(genericType.toCanonicalString()).append('>');

    final Type type = qualifiedNameToType.get(className.toString());
    return type != null ? type : new Type(kind, packageName, compoundName, superCompoundName == null ? null : getType(Kind.CLASS, superPackageName, superCompoundName, null, null, null), genericType);
  }

  Type getType(final Class<?> cls, final Type genericType) {
    final String name = cls.getName() + "<" + (genericType != null ? genericType.toCanonicalString() : Object.class.getName()) + ">";
    final Type type = qualifiedNameToType.get(name);
    return type != null ? type : new Type(cls, genericType);
  }

  Type getType(final Class<?> cls) {
    return getType(cls.isAnnotation() ? Kind.ANNOTATION : Kind.CLASS, cls.getPackage().getName(), Classes.getCompoundName(cls), cls.getSuperclass() == null ? null : cls.getSuperclass().getPackage().getName(), cls.getSuperclass() == null ? null : Classes.getCompoundName(cls.getSuperclass()), (Type)null);
  }

  Type getType(final Class<?> ... generics) {
    return getType(0, generics);
  }

  private Type getType(final int index, final Class<?> ... generics) {
    return index == generics.length - 1 ? getType(generics[index]) : getType(generics[index], getType(index + 1, generics));
  }

  private final LinkedHashMap<String,Model> refToModel = new LinkedHashMap<>();
  private final LinkedHashMap<String,ReferrerManifest> refToReferrers = new LinkedHashMap<>();

  final String packageName;

  Registry(final String packageName) {
    this.packageName = packageName;
  }

  Registry(final Set<Class<?>> classes) {
    for (final Class<?> cls : classes) {
      if (cls.isAnnotation())
        ArrayModel.referenceOrDeclare(this, cls);
      else
        ObjectModel.referenceOrDeclare(this, cls);
    }

    this.packageName = getClassPrefix();
  }

  private String getClassPrefix() {
    final Set<Registry.Type> types = new HashSet<>();
    if (getModels() != null)
      for (final Model member : getModels())
        member.getDeclaredTypes(types);

    final String classPrefix = Strings.getCommonPrefix(types.stream().map(t -> t.getPackage()).toArray(String[]::new));
    if (classPrefix == null)
      return null;

    return classPrefix;
  }

  private static class ReferrerManifest {
    final List<Referrer<?>> referrers = new ArrayList<>();
    final Set<Class<?>> referrerTypes = new HashSet<>();

    void add(final Referrer<?> referrer) {
      referrers.add(referrer);
      referrerTypes.add(referrer.getClass());
    }

    int getNumReferrers() {
      return referrers.size();
    }

    boolean hasReferrerType(final Class<?> type) {
      return referrerTypes.contains(type);
    }
  }

  private final HashMap<String,Type> qualifiedNameToType = new HashMap<>();
  final Type OBJECT = getType(Object.class);

  Value declare(final Jsonx.BooleanType binding) {
    return new Value(binding.getName$().text());
  }

  Value declare(final Jsonx.NumberType binding) {
    return new Value(binding.getName$().text());
  }

  Value declare(final Jsonx.StringType binding) {
    return new Value(binding.getName$().text());
  }

  Value declare(final Jsonx.ArrayType binding) {
    return new Value(binding.getName$().text());
  }

  Value declare(final Jsonx.ObjectType binding) {
    return new Value(binding.getName$().text());
  }

  Value declare(final Id id) {
    return new Value(id.toString());
  }

  Value declare(final $Object binding) {
    return new Value(ObjectModel.getFullyQualifiedName(binding));
  }

  <T extends Member>T reference(final T model, final Referrer<?> referrer) {
    if (referrer == null)
      return model;

    final String key = model.id().toString();
    ReferrerManifest referrers = refToReferrers.get(key);
    if (referrers == null)
      refToReferrers.put(key, referrers = new ReferrerManifest());

    referrers.add(referrer);
    return model;
  }

  Model getModel(final Id id) {
    return refToModel.get(id.toString());
  }

  Collection<Model> getModels() {
    return refToModel.values();
  }

  boolean isRootMember(final Member member, final Settings settings) {
    final ReferrerManifest referrers = refToReferrers.get(member.id().toString());
    final int numReferrers = referrers == null ? 0 : referrers.getNumReferrers();
    if (member instanceof ArrayModel)
      return ((ArrayModel)member).classType() != null;

    if (member instanceof ObjectModel)
      return numReferrers == 0 || numReferrers > 1 || referrers.hasReferrerType(ArrayModel.class);

    return numReferrers >= settings.getTemplateThreshold();
  }
}