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

package io.codelaser.maddi.cst.impl.analysis;

import io.codelaser.maddi.cst.api.analysis.Property;

import java.util.*;

import static io.codelaser.maddi.cst.impl.analysis.PropertyImpl.*;

public class PropertyProviderImpl {

    private PropertyProviderImpl() {}

    private static final List<Property> properties = new ArrayList<>();
    private static final Map<String, Property> propertyMap = new HashMap<>();

    static {
        Collections.addAll(properties,
                ALWAYS_ESCAPES,
                ANALYZER_ERROR,
                ANNOTATED_API,
                COMMUTABLE_METHODS,
                CONTAINER_FIELD,
                CONTAINER_METHOD,
                CONTAINER_PARAMETER,
                CONTAINER_TYPE,
                DEFAULTS_ANALYZER,
                //DOWNCAST_FIELD,
                DOWNCAST_PARAMETER,
                EVENTUAL_METHOD,
                EVENTUAL_PARAMETER,
                EVENTUALLY_FINAL_FIELD,
                EVENTUALLY_IMMUTABLE_TYPE,
                EVENTUALLY_NON_MODIFYING_METHOD,
                EVENTUALLY_UNMODIFIED_PARAMETER,
                FINAL_FIELD,
                FINAL_TYPE,
                FINALIZER_METHOD,
                FLUENT_METHOD,
                GET_SET_EQUIVALENT,
                GET_SET_FIELD,
                IDENTITY_METHOD,
                IGNORE_MODIFICATIONS_FIELD,
                IGNORE_MODIFICATION_METHOD,
                IGNORE_MODIFICATIONS_PARAMETER,
                IMMUTABLE_FIELD,
                IMMUTABLE_METHOD,
                IMMUTABLE_PARAMETER,
                IMMUTABLE_TYPE,
                IMMUTABLE_TYPE_INDEPENDENT_OF_TYPE_PARAMETERS,
                IMPLEMENTATIONS,
                EXTERNAL_IMPLEMENTATIONS,
                INDEPENDENT_FIELD,
                INDEPENDENT_METHOD,
                INDEPENDENT_PARAMETER,
                INDEPENDENT_TYPE,
                INDICES_OF_ESCAPE_METHOD);
        Collections.addAll(properties,
                METHOD_ALLOWS_INTERRUPTS,
                NON_MODIFYING_METHOD,
                STATIC_SIDE_EFFECTS_METHOD,
                NOT_NULL_FIELD,
                NOT_NULL_METHOD,
                NOT_NULL_PARAMETER,
                OWN_FIELDS_READ_MODIFIED_IN_METHOD,
                PARALLEL_PARAMETER_GROUPS,
                PARAMETER_ASSIGNED_TO_FIELD,
                POST_CONDITIONS_METHOD,
                PRECONDITION_METHOD,
                UNMODIFIED_FIELD,
                UNMODIFIED_PARAMETER,
                UTILITY_CLASS
        );
        // ⛔ Added 2026-09-22. A property the ENCODER can write but this provider does not know makes the
        // decoder assert ("Have no property object for key ..."), so the whole results file is unreadable.
        // Both of these are written by live analyzers — DEGRADED_ANALYSIS_METHOD by LinkComputerImpl and
        // SingleIterationAnalyzerImpl, INDEPENDENT_TYPE_PARAMETER by ShallowTypeAnalyzer — and neither was
        // registered, so any run whose results contained one could not be read back at all.
        // TestEveryWritablePropertyDecodes pins the class of defect, unconditionally: AnalysisTier is about
        // RELOAD COST, not persistence (FINAL_FIELD is INTRINSIC and has always been registered), so no tier
        // earns an exemption. INSTANCEOF_SCOPE is registered for the same reason — if it is never written,
        // knowing how to read it costs nothing; if it ever is, the alternative is an unreadable file.
        Collections.addAll(properties,
                DEGRADED_ANALYSIS_METHOD,
                INDEPENDENT_TYPE_PARAMETER,
                INSTANCEOF_SCOPE);
        properties.forEach(p -> propertyMap.put(p.key(), p));
    }

    public static Property get(String propertyName) {
        return propertyMap.get(propertyName);
    }
}
