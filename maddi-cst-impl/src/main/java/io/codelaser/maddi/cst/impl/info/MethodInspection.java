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

