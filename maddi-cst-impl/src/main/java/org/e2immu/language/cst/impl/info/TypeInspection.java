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

package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeModifier;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;
import org.e2immu.language.cst.api.info.TypeParameter;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface TypeInspection extends Inspection {

    List<FieldInfo> fields();

    Set<TypeInfo> superTypesExcludingJavaLangObject();

    List<TypeParameter> typeParameters();

    boolean isAbstract();

    Stream<MethodInfo> methodStream();

    List<MethodInfo> constructors();

    TypeNature typeNature();

    /**
     * only returns null for JLO, primitives.
     *
     * @return the parent of this type
     */
    ParameterizedType parentClass();

    List<ParameterizedType> interfacesImplemented();

    MethodInfo singleAbstractMethod();

    List<TypeInfo> subTypes();

    Set<TypeModifier> modifiers();

    boolean fieldsAccessedInRestOfPrimaryType();

    MethodInfo enclosingMethod();

    List<TypeInfo> permittedWhenSealed();

    int anonymousTypes();

    boolean isFinal();
}
