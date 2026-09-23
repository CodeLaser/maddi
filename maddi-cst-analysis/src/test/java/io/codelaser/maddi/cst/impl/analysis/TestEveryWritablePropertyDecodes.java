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
import io.codelaser.maddi.cst.api.analysis.Property.AnalysisTier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⛔ <b>Everything the encoder can write, the decoder must be able to read.</b> A {@link Property} declared
 * in {@link PropertyImpl} but absent from {@link PropertyProviderImpl} does not degrade gracefully: the
 * decoder asserts "Have no property object for key ...", and the ENTIRE results file is lost — not just
 * that one value.
 *
 * <p>Found 2026-09-22 while establishing whether a Kotlin analysis can round-trip.
 * {@code DEGRADED_ANALYSIS_METHOD} (written by {@code LinkComputerImpl} and
 * {@code SingleIterationAnalyzerImpl}) and {@code INDEPENDENT_TYPE_PARAMETER} (written by
 * {@code ShallowTypeAnalyzer}) were both missing, so any run whose results contained one was unreadable.
 * Two registrations fixed the instances; this test removes the class.
 *
 * <p>⚠ There is NO exemption, and the first draft of this test wrongly invented one. {@code AnalysisTier}
 * grades RELOAD COST, not persistence — {@code FINAL_FIELD} is {@code INTRINSIC} and has always been
 * registered — so an "INTRINSIC means never written" rule fails on the first property it meets. Since an
 * unregistered property costs the whole file and a registered-but-never-written one costs nothing, the
 * invariant is unconditional.
 */
public class TestEveryWritablePropertyDecodes {

    @Test
    public void everyNonIntrinsicPropertyResolves() throws IllegalAccessException {
        List<String> unresolvable = new ArrayList<>();
        int checked = 0;
        for (Field field : PropertyImpl.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !Property.class.isAssignableFrom(field.getType())) {
                continue;
            }
            Property property = (Property) field.get(null);
            if (property == null) continue;
            ++checked;
            if (PropertyProviderImpl.get(property.key()) == null) {
                unresolvable.add(field.getName() + " (" + property.key() + ", tier " + property.analysisTier() + ")");
            }
        }
        // the identity check: if reflection ever stops finding the constants this test passes vacuously
        assertTrue(checked > 40, "expected to check every PropertyImpl constant, only saw " + checked);
        assertEquals(List.of(), unresolvable,
                "declared in PropertyImpl but unknown to PropertyProviderImpl; a results file containing one"
                + " cannot be decoded AT ALL — not just that value, the whole file. Add it to the provider.");
    }
}
