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

package org.e2immu.analyzer.aapi.parser;

import org.e2immu.analyzer.modification.common.defaults.DebugVisitor;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.integration.ToolChain;

import java.util.Set;

public interface RunVisitor {

    default void afterAnnotatedApiParsing(JavaInspector javaInspector) {
    }

    default void setContext(String libIn, String libOut, ToolChain.JRE jre) {
    }

    default DebugVisitor debugVisitor() {
        return null;
    }

    default void writeAnalysis(Set<TypeInfo> writeOut) {
    }
}
