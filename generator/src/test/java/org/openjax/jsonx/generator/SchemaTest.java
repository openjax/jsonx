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

package org.openjax.jsonx.generator;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openjax.standard.jci.CompilationException;
import org.openjax.standard.jci.InMemoryCompiler;
import org.openjax.standard.lang.PackageNotFoundException;
import org.openjax.standard.test.AssertXml;
import org.openjax.standard.util.Classes;
import org.openjax.standard.xml.api.Element;
import org.openjax.standard.xml.api.ValidationException;
import org.openjax.standard.xml.sax.Validator;
import org.openjax.jsonx.jsonx_0_9_8.xL3gluGCXYYJc.Jsonx;
import org.openjax.xsb.runtime.Bindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class SchemaTest {
  private static final Logger logger = LoggerFactory.getLogger(SchemaTest.class);
  private static final URL jsonxXsd = Thread.currentThread().getContextClassLoader().getResource("jsonx.xsd");
  private static final File generatedSourcesDir = new File("target/generated-test-sources/jsonx");
  private static final File generatedResourcesDir = new File("target/generated-test-resources");
  private static final File compiledClassesDir = new File("target/test-classes");
  private static final List<Settings> settings = new ArrayList<>();

  static {
    generatedSourcesDir.mkdirs();
    generatedResourcesDir.mkdirs();
    try {
      Files.walk(generatedSourcesDir.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).filter(f -> !f.equals(generatedSourcesDir)).forEach(File::delete);
    }
    catch (final IOException e) {
      throw new ExceptionInInitializerError(e);
    }

    for (int i = 0; i < 10; ++i)
      settings.add(new Settings(i, 2));

    settings.add(new Settings(Integer.MAX_VALUE, 2));
  }

  private static Jsonx newControlBinding(final String fileName) throws IOException, ValidationException {
    try (final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
      return (Jsonx)Bindings.parse(in);
    }
  }

  private static Element toXml(final Schema schema, final Settings settings) {
    final Element xml = schema.toXml(settings);
    xml.getAttributes().put("xsi:schemaLocation", "http://jsonx.openjax.org/jsonx-0.9.8.xsd " + jsonxXsd);
    return xml;
  }

  private static Schema testParseJsonx(final Jsonx controlBinding, final String pkg) throws IOException, SAXException {
    logger.info("  Parse XML...");
    logger.info("    a) XML(1) -> JSONX");
    final Schema controlSchema = new Schema(controlBinding, pkg);
    logger.info("    b) JSONX -> XML(2)");
    final String xml = toXml(controlSchema, Settings.DEFAULT).toString();
    final Jsonx testBinding = (Jsonx)Bindings.parse(xml);
    logger.info("    c) XML(1) == XML(2)");
    AssertXml.compare(controlBinding.toDOM(), testBinding.toDOM()).assertEqual(true);
    return controlSchema;
  }

  private static void writeFile(final String fileName, final String data) throws IOException {
    try (final OutputStreamWriter out = new FileWriter(new File(generatedResourcesDir, fileName))) {
      out.write("<!--\n");
      out.write("  Copyright (c) 2017 OpenJAX\n\n");
      out.write("  Permission is hereby granted, free of charge, to any person obtaining a copy\n");
      out.write("  of this software and associated documentation files (the \"Software\"), to deal\n");
      out.write("  in the Software without restriction, including without limitation the rights\n");
      out.write("  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n");
      out.write("  copies of the Software, and to permit persons to whom the Software is\n");
      out.write("  furnished to do so, subject to the following conditions:\n\n");
      out.write("  The above copyright notice and this permission notice shall be included in\n");
      out.write("  all copies or substantial portions of the Software.\n\n");
      out.write("  You should have received a copy of The MIT License (MIT) along with this\n");
      out.write("  program. If not, see <http://opensource.org/licenses/MIT/>.\n");
      out.write("-->\n");
      out.write(data);
    }
  }

  // This is necessary for jdk1.8, and is replaced with ClassLoader#getDeclaredPackage() in jdk9
  private static Package getPackage(final ClassLoader classLoader, final String packageName) {
    try {
      final Method method = Classes.getDeclaredMethodDeep(classLoader.getClass(), "getPackage", String.class);
      method.setAccessible(true);
      return (Package)method.invoke(classLoader, packageName);
    }
    catch (final IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Schema newSchema(final ClassLoader classLoader, final String packageName) throws IOException, PackageNotFoundException {
    return new Schema(getPackage(classLoader, packageName), classLoader, c -> c.getClassLoader() == classLoader);
  }

  private static void assertSources(final Map<String,String> expected, final Map<String,String> actual) {
    for (final Map.Entry<String,String> entry : expected.entrySet())
      assertEquals(entry.getValue(), actual.get(entry.getKey()));

    try {
      assertEquals(expected.size(), actual.size());
    }
    catch (final AssertionError e) {
      if (expected.size() < actual.size()) {
        actual.keySet().removeAll(expected.keySet());
        logger.error(actual.toString());
      }
      else {
        expected.keySet().removeAll(actual.keySet());
        logger.error(expected.toString());
      }

      throw e;
    }
  }

  public static void test(final String fileName, final String pkg) throws ClassNotFoundException, CompilationException, IOException, PackageNotFoundException, SAXException {
    logger.info(fileName + "...");
    final Jsonx controlBinding = newControlBinding(fileName);
    final Schema controlSchema = testParseJsonx(controlBinding, pkg);

    logger.info("  4) JSONX -> Java(1)");
    final Map<String,String> test1Sources = controlSchema.toSource(generatedSourcesDir);
    final InMemoryCompiler compiler = new InMemoryCompiler();
    for (final Map.Entry<String,String> entry : test1Sources.entrySet())
      compiler.addSource(entry.getValue());

    logger.info("  5) -- Java(1) Compile --");
    final ClassLoader classLoader = compiler.compile(compiledClassesDir, "-g");

    logger.info("  6) Java(1) -> JSONX");
    final Schema test1Schema = newSchema(classLoader, pkg);
    final String xml = toXml(test1Schema, Settings.DEFAULT).toString();
    logger.info("  7) Validate JSONX");
    writeFile("out-" + fileName, xml);
    try {
      Validator.validate(xml, false);
    }
    catch (final SAXException e) {
      logger.error(xml);
      throw e;
    }

    final Schema test2Schema = testParseJsonx((Jsonx)Bindings.parse(xml), pkg);
    logger.info("  8) JSONX -> Java(2)");
    final Map<String,String> test2Sources = test2Schema.toSource();
    logger.info("  9) Java(1) == Java(2)");
    assertSources(test1Sources, test2Sources);

    testSettings(fileName, pkg, test1Sources);
  }

  private static void testSettings(final String fileName, final String pkg, final Map<String,String> originalSources) throws ClassNotFoundException, CompilationException, IOException, PackageNotFoundException, ValidationException {
    for (final Settings settings : SchemaTest.settings) {
      logger.info("   testSettings(\"" + fileName + "\", new Settings(" + settings.getTemplateThreshold() + "))");
      final Jsonx controlBinding = newControlBinding(fileName);
      final Schema controlSchema = new Schema(controlBinding, pkg);
      writeFile("a" + settings.getTemplateThreshold() + fileName, toXml(controlSchema, settings).toString());
      final Map<String,String> test1Sources = controlSchema.toSource(generatedSourcesDir);
      final InMemoryCompiler compiler = new InMemoryCompiler();
      for (final Map.Entry<String,String> entry : test1Sources.entrySet())
        compiler.addSource(entry.getValue());

      assertSources(originalSources, test1Sources);

      final ClassLoader classLoader = compiler.compile(null);
      final Schema test1Schema = newSchema(classLoader, pkg);
      final String schema = toXml(test1Schema, settings).toString();
      writeFile("b" + settings.getTemplateThreshold() + fileName, schema);
      final Schema test2Schema = new Schema((Jsonx)Bindings.parse(schema), pkg);
      final Map<String,String> test2Sources = test2Schema.toSource();
      assertSources(test1Sources, test2Sources);
    }
  }

  @Test
  public void testArray() throws ClassNotFoundException, CompilationException, IOException, MalformedURLException, PackageNotFoundException, SAXException {
    test("array.jsonx", "org.openjax.jsonx.generator");
  }

  @Test
  public void testDataType() throws ClassNotFoundException, CompilationException, IOException, MalformedURLException, PackageNotFoundException, SAXException {
    test("datatype.jsonx", "org.openjax.jsonx.generator.datatype");
  }

  @Test
  public void testTemplate() throws ClassNotFoundException, CompilationException, IOException, MalformedURLException, PackageNotFoundException, SAXException {
    test("template.jsonx", "org.openjax.jsonx.generator");
  }

  @Test
  public void testReference() throws ClassNotFoundException, CompilationException, IOException, MalformedURLException, PackageNotFoundException, SAXException {
    test("reference.jsonx", "org.openjax.jsonx.generator.reference");
  }

  @Test
  public void testReserved() throws ClassNotFoundException, CompilationException, IOException, MalformedURLException, PackageNotFoundException, SAXException {
    test("reserved.jsonx", "org.openjax.jsonx.generator.reserved");
  }

  @Test
  public void testComplete() throws ClassNotFoundException, CompilationException, IOException, MalformedURLException, PackageNotFoundException, SAXException {
    test("complete.jsonx", "org.openjax.jsonx.generator.complete");
  }
}