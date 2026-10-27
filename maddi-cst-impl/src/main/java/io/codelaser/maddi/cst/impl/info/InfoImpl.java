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

import io.codelaser.maddi.cst.api.analysis.PropertyValueMap;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.PropertyValueMapImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.cst.impl.variable.DescendModeEnum;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;

import java.util.stream.Stream;

public abstract class InfoImpl implements Info {

    // The analysis overlay is derived analyzer metadata, orthogonal to the immutability of the CST structure it
    // decorates: the analyzer fills it (via analysis().set(...)) AFTER inspection is committed, and it is never
    // part of the committed object. @IgnoreModifications makes it manual hidden content -- its modifications are
    // confined to the ignored stratum and never escape into the structural (accessible) content, so they do not
    // bear on this type's stated immutability. See road-to-immutability section 050, "Ignoring modifications as
    // manual hidden content".
    @IgnoreModifications
    private final PropertyValueMap propertyValueMap = new PropertyValueMapImpl();

    @Override
    public Stream<Variable> variableStreamDescend() {
        return variables(DescendModeEnum.YES);
    }

    @Override
    public Stream<Variable> variableStreamDoNotDescend() {
        return variables(DescendModeEnum.NO);
    }

    @Override
    public PropertyValueMap analysis() {
        if (io.codelaser.maddi.cst.impl.analysis.ConsumptionEdgeRecorder.ENABLED) {
            io.codelaser.maddi.cst.impl.analysis.ConsumptionEdgeRecorder.record(this);
        }
        return propertyValueMap;
    }

    @Override
    public boolean hasBeenAnalyzed() {
        // TODO should add computational analyzer too, later
        return analysis().getOrDefault(PropertyImpl.DEFAULTS_ANALYZER, ValueImpl.BoolImpl.FALSE).isTrue();
    }
}
