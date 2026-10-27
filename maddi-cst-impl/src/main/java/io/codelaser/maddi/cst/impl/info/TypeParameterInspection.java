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

package io.codelaser.maddi.cst.impl.info;

import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.info.Access;
import io.codelaser.maddi.cst.api.info.Variance;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.List;

public interface TypeParameterInspection extends Inspection {
    List<ParameterizedType> typeBounds();

    boolean typeBoundsAreSet();

    default Variance variance() {
        return Variance.INVARIANT;
    }

    @Override
    default Access access() {
        throw new UnsupportedOperationException("There is no access for type parameters");
    }

    @Override
    default JavaDoc javaDoc() {
        throw new UnsupportedOperationException("There are no javadocs for type parameters");
    }
}
