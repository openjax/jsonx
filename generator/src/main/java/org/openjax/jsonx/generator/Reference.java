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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import org.libx4j.jsonx.jsonx_0_9_8.xL2gluGCXYYJc.$Array;
import org.libx4j.jsonx.jsonx_0_9_8.xL2gluGCXYYJc.$MaxCardinality;
import org.libx4j.jsonx.jsonx_0_9_8.xL2gluGCXYYJc.$Reference;
import org.openjax.jsonx.runtime.Use;
import org.w3.www._2001.XMLSchema.yAA.$Boolean;
import org.w3.www._2001.XMLSchema.yAA.$NonNegativeInteger;

final class Reference extends Member {
  private final Member model;
  private final Id id;

  public Reference(final Registry registry, final $Array.Reference binding, final Member model) {
    super(registry, binding.getNullable$(), binding.getMinOccurs$(), binding.getMaxOccurs$());
    this.model = model;
    this.id = new Id(this);
  }

  public Reference(final Registry registry, final $Reference binding, final Model model) {
    super(registry, binding.getName$(), binding.getUse$());
    this.model = model;
    this.id = new Id(this);
  }

  public Reference(final Registry registry, final String name, final Use use, final Model model) {
    super(registry, name, null, use, null, null);
    this.model = model;
    this.id = new Id(this);
  }

  public Reference(final Registry registry, final boolean nullable, final Integer minOccurs, final Integer maxOccurs, final Model model) {
    super(registry, null, nullable, null, minOccurs, maxOccurs);
    this.model = model;
    this.id = new Id(this);
  }

  public Reference(final Registry registry, final $Boolean nullable, final $NonNegativeInteger minOccurs, final $MaxCardinality maxOccurs, final Model model) {
    super(registry, nullable, minOccurs, maxOccurs);
    this.model = model;
    this.id = new Id(this);
  }

  @Override
  protected Id id() {
    return id;
  }

  public Member reference() {
    return this.model;
  }

  @Override
  protected Registry.Type type() {
    return model.type();
  }

  @Override
  protected String elementName() {
    return "reference";
  }

  @Override
  protected Class<? extends Annotation> propertyAnnotation() {
    return model.propertyAnnotation();
  }

  @Override
  protected Class<? extends Annotation> elementAnnotation() {
    return model.elementAnnotation();
  }

  @Override
  protected org.fastjax.xml.Element toXml(final Settings settings, final Element owner, final String packageName) {
    final Map<String,String> attributes = super.toXmlAttributes(owner, packageName);
    if (!registry.isRootMember(model, settings)) {
      final org.fastjax.xml.Element element = model.toXml(settings, owner, packageName);
      // It is necessary to remove the nullable, required, minOccurs and maxOccurs attributes,
      // because the template object is responsible for these attributes, and it may have happened
      // that when the reflection mechanism constructed the model, it used a declaration that had
      // these attributes set as well
      element.getAttributes().remove("minOccurs");
      element.getAttributes().remove("maxOccurs");
      element.getAttributes().remove("nullable");
      element.getAttributes().remove("use");
      element.getAttributes().putAll(attributes);
      return element;
    }

    if (model != null)
      attributes.put("type", Registry.getSubName(model.id().toString(), packageName));

    if (!(owner instanceof ObjectModel))
      return new org.fastjax.xml.Element(elementName(), attributes, null);

    attributes.put("xsi:type", elementName());
    return new org.fastjax.xml.Element("property", attributes, null);
  }

  @Override
  protected void toAnnotationAttributes(final AttributeMap attributes, final Member owner) {
    super.toAnnotationAttributes(attributes, owner);
    model.toAnnotationAttributes(attributes, owner);
  }

  @Override
  protected List<AnnotationSpec> toElementAnnotations() {
    return model.toElementAnnotations();
  }
}