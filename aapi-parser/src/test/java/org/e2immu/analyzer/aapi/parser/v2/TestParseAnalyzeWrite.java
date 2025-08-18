package org.e2immu.analyzer.aapi.parser.v2;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfigurationImpl;
import org.e2immu.analyzer.aapi.parser.AnnotatedApiParser;
import org.e2immu.analyzer.aapi.parser.WriteDecoratedAAPI;
import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.ToolChain;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.util.internal.util.Trie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.constant.Constable;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.http.HttpRequest;
import java.security.Guard;
import java.security.MessageDigest;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

import static org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer.AnnotationOrigin.*;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestParseAnalyzeWrite {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TestParseAnalyzeWrite.class);
    public static final String NOTE_CHARSEQUENCE = " NOTE: can we demand that the result is @Independent?";
    public static final String JDK_PACKAGE = "org.e2immu.analyzer.aapi.archive.v2.jdk";

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @Test
    public void test() throws IOException {
        AnnotatedApiParser annotatedApiParser = new AnnotatedApiParser();
        annotatedApiParser.initialize(new InputConfigurationImpl.Builder()
                        .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                        .addClassPath(ToolChain.CLASSPATH_SLF4J_LOGBACK)
                        .addClassPath(ToolChain.CLASSPATH_JUNIT)
                        .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                        .addSources("../e2immu-aapi-archive/src/main/java")
                        .addRestrictSourceToPackages(JDK_PACKAGE).build(),
                new AnnotatedAPIConfigurationImpl.Builder().build());

        List<TypeInfo> types = annotatedApiParser.typesParsed();
        assertEquals(25, types.size());
        for (TypeInfo typeInfo : types) {
            if ("org.e2immu.analyzer.aapi.archive.v2.jdk.JavaLang".equals(typeInfo.fullyQualifiedName())) {
                TypeInfo charSeq = typeInfo.findSubType("CharSequence$");
                MethodInfo sub = charSeq.findUniqueMethod("subSequence", 2);
                assertFalse(sub.comments().isEmpty());
                assertEquals(NOTE_CHARSEQUENCE, sub.comments().getFirst().comment());

                String uri = typeInfo.compilationUnitOrEnclosingType().getLeft().uri().toString();
                assertTrue(uri.endsWith("aapi/archive/v2/jdk/JavaLang.java"), "Have: " + uri);
            }
        }
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(annotatedApiParser.runtime(), annotatedApiParser,
                true);

        List<Class<?>> extraClasses = new ArrayList<>();
        Collections.addAll(extraClasses,
                TreeMap.class, SortedMap.class, NavigableMap.class, SequencedMap.class, LinkedList.class,
                ProcessHandle.class,
                Readable.class, Reader.class, BufferedReader.class, RandomAccessFile.class,
                IntFunction.class, BinaryOperator.class, BiConsumer.class, Predicate.class,
                Annotation.class,
                Constable.class,
                Array.class,
                SecureRandom.class, MessageDigest.class, RandomGenerator.class, Guard.class, PrivilegedAction.class,
                ZipOutputStream.class,
                Container.class, Component.class, Color.class, Graphics.class,
                HttpRequest.class, HttpRequest.Builder.class, URI.class,
                DefaultComboBoxModel.class, JLabel.class, JComboBox.class, JTable.class, AbstractButton.class,
                TableColumn.class, JTextComponent.class,
                org.slf4j.Logger.class, LoggerFactory.class, ILoggerFactory.class,
                Assertions.class, ThrowingSupplier.class, Executable.class);
        Stream<TypeInfo> extra = extraClasses.stream()
                .map(c -> annotatedApiParser.javaInspector().compiledTypesManager().getOrLoad(c));
        List<TypeInfo> typesToAnalyze = Stream.concat(annotatedApiParser.types().stream(), extra).distinct().toList();
        LOGGER.info("Have {} types for the shallow analyzer", typesToAnalyze.size());
        ShallowAnalyzer.Result rs = shallowAnalyzer.go(typesToAnalyze);
        Trie<TypeInfo> trie = new Trie<>();
        for (TypeInfo ti : typesToAnalyze) {
            if (ti.isPrimaryType()) {
                trie.add(ti.packageName().split("\\."), ti);
            }
            if ("java.lang.CharSequence".equals(ti.fullyQualifiedName())) {
                MethodInfo sub = ti.findUniqueMethod("subSequence", 2);
                assertTrue(sub.comments().isEmpty());
                AnnotatedApiParser.Data data = annotatedApiParser.data(sub);
                assertEquals(NOTE_CHARSEQUENCE, data.comments().getFirst().comment());
            }
            if ("java.util.Collection".equals(ti.fullyQualifiedName())) {
                {
                    MethodInfo contains = ti.findUniqueMethod("contains", 1);
                    assertEquals("java.util.Collection.contains(Object)", contains.fullyQualifiedName());
                    ShallowAnalyzer.InfoData id = rs.dataMap().get(contains);
                    assertSame(ANNOTATED, id.origin(NON_MODIFYING_METHOD));
                    ParameterInfo p0 = contains.parameters().getFirst();
                    assertEquals("java.util.Collection.contains(Object):0:object", p0.fullyQualifiedName());
                    ShallowAnalyzer.InfoData id0 = rs.dataMap().get(p0);
                    assertSame(FROM_OWNER, id0.origin(UNMODIFIED_PARAMETER));
                }
                {
                    MethodInfo add = ti.findUniqueMethod("add", 1);
                    ParameterInfo p0 = add.parameters().getFirst();
                    assertEquals("java.util.Collection.add(E):0:e", p0.fullyQualifiedName());
                    ShallowAnalyzer.InfoData id0 = rs.dataMap().get(p0);
                    assertSame(FROM_TYPE, id0.origin(INDEPENDENT_PARAMETER));
                    assertTrue(p0.analysis().getOrDefault(INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.DEPENDENT)
                            .isIndependentHc());
                }
            } else if ("java.util.AbstractCollection".equals(ti.fullyQualifiedName())) {
                {
                    MethodInfo toString = ti.findUniqueMethod("toString", 0);
                    assertTrue(toString.analysis().getOrDefault(NON_MODIFYING_METHOD, ValueImpl.BoolImpl.FALSE).isTrue());
                }
                {
                    MethodInfo constructor = ti.findConstructor(0);
                    ShallowAnalyzer.InfoData id = rs.dataMap().get(constructor);
                    assertNull(id);
                    assertFalse(constructor.isPublic());
                }
            }
        }

        File decorated = new File("build/decorated");
        if (decorated.mkdirs()) LOGGER.info("Created {}", decorated);
        WriteDecoratedAAPI writeDecoratedAAPI = new WriteDecoratedAAPI(annotatedApiParser.javaInspector(),
                annotatedApiParser::data, rs.dataMap()::get);
        writeDecoratedAAPI.write(decorated.getAbsolutePath(), trie, JDK_PACKAGE);

    }
}
