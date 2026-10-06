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
package io.codelaser.maddi.cst.print.kotlin;

import java.util.Objects;

/**
 * How Java is printed as Kotlin, beyond the formatter's options.
 *
 * @param verdicts  the nullability of declarations ({@link NullabilityVerdicts#NONE}: every type as declared)
 * @param nullCheck what to write where Kotlin types a value as nullable and its use needs it non-null
 */
public record KotlinPrintOptions(NullabilityVerdicts verdicts, NullCheck nullCheck) {

    public static final KotlinPrintOptions DEFAULT = new KotlinPrintOptions(NullabilityVerdicts.NONE, NullCheck.ASSERT);

    /**
     * A nullable value used where Kotlin needs a non-null one: as a receiver, an argument for a non-null
     * parameter, the value of a non-null return or declaration. The nullability analysis is flow-insensitive, so
     * this is a policy, not a proof.
     */
    public enum NullCheck {
        /** {@code x!!}: throws where Java would have thrown a NullPointerException later. */
        ASSERT,
        /** {@code x?.m()} for a receiver (the call is skipped on null), {@code x!!} elsewhere. */
        SAFE_CALL,
    }

    public KotlinPrintOptions {
        Objects.requireNonNull(verdicts);
        Objects.requireNonNull(nullCheck);
    }
}
