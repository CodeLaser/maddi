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

    // TRUSTED LEAF (docs/design/eventual-design-improvements.md §4): the committed face is Set.copyOf-backed
    // (FieldInspectionImpl's constructor), so the exposed wrapper shares only hidden content -- a fact the
    // analyzer cannot compute from the declared java.util.Set. The Builder is the before-state face, as
    // everywhere in the eventual style.
    @Independent(hc = true)
    Set<FieldModifier> fieldModifiers();

}
