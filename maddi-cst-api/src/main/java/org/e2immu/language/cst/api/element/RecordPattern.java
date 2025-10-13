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

package org.e2immu.language.cst.api.element;

import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;

import java.util.List;

public interface RecordPattern extends Element {
    // situation 1

    boolean unnamedPattern(); // "_" without a type specification

    // situation 2

    LocalVariable localVariable(); // "Circle c" or "Circle _", "var _", but not "_"

    // situation 3

    ParameterizedType recordType(); // Box<String>(...)

    List<RecordPattern> patterns();

    ParameterizedType parameterizedType();

    RecordPattern translate(TranslationMap translationMap);

    interface Builder extends Element.Builder<Builder> {
        Builder setUnnamedPattern(boolean unnamedPattern);

        Builder setLocalVariable(LocalVariable localVariable);

        Builder setRecordType(ParameterizedType recordType);

        Builder setPatterns(List<RecordPattern> patterns);

        RecordPattern build();
    }
}
