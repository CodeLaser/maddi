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

package io.codelaser.maddi.modification.analyzer.impl;

import io.codelaser.maddi.modification.common.util.TolerantWrite;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.TypeContainerAnalyzer;
import io.codelaser.maddi.cst.api.analysis.Message;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;

public class TypeContainerAnalyzerImpl extends CommonAnalyzerImpl implements TypeContainerAnalyzer {

    protected TypeContainerAnalyzerImpl(IteratingAnalyzer.Configuration configuration, AtomicInteger propertiesChanged, List<Message> analyzerMessages) {
        super(configuration, propertiesChanged, analyzerMessages);
    }

    @Override
    public void go(TypeInfo typeInfo) {
        Value.Bool container = typeInfo.analysis().getOrDefault(PropertyImpl.CONTAINER_TYPE, FALSE);
        if (container.isTrue()) {
            return; // no point
        }
        boolean isContainer = typeInfo.constructorAndMethodStream()
                .filter(mi -> !mi.access().isPrivate())
                .flatMap(mi -> mi.parameters().stream())
                .allMatch(ParameterInfo::isUnmodified);
        if (TolerantWrite.setAllowControlledOverwrite(typeInfo.analysis(), PropertyImpl.CONTAINER_TYPE,
                ValueImpl.BoolImpl.from(isContainer), typeInfo)) {
            DECIDE.debug("TC: Decide container of type {} = {}", typeInfo, isContainer);
            propertyChanges.incrementAndGet();
        }
    }
}
