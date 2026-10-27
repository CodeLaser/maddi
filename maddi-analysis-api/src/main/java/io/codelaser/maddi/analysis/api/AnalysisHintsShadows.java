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

package io.codelaser.maddi.analysis.api;

import io.codelaser.maddi.cst.api.expression.StringConstant;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;

/**
 * Recognising the analysis-hints classes and their shadow types in a parse. Moved here from maddi-mod's
 * {@code AnalysisHintsParser} (split stage 3), which delegates: the run drivers assert that no shadow reached
 * the primary types or the call graph, and they must be able to say so without the hints compiler.
 */
public final class AnalysisHintsShadows {
    private AnalysisHintsShadows() {
    }

    /**
     * The package a hints class stands for: the value of its {@code PACKAGE_NAME} string constant, or null when
     * {@code typeInfo} is not a hints class.
     */
    public static String analysisHintsPackage(TypeInfo typeInfo) {
        FieldInfo packageName = typeInfo.getFieldByName("PACKAGE_NAME", false);
        if (packageName != null && packageName.initializer() instanceof StringConstant sc) {
            return sc.constant();
        }
        return null;
    }

    /**
     * Is {@code typeInfo} a hints SHADOW, an {@code X$} stand-in for the library type {@code X}? Both halves are
     * needed: {@code $} is a legal identifier and real libraries use it as a type name ({@code io.vavr.$}), so a
     * name test alone misreads ordinary code as hints. A shadow is always nested inside a hints class.
     */
    public static boolean isAnalysisHintsShadow(TypeInfo typeInfo) {
        return typeInfo.simpleName().endsWith("$")
               && typeInfo.compilationUnitOrEnclosingType().isRight()
               && analysisHintsPackage(typeInfo.compilationUnitOrEnclosingType().getRight()) != null;
    }
}
