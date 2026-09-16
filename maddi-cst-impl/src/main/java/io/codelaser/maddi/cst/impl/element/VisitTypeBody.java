/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, Bart Naudts, https://github.com/CodeLaser/maddi
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

package io.codelaser.maddi.cst.impl.element;

import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Block;

/**
 * What is written inside a type declared in the middle of other code — an anonymous class, or a class or
 * record declared inside a method — offered to a {@link Visitor} that asked for it.
 *
 * <h2>Why both callers share this</h2>
 * {@code ConstructorCallImpl} (the anonymous class) and {@code LocalTypeDeclarationImpl} (the local class)
 * had the same refusal, written once and then copied with the comment "following anonymous class, we're not
 * going deeper here". Two copies of a refusal become two copies of a descent, and then they drift: one
 * walks field initialisers and the other does not, and a metric reports different numbers for the same code
 * written two ways. So there is one of it.
 *
 * <h2>What "inside" means here</h2>
 * Field initialisers, constructors, and method bodies — every expression and statement the author wrote
 * between the braces. Nested types inside it are reached the same way, because walking a method body meets
 * their declarations and asks the visitor again.
 */
public class VisitTypeBody {

    private VisitTypeBody() {
    }

    /** Asks {@code visitor} whether to enter {@code typeInfo}, and walks it if the answer is yes. */
    public static void visit(Visitor visitor, TypeInfo typeInfo) {
        if (typeInfo == null || !visitor.beforeType(typeInfo)) return;
        try {
            for (FieldInfo fieldInfo : typeInfo.fields()) {
                if (fieldInfo.initializer() != null) fieldInfo.initializer().visit(visitor);
            }
            typeInfo.constructorAndMethodStream().forEach(methodInfo -> body(visitor, methodInfo));
        } finally {
            // ⛔ IN A finally. The visitor's own beforeStatement may throw -- several in this codebase stop a
            // walk that way rather than carry a flag -- and an afterType that did not run would leave the
            // visitor believing it is still inside the type for everything that follows.
            visitor.afterType(typeInfo);
        }
    }

    private static void body(Visitor visitor, MethodInfo methodInfo) {
        Block block = methodInfo.methodBody();
        if (block != null && !block.isEmpty()) block.visit(visitor);
    }
}
