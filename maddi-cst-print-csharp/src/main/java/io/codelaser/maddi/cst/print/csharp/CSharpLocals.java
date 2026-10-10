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
import io.codelaser.maddi.cst.api.expression.InstanceOf;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.variable.LocalVariable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The names a block declares at its own level, for {@link CSharpContext#enterScope}. C#'s scoping of locals is
 * stricter than Java's in two ways:
 * <ul>
 *   <li>a local's scope is its whole block, before its declaration too: a nested block may not declare a name its
 *   enclosing block declares later ({@code for (…) { int i; } int i;} is CS0136);</li>
 *   <li>a pattern variable in an {@code if} condition, an expression statement or a declaration is in scope in the
 *   enclosing block, so two {@code if (o is A a)} in one block declare {@code a} twice (CS0128), where Java's flow
 *   scoping sees two variables.</li>
 * </ul>
 * The context renames a declaration that would clash ({@code a}, {@code a2}, …), and every use of it.
 */
final class CSharpLocals {

    private CSharpLocals() {
    }

    /**
     * The locals and pattern variables {@code statements} declare at their own level: not inside a nested block or a
     * lambda, which are scopes of their own. Conditions of loops are included: an extra name only costs a rename.
     */
    static Set<String> declaredIn(List<Statement> statements) {
        Set<String> names = new HashSet<>();
        for (Statement s : statements) {
            if (s instanceof LocalVariableCreation lvc) {
                lvc.localVariableStream().map(LocalVariable::simpleName).forEach(names::add);
            }
            s.visit((Element e) -> {
                if (e instanceof InstanceOf io && io.patternVariable() != null && io.patternVariable().localVariable() != null) {
                    names.add(io.patternVariable().localVariable().simpleName());
                }
                return e == s || !(e instanceof Block) && !(e instanceof Lambda) && !(e instanceof Statement);
            });
        }
        return names;
    }
}
