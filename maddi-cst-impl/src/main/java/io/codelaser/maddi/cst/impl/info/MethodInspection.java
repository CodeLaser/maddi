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

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.MethodModifier;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.info.TypeParameter;

import java.util.List;
import java.util.Set;

public interface MethodInspection extends Inspection {
    List<ParameterizedType> exceptionTypes();

    Set<MethodInfo> overrides();

    List<TypeParameter> typeParameters();

    Set<MethodModifier> modifiers();

    enum OperatorType {
        NONE, INFIX, PREFIX, POSTFIX,
    }

    ParameterizedType returnType();

    OperatorType operatorType();

    Block methodBody();

    String fullyQualifiedName();

    List<ParameterInfo> parameters();

    MethodInfo.MissingData missingData();
}

