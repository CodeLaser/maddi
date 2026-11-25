/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.support;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.codelaser.maddi.annotation.Modified;

/**
 * CodeLaser/maddi#182: maddi-annotation and maddi-support are published to Maven Central on a version line of their
 * own ({@code annotationsVersion}), and only when they change. Every maddi release must understand every published
 * version of them, so their API may grow but never shrink: a type, member, attribute or default that was published
 * may not be removed or renamed.
 * <p>
 * {@code published-api.txt} lists the public API of both jars: each public type with its own annotations, and its
 * public and protected members (for an annotation type, its attributes with their defaults). A line that is in the
 * list but no longer in the code is a removal or a rename, and fails. A line that is in the code but not yet in the
 * list also fails, so that the list stays complete: add it, and bump {@code annotationsVersion} in
 * {@code gradle.properties} if that version is already on Central (the release refuses changed sources under an
 * unchanged version). The test writes the current API to {@code build/published-api.txt}.
 */
public class TestPublishedApi {

    private static final String LIST = "/published-api.txt";

    @Test
    public void theApiOnlyGrows() throws IOException, URISyntaxException {
        TreeSet<String> current = new TreeSet<>();
        for (Class<?> anchor : List.of(Modified.class, Lazy.class)) {
            for (Class<?> type : publicTypes(anchor)) current.addAll(lines(type));
        }
        assertTrue(current.size() > 100, "the API walk found only " + current.size() + " lines; it checked nothing");
        Path written = Path.of("build/published-api.txt");
        Files.createDirectories(written.getParent());
        Files.write(written, current, StandardCharsets.UTF_8);

        List<String> listed;
        try (InputStream in = TestPublishedApi.class.getResourceAsStream(LIST)) {
            assertNotNull(in, "no " + LIST + " on the test class path");
            listed = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(l -> !l.isBlank() && !l.startsWith("#")).toList();
        }
        List<String> removed = listed.stream().filter(l -> !current.contains(l)).toList();
        assertTrue(removed.isEmpty(), "published API removed or renamed; maddi-annotation and maddi-support may only"
                                      + " grow (#182). Missing now:\n  " + String.join("\n  ", removed));
        TreeSet<String> added = new TreeSet<>(current);
        listed.forEach(added::remove);
        assertTrue(added.isEmpty(), "new API, not yet in src/test/resources" + LIST + ". Add these lines (all of"
                                      + " them are in " + written.toAbsolutePath() + "), and bump annotationsVersion"
                                      + " if the current one is already on Maven Central:\n  "
                                      + String.join("\n  ", added));
    }

    /** The public top-level and nested types in the jar or class directory that holds the anchor. */
    private static List<Class<?>> publicTypes(Class<?> anchor) throws IOException, URISyntaxException {
        Path location = Path.of(anchor.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<Class<?>> result = new ArrayList<>();
        if (Files.isDirectory(location)) {
            collect(location, anchor.getClassLoader(), result);
        } else {
            try (FileSystem jar = FileSystems.newFileSystem(location, Map.of())) {
                collect(jar.getPath("/"), anchor.getClassLoader(), result);
            }
        }
        return result;
    }

    private static void collect(Path root, ClassLoader loader, List<Class<?>> result) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                String relative = root.relativize(p).toString();
                if (relative.endsWith("module-info.class") || relative.endsWith("package-info.class")
                    || relative.startsWith("META-INF")) continue;
                String name = relative.substring(0, relative.length() - ".class".length())
                        .replace('/', '.').replace('\\', '.');
                try {
                    Class<?> c = Class.forName(name, false, loader);
                    if (isPublicApi(c)) result.add(c);
                } catch (ClassNotFoundException e) {
                    throw new AssertionError("cannot load " + name + " from " + root, e);
                }
            }
        }
    }

    private static boolean isPublicApi(Class<?> c) {
        if (c.isSynthetic() || c.isAnonymousClass() || c.isLocalClass()) return false;
        for (Class<?> k = c; k != null; k = k.getDeclaringClass()) {
            if (!Modifier.isPublic(k.getModifiers())) return false;
        }
        return true;
    }

    private static List<String> lines(Class<?> type) {
        List<String> result = new ArrayList<>();
        String t = type.getName();
        result.add(t + " " + kind(type) + annotations(type.getDeclaredAnnotations()));
        if (type.getGenericSuperclass() != null && type.getSuperclass() != Object.class) {
            result.add(t + " extends " + type.getGenericSuperclass().getTypeName());
        }
        Arrays.stream(type.getGenericInterfaces())
                .filter(i -> !(type.isAnnotation() && i == Annotation.class))
                .forEach(i -> result.add(t + " implements " + i.getTypeName()));
        for (Constructor<?> c : type.getDeclaredConstructors()) {
            if (visible(c)) result.add(t + " " + c.toGenericString());
        }
        for (Method m : type.getDeclaredMethods()) {
            if (!visible(m) || m.isBridge()) continue;
            if (type.isEnum() && (m.getName().equals("values") || m.getName().equals("valueOf"))) continue;
            String defaultValue = m.getDefaultValue() == null ? "" : " default " + render(m.getDefaultValue());
            result.add(t + " " + m.toGenericString() + defaultValue + annotations(m.getDeclaredAnnotations()));
        }
        for (Field f : type.getDeclaredFields()) {
            if (visible(f)) result.add(t + " " + f.toGenericString());
        }
        return result;
    }

    private static boolean visible(Member m) {
        return !m.isSynthetic() && (Modifier.isPublic(m.getModifiers()) || Modifier.isProtected(m.getModifiers()));
    }

    private static String kind(Class<?> type) {
        if (type.isAnnotation()) return "@interface";
        if (type.isInterface()) return "interface";
        if (type.isEnum()) return "enum";
        if (type.isRecord()) return "record";
        return (Modifier.isAbstract(type.getModifiers()) ? "abstract " : "")
               + (Modifier.isFinal(type.getModifiers()) ? "final " : "") + "class";
    }

    /** Retention, target and the like are API too: a narrower @Target breaks code that compiled. Sorted, stable. */
    private static String annotations(Annotation[] annotations) {
        if (annotations.length == 0) return "";
        TreeSet<String> sorted = new TreeSet<>();
        for (Annotation a : annotations) sorted.add(a.toString());
        return " " + String.join(" ", sorted);
    }

    private static String render(Object value) {
        if (value instanceof Object[] array) return Arrays.deepToString(array);
        if (value.getClass().isArray()) {
            return Arrays.deepToString(new Object[]{value}).replaceAll("^\\[|]$", "");
        }
        if (value instanceof String s) return "\"" + s + "\"";
        if (value instanceof Class<?> c) return c.getName() + ".class";
        return String.valueOf(value);
    }
}
