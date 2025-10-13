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

public interface DependentVariable extends Variable {
    Variable arrayVariable();

    default Variable arrayVariableBase() {
        Variable av = arrayVariable();
        while (av instanceof DependentVariable dv) {
            av = dv.arrayVariable();
        }
        return av;
    }

    Variable indexVariable();

    Expression arrayExpression();

    Expression indexExpression();

    @Override
    default Variable fieldReferenceBase() {
        return arrayVariable().fieldReferenceBase();
    }

    @Override
    default FieldReference fieldReferenceScope() {
        return arrayVariable().fieldReferenceScope();
    }
}
