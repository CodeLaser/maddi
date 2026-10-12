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

import io.codelaser.maddi.cst.impl.analysis.ModificationFacts;
import io.codelaser.maddi.cst.impl.analysis.NullabilityFacts;

import java.util.Objects;

/**
 * How Java is printed as Kotlin, beyond the formatter's options.
 *
 * @param verdicts  the nullability of declarations and locals, and the use-site facts ({@link NullabilityFacts#STORED}:
 *                  what the analysis stored, if it ran; {@link NullabilityFacts#NONE}: every type as declared)
 * @param nullCheck what to write where Kotlin types a value as nullable and its use needs it non-null
 * @param modification which collections are not modified, for read-only types ({@link ModificationFacts#STORED}:
 *                     what the analysis stored, if it ran; {@link ModificationFacts#NONE}: every collection mutable)
 * @param world        the code translated together, whose uses of a member decide its read-only type
 *                     ({@link KotlinWorld#FILE}: the compilation unit being printed)
 */
public record KotlinPrintOptions(NullabilityFacts verdicts, NullCheck nullCheck, ModificationFacts modification,
                                 KotlinWorld world) {

    /** The verdicts the analysis stored as properties, if it ran; {@code !!} where needed. */
    public static final KotlinPrintOptions DEFAULT = new KotlinPrintOptions(NullabilityFacts.STORED, NullCheck.ASSERT);

    /** With the modification verdicts the analysis stored, if it ran. */
    public KotlinPrintOptions(NullabilityFacts verdicts, NullCheck nullCheck) {
        this(verdicts, nullCheck, ModificationFacts.STORED);
    }

    /** Printing one compilation unit at a time. */
    public KotlinPrintOptions(NullabilityFacts verdicts, NullCheck nullCheck, ModificationFacts modification) {
        this(verdicts, nullCheck, modification, KotlinWorld.FILE);
    }

    /** These options with other modification facts: {@link ModificationFacts#NONE} for the printer without them. */
    public KotlinPrintOptions withModification(ModificationFacts modification) {
        return new KotlinPrintOptions(verdicts, nullCheck, modification, world);
    }

    /** These options with other nullability facts: {@link NullabilityFacts#NONE} for the printer without them. */
    public KotlinPrintOptions withVerdicts(NullabilityFacts verdicts) {
        return new KotlinPrintOptions(verdicts, nullCheck, modification, world);
    }

    /** These options for the code translated together: {@link KotlinWorld#FILE} for one compilation unit. */
    public KotlinPrintOptions withWorld(KotlinWorld world) {
        return new KotlinPrintOptions(verdicts, nullCheck, modification, world);
    }

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
        Objects.requireNonNull(modification);
        Objects.requireNonNull(world);
    }
}
