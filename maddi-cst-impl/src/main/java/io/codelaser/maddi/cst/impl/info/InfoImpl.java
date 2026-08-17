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
