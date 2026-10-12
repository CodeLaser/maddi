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
package io.codelaser.maddi.cst.print.translate;

import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;

/** Output that Java, Kotlin and C# write with the same tokens, built from what a target printer printed. */
public final class Emission {

    private Emission() {
    }

    /**
     * {@code ++x}, {@code x--}: an assignment that is an increment or decrement, with its target as printed; null for
     * any other assignment.
     */
    public static OutputBuilder increment(Assignment a, OutputBuilder target) {
        if (a.prefixPrimitiveOperator() == null) return null;
        String operator = a.assignmentOperatorIsPlus() ? "++" : "--";
        return a.prefixPrimitiveOperator()
                ? new OutputBuilderImpl().add(SymbolEnum.plusPlusPrefix(operator)).add(target)
                : new OutputBuilderImpl().add(target).add(SymbolEnum.plusPlusSuffix(operator));
    }
}
