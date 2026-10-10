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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.This;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

    public record Policy(FunctionalInterfaces functionalInterfaces, Inheritance inheritance) {
        public static final Policy DEFAULT = new Policy(FunctionalInterfaces.DELEGATE_WHERE_POSSIBLE,
                Inheritance.CLOSED_WORLD);
    }

    /** No knowledge of the other files: functional interfaces stay interfaces, classes and methods stay open. */
    public static final CSharpProgram NONE = new CSharpProgram(Set.of(), Set.of(), Set.of(), Inheritance.OPEN, Map.of(),
            Set.of());

    private final Set<TypeInfo> delegates;
    private final Set<TypeInfo> extended;
    private final Set<MethodInfo> overridden;
    private final Inheritance inheritance;
    private final Map<String, String> namespaceSegments;
    private final Set<String> ambiguous;

    private CSharpProgram(Set<TypeInfo> delegates, Set<TypeInfo> extended, Set<MethodInfo> overridden,
                          Inheritance inheritance, Map<String, String> namespaceSegments, Set<String> ambiguous) {
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
        return new CSharpProgram(delegates, Set.copyOf(extended), Set.copyOf(overridden), policy.inheritance(),
                namespaceSegments(primaryTypes, all), ambiguous(primaryTypes));
    }

    /**
     * The simple names of the program's top-level types that two namespaces declare, or the BCL does: where usings
     * bring both into scope, C# finds the name ambiguous.
     */
    private static Set<String> ambiguous(Collection<TypeInfo> primaryTypes) {
        Map<String, Set<String>> namespaces = new java.util.HashMap<>();
        for (TypeInfo t : primaryTypes) {
            if (!CSharpNames.translated(t)) continue;
            String simple = t.simpleName();
            String name = t.isInterface() && !t.typeNature().isAnnotation()
                          && !(simple.length() > 1 && simple.charAt(0) == 'I' && Character.isUpperCase(simple.charAt(1)))
                    ? "I" + simple : simple;
            namespaces.computeIfAbsent(name, n -> new HashSet<>()).add(t.packageName());
        }
        Set<String> bcl = CSharpBcl.typeNames();
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
    private static Map<String, String> namespaceSegments(Collection<TypeInfo> primaryTypes, List<TypeInfo> all) {
        Set<String> typeNames = new HashSet<>(CSharpBcl.typeNames());
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
        List<Element> code = new ArrayList<>();
        Stream.concat(t.constructors().stream(), t.methods().stream())
                .filter(m -> m.methodBody() != null).forEach(m -> code.add(m.methodBody()));
        for (FieldInfo f : t.fields()) {
            if (f.initializer() != null && !f.initializer().isEmpty()) code.add(f.initializer());
        }
        for (Element element : code) {
            element.visit((Element e) -> {
                if (e instanceof ConstructorCall cc && cc.anonymousClass() != null) collect(cc.anonymousClass(), all);
                if (e instanceof LocalTypeDeclaration ltd) collect(ltd.typeInfo(), all);
                return true;
            });
        }
    }
}
