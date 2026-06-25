/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.inspection.api.resource;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A flat map from a method's canonical, source-set-independent key to the list of its formal parameter names.
 * <p>
 * Built (with faithful names, read from the {@code LocalVariableTable} or synthesised) by the ASM-based loader in
 * {@code maddi-java-bytecode}, written to a simple text file, and later consulted by any loader that does not have
 * those names (e.g. the javac-based one, which only sees {@code arg0}). It lives in inspection-api so both the
 * builder and the consumer can use it without depending on each other.
 * <p>
 * The key deliberately uses {@link org.e2immu.language.cst.api.type.ParameterizedType#erasedForFQN()}
 * fully-qualified names (the same erasure {@code MethodMap} uses), <em>not</em> the model's
 * {@code descriptor()}: the latter carries a {@code sourceSet::} prefix, which would make the same JDK/library
 * method key differently depending on how it was loaded.
 */
public class ParameterNameIndex {

    private final Map<String, List<String>> namesByKey = new HashMap<>();

    /**
     * Canonical key: {@code <typeFqn>.<name>(<erasedParamFqn>,<erasedParamFqn>,...)}, where {@code <name>} is
     * {@code <init>} for a constructor. Derivable identically from any {@link MethodInfo}, regardless of loader.
     */
    public static String key(MethodInfo methodInfo) {
        String params = methodInfo.parameters().stream()
                .map(pi -> pi.parameterizedType().erasedForFQN().fullyQualifiedName())
                .collect(Collectors.joining(","));
        String name = methodInfo.isConstructor() ? "<init>" : methodInfo.name();
        return methodInfo.typeInfo().fullyQualifiedName() + "." + name + "(" + params + ")";
    }

    public void put(MethodInfo methodInfo) {
        if (methodInfo.parameters().isEmpty()) return;
        namesByKey.put(key(methodInfo), methodInfo.parameters().stream().map(ParameterInfo::name).toList());
    }

    /** adds all methods and constructors of {@code typeInfo} and, recursively, of its subtypes */
    public void putRecursively(TypeInfo typeInfo) {
        typeInfo.constructorAndMethodStream().forEach(this::put);
        typeInfo.subTypes().forEach(this::putRecursively);
    }

    public List<String> parameterNames(String key) {
        return namesByKey.get(key);
    }

    public int size() {
        return namesByKey.size();
    }

    // ---- serialization: dependency-free, one line per method, "key\tname1,name2,..." (names are identifiers,
    // so they never contain a tab, comma or newline; the key is everything before the first tab) ----

    public void write(Writer writer) throws IOException {
        BufferedWriter w = writer instanceof BufferedWriter bw ? bw : new BufferedWriter(writer);
        // sorted for a stable, diffable file
        for (Map.Entry<String, List<String>> e : new TreeMap<>(namesByKey).entrySet()) {
            w.write(e.getKey());
            w.write('\t');
            w.write(String.join(",", e.getValue()));
            w.write('\n');
        }
        w.flush();
    }

    public static ParameterNameIndex read(Reader reader) throws IOException {
        ParameterNameIndex index = new ParameterNameIndex();
        BufferedReader r = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        String line;
        while ((line = r.readLine()) != null) {
            int tab = line.indexOf('\t');
            if (tab < 0) continue;
            String key = line.substring(0, tab);
            index.namesByKey.put(key, List.of(line.substring(tab + 1).split(",", -1)));
        }
        return index;
    }

    // ---- file convenience: UTF-8 text, gzip-compressed when the file name ends with ".gz" ----

    public void write(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        boolean gz = file.getFileName().toString().endsWith(".gz");
        OutputStream rawOut = Files.newOutputStream(file);
        OutputStream out = gz ? new GZIPOutputStream(rawOut) : rawOut;
        try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            write(w);
        }
    }

    public static ParameterNameIndex read(Path file) throws IOException {
        boolean gz = file.getFileName().toString().endsWith(".gz");
        InputStream rawIn = Files.newInputStream(file);
        InputStream in = gz ? new GZIPInputStream(rawIn) : rawIn;
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return read(r);
        }
    }
}
