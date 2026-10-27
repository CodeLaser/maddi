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
import io.codelaser.maddi.cst.api.element.RecordPattern;
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
                if (e instanceof InstanceOf io && io.patternVariable() != null) patternVariables(io.patternVariable(), names);
                return e == s || !(e instanceof Block) && !(e instanceof Lambda) && !(e instanceof Statement);
            });
        }
        return names;
    }

    /** The variables of a pattern, a record pattern's at any depth. */
    private static void patternVariables(RecordPattern pattern, Set<String> names) {
        if (pattern.localVariable() != null) names.add(pattern.localVariable().simpleName());
        if (pattern.patterns() != null) pattern.patterns().forEach(p -> patternVariables(p, names));
    }
}
