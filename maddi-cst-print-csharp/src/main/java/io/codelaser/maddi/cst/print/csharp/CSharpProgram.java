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

import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.This;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * What the printer knows of the whole translated program, beyond the file it prints: computed once, before the
 * files are printed, and handed to each {@link CSharpCompilationUnitPrinter}. Without it ({@link #NONE}), every
 * decision is the one that is right whatever the other files contain. Its decisions, each with a {@link Policy}:
 * <ul>
 *   <li>a translated functional interface that no class implements and no interface extends is a C#
 *   {@code delegate}: its lambdas are plain lambdas and its calls invocations. Any other stays an interface with an
 *   adapter class for its lambdas, see {@link CSharpTypePrinter};</li>
 *   <li>a namespace segment with the name of a type, of the program or of the BCL, is plural: in C#, the namespace
 *   {@code Dev.Langchain4j.Exception} hides {@code System.Exception} from the code in {@code Dev.Langchain4j};
 *   {@code Dev.Langchain4j.Exceptions} is .NET's naming too;</li>
 *   <li>C# members are not virtual unless declared so, Java's are: a class nothing extends is {@code sealed}, and a
 *   method nothing overrides is not {@code virtual}. Code outside the program may extend a public API, which the
 *   policy can keep open.</li>
 * </ul>
 */
public final class CSharpProgram {

    /** How a translated functional interface is printed. */
    public enum FunctionalInterfaces {
        /** As a delegate where the whole program allows it, as an interface with a lambda adapter elsewhere. */
        DELEGATE_WHERE_POSSIBLE,
        /** Always as an interface with a lambda adapter. */
        ADAPTER,
    }

    /** Which classes and methods stay open for extension. */
    public enum Inheritance {
        /** The program is all the code there is: only what it extends or overrides is open. */
        CLOSED_WORLD,
        /** Also the public classes, and their public and protected methods: code outside may extend them. */
        OPEN_PUBLIC_API,
        /** Every class that Java allows to be extended, every method Java allows to be overridden. */
        OPEN,
    }

    /** Getters and setters: C#'s properties where they can be (see {@link CSharpProperties}), or Java's methods. */
    public enum Accessors {
        PROPERTIES,
        METHODS,
    }

    /** {@code libraries}: what the libraries the program uses are in C#, see {@link CSharpLibraries}. */
    /** {@code facts}: what the analysis knows about the program, see {@link CSharpFacts}. */
    public record Policy(FunctionalInterfaces functionalInterfaces, Inheritance inheritance, CSharpLibraries libraries,
                         Accessors accessors, CSharpFacts facts) {
        public static final Policy DEFAULT = new Policy(FunctionalInterfaces.DELEGATE_WHERE_POSSIBLE,
                Inheritance.CLOSED_WORLD, CSharpLibraries.DEFAULT, Accessors.PROPERTIES, CSharpFacts.STORED);

        public Policy(FunctionalInterfaces functionalInterfaces, Inheritance inheritance) {
            this(functionalInterfaces, inheritance, CSharpLibraries.DEFAULT, Accessors.PROPERTIES, CSharpFacts.STORED);
        }

        public Policy withLibraries(CSharpLibraries libraries) {
            return new Policy(functionalInterfaces, inheritance, libraries, accessors, facts);
        }

        public Policy withAccessors(Accessors accessors) {
            return new Policy(functionalInterfaces, inheritance, libraries, accessors, facts);
        }

        public Policy withFacts(CSharpFacts facts) {
            return new Policy(functionalInterfaces, inheritance, libraries, accessors, facts);
        }
    }

    /** No knowledge of the other files: functional interfaces stay interfaces, classes and methods stay open. */
    public static final CSharpProgram NONE = new CSharpProgram(Set.of(), Set.of(), Set.of(), Inheritance.OPEN, Map.of(),
            Set.of(), Set.of(), Map.of(), CSharpLibraries.DEFAULT, CSharpProperties.Analysis.NONE, CSharpFacts.NONE,
            Set.of());

    private final Set<TypeInfo> delegates;
    private final Set<TypeInfo> extended;
    private final Set<MethodInfo> overridden;
    private final Inheritance inheritance;
    private final Map<String, String> namespaceSegments;
    private final Set<String> ambiguous;
    private final Set<TypeInfo> prefixedHoisted;
    private final Map<MethodInfo, Integer> typeTokens;
    private final CSharpLibraries libraries;
    private final Map<FieldInfo, CSharpProperties.Property> properties;
    private final Map<MethodInfo, String> computedProperties;
    private final CSharpFacts facts;
    private final Set<ParameterInfo> readOnly;
    private final Map<MethodInfo, CSharpProperties.Property> accessors = new java.util.HashMap<>();
    private final Map<TypeInfo, Boolean> inner = new java.util.concurrent.ConcurrentHashMap<>();
    private Set<MethodInfo> erasedGenerics = Set.of();
    private Set<TypeInfo> exposedBases = Set.of();
    private Map<String, TypeInfo> packageInfos = Map.of();
    private List<TypeInfo> types = List.of();
    private Set<TypeParameter> notNullKeys = Set.of();
    private final Map<TypeInfo, List<TypeParameter>> companions = new java.util.concurrent.ConcurrentHashMap<>();

    private CSharpProgram(Set<TypeInfo> delegates, Set<TypeInfo> extended, Set<MethodInfo> overridden,
                          Inheritance inheritance, Map<String, String> namespaceSegments, Set<String> ambiguous,
                          Set<TypeInfo> prefixedHoisted, Map<MethodInfo, Integer> typeTokens,
                          CSharpLibraries libraries, CSharpProperties.Analysis analysis, CSharpFacts facts,
                          Set<ParameterInfo> readOnly) {
        this.libraries = libraries;
        this.facts = facts;
        this.readOnly = readOnly;
        this.properties = analysis.fields();
        this.computedProperties = analysis.computed();
        properties.values().forEach(p -> {
            accessors.put(p.getter(), p);
            if (p.setter() != null) accessors.put(p.setter(), p);
        });
        this.prefixedHoisted = prefixedHoisted;
        this.typeTokens = typeTokens;
        this.ambiguous = ambiguous;
        this.delegates = delegates;
        this.extended = extended;
        this.overridden = overridden;
        this.inheritance = inheritance;
        this.namespaceSegments = namespaceSegments;
    }

    public static CSharpProgram analyze(Collection<TypeInfo> primaryTypes) {
        return analyze(primaryTypes, Policy.DEFAULT);
    }

    /** The facts about the program of {@code primaryTypes}: the translated code, all of it. */
    public static CSharpProgram analyze(Collection<TypeInfo> primaryTypes, Policy policy) {
        // what the policy's mappings exclude is no program type, as the printing will have it (CSharpNames.translated)
        CSharpContext.analyzing(policy.libraries());
        try {
            return analyzeTranslated(primaryTypes.stream().filter(t -> !policy.libraries().excludes(t)).toList(), policy);
        } finally {
            CSharpContext.analyzing(null);
        }
    }

    private static CSharpProgram analyzeTranslated(Collection<TypeInfo> primaryTypes, Policy policy) {
        List<TypeInfo> all = new ArrayList<>();
        primaryTypes.forEach(t -> collect(t, all));
        Set<TypeInfo> candidates = new HashSet<>();
        Set<TypeInfo> implemented = new HashSet<>();
        Set<TypeInfo> extended = new HashSet<>();
        Set<MethodInfo> overridden = new HashSet<>();
        for (TypeInfo t : all) {
            if (delegateShape(t)) candidates.add(t);
            if (t.parentClass() != null && t.parentClass().typeInfo() != null) extended.add(t.parentClass().typeInfo());
            Stream.concat(t.methods().stream(), t.constructors().stream())
                    .forEach(m -> m.overrides().stream().filter(o -> o != m).forEach(overridden::add));
            // an anonymous class that becomes a lambda is no implementation
            if (t.isAnonymous() && lambdaLike(t)) continue;
            for (ParameterizedType i : t.interfacesImplemented()) {
                if (i.typeInfo() != null) implemented.add(i.typeInfo());
            }
        }
        candidates.removeAll(implemented);
        Set<TypeInfo> delegates = policy.functionalInterfaces() == FunctionalInterfaces.ADAPTER ? Set.of()
                : Set.copyOf(candidates);
        CSharpProgram program = new CSharpProgram(delegates, Set.copyOf(extended), Set.copyOf(overridden),
                policy.inheritance(), namespaceSegments(primaryTypes, all, policy.libraries()),
                ambiguous(primaryTypes, policy.libraries()), prefixedHoisted(all), typeTokens(all), policy.libraries(),
                policy.accessors() == Accessors.PROPERTIES ? CSharpProperties.analyze(all, overridden, delegates)
                        : CSharpProperties.Analysis.NONE,
                policy.facts(), CSharpReadOnly.analyze(all, policy.facts(), delegates));
        program.erasedGenerics = erasedGenerics(all);
        program.exposedBases = exposedBases(all);
        Map<String, TypeInfo> packageInfos = new java.util.HashMap<>();
        primaryTypes.stream().filter(t -> t.typeNature().isPackageInfo())
                .forEach(t -> packageInfos.putIfAbsent(t.packageName(), t));
        program.packageInfos = Map.copyOf(packageInfos);
        program.types = List.copyOf(all);
        // only C#'s nullable context checks a dictionary's key (CS8714)
        if (policy.facts().nullabilityAnalysed()) program.notNullKeys = CSharpNotNullKeys.analyze(all);
        return program;
    }

    /**
     * Java's public class may extend a package-private one, {@code public final class Group0Mapping1CollectorBiNode
     * extends AbstractGroupBiNode}; C#'s base class or interface is at least as accessible as the type (CS0060,
     * CS0061). The supertypes of a public type that are not public, transitively, are printed public.
     */
    private static Set<TypeInfo> exposedBases(List<TypeInfo> all) {
        Set<TypeInfo> exposed = new HashSet<>();
        java.util.Deque<TypeInfo> todo = new java.util.ArrayDeque<>();
        for (TypeInfo t : all) {
            if (publicThroughout(t)) todo.push(t);
        }
        Set<TypeInfo> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            TypeInfo t = todo.pop();
            if (!seen.add(t)) continue;
            List<ParameterizedType> supers = new ArrayList<>(t.interfacesImplemented());
            if (t.parentClass() != null) supers.add(t.parentClass());
            for (ParameterizedType s : supers) {
                TypeInfo st = s.typeInfo();
                if (st == null || !CSharpNames.translated(st)) continue;
                // a nested base needs the types it is nested in as visible
                for (TypeInfo x = st; x != null && !publicThroughout(x); x = CstQueries.enclosing(x)) exposed.add(x);
                todo.push(st);
            }
        }
        return Set.copyOf(exposed);
    }

    /** Public, as declared or as a member of an interface, and so is every type it is nested in. */
    private static boolean publicThroughout(TypeInfo t) {
        for (TypeInfo x = t; x != null; x = CstQueries.enclosing(x)) {
            TypeInfo enclosing = CstQueries.enclosing(x);
            if (x.typeModifiers().stream().noneMatch(io.codelaser.maddi.cst.api.info.TypeModifier::isPublic)
                && (enclosing == null || !enclosing.isInterface())) {
                return false;
            }
        }
        return true;
    }

    /** A type printed public because a public type extends or implements it ({@link #exposedBases}). */
    boolean exposedBase(TypeInfo t) {
        return exposedBases.contains(t);
    }

    /**
     * Java overrides {@code <J> J mapping()} with a method that is not generic, {@code TriFunction<…> mapping()}, by
     * erasure; C# does not. Such a method, whose type parameters only its result mentions, is printed with them erased
     * to their bounds, {@code object Mapping()}, and so are its generic overrides; the others override it with a
     * covariant result, and a call through it casts the result to the type Java inferred.
     * <p>
     * So is a method whose type parameters are self-bounded and only its result mentions,
     * {@code <Score_ extends Score<Score_>> InnerScore<Score_> getScore()}, with all its overrides: where Java infers
     * a raw or captured {@code Score_}, C# has no type argument that satisfies the constraint (CS0411). Erased, each
     * such type parameter is a wildcard, {@code InnerScore<IScore> GetScore()} ({@link CSharpTypeName}).
     */
    private static Set<MethodInfo> erasedGenerics(List<TypeInfo> all) {
        List<MethodInfo> methods = all.stream().flatMap(t -> t.methods().stream()).toList();
        Map<MethodInfo, Set<MethodInfo>> family = new java.util.HashMap<>();
        Set<MethodInfo> erased = new HashSet<>();
        for (MethodInfo m : methods) {
            for (MethodInfo o : m.overrides()) {
                if (o == m || !CSharpNames.translated(o.typeInfo())) continue;
                family.computeIfAbsent(m, x -> new HashSet<>()).add(o);
                family.computeIfAbsent(o, x -> new HashSet<>()).add(m);
                if (m.typeParameters().isEmpty() && !o.typeParameters().isEmpty() && erasable(o)) erased.add(o);
            }
        }
        for (MethodInfo m : methods) {
            if (erased.contains(m) || !selfBoundErasable(m)) continue;
            Set<MethodInfo> component = new HashSet<>(Set.of(m));
            java.util.Deque<MethodInfo> pending = new java.util.ArrayDeque<>(component);
            while (!pending.isEmpty()) {
                for (MethodInfo f : family.getOrDefault(pending.pop(), Set.of())) {
                    if (component.add(f)) pending.push(f);
                }
            }
            if (component.stream().allMatch(CSharpProgram::selfBoundErasable)) erased.addAll(component);
        }
        if (erased.isEmpty()) return Set.of();
        java.util.Deque<MethodInfo> todo = new java.util.ArrayDeque<>(erased);
        while (!todo.isEmpty()) {
            for (MethodInfo f : family.getOrDefault(todo.pop(), Set.of())) {
                if (!f.typeParameters().isEmpty() && erasable(f) && erased.add(f)) todo.push(f);
            }
        }
        return Set.copyOf(erased);
    }

    /**
     * A class's method whose result is one of its type parameters, {@code <J> J}, which no parameter mentions: erased,
     * it is the bound, which an override's result is covariantly. Not an interface's, whose implementations C# matches
     * by their exact result.
     */
    private static boolean erasable(MethodInfo m) {
        ParameterizedType result = m.returnType();
        return !m.typeInfo().isInterface() && result.isTypeParameter() && result.arrays() == 0
               && m.typeParameters().contains(result.typeParameter())
               && m.parameters().stream().noneMatch(p -> m.typeParameters().stream()
                .anyMatch(tp -> CstQueries.mentions(p.parameterizedType(), tp)));
    }

    /**
     * Every type parameter of {@code m} self-bounded by a program type, which has a companion
     * ({@code Score_ extends Score<Score_>}, not {@code Comparable<? super R>}), none mentioned by a parameter, one by
     * the result.
     */
    private static boolean selfBoundErasable(MethodInfo m) {
        List<TypeParameter> typeParameters = m.typeParameters();
        return !typeParameters.isEmpty() && CSharpNames.translated(m.typeInfo())
               && typeParameters.stream().anyMatch(tp -> CstQueries.mentions(m.returnType(), tp))
               && typeParameters.stream().allMatch(tp -> tp.typeBounds().stream().anyMatch(b -> CstQueries.mentions(b, tp)
                    && b.typeInfo() != null && CSharpNames.translated(b.typeInfo()))
                    && m.parameters().stream().noneMatch(p -> CstQueries.mentions(p.parameterizedType(), tp)));
    }

    /** A generic method printed with its type parameters erased ({@link #erasedGenerics}). */
    boolean erasedGeneric(MethodInfo m) {
        return erasedGenerics.contains(m);
    }

    /** What the analysis knows about the program. */
    public CSharpFacts facts() {
        return facts;
    }

    /** The parameter is a read-only collection in C# ({@link CSharpReadOnly}). */
    boolean readOnly(ParameterInfo parameter) {
        return readOnly.contains(parameter);
    }

    /** The property that takes {@code field}'s place; null when it stays a field. */
    /** {@code where T : notnull}: a type parameter that is, or is passed as, a dictionary's key (CSharpNotNullKeys). */
    boolean notNullKey(TypeParameter typeParameter) {
        return notNullKeys.contains(typeParameter);
    }

    /** The program's types, the nested and local ones too. */
    List<TypeInfo> types() {
        return types;
    }

    /** The {@code package-info} of {@code packageName}, whose annotations are the package's; null without one. */
    TypeInfo packageInfo(String packageName) {
        return packageInfos.get(packageName);
    }

    CSharpProperties.Property property(FieldInfo field) {
        return properties.get(field);
    }

    /** The name of the property {@code method} is, without a field: computed or abstract; null otherwise. */
    String computedProperty(MethodInfo method) {
        return computedProperties.get(method);
    }

    /** The name of the property {@code method} reads, with or without a field; null when it is a method. */
    String propertyRead(MethodInfo method) {
        CSharpProperties.Property p = accessors.get(method);
        if (p != null) return method == p.getter() ? p.name() : null;
        return computedProperties.get(method);
    }

    /** The property whose getter or setter {@code method} is; null when it is a method. */
    CSharpProperties.Property accessorOf(MethodInfo method) {
        return accessors.get(method);
    }

    /** The library mappings of the translation. */
    public CSharpLibraries libraries() {
        return libraries;
    }

    /**
     * The methods whose {@code Class<T>} parameter, a type token, becomes their type argument:
     * {@code <T> T fromJson(String json, Class<T> type)} is {@code T FromJson<T>(string json)}, which reads
     * {@code typeof(T)}, and {@code fromJson(s, Foo.class)} is {@code FromJson<Foo>(s)}. A method qualifies when
     * {@code T} is its only type parameter, every call in the program passes a {@code Class<X>} of a known
     * {@code X}, no method reference names it, and the methods it overrides or that override it qualify too.
     */
    private static Map<MethodInfo, Integer> typeTokens(List<TypeInfo> all) {
        Map<MethodInfo, Integer> tokens = new java.util.HashMap<>();
        List<MethodInfo> methods = all.stream().flatMap(t -> t.methods().stream()).toList();
        for (MethodInfo m : methods) {
            int index = typeTokenIndex(m);
            if (index >= 0) tokens.put(m, index);
        }
        if (tokens.isEmpty()) return Map.of();
        Set<MethodInfo> rejected = new HashSet<>();
        Map<MethodInfo, Set<MethodInfo>> family = new java.util.HashMap<>();
        for (MethodInfo m : methods) {
            for (MethodInfo o : m.overrides()) {
                if (o == m) continue;
                family.computeIfAbsent(m, x -> new HashSet<>()).add(o);
                family.computeIfAbsent(o, x -> new HashSet<>()).add(m);
                if (!java.util.Objects.equals(tokens.get(m), tokens.get(o)) || !CSharpNames.translated(o.typeInfo())) {
                    rejected.add(m);
                    rejected.add(o);
                }
            }
        }
        for (TypeInfo t : all) {
            for (Element element : code(t)) {
                element.visit((Element e) -> {
                    if (e instanceof MethodReference mr) rejected.add(mr.methodInfo());
                    if (e instanceof MethodCall mc && tokens.containsKey(mc.methodInfo())) {
                        ParameterizedType argument = mc.parameterExpressions().get(tokens.get(mc.methodInfo()))
                                .parameterizedType();
                        if (!concreteClass(argument)) rejected.add(mc.methodInfo());
                    }
                    return true;
                });
            }
        }
        java.util.Deque<MethodInfo> todo = new java.util.ArrayDeque<>(rejected);
        while (!todo.isEmpty()) {
            for (MethodInfo f : family.getOrDefault(todo.pop(), Set.of())) {
                if (rejected.add(f)) todo.push(f);
            }
        }
        tokens.keySet().removeAll(rejected);
        return Map.copyOf(tokens);
    }

    /** The index of the method's {@code Class<T>} parameter, {@code T} its only type parameter; -1 when none. */
    private static int typeTokenIndex(MethodInfo m) {
        if (m.isConstructor() || m.typeParameters().size() != 1 || !CSharpNames.translated(m.typeInfo())) return -1;
        int found = -1;
        for (int i = 0; i < m.parameters().size(); i++) {
            ParameterizedType pt = m.parameters().get(i).parameterizedType();
            if (pt.arrays() == 0 && pt.typeInfo() != null && "java.lang.Class".equals(pt.typeInfo().fullyQualifiedName())
                && pt.parameters().size() == 1 && pt.parameters().getFirst().wildcard() == null
                && pt.parameters().getFirst().arrays() == 0 && pt.parameters().getFirst().isTypeParameter()
                && pt.parameters().getFirst().typeParameter().equals(m.typeParameters().getFirst())) {
                if (found >= 0 || m.parameters().get(i).isVarArgs()) return -1;
                found = i;
            }
        }
        return found;
    }

    /** {@code Class<X>} of a known {@code X}: the type argument of a call. */
    static boolean concreteClass(ParameterizedType pt) {
        return pt != null && pt.arrays() == 0 && pt.typeInfo() != null
               && "java.lang.Class".equals(pt.typeInfo().fullyQualifiedName()) && pt.parameters().size() == 1
               && pt.parameters().getFirst().wildcard() == null
               && (pt.parameters().getFirst().typeInfo() != null || pt.parameters().getFirst().isTypeParameter());
    }

    /** The index of the method's type token parameter, which C# drops for the type argument; -1 when none. */
    public int typeToken(MethodInfo method) {
        return typeTokens.getOrDefault(method, -1);
    }

    /** The self-bounded type parameters of the program's types ({@link CSharpCompanions#removed}), as they are asked for. */
    Map<TypeInfo, List<TypeParameter>> companions() {
        return companions;
    }

    /** Which inner classes take an enclosing instance ({@link CSharpInner#withOuter}), as they are asked for. */
    Map<TypeInfo, Boolean> inner() {
        return inner;
    }

    static List<Element> code(TypeInfo t) {
        return CstQueries.code(t);
    }

    /**
     * The hoisted types ({@link CSharpNames#hoisted}) whose simple name another type of their namespace has too:
     * {@code RequestContext.Add} and {@code ResponseContext.Add} both land in the namespace.
     */
    private static Set<TypeInfo> prefixedHoisted(List<TypeInfo> all) {
        Map<String, List<TypeInfo>> byName = new java.util.HashMap<>();
        for (TypeInfo t : all) {
            if (t.isAnonymous() || !CSharpNames.translated(t)) continue;
            if (CSharpNames.enclosing(t) != null && !CSharpNames.hoisted(t)) continue;
            byName.computeIfAbsent(t.packageName() + "." + t.simpleName(), n -> new ArrayList<>()).add(t);
        }
        Set<TypeInfo> prefixed = new HashSet<>();
        byName.values().stream().filter(ts -> ts.size() > 1)
                .forEach(ts -> ts.stream().filter(CSharpNames::hoisted).forEach(prefixed::add));
        return Set.copyOf(prefixed);
    }

    /** A hoisted type whose name is prefixed with its enclosing types' names, which keeps it unique. */
    public boolean prefixedHoisted(TypeInfo typeInfo) {
        return prefixedHoisted.contains(typeInfo);
    }

    /**
     * The simple names of the program's top-level types that two namespaces declare, or the BCL does: where usings
     * bring both into scope, C# finds the name ambiguous.
     */
    private static Set<String> ambiguous(Collection<TypeInfo> primaryTypes, CSharpLibraries libraries) {
        Map<String, Set<String>> namespaces = new java.util.HashMap<>();
        for (TypeInfo t : primaryTypes) {
            if (!CSharpNames.translated(t)) continue;
            String simple = t.simpleName();
            String name = t.isInterface() && !t.typeNature().isAnnotation()
                          && !(simple.length() > 1 && simple.charAt(0) == 'I' && Character.isUpperCase(simple.charAt(1)))
                    ? "I" + simple : simple;
            namespaces.computeIfAbsent(name, n -> new HashSet<>()).add(t.packageName());
        }
        Set<String> bcl = libraries.typeNames();
        Set<String> ambiguous = new HashSet<>();
        namespaces.forEach((name, packages) -> {
            if (packages.size() > 1 || bcl.contains(name)) ambiguous.add(name);
        });
        return Set.copyOf(ambiguous);
    }

    /** A type of this simple name is qualified outside its namespace. */
    public boolean ambiguous(String simpleName) {
        return ambiguous.contains(simpleName);
    }

    /** {@code Exception} → {@code Exceptions}: the segments of the program's namespaces that are a type's name too. */
    private static Map<String, String> namespaceSegments(Collection<TypeInfo> primaryTypes, List<TypeInfo> all,
                                                         CSharpLibraries libraries) {
        Set<String> typeNames = new HashSet<>(libraries.typeNames());
        all.stream().filter(t -> !t.isAnonymous()).forEach(t -> typeNames.add(CSharpNames.pascal(t.simpleName())));
        Map<String, String> renamed = new java.util.HashMap<>();
        for (TypeInfo t : primaryTypes) {
            if (!CSharpNames.translated(t) || t.packageName() == null) continue;
            for (String segment : t.packageName().split("\\.")) {
                String pascal = CSharpNames.pascal(segment);
                if (typeNames.contains(pascal)) renamed.put(pascal, plural(pascal));
            }
        }
        return Map.copyOf(renamed);
    }

    static String plural(String word) {
        if (word.endsWith("y") && word.length() > 1 && "aeiou".indexOf(word.charAt(word.length() - 2)) < 0) {
            return word.substring(0, word.length() - 1) + "ies";
        }
        if (word.endsWith("s") || word.endsWith("x") || word.endsWith("ch") || word.endsWith("sh")) return word + "es";
        return word + "s";
    }

    /** A namespace segment, in PascalCase, as printed. */
    public String namespaceSegment(String pascal) {
        return namespaceSegments.getOrDefault(pascal, pascal);
    }

    /** {@code typeInfo} is printed as a C# delegate. */
    public boolean delegate(TypeInfo typeInfo) {
        return delegates.contains(typeInfo);
    }

    /** A class that may have subclasses: one of the program extends it, or the policy keeps it open. */
    public boolean open(TypeInfo typeInfo) {
        return switch (inheritance) {
            case OPEN -> true;
            case OPEN_PUBLIC_API -> extended.contains(typeInfo) || typeInfo.isPubliclyAccessible();
            case CLOSED_WORLD -> extended.contains(typeInfo);
        };
    }

    /** A method that may be overridden: a method of the program overrides it, or the policy keeps it open. */
    public boolean overridable(MethodInfo method) {
        return switch (inheritance) {
            case OPEN -> true;
            case OPEN_PUBLIC_API -> overridden.contains(method) || method.typeInfo().isPubliclyAccessible()
                                    && method.access() != null && (method.access().isPublic() || method.access().isProtected());
            case CLOSED_WORLD -> overridden.contains(method);
        };
    }

    /** The abstract method of a delegate type, which a call invokes. */
    boolean isInvoke(MethodInfo method) {
        return !method.isStatic() && delegate(method.typeInfo()) && method.equals(invoked(method.typeInfo()));
    }

    static MethodInfo invoked(TypeInfo delegate) {
        return CSharpNames.singleAbstractMethod(delegate);
    }

    /** One abstract method, no type parameters of its own, and nothing else: no fields, other methods, nested types. */
    private static boolean delegateShape(TypeInfo t) {
        if (!t.isInterface() || t.typeNature().isAnnotation() || !CSharpNames.translated(t)) return false;
        if (!t.interfacesImplemented().isEmpty()) return false;
        MethodInfo sam = CSharpNames.singleAbstractMethod(t);
        return sam != null && sam.typeParameters().isEmpty()
               && t.methods().stream().filter(m -> !m.isSynthetic()).count() == 1
               && t.fields().stream().allMatch(FieldInfo::isSynthetic)
               && t.subTypes().stream().allMatch(TypeInfo::isSynthetic);
    }

    /**
     * An anonymous class of one interface that only implements its one method, without fields and without using
     * itself: printed as a lambda.
     */
    static boolean lambdaLike(TypeInfo anonymous) {
        if (anonymous.interfacesImplemented().size() != 1) return false;
        List<MethodInfo> methods = anonymous.methods().stream().filter(m -> !m.isSynthetic()).toList();
        if (methods.size() != 1 || anonymous.fields().stream().anyMatch(f -> !f.isSynthetic())
            || anonymous.subTypes().stream().anyMatch(st -> !st.isSynthetic()) || methods.getFirst().methodBody() == null) {
            return false;
        }
        boolean[] usesThis = {false};
        methods.getFirst().methodBody().visit((Element e) -> {
            if (e instanceof VariableExpression ve && ve.variable() instanceof This t && t.typeInfo().equals(anonymous)) {
                usesThis[0] = true;
            }
            return !usesThis[0];
        });
        return !usesThis[0];
    }

    /** The type, its nested types, and the local and anonymous types its code declares, at any depth. */
    private static void collect(TypeInfo t, List<TypeInfo> all) {
        all.add(t);
        t.subTypes().forEach(st -> collect(st, all));
        for (Element element : code(t)) {
            element.visit((Element e) -> {
                if (e instanceof ConstructorCall cc && cc.anonymousClass() != null) collect(cc.anonymousClass(), all);
                if (e instanceof LocalTypeDeclaration ltd) collect(ltd.typeInfo(), all);
                return true;
            });
        }
    }
}
