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
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Java inner class, a non-static member class, has an enclosing instance; a C# nested class has none. One that uses
 * its enclosing instance gets it as a field, {@code outer}, which every constructor takes first:
 * {@code private readonly LongValueRange outer;}, {@code internal Iterator(LongValueRange outer, …)}. Its uses of
 * the enclosing instance's members go through it ({@code outer.to}), {@code Outer.this} is {@code outer}, and a
 * creation passes it: {@code new Iterator(this, …)} in the enclosing type's code, {@code outer} in a sibling inner
 * class's, the scope of {@code range.new Iterator()}. A class whose superclass takes an outer takes one too, to pass it
 * on. As {@link CSharpAnonymous}'s hoisted classes do for anonymous ones.
 */
final class CSharpInner {

    private static final ThreadLocal<Set<TypeInfo>> IN_PROGRESS = ThreadLocal.withInitial(HashSet::new);

    private CSharpInner() {
    }

    /** An inner class of the program that uses its enclosing instance, or whose superclass does. */
    static boolean withOuter(TypeInfo t) {
        if (t == null || !t.isInnerClass() || t.isAnonymous() || t.enclosingMethod() != null
            || !CSharpNames.translated(t)) {
            return false;
        }
        java.util.Map<TypeInfo, Boolean> cache = CSharpContext.program().inner();
        Boolean known = cache.get(t);
        if (known != null) return known;
        Set<TypeInfo> busy = IN_PROGRESS.get();
        if (!busy.add(t)) return false;
        try {
            boolean computed = usesOuter(t);
            cache.put(t, computed);
            return computed;
        } finally {
            busy.remove(t);
        }
    }

    /** The enclosing type, whose instance {@code outer} is. */
    static TypeInfo outerType(TypeInfo t) {
        return CSharpNames.enclosing(t);
    }

    /** {@code Outer<T>}, the type of the {@code outer} field and parameter. */
    static String outerTypeName(TypeInfo t) {
        TypeInfo outer = outerType(t);
        return CSharpNames.type(outer) + (outer.typeParameters().isEmpty() ? "" : outer.typeParameters().stream()
                .map(CSharpNames::typeParameter).collect(Collectors.joining(", ", "<", ">")));
    }

    private static boolean usesOuter(TypeInfo t) {
        ParameterizedType parent = t.parentClass();
        if (parent != null && parent.typeInfo() != null && withOuter(parent.typeInfo())) return true;
        boolean[] uses = {false};
        for (Element element : code(t)) {
            element.visit((Element e) -> {
                if (uses[0]) return false;
                switch (e) {
                    case VariableExpression ve when ve.variable() instanceof FieldReference fr -> {
                        if (!fr.fieldInfo().isStatic() && CSharpAnonymous.thisScope(fr)
                            && CSharpAnonymous.viaOuter(fr.fieldInfo().owner(), t)) {
                            uses[0] = true;
                        }
                    }
                    case VariableExpression ve when ve.variable() instanceof This th -> {
                        if (th.explicitlyWriteType() != null && !within(t, th.explicitlyWriteType())
                            && !th.explicitlyWriteType().equals(t)) {
                            uses[0] = true;
                        }
                    }
                    case MethodCall mc -> {
                        if (!mc.methodInfo().isStatic() && mc.objectIsImplicit()
                            && CSharpAnonymous.viaOuter(mc.methodInfo().typeInfo(), t)) {
                            uses[0] = true;
                        }
                    }
                    case ConstructorCall cc -> {
                        // a sibling inner class, created with this one's enclosing instance
                        if (cc.object() == null && cc.anonymousClass() == null && cc.constructor() != null) {
                            TypeInfo created = cc.constructor().typeInfo();
                            TypeInfo needed = outerType(created);
                            if (withOuter(created) && needed != null && !needed.equals(t) && !CSharpNames.inherits(t, needed)) {
                                uses[0] = true;
                            }
                        }
                    }
                    default -> {
                    }
                }
                return true;
            });
            if (uses[0]) return true;
        }
        return false;
    }

    private static boolean within(TypeInfo inner, TypeInfo outer) {
        for (TypeInfo at = CSharpNames.enclosing(inner); at != null; at = CSharpNames.enclosing(at)) {
            if (at.equals(outer)) return true;
        }
        return false;
    }

    private static List<Element> code(TypeInfo t) {
        List<Element> code = new ArrayList<>();
        Stream.concat(t.constructors().stream(), t.methods().stream())
                .filter(m -> m.methodBody() != null).forEach(m -> code.add(m.methodBody()));
        for (FieldInfo f : t.fields()) {
            if (f.initializer() != null && !f.initializer().isEmpty()) code.add(f.initializer());
        }
        return code;
    }

    /**
     * The instance fields' initializers, as assignments in the constructor after {@code this.outer = outer;}: Java runs
     * them there, and they may use the enclosing instance, which a C# field initializer cannot (CS0236).
     */
    static List<io.codelaser.maddi.cst.api.output.OutputBuilder> fieldInitializers(TypeInfo t,
                                                                                   io.codelaser.maddi.cst.api.output.Qualification q) {
        List<io.codelaser.maddi.cst.api.output.OutputBuilder> assignments = new ArrayList<>();
        for (FieldInfo f : t.fields()) {
            if (f.isSynthetic() || f.isStatic() || f.initializer() == null || f.initializer().isEmpty()) continue;
            assignments.add(new io.codelaser.maddi.cst.impl.output.OutputBuilderImpl()
                    .add(new io.codelaser.maddi.cst.impl.output.TextImpl("this." + CSharpNames.field(f)))
                    .add(io.codelaser.maddi.cst.impl.output.SymbolEnum.assignment("="))
                    .add(CSharpExpressionPrinter.initializer(f.initializer(), f.type(), q))
                    .add(io.codelaser.maddi.cst.impl.output.SymbolEnum.SEMICOLON));
        }
        return assignments;
    }

    /** The type being printed has an enclosing instance in C#: an inner class's, or a hoisted anonymous class's. */
    private static boolean hasOuter(TypeInfo at) {
        if (withOuter(at)) return true;
        CSharpAnonymous.Hoisted h = at.isAnonymous() ? CSharpContext.hoisted(at) : null;
        return h != null && h.outer();
    }

    /**
     * The path from the code being printed to the instance whose member of {@code owner} it uses implicitly:
     * {@code outer}, {@code outer.outer}; null when that is {@code this}, or no outer leads there.
     */
    static String pathTo(TypeInfo owner) {
        TypeInfo at = CSharpContext.currentType();
        String path = null;
        while (at != null && CSharpAnonymous.viaOuter(owner, at)) {
            if (!hasOuter(at)) return null;
            path = path == null ? "outer" : path + ".outer";
            at = CSharpNames.enclosing(at);
        }
        return at == null ? null : path;
    }

    /**
     * The enclosing instance of a new {@code created}, an inner class with an outer, from the code being printed:
     * {@code this}, {@code outer}, {@code outer.outer}; null when none leads there.
     */
    static String enclosingInstance(TypeInfo created) {
        TypeInfo needed = outerType(created);
        TypeInfo at = CSharpContext.currentType();
        String path = "this";
        while (at != null) {
            if (at.equals(needed) || CSharpNames.inherits(at, needed)) return path;
            if (!hasOuter(at)) return null;
            path = "this".equals(path) ? "outer" : path + ".outer";
            at = CSharpNames.enclosing(at);
        }
        return null;
    }
}
