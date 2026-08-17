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

package io.codelaser.maddi.modification.common.defaults;

import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.runtime.Runtime;

import java.util.Map;

/**
 * Reads the contracts a user wrote in source code: the annotations on an element, translated to property
 * values. Contracts are re-derived on demand from the CST rather than persisted, so the guard can always
 * distinguish "what the user promised" (this reader) from "what the analyzer computed" ({@code analysis()}).
 */
public class ContractReader extends AnnotationToProperty {

    public ContractReader(Runtime runtime) {
        super(runtime, Element::annotations);
    }

    /** The user-written contracts on this element, as property values; empty when not annotated. */
    public Map<Property, Value> contracts(Info info) {
        return annotationsToMap(info, annotationProvider.annotations(info));
    }
}
