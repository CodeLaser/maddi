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

import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.cst.api.analysis.PropertyValueMap;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.FieldModifier;

import java.util.Set;

public interface FieldInspection extends Inspection {

    /*
    Expressions (currently) don't have an analysis object, so we add one here.
    Required in modification-prepwork. Only implemented on fully built FieldInspectionImpl objects.
     */
    PropertyValueMap analysisOfInitializer();

    Expression initializer();

    // TRUSTED LEAF (docs/eventual-design-improvements.md §4): the committed face is Set.copyOf-backed
    // (FieldInspectionImpl's constructor), so the exposed wrapper shares only hidden content -- a fact the
    // analyzer cannot compute from the declared java.util.Set. The Builder is the before-state face, as
    // everywhere in the eventual style.
    @Independent(hc = true)
    Set<FieldModifier> fieldModifiers();

}
