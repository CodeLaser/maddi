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

package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.TypeContainerAnalyzer;
import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TypeContainerAnalyzerImpl extends CommonAnalyzerImpl implements TypeContainerAnalyzer {

    protected TypeContainerAnalyzerImpl(IteratingAnalyzer.Configuration configuration) {
        super(configuration);
    }

    private record OutputImpl(Set<MethodInfo> externalWaitForCannotCauseCycles) implements Output {

        @Override
        public List<AnalyzerException> analyzerExceptions() {
            return List.of();
        }
    }

    @Override
    public Output go(TypeInfo typeInfo) {
        Value.Bool container = typeInfo.analysis().getOrNull(PropertyImpl.CONTAINER_TYPE, ValueImpl.BoolImpl.class);
        if (container == null) {
            Set<MethodInfo> externalWaitForCannotCauseCycles = new HashSet<>();
            boolean isContainer = true;
            for (MethodInfo methodInfo : typeInfo.constructorsAndMethods()) {
                for (ParameterInfo pi : methodInfo.parameters()) {
                    Value.Bool unmodified = pi.analysis().getOrNull(PropertyImpl.UNMODIFIED_PARAMETER,
                            ValueImpl.BoolImpl.class);
                    if (unmodified == null) {
                        externalWaitForCannotCauseCycles.add(methodInfo);
                    } else if (unmodified.isFalse()) {
                        isContainer = false;
                        break;
                    }
                }
                if (!isContainer) break;
            }
            if (externalWaitForCannotCauseCycles.isEmpty()) {
                typeInfo.analysis().set(PropertyImpl.CONTAINER_TYPE, ValueImpl.BoolImpl.from(isContainer));
                DECIDE.debug("TC: Decide container of type {} = {}", typeInfo, isContainer);
            } else {
                UNDECIDED.debug("TC: Container of type {} undecided: {}", typeInfo, externalWaitForCannotCauseCycles);
            }
            return new OutputImpl(externalWaitForCannotCauseCycles);
        }
        return new OutputImpl(Set.of());
    }
}
