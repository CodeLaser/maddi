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

package io.codelaser.maddi.cst.print.rust;

import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@code @IgnoreModifications} on a method (a log method writing a buffer) as interior mutability. The method takes
 * {@code &self}, as the verdict treats it as not modifying; the fields it modifies are {@code RefCell<T>}: borrowed
 * mutably in it ({@code borrow_mut()}), read elsewhere through {@code borrow()}, and reached directly through
 * {@code get_mut()} where the object is held exclusively. rustc no longer checks those fields: a conflicting borrow is
 * a run-time {@code BorrowMutError}, the price of the annotation, in Rust as in the book.
 */
final class RustCells {
    private final RustFacts facts;
    private final Map<TypeInfo, Set<FieldInfo>> cells = new HashMap<>();

    RustCells(RustFacts facts) {
        this.facts = facts;
    }

    boolean isCell(FieldInfo f) {
        return !f.isStatic() && cells(f.owner()).contains(f);
    }

    /** The fields of {@code typeInfo} that a method with {@code @IgnoreModifications} modifies. */
    Set<FieldInfo> cells(TypeInfo typeInfo) {
        return cells.computeIfAbsent(typeInfo, this::compute);
    }

    private Set<FieldInfo> compute(TypeInfo typeInfo) {
        Set<FieldInfo> out = new LinkedHashSet<>();
        if (typeInfo.typeNature().isRecord() || typeInfo.isInterface()) return out;
        for (MethodInfo m : typeInfo.methods()) {
            if (!facts.ignoreModifications(m) || m.methodBody() == null) continue;
            m.methodBody().visit(e -> {
                if (e instanceof Assignment a) add(typeInfo, a.target().variable(), out);
                if (e instanceof MethodCall mc && mc.object() instanceof VariableExpression ve && modifies(mc)) {
                    add(typeInfo, ve.variable(), out);
                }
                return true;
            });
        }
        return out;
    }

    private boolean modifies(MethodCall mc) {
        MethodInfo callee = mc.methodInfo();
        if (callee.typeInfo().compilationUnit().externalLibrary()) return RustJdk.modifies(mc);
        return facts.receiver(callee) == RustFacts.Receiver.EXCLUSIVE;
    }

    private static void add(TypeInfo typeInfo, Variable v, Set<FieldInfo> out) {
        if (v instanceof FieldReference fr && (fr.scopeIsThis() || fr.isDefaultScope()) && !fr.fieldInfo().isStatic()
            && fr.fieldInfo().owner() == typeInfo) {
            out.add(fr.fieldInfo());
        }
    }
}
