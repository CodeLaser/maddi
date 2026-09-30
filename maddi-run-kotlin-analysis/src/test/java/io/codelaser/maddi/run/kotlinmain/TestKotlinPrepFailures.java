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

package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.modification.common.AnalyzerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The two shapes prep could not analyse on detekt (#32), each written down as the source that made it fail. Prep
 * isolated 8 methods there; both causes were in the Kotlin front end.
 * <ul>
 *     <li>the <b>elvis</b> operator was lowered with its left operand in the test and in the branch, as ONE CST
 *     instance. The CST is a tree, so every walker visited those statements twice and prep threw "Trying to
 *     overwrite a value for property variableData" -- 7 of the 8. The same held for a safe call's receiver;</li>
 *     <li>a <b>delegated</b> property's setter got its statement from a convenience constructor that left the
 *     source null, and prep read {@code statement.source().index()} -- the 8th, detekt's
 *     {@code AutocorrectKt.setModifiedText}.</li>
 * </ul>
 */
public class TestKotlinPrepFailures {

    /** As detekt's `UnnecessaryLetKt.canBeReplacedWithCall`: `when (val s = …) { … } ?: return false`. */
    private static final String ELVIS_OVER_A_WHEN = """
            package a

            fun pick(x: Any?): String {
                val first = when (val s = x) {
                    is String -> s
                    is Int -> s.toString()
                    else -> null
                } ?: return "none"
                return first
            }

            fun safe(x: String?): Int = x?.length ?: 0
            """;

    /** As detekt's `var KtFile.modifiedText: String? by UserDataProperty(…)`: a delegated extension property. */
    private static final String DELEGATED_EXTENSION_PROPERTY = """
            package a

            import kotlin.reflect.KProperty

            class Slot {
                private var held: String? = null
                operator fun getValue(thisRef: Any?, property: KProperty<*>): String? = held
                operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
                    held = value
                }
            }

            var String.tag: String? by Slot()

            fun use(s: String) {
                s.tag = "t"
            }
            """;

    @Test
    public void prepAnalysesBothShapes(@TempDir Path tmp) throws Exception {
        KotlinFixture fixture = KotlinFixture.of(tmp, Map.of(
                "a/Elvis.kt", ELVIS_OVER_A_WHEN,
                "a/Delegated.kt", DELEGATED_EXTENSION_PROPERTY), Map.of());

        List<String> isolated = fixture.prepAnalyzer().exceptions().stream()
                .map(AnalyzerException::toString).toList();
        assertEquals(List.of(), isolated, "prep isolated an element");
    }
}
