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
