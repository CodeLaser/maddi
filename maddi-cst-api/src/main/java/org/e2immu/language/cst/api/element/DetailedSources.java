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

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

public interface DetailedSources {
    // used to grab the closing parenthesis of the record field list,
    // the closing parenthesis of any method declaration's form parameter list
    Object END_OF_PARAMETER_LIST = new Object();
    // marker for the "extends" keyword, see TypeInfo.hasImplicitParent()
    Object EXTENDS = new Object();
    // the whole field declaration
    Object FIELD_DECLARATION = new Object();
    // for any sequence separated by ,: this one is added on the element that follows the ,
    Object PRECEDING_COMMA = new Object();
    // for any sequence separated by ,: this one is added on the element that precedes the ,
    Object SUCCEEDING_COMMA = new Object();
    // while the two above work nicely for parameters, fields, type parameters, we need special lists for
    Object ARGUMENT_COMMAS = new Object(); // method call, constructor call, annotation expression
    Object END_OF_ARGUMENT_LIST = new Object(); // method call, constructor call, annotation expression, pos of ')'
    Object TYPE_ARGUMENT_COMMAS = new Object();
    Object EXTENDS_COMMAS = new Object();
    Object IMPLEMENTS_COMMAS = new Object();
    Object PERMITS_COMMAS = new Object();
    Object THROWS_COMMAS = new Object();
    Object LOCAL_VARIABLE_COMMAS = new Object();

    Source detail(Object object);

    // use for typeInfo objects when the detailed sources contain parameterized types, where the same typeInfo object
    // can occur multiple times
    @NotNull
    List<Source> details(Object object);

    DetailedSources merge(DetailedSources other);

    // parameterized types use this as the copyWithoutArrays
    // in this way, we can have the sources of the type without the []
    // see ParseType
    Object associatedObject(Object object);

    DetailedSources withSources(Object o, List<Source> sources);

    interface Builder {

        Builder addAll(DetailedSources detailedSources);

        Builder copy();

        Object getAssociated(Object pt);

        Builder put(Object object, Source source);

        Builder putList(Object object, List<Source> sourceList);

        DetailedSources build();

        // associated object
        Builder putWithArrayToWithoutArray(ParameterizedType withArray, ParameterizedType withoutArray);

        // associated object
        record TypeInfoSource(TypeInfo typeInfo, Source source) {
        }

        Builder putTypeQualification(TypeInfo typeInfo, List<TypeInfoSource> associatedList);
    }
}
