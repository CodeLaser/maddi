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
import java.util.Set;
import java.util.stream.Stream;

/**
 * What the printer knows of the whole translated program, beyond the file it prints: computed once, before the
 * files are printed, and handed to each {@link CSharpCompilationUnitPrinter}. Without it ({@link #NONE}), every
 * decision is the one that is right whatever the other files contain.
 * <p>
 * Its first decision is the form of a translated functional interface. As a C# {@code delegate}, its lambdas are
 * plain lambdas and its calls invocations, which is idiomatic; that is only possible when no class implements it, and
 * no interface extends it. Otherwise it stays an interface with an adapter class for its lambdas, see
 * {@link CSharpTypePrinter}.
 */
public final class CSharpProgram {

    /** How a translated functional interface is printed. */
    public enum FunctionalInterfaces {
        /** As a delegate where the whole program allows it, as an interface with a lambda adapter elsewhere. */
        DELEGATE_WHERE_POSSIBLE,
        /** Always as an interface with a lambda adapter. */
        ADAPTER,
    }

    /** No knowledge of the other files: functional interfaces stay interfaces. */
    public static final CSharpProgram NONE = new CSharpProgram(Set.of());

    private final Set<TypeInfo> delegates;

    private CSharpProgram(Set<TypeInfo> delegates) {
        this.delegates = delegates;
    }

    public static CSharpProgram analyze(Collection<TypeInfo> primaryTypes) {
        return analyze(primaryTypes, FunctionalInterfaces.DELEGATE_WHERE_POSSIBLE);
    }

    /** The facts about the program of {@code primaryTypes}: the translated code, all of it. */
    public static CSharpProgram analyze(Collection<TypeInfo> primaryTypes, FunctionalInterfaces functionalInterfaces) {
        if (functionalInterfaces == FunctionalInterfaces.ADAPTER) return NONE;
        List<TypeInfo> all = new ArrayList<>();
        primaryTypes.forEach(t -> collect(t, all));
        Set<TypeInfo> candidates = new HashSet<>();
        Set<TypeInfo> implemented = new HashSet<>();
        for (TypeInfo t : all) {
            if (delegateShape(t)) candidates.add(t);
            // an anonymous class that becomes a lambda is no implementation
            if (t.isAnonymous() && lambdaLike(t)) continue;
            for (ParameterizedType i : t.interfacesImplemented()) {
                if (i.typeInfo() != null) implemented.add(i.typeInfo());
            }
        }
        candidates.removeAll(implemented);
        return new CSharpProgram(Set.copyOf(candidates));
    }

    /** {@code typeInfo} is printed as a C# delegate. */
    public boolean delegate(TypeInfo typeInfo) {
        return delegates.contains(typeInfo);
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
