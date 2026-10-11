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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A library mapping written as text, a {@code .csmap} file: what a client writes to override rules without Java, and
 * how the simpler library mappings are written. One directive per line, {@code #} starts a comment:
 * <pre>
 * mapping slf4j:ms-logging                                  the id: the library, and the option
 * type org.slf4j.Logger -> ILogger using Microsoft.Extensions.Logging
 * plain java.lang.Class -> Type using System                a generic type whose C# counterpart is not generic
 * pascal com.example.Codec -> Codec using Example.CSharp    a class whose members are the Java ones in PascalCase
 * member org.slf4j.Logger.info/1 -> $0.LogInformation($1)
 * member org.slf4j.Logger.warn/2 ?lastArgumentThrowable -> $0.LogWarning($2, $1)
 * member org.slf4j.Logger.warn/2 -> $0.LogWarning($1, $2)
 * statement java.util.Map.put/2 -> $0[$1] = $2              the template for a call whose value is not used
 * member java.util.UUID.new/2 -> new Guid(...)              a constructor: owner.new
 * field java.lang.System.out -> Console.Out using System
 * override java.lang.AutoCloseable.close/0 -> Dispose       the name of a translated method that overrides it
 * name ILogger                                              a C# type name the mapping writes, besides the types'
 * </pre>
 * A member's key is the declaring type, the name, and the number of parameters ({@code List.get/1}) or their erased
 * simple types ({@code List.remove(int)}); a call matches the rules of its method or of a method it overrides, the
 * most specific type first. Templates are those of {@link LibraryMapping.Rule}. A rule with a guard,
 * {@code ?name}, applies only when the guard holds; the rules of one key are tried in their order. The guards are
 * named predicates of a call ({@link #guard(String, Guard)} registers more): {@code arguments=N},
 * {@code lastArgumentThrowable}, {@code receiver=fqn}, {@code methodReference}.
 */
public final class CSharpMappingFile implements LibraryMapping {

    /** A predicate of a call ({@code call} is null for a method reference) that chooses a rule. */
    public interface Guard extends BiPredicate<MethodInfo, MethodCall> {
    }

    private static final Map<String, Guard> GUARDS = new ConcurrentHashMap<>();

    static {
        guard("lastArgumentThrowable", (method, call) -> call != null && !call.parameterExpressions().isEmpty()
                                                         && throwable(call.parameterExpressions().getLast()));
        guard("methodReference", (method, call) -> call == null);
    }

    /** Registers a named guard, for the files' {@code ?name}. */
    public static void guard(String name, Guard guard) {
        GUARDS.put(name, guard);
    }

    private static Guard resolveGuard(String name) {
        if (name.startsWith("arguments=")) {
            int n = Integer.parseInt(name.substring("arguments=".length()));
            return (method, call) -> call != null && call.parameterExpressions().size() == n;
        }
        if (name.startsWith("receiver=")) {
            String fqn = name.substring("receiver=".length());
            return (method, call) -> call != null && call.object() != null && call.object().parameterizedType() != null
                                     && call.object().parameterizedType().typeInfo() != null
                                     && fqn.equals(call.object().parameterizedType().typeInfo().fullyQualifiedName());
        }
        Guard guard = GUARDS.get(name);
        if (guard == null) throw new IllegalArgumentException("no guard ?" + name);
        return guard;
    }

    private static boolean throwable(Expression e) {
        ParameterizedType pt = e.parameterizedType();
        if (pt == null || pt.typeInfo() == null || pt.arrays() > 0) return false;
        TypeInfo t = pt.typeInfo();
        return "java.lang.Throwable".equals(t.fullyQualifiedName()) || t.superTypesExcludingJavaLangObject().stream()
                .anyMatch(s -> "java.lang.Throwable".equals(s.fullyQualifiedName()));
    }

    private record Entry(Guard guard, String template, List<String> namespaces) {
        boolean applies(MethodInfo method, MethodCall call) {
            return guard == null || guard.test(method, call);
        }
    }

    private final String id;
    private final Map<String, TypeMapping> types = new HashMap<>();
    private final Map<String, List<Entry>> values = new HashMap<>();
    private final Map<String, List<Entry>> statements = new HashMap<>();
    private final Map<String, FieldMapping> fields = new HashMap<>();
    private final Map<String, String> overrides = new HashMap<>();
    private final Set<String> typeNames = new HashSet<>();

    private CSharpMappingFile(String id) {
        this.id = id;
    }

    private static final Pattern USING = Pattern.compile("^(.*?)\\s+using\\s+([\\w.]+(?:\\s*,\\s*[\\w.]+)*)\\s*$");
    private static final Pattern MEMBER_KEY = Pattern.compile("^(\\S+)(?:\\s+\\?(\\S+))?$");

    /** The mapping written in {@code text}; {@code source} names it in error messages. */
    public static CSharpMappingFile parse(String text, String source) {
        CSharpMappingFile file = null;
        int lineNumber = 0;
        for (String raw : text.split("\n", -1)) {
            lineNumber++;
            String line = raw.strip();
            int hash = line.indexOf(" #");
            if (hash >= 0) line = line.substring(0, hash).strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int space = line.indexOf(' ');
            String directive = space < 0 ? line : line.substring(0, space);
            String rest = space < 0 ? "" : line.substring(space + 1).strip();
            try {
                if ("mapping".equals(directive)) {
                    if (file != null) throw new IllegalArgumentException("a second mapping directive");
                    file = new CSharpMappingFile(rest);
                    continue;
                }
                if (file == null) throw new IllegalArgumentException("the first directive must be: mapping <id>");
                file.directive(directive, rest);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(source + ":" + lineNumber + ": " + e.getMessage(), e);
            }
        }
        if (file == null) throw new IllegalArgumentException(source + ": no mapping directive");
        return file;
    }

    /** The mapping in the file {@code path}. */
    public static CSharpMappingFile load(Path path) {
        try {
            return parse(Files.readString(path), path.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A mapping that is a resource beside this class: one of the printer's own. */
    static CSharpMappingFile resource(String name) {
        try (InputStream in = CSharpMappingFile.class.getResourceAsStream(name)) {
            if (in == null) throw new IllegalStateException("no " + name + " beside " + CSharpMappingFile.class);
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void directive(String directive, String rest) {
        if ("name".equals(directive)) {
            typeNames.add(rest);
            return;
        }
        int arrow = rest.indexOf(" -> ");
        if (arrow < 0) throw new IllegalArgumentException("expected: " + directive + " <key> -> <value>");
        String key = rest.substring(0, arrow).strip();
        String value = rest.substring(arrow + 4).strip();
        List<String> namespaces = List.of();
        Matcher using = USING.matcher(value);
        if (using.matches()) {
            value = using.group(1).strip();
            namespaces = List.of(using.group(2).split("\\s*,\\s*"));
        }
        String namespace = namespaces.isEmpty() ? null : namespaces.getFirst();
        switch (directive) {
            case "type" -> addType(key, new TypeMapping(value, namespace, false, false));
            case "plain" -> addType(key, new TypeMapping(value, namespace, false, true));
            case "pascal" -> addType(key, new TypeMapping(value, namespace, true, false));
            case "member", "statement" -> {
                Matcher m = MEMBER_KEY.matcher(key);
                if (!m.matches()) throw new IllegalArgumentException("expected: <key> [?guard]");
                Guard guard = m.group(2) == null ? null : resolveGuard(m.group(2));
                ("member".equals(directive) ? values : statements).computeIfAbsent(m.group(1), k -> new ArrayList<>())
                        .add(new Entry(guard, value, namespaces));
            }
            case "field" -> fields.put(key, new FieldMapping(value, namespace));
            case "override" -> overrides.put(key, value);
            default -> throw new IllegalArgumentException("unknown directive " + directive);
        }
    }

    private void addType(String fqn, TypeMapping mapping) {
        types.put(fqn, mapping);
        String t = mapping.template();
        int cut = t.indexOf('<');
        if (cut >= 0) t = t.substring(0, cut);
        if (!t.isEmpty() && Character.isUpperCase(t.charAt(0))) typeNames.add(t);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public TypeMapping type(TypeInfo typeInfo) {
        return types.get(typeInfo.fullyQualifiedName());
    }

    @Override
    public Rule call(MethodInfo method, MethodCall call) {
        for (MethodInfo candidate : CSharpBcl.candidates(method)) {
            String base = candidate.typeInfo().fullyQualifiedName() + "." + candidate.name();
            Rule rule = rule(List.of(base + "(" + CSharpBcl.signature(candidate) + ")",
                    base + "/" + candidate.parameters().size(), base), method, call);
            if (rule != null) return rule;
            TypeMapping mapping = types.get(candidate.typeInfo().fullyQualifiedName());
            if (mapping != null && mapping.compat()) {
                String name = CSharpNames.pascal(candidate.name());
                String owner = mapping.template().contains("<") ? mapping.template()
                        .substring(0, mapping.template().indexOf('<')) : mapping.template();
                return new Rule(candidate.isStatic() ? owner + "." + name + "($*)" : "$0." + name + "($*)", null,
                        mapping.namespace() == null ? List.of() : List.of(mapping.namespace()));
            }
        }
        return null;
    }

    @Override
    public Rule constructor(MethodInfo constructor) {
        String base = constructor.typeInfo().fullyQualifiedName() + ".new";
        return rule(List.of(base + "(" + CSharpBcl.signature(constructor) + ")",
                base + "/" + constructor.parameters().size(), base), constructor, null);
    }

    /** The rule of the first key with a value template whose guard holds, and that key's statement template. */
    private Rule rule(List<String> keys, MethodInfo method, MethodCall call) {
        for (String key : keys) {
            Entry value = first(values.get(key), method, call);
            if (value == null) continue;
            Entry statement = first(statements.get(key), method, call);
            Set<String> namespaces = new LinkedHashSet<>(value.namespaces());
            if (statement != null) namespaces.addAll(statement.namespaces());
            return new Rule(value.template(), statement == null ? null : statement.template(), List.copyOf(namespaces));
        }
        return null;
    }

    private static Entry first(List<Entry> entries, MethodInfo method, MethodCall call) {
        if (entries == null) return null;
        return entries.stream().filter(e -> e.applies(method, call)).findFirst().orElse(null);
    }

    @Override
    public FieldMapping field(FieldInfo fieldInfo) {
        return fields.get(fieldInfo.owner().fullyQualifiedName() + "." + fieldInfo.name());
    }

    @Override
    public String overrideName(MethodInfo method) {
        for (MethodInfo overridden : method.overrides()) {
            if (overridden == method) continue;
            String base = overridden.typeInfo().fullyQualifiedName() + "." + overridden.name();
            for (String key : List.of(base + "(" + CSharpBcl.signature(overridden) + ")",
                    base + "/" + overridden.parameters().size(), base)) {
                String name = overrides.get(key);
                if (name != null) return name;
            }
        }
        return null;
    }

    @Override
    public Set<String> typeNames() {
        return Set.copyOf(typeNames);
    }
}
