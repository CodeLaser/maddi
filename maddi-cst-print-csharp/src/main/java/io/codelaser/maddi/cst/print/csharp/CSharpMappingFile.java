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

import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.StringConstant;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
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
import java.util.function.Function;
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
 * statement java.util.Map.put/2 -> $0.Add($1, $2)          the template for a call whose value is not used; alone,
 *                                                          the next mapping supplies the value template
 * member java.util.UUID.new/2 -> new Guid(...)              a constructor: owner.new
 * field java.lang.System.out -> Console.Out using System
 * override java.lang.AutoCloseable.close/0 -> Dispose       the name of a translated method that overrides it
 * annotation com.fasterxml.jackson.annotation.JsonProperty ?has=value -> JsonPropertyName($value)
 * annotation com.fasterxml.jackson.annotation.JsonInclude ?on=field ?value=NON_NULL -> JsonIgnore(Condition = ...)
 * annotation com.fasterxml.jackson.annotation.JsonCreator -> JsonConstructor ][ Obsolete    two attributes; - drops it
 * name ILogger                                              a C# type name the mapping writes, besides the types'
 * nuget Microsoft.Extensions.Logging.Abstractions 10.0.0    a package the translated code references
 * compat JavaLogging.cs                                     a source that goes with it, beside the file
 * exclude com.example.gizmo.GizmoClassLoader                a program's primary type left out, used through the rules;
 * exclude com.example.gizmo                                   or a package, and the packages in it
 * </pre>
 * A member's key is the declaring type, the name, and the number of parameters ({@code List.get/1}) or their erased
 * simple types ({@code List.remove(int)}); a call matches the rules of its method or of a method it overrides, the
 * most specific type first. Templates are those of {@link LibraryMapping.Rule}, and {@code $#1} writes argument 1, an
 * slf4j message, as a .NET message template whose holes are named after their arguments. A rule with guards,
 * {@code ?name ?other}, applies only when they all hold; the rules of one key are tried in their order. The guards are
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

    /**
     * {@code target}, when not null: the annotation is on it; {@code element}, when not null: the use has it
     * ({@code value} null), or it is {@code value}.
     */
    private record AnnotationEntry(Target target, String element, String value, List<String> attributes,
                                   List<String> namespaces) {
        boolean applies(AnnotationExpression use, Target on) {
            if (target != null && target != on) return false;
            if (element == null) return true;
            Expression v = elementValue(use, element);
            return v != null && (value == null || value.equals(simpleText(v)));
        }
    }

    private static Expression elementValue(AnnotationExpression use, String element) {
        for (AnnotationExpression.KV kv : use.keyValuePairs()) {
            if (kv.key().equals(element) || "value".equals(element) && kv.keyIsDefault()) return kv.value();
        }
        return null;
    }

    /** An element's value as a guard compares it: a constant's name ({@code NON_NULL}), a string's text. */
    private static String simpleText(Expression e) {
        Expression x = CSharpExpressionPrinter.unwrap(e);
        if (x instanceof StringConstant sc) return sc.constant();
        if (x instanceof VariableExpression ve) return ve.variable().simpleName().replaceFirst("^.*\\.", "");
        return x.toString();
    }

    private static final Pattern ELEMENT = Pattern.compile("\\$([A-Za-z]\\w*)");

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
    private final Map<String, List<AnnotationEntry>> annotations = new HashMap<>();
    private final Map<String, String> overrides = new HashMap<>();
    private final Set<String> typeNames = new HashSet<>();
    private final List<NuGetPackage> packages = new ArrayList<>();
    private final List<Source> sources = new ArrayList<>();
    private final List<String> excluded = new ArrayList<>();
    /** Where the file's {@code compat} sources are: the file's directory, or null for a resource beside this class. */
    private final Path directory;

    private CSharpMappingFile(String id, Path directory) {
        this.id = id;
        this.directory = directory;
    }

    private static final Pattern USING = Pattern.compile("^(.*?)\\s+using\\s+([\\w.]+(?:\\s*,\\s*[\\w.]+)*)\\s*$");
    private static final Pattern MEMBER_KEY = Pattern.compile("^(\\S+)((?:\\s+\\?\\S+)*)$");

    /** The mapping written in {@code text}; {@code source} names it in error messages. */
    public static CSharpMappingFile parse(String text, String source) {
        return parse(text, source, null);
    }

    private static CSharpMappingFile parse(String text, String source, Path directory) {
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
                    file = new CSharpMappingFile(rest, directory);
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

    @Override
    public boolean excludes(TypeInfo primaryType) {
        if (excluded.isEmpty()) return false;
        String fqn = primaryType.fullyQualifiedName();
        return excluded.stream().anyMatch(e -> fqn.equals(e) || fqn.startsWith(e + "."));
    }

    /** The mapping in the file {@code path}. */
    public static CSharpMappingFile load(Path path) {
        try {
            return parse(Files.readString(path), path.toString(), path.toAbsolutePath().getParent());
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
        if ("nuget".equals(directive)) {
            String[] parts = rest.split("\\s+");
            if (parts.length != 2) throw new IllegalArgumentException("expected: nuget <package> <version>");
            packages.add(new NuGetPackage(parts[0], parts[1]));
            return;
        }
        if ("exclude".equals(directive)) {
            if (rest.isEmpty() || rest.contains(" ")) throw new IllegalArgumentException("expected: exclude <type or package>");
            excluded.add(rest);
            return;
        }
        if ("compat".equals(directive)) {
            sources.add(new Source(Path.of(rest).getFileName().toString(), () -> compatSource(rest)));
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
        if (namespaces.size() > 1 && Set.of("type", "plain", "pascal", "field").contains(directive)) {
            // a type or field mapping has one namespace; only a member's template may need several
            throw new IllegalArgumentException(directive + " takes one namespace, not " + String.join(", ", namespaces));
        }
        switch (directive) {
            case "type" -> addType(key, new TypeMapping(value, namespace, false, false));
            case "plain" -> addType(key, new TypeMapping(value, namespace, false, true));
            case "pascal" -> addType(key, new TypeMapping(value, namespace, true, false));
            case "member", "statement" -> {
                Matcher m = MEMBER_KEY.matcher(key);
                if (!m.matches()) throw new IllegalArgumentException("expected: <key> [?guard]...");
                Guard guard = null;
                for (String name : m.group(2).strip().split("\\s+")) {
                    if (name.isEmpty()) continue;
                    Guard g = resolveGuard(name.substring(1));
                    Guard previous = guard;
                    guard = previous == null ? g : (method, call) -> previous.test(method, call) && g.test(method, call);
                }
                ("member".equals(directive) ? values : statements).computeIfAbsent(m.group(1), k -> new ArrayList<>())
                        .add(new Entry(guard, value, namespaces));
            }
            case "field" -> fields.put(key, new FieldMapping(value, namespace));
            case "annotation" -> {
                Matcher m = MEMBER_KEY.matcher(key);
                if (!m.matches()) {
                    throw new IllegalArgumentException("expected: <annotation type> [?on=target] [?element[=value]]");
                }
                Target target = null;
                String element = null;
                String elementValue = null;
                for (String token : m.group(2).strip().split("\\s+")) {
                    if (token.isEmpty()) continue;
                    String guard = token.substring(1);
                    if (guard.startsWith("on=")) {
                        target = Target.valueOf(guard.substring(3).toUpperCase(java.util.Locale.ROOT));
                        continue;
                    }
                    if (guard.startsWith("has=")) guard = guard.substring(4);
                    int eq = guard.indexOf('=');
                    element = eq < 0 ? guard : guard.substring(0, eq);
                    elementValue = eq < 0 ? null : guard.substring(eq + 1);
                }
                List<String> attributes = "-".equals(value) ? List.of() : List.of(value.split("\\s*\\]\\[\\s*"));
                annotations.computeIfAbsent(m.group(1), k -> new ArrayList<>())
                        .add(new AnnotationEntry(target, element, elementValue, attributes, namespaces));
            }
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

    /** A {@code compat} source: beside the file, or a resource beside this class for the printer's own mappings. */
    private String compatSource(String name) {
        try {
            if (directory != null) return Files.readString(directory.resolve(name));
            try (InputStream in = CSharpMappingFile.class.getResourceAsStream(name)) {
                if (in == null) throw new IllegalStateException("no " + name + " beside " + CSharpMappingFile.class);
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Requirements requirements() {
        return new Requirements(List.copyOf(packages), List.copyOf(sources));
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

    /**
     * The rule of the first key with a value template whose guard holds, and that key's statement template, or a more
     * specific key's; a statement template without any value template is a rule of its own, whose value the next
     * mapping supplies ({@link CSharpLibraries#call}).
     */
    private Rule rule(List<String> keys, MethodInfo method, MethodCall call) {
        Entry pending = null;
        for (String key : keys) {
            Entry value = first(values.get(key), method, call);
            Entry statement = first(statements.get(key), method, call);
            if (statement == null) statement = pending;
            if (value == null) {
                pending = statement;
                continue;
            }
            Set<String> namespaces = new LinkedHashSet<>(value.namespaces());
            if (statement != null) namespaces.addAll(statement.namespaces());
            return new Rule(value.template(), statement == null ? null : statement.template(), List.copyOf(namespaces));
        }
        return pending == null ? null : new Rule(null, pending.template(), pending.namespaces());
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
    public AttributeMapping annotation(AnnotationExpression use, Target target, Function<Expression, String> print) {
        List<AnnotationEntry> entries = annotations.get(use.typeInfo().fullyQualifiedName());
        if (entries == null) return null;
        for (AnnotationEntry entry : entries) {
            if (!entry.applies(use, target)) continue;
            List<String> attributes = entry.attributes().stream().map(a -> ELEMENT.matcher(a).replaceAll(r -> {
                Expression v = elementValue(use, r.group(1));
                return Matcher.quoteReplacement(v == null ? r.group() : print.apply(v));
            })).toList();
            return new AttributeMapping(attributes, entry.namespaces());
        }
        return null;
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
