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

import io.codelaser.maddi.cst.print.translate.PrintMessage;
import io.codelaser.maddi.cst.print.translate.PrintMessage.Severity;

/**
 * What the Java-to-Kotlin printer did that the text does not say by itself, one {@link PrintMessage} per occurrence: the codes, each
 * with its {@link Severity}. KotlinCompilationUnitPrinter#printWithMessages returns the messages.
 */
public enum KotlinPrintCode implements PrintMessage.Code {
    /**
     * An override that returns null where Kotlin's member cannot ({@code clone()}, {@code Map.Entry.setValue},
     * {@code Iterator.next}, …) throws instead: CloneNotSupportedException, UnsupportedOperationException.
     */
    NULL_OVERRIDE_THROWS(Severity.BEHAVIOUR_CHANGE),
    /**
     * {@code x!!} into a non-null declaration: Java stores the null and may never fail, Kotlin throws here
     * (a parameter, a field, a return, an element). The nullability verdicts decide which declarations are non-null.
     */
    ASSERT_INTO_NON_NULL(Severity.BEHAVIOUR_CHANGE),
    /**
     * {@code val b = blocks.getWithKey(j)!!}: Java dereferences the local unconditionally later in its block; the
     * NullPointerException moves to the declaration, before what Java did with the null in between.
     */
    ASSERT_AT_DECLARATION(Severity.BEHAVIOUR_CHANGE),
    /**
     * As ASSERT_AT_DECLARATION, where the linking engine proves that nothing between the declaration and the first
     * dereference refers to the local: the NullPointerException moves up, nothing observable changes with it.
     */
    ASSERT_AT_DECLARATION_UNOBSERVED(Severity.INFO),
    /**
     * A parameter declared non-null as its method's precondition (NullabilityFacts.preconditionNonNull): a caller
     * that may pass null asserts the argument, so the NullPointerException moves from the method's body to the call.
     */
    PRECONDITION_NON_NULL(Severity.BEHAVIOUR_CHANGE),
    /**
     * A non-null parameter whose method answers null with its own exception (NullabilityFacts.rejectsNull): a Java
     * caller's null meets Kotlin's entry check, a NullPointerException, instead (CodeLaser/maddi-mod#28).
     */
    NULL_REJECTION_REPLACED(Severity.BEHAVIOUR_CHANGE),
    /** {@code x?.m()} (NullCheck.SAFE_CALL): where Java throws a NullPointerException, Kotlin goes on with null. */
    SAFE_CALL(Severity.BEHAVIOUR_CHANGE),
    /** {@code x!!.m()}, {@code a!![i]}, {@code for (e in xs!!)}: Java dereferences, and throws, at the same point. */
    ASSERT_AT_DEREFERENCE(Severity.INFO),
    /** {@code map.get(k)!!} into an int, a && operand: Java unboxes, and throws, at the same point. */
    ASSERT_AT_UNBOXING(Severity.INFO),
    /** {@code (x as T)}: Java's unchecked conversion of a raw value, or of a null into a type variable. */
    UNCHECKED_CAST(Severity.INFO),
    /** protected printed without a modifier, public: Java's protected also grants the package access. */
    PROTECTED_AS_PUBLIC(Severity.INFO),
    /** A statement or expression form the printer does not know, printed as Java. */
    JAVA_FALLBACK(Severity.ERROR);

    private final Severity severity;

    KotlinPrintCode(Severity severity) {
        this.severity = severity;
    }

    @Override
    public Severity severity() {
        return severity;
    }
}
