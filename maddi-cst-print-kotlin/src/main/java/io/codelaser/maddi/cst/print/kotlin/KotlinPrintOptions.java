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
    @SuppressWarnings("unused") // the ablation's switch (maddi-mod's TestJavaToKotlinAblation, KotlinConf): no production caller by design
    public KotlinPrintOptions withModification(ModificationFacts modification) {
        return new KotlinPrintOptions(verdicts, nullCheck, modification, world);
    }

    /** These options with other nullability facts: {@link NullabilityFacts#NONE} for the printer without them. */
    @SuppressWarnings("unused") // the ablation's switch (maddi-mod's TestJavaToKotlinAblation, KotlinConf): no production caller by design
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
