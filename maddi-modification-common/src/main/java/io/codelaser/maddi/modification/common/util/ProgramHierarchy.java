/*
 * Copyright (c) 2022-2026, CodeLaser BV, Belgium.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * Proprietary and confidential.
 */
package io.codelaser.maddi.modification.common.util;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * What a WHOLE PROGRAM knows about its class hierarchy, for a consumer that will only ever see a sliver of it.
 *
 * <h2>Visible finality</h2>
 * An isolate keeps one type and stubs the rest, so anything decided from "who extends this?" is decided on a
 * tree that has lost nearly every subclass there ever was. The keyword {@code final} survives isolation
 * ({@code IsolationCore.applyStubTypeAccess}); the far commoner fact — <em>nothing in this program extends
 * this class</em> — does not, because it is not written anywhere. The isolator is the one party that has the
 * whole program in hand, so it can carry that fact into the stub as the only thing a declaration can say:
 * {@code final}. That is a statement about the closed program the isolate was cut from, not about the
 * original's source text, and it is opt-in ({@code IsolateClass.withClassesNeverExtended}) for that reason.
 * <p>
 * Every class is looked at, not just the named ones: a member class, a local class, and — the one that is easy
 * to miss — an anonymous {@code new Base() { ... }}, which extends {@code Base} as surely as a named subclass
 * and may override the very method somebody is about to call unoverridable. Bodies of lambdas are walked too,
 * since an anonymous class can sit inside one.
 * <p>
 * Only the program's own source classes are candidates. A library class is known from its class file alone, its
 * subclasses in the same library are not loaded until something names them, and "nobody extends it" would be a
 * statement about what happened to be loaded.
 */
public final class ProgramHierarchy {

    private ProgramHierarchy() {
    }

    /**
     * @param primaryTypes the program's own primary types, i.e. parsed from source
     * @return the plain, non-abstract, non-final classes among them and their member types that no type of the
     * program — named, local or anonymous — has as its parent class
     */
    public static Set<TypeInfo> classesNeverExtended(Collection<TypeInfo> primaryTypes) {
        Set<TypeInfo> declared = new HashSet<>();
        Set<TypeInfo> extended = new HashSet<>();
        Set<TypeInfo> seen = new HashSet<>();
        for (TypeInfo primary : primaryTypes) {
            primary.recursiveSubTypeStream().forEach(t -> {
                declared.add(t);
                walk(t, extended, seen);
            });
        }
        Set<TypeInfo> result = new HashSet<>();
        for (TypeInfo t : declared) {
            if (t.typeNature().isClass() && !t.isAbstract() && !t.isFinal() && !extended.contains(t)) result.add(t);
        }
        return result;
    }

    private static void walk(TypeInfo type, Set<TypeInfo> extended, Set<TypeInfo> seen) {
        if (!seen.add(type)) return;
        ParameterizedType parent = type.parentClass();
        if (parent != null && parent.typeInfo() != null) extended.add(parent.typeInfo());
        type.constructorAndMethodStream().forEach(m -> walk(m.methodBody(), extended, seen));
        for (FieldInfo field : type.fields()) {
            if (field.initializer() != null) field.initializer().visit(e -> visit(e, extended, seen));
        }
    }

    private static void walk(Block body, Set<TypeInfo> extended, Set<TypeInfo> seen) {
        if (body != null) body.visit(e -> visit(e, extended, seen));
    }

    private static boolean visit(Element e, Set<TypeInfo> extended, Set<TypeInfo> seen) {
        if (e instanceof ConstructorCall cc && cc.anonymousClass() != null) {
            walk(cc.anonymousClass(), extended, seen);
        } else if (e instanceof LocalTypeDeclaration ltd) {
            ltd.typeInfo().recursiveSubTypeStream().forEach(t -> walk(t, extended, seen));
        } else if (e instanceof Lambda lambda) {
            walk(lambda.methodBody(), extended, seen);
        }
        return true;
    }
}
