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

import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The library mappings of a translation, in order: the first that answers wins. A client puts its own overrides in
 * front, and leaves out the mapping of a library it does not want translated; the JDK's is normally last.
 * <p>
 * The printer has options of its own per library ({@link #OPTIONS}), the first the default:
 * <ul>
 *   <li>{@code slf4j}: {@code ms-logging} (Microsoft.Extensions.Logging's ILogger), {@code serilog};</li>
 *   <li>{@code jackson}: {@code system-text-json}, {@code newtonsoft} (Json.NET);</li>
 *   <li>{@code jdk}: {@code bcl}, the BCL with the compatibility library where Java's behaviour differs.</li>
 * </ul>
 * Option {@code none} leaves a library untranslated. {@link #choose} picks options, {@code slf4j=serilog,jackson=none}.
 */
public final class CSharpLibraries implements LibraryMapping {

    /** Library → option → the mappings of that option, the first option the default; in the order they are asked. */
    public static final Map<String, Map<String, Supplier<List<LibraryMapping>>>> OPTIONS = options();

    private static Map<String, Map<String, Supplier<List<LibraryMapping>>>> options() {
        Map<String, Map<String, Supplier<List<LibraryMapping>>>> options = new LinkedHashMap<>();
        Map<String, Supplier<List<LibraryMapping>>> slf4j = new LinkedHashMap<>();
        slf4j.put("ms-logging", () -> List.of(CSharpMappingFile.resource("slf4j-ms-logging.csmap")));
        slf4j.put("serilog", () -> List.of(CSharpMappingFile.resource("slf4j-serilog.csmap")));
        options.put("slf4j", slf4j);
        Map<String, Supplier<List<LibraryMapping>>> jackson = new LinkedHashMap<>();
        jackson.put("system-text-json", () -> List.of(JacksonAttributes.INSTANCE,
                CSharpMappingFile.resource("jackson-system-text-json.csmap")));
        jackson.put("newtonsoft", () -> List.of(CSharpMappingFile.resource("jackson-newtonsoft.csmap")));
        options.put("jackson", jackson);
        options.put("jdk", Map.of("bcl", () -> List.of(CSharpBcl.JDK)));
        return java.util.Collections.unmodifiableMap(options);
    }

    /** The default option of every library. */
    public static final CSharpLibraries DEFAULT = choose(Map.of());

    /**
     * The printer's mappings with {@code choices}, library → option; a library not chosen has its default option,
     * {@code none} leaves it out.
     */
    public static CSharpLibraries choose(Map<String, String> choices) {
        for (String library : choices.keySet()) {
            if (!OPTIONS.containsKey(library)) {
                throw new IllegalArgumentException("no library " + library + "; there are " + OPTIONS.keySet());
            }
        }
        List<LibraryMapping> mappings = new ArrayList<>();
        OPTIONS.forEach((library, options) -> {
            String option = choices.getOrDefault(library, options.keySet().iterator().next());
            if ("none".equals(option)) return;
            Supplier<List<LibraryMapping>> chosen = options.get(option);
            if (chosen == null) {
                throw new IllegalArgumentException("no option " + option + " for " + library + "; there are "
                                                   + options.keySet() + " and none");
            }
            mappings.addAll(chosen.get());
        });
        return new CSharpLibraries(mappings);
    }

    /** {@link #choose} from {@code slf4j=serilog,jackson=none}; blank for the defaults. */
    public static CSharpLibraries choose(String choices) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String choice : choices.split(",")) {
            if (choice.isBlank()) continue;
            String[] kv = choice.strip().split("=", 2);
            if (kv.length != 2) throw new IllegalArgumentException("expected library=option, not " + choice);
            map.put(kv[0].strip(), kv[1].strip());
        }
        return choose(map);
    }

    private final List<LibraryMapping> mappings;
    private final Set<String> typeNames;

    public CSharpLibraries(List<LibraryMapping> mappings) {
        this.mappings = List.copyOf(mappings);
        Set<String> names = new HashSet<>();
        this.mappings.forEach(m -> names.addAll(m.typeNames()));
        this.typeNames = Set.copyOf(names);
    }

    public static CSharpLibraries of(LibraryMapping... mappings) {
        return new CSharpLibraries(List.of(mappings));
    }

    /** These mappings, with {@code first} in front of them: its answers win. */
    public CSharpLibraries withFirst(LibraryMapping first) {
        return new CSharpLibraries(java.util.stream.Stream.concat(java.util.stream.Stream.of(first), mappings.stream())
                .toList());
    }

    public List<LibraryMapping> mappings() {
        return mappings;
    }

    @Override
    public String id() {
        return String.join(",", mappings.stream().map(LibraryMapping::id).distinct().toList());
    }

    private <T> T first(Function<LibraryMapping, T> ask) {
        return mappings.stream().map(ask).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public TypeMapping type(TypeInfo typeInfo) {
        return first(m -> m.type(typeInfo));
    }

    @Override
    public Rule call(MethodInfo method, MethodCall call) {
        return first(m -> m.call(method, call));
    }

    @Override
    public Rule constructor(MethodInfo constructor) {
        return first(m -> m.constructor(constructor));
    }

    @Override
    public FieldMapping field(FieldInfo fieldInfo) {
        return first(m -> m.field(fieldInfo));
    }

    @Override
    public PatternMapping pattern(ParameterizedType tested, ParameterizedType expression) {
        return first(m -> m.pattern(tested, expression));
    }

    @Override
    public AttributeMapping annotation(io.codelaser.maddi.cst.api.expression.AnnotationExpression use, Target target,
                                       Function<io.codelaser.maddi.cst.api.expression.Expression, String> print) {
        return first(m -> m.annotation(use, target, print));
    }

    @Override
    public String overrideName(MethodInfo method) {
        return first(m -> m.overrideName(method));
    }

    /** The union of the mappings' requirements: each package and each source once, the first mapping's first. */
    @Override
    public Requirements requirements() {
        java.util.Map<String, NuGetPackage> packages = new java.util.LinkedHashMap<>();
        java.util.Map<String, Source> sources = new java.util.LinkedHashMap<>();
        for (LibraryMapping m : mappings) {
            m.requirements().packages().forEach(p -> packages.putIfAbsent(p.id(), p));
            m.requirements().sources().forEach(src -> sources.putIfAbsent(src.fileName(), src));
        }
        return new Requirements(List.copyOf(packages.values()), List.copyOf(sources.values()));
    }

    /**
     * A project file for the translated code: an SDK library on {@code targetFramework} that references the packages
     * of the mappings; its sources are the files beside it.
     */
    public String projectFile(String targetFramework) {
        return projectFile(targetFramework, false);
    }

    /** The project file; with {@code nullable}, C#'s nullable reference types enabled ({@link CSharpFacts#nullabilityAnalysed}). */
    public String projectFile(String targetFramework, boolean nullable) {
        StringBuilder sb = new StringBuilder("<Project Sdk=\"Microsoft.NET.Sdk\">\n")
                .append("  <!-- generated by maddi's Java -> C# translation; libraries: ").append(id()).append(" -->\n")
                .append("  <PropertyGroup>\n")
                .append("    <TargetFramework>").append(targetFramework).append("</TargetFramework>\n")
                .append("    <Nullable>").append(nullable ? "enable" : "disable").append("</Nullable>\n")
                .append("    <ImplicitUsings>disable</ImplicitUsings>\n")
                .append("  </PropertyGroup>\n");
        List<NuGetPackage> packages = requirements().packages();
        if (!packages.isEmpty()) {
            sb.append("  <ItemGroup>\n");
            packages.forEach(p -> sb.append("    <PackageReference Include=\"").append(p.id()).append("\" Version=\"")
                    .append(p.version()).append("\" />\n"));
            sb.append("  </ItemGroup>\n");
        }
        return sb.append("</Project>\n").toString();
    }

    @Override
    public Set<String> typeNames() {
        return typeNames;
    }
}
