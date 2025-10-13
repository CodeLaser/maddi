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

package org.e2immu.language.cst.api.variable;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldInfo;

public interface FieldReference extends Variable {
    FieldInfo fieldInfo();

    /**
     * when null, the scope is implicitly an instance of "this"
     *
     * @return the scope of the field, as in "scope.field" or "someMethod().field"
     */
    Expression scope();

    /**
     * @return not-null when the <code>scope()</code> expression is a variable
     */
    Variable scopeVariable();

    boolean scopeIsRecursivelyThis();

    /**
     * True when the source code does not contain a scope; the field is referenced directly without '.'
     */
    boolean isDefaultScope();

    boolean scopeIsThis();

    @Override
    default FieldReference fieldReferenceScope() {
        return this;
    }

    @Override
    default Variable fieldReferenceBase() {
        Variable scopeVariable = scopeVariable();
        if (scopeVariable instanceof FieldReference fr) return fr.fieldReferenceBase();
        return scopeVariable;
    }
}
