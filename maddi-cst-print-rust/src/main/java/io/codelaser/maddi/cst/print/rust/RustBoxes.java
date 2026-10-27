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

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A struct that holds itself by value has no size: {@code class Node { Node parent; }} is rustc's E0072. A field whose
 * struct leads back to the field's owner through fields held by value, directly or through other structs
 * ({@code XPing.pong}, {@code XPong.ping}), is a {@code Box<T>}, {@code Option<Box<T>>} when nullable. A field in a
 * {@code Vec} or a map, a borrowed one ({@link RustBorrows}), a shared one ({@code Rc}) or a {@code dyn} one is
 * behind a pointer already. In a body, a non-null boxed field is the place {@code (*self.f)}, a nullable one the
 * {@code Option<&T>} of {@code self.f.as_deref()}; what is stored in it is boxed.
 */
final class RustBoxes {
    private final RustFacts facts;
    private final RustBorrows borrows;
    private final Map<FieldInfo, Boolean> boxed = new HashMap<>();

    RustBoxes(RustFacts facts, RustBorrows borrows) {
        this.facts = facts;
        this.borrows = borrows;
    }

    boolean isBoxed(FieldInfo f) {
        return boxed.computeIfAbsent(f, g -> {
            TypeInfo held = byValue(g);
            return held != null && reaches(held, g.owner(), new HashSet<>());
        });
    }

    private boolean reaches(TypeInfo from, TypeInfo to, Set<TypeInfo> seen) {
        if (from == to) return true;
        if (!seen.add(from)) return false;
        for (FieldInfo g : from.fields()) {
            TypeInfo held = byValue(g);
            if (held != null && reaches(held, to, seen)) return true;
        }
        return false;
    }

    // the source struct a field holds by value, or null
    private TypeInfo byValue(FieldInfo f) {
        if (f.isStatic() || f.type().arrays() > 0 || borrows.fields(f.owner()).containsKey(f)) return null;
        TypeInfo ti = f.type().typeInfo();
        if (ti == null || ti.compilationUnit().externalLibrary() || ti.isInterface() || ti.typeNature().isEnum()
            || RustTypes.isShared(ti, facts)) {
            return null;
        }
        return ti;
    }

    /** {@code value}, an owned value for the field (an {@code Option} when nullable), as the field holds it. */
    static String box(String value, boolean nullable, boolean valueNullable) {
        if (!nullable) return "Box::new(" + value + ")";
        if ("None".equals(value)) return value;
        if (!valueNullable) return "Some(Box::new(" + value + "))";
        if (value.startsWith("Some(") && value.endsWith(")")) {
            return "Some(Box::new(" + value.substring(5, value.length() - 1) + "))";
        }
        return value + ".map(Box::new)";
    }
}
