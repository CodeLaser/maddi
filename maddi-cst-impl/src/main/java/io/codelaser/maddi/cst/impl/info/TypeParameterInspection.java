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
