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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private CSharpInner() {
    }

    /** An inner class of the program that uses its enclosing instance, or whose superclass does. */
    static boolean withOuter(TypeInfo t) {
        if (t == null || !t.isInnerClass() || t.isAnonymous() || t.enclosingMethod() != null
            || !CSharpNames.translated(t)) {
            return false;
        }
        Map<TypeInfo, Boolean> cache = CSharpContext.memo().inner;
        Boolean known = cache.get(t);
        if (known != null) return known;
        List<TypeInfo> stack = CSharpContext.innerInProgress();
        Set<TypeInfo> provisional = CSharpContext.innerProvisional();
        int at = stack.indexOf(t);
        if (at >= 0) {
            // a cycle of creations: t is without, for now; what was decided on top of that is provisional. The
            // answer is the least one (true only when a class of the cycle uses its enclosing instance itself).
            for (int i = at + 1; i < stack.size(); i++) provisional.add(stack.get(i));
            return false;
        }
        stack.add(t);
        try {
            boolean computed = usesOuter(t);
            // true is final; false is, unless it rests on a class still being decided
            if (computed || !provisional.contains(t)) cache.put(t, computed);
            return computed;
        } finally {
            stack.removeLast();
            provisional.remove(t);
        }
    }

    /** {@code Outer<T>}, the type of the {@code outer} field and parameter. */
    static String outerTypeName(TypeInfo t) {
        TypeInfo outer = CstQueries.enclosing(t);
        return CSharpNames.type(outer) + CSharpNames.typeParameters(outer.typeParameters());
    }

    private static boolean usesOuter(TypeInfo t) {
        ParameterizedType parent = t.parentClass();
        if (parent != null && parent.typeInfo() != null && withOuter(parent.typeInfo())) return true;
        boolean[] uses = {false};
        for (Element element : CstQueries.code(t)) {
            element.visit((Element e) -> {
                if (uses[0]) return false;
                switch (e) {
                    case VariableExpression ve when ve.variable() instanceof FieldReference fr -> {
                        if (!fr.fieldInfo().isStatic() && CstQueries.onThis(fr)
                            && CSharpNames.foreign(fr.fieldInfo().owner(), t)) {
                            uses[0] = true;
                        }
                    }
                    case VariableExpression ve when ve.variable() instanceof This th -> {
                        if (th.explicitlyWriteType() != null && !CstQueries.within(t, th.explicitlyWriteType())) {
                            uses[0] = true;
                        }
                    }
                    case MethodCall mc -> {
                        if (!mc.methodInfo().isStatic() && mc.objectIsImplicit()
                            && CSharpNames.foreign(mc.methodInfo().typeInfo(), t)) {
                            uses[0] = true;
                        }
                    }
                    case ConstructorCall cc -> {
                        // a sibling inner class, created with this one's enclosing instance
                        if (cc.object() == null && cc.anonymousClass() == null && cc.constructor() != null) {
                            TypeInfo created = cc.constructor().typeInfo();
                            TypeInfo needed = CstQueries.enclosing(created);
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


    /**
     * A class whose instance fields are initialized in its constructors, as Java does after {@code super(…)}: an inner
     * class with an outer, and one with an initializer that uses {@code this}, explicitly or through an instance member
     * ({@code getLogger(getClass())}), which a C# field initializer cannot (CS0027, CS0236).
     */
    static boolean initializedInConstructor(TypeInfo t) {
        if (t == null || t.isInterface() || t.isAnonymous() || t.typeNature().isRecord() || t.typeNature().isEnum()
            || !CSharpNames.translated(t)) {
            return false;
        }
        if (withOuter(t)) return true;
        for (FieldInfo f : t.fields()) {
            if (f.isStatic() || f.isSynthetic() || f.initializer() == null || f.initializer().isEmpty()) continue;
            boolean[] uses = {false};
            f.initializer().visit((Element e) -> {
                if (uses[0]) return false;
                switch (e) {
                    case VariableExpression ve when ve.variable() instanceof This -> uses[0] = true;
                    case VariableExpression ve when ve.variable() instanceof FieldReference fr
                                                    && !fr.fieldInfo().isStatic() && CstQueries.onThis(fr) ->
                            uses[0] = true;
                    case MethodCall mc when !mc.methodInfo().isStatic() && mc.objectIsImplicit() -> uses[0] = true;
                    default -> {
                    }
                }
                return true;
            });
            if (uses[0]) return true;
        }
        return false;
    }

    /**
     * What a constructor of {@code t} starts with, before its own statements: {@code this.outer = outer;} when it takes
     * the enclosing instance, then the {@link #fieldInitializers}.
     */
    static List<OutputBuilder> prologue(TypeInfo t, boolean outer, Qualification q) {
        List<OutputBuilder> prologue = new ArrayList<>();
        if (outer) prologue.add(outerAssignment());
        prologue.addAll(fieldInitializers(t, q));
        return prologue;
    }

    static OutputBuilder outerAssignment() {
        return new OutputBuilderImpl().add(new TextImpl("this.outer = outer;"));
    }

    /**
     * The instance fields' initializers, as assignments in the constructor after {@code this.outer = outer;}: Java runs
     * them there, and they may use the enclosing instance, which a C# field initializer cannot (CS0236).
     */
    static List<OutputBuilder> fieldInitializers(TypeInfo t, Qualification q) {
        List<OutputBuilder> assignments = new ArrayList<>();
        for (FieldInfo f : t.fields()) {
            if (f.isSynthetic() || f.isStatic() || f.initializer() == null || f.initializer().isEmpty()) continue;
            assignments.add(new OutputBuilderImpl()
                    .add(new TextImpl("this." + CSharpNames.field(f)))
                    .add(SymbolEnum.assignment("="))
                    .add(CSharpExpressionPrinter.initializer(f.initializer(), f.type(), q))
                    .add(SymbolEnum.SEMICOLON));
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
        while (at != null && CSharpNames.foreign(owner, at)) {
            if (!hasOuter(at)) return null;
            path = path == null ? "outer" : path + ".outer";
            at = CstQueries.enclosing(at);
        }
        return at == null ? null : path;
    }

    /**
     * The enclosing instance of a new {@code created}, an inner class with an outer, from the code being printed:
     * {@code this}, {@code outer}, {@code outer.outer}; null when none leads there.
     */
    static String enclosingInstance(TypeInfo created) {
        TypeInfo needed = CstQueries.enclosing(created);
        TypeInfo at = CSharpContext.currentType();
        String path = "this";
        while (at != null) {
            if (at.equals(needed) || CSharpNames.inherits(at, needed)) return path;
            if (!hasOuter(at)) return null;
            path = "this".equals(path) ? "outer" : path + ".outer";
            at = CstQueries.enclosing(at);
        }
        return null;
    }
}
