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

package io.codelaser.maddi.cst.print.rust;

import io.codelaser.maddi.cst.print.translate.PrintMessage;
import io.codelaser.maddi.cst.print.translate.PrintMessage.Severity;

/**
 * What the Java-to-Rust printer reports about its translation, each code with its {@link Severity}.
 * {@link RustTranslator#module} returns the messages.
 */
public enum RustPrintCode implements PrintMessage.Code {
    /** A superclass other than {@code Object}: Rust has no class inheritance. The inherited members are missing. */
    INHERITANCE(Severity.ERROR),
    /**
     * A source superclass: a {@code base} field holds the parent's state, and inherited fields and methods are reached
     * through it. A call from the parent to a method the subclass overrides stays in the parent: no dynamic dispatch.
     */
    INHERITANCE_AS_COMPOSITION(Severity.LOSS),
    /** A static field that is not a constant: Rust has no mutable statics without {@code unsafe}. Left out. */
    STATIC_STATE(Severity.ERROR),
    /**
     * An eventually immutable type ({@code @Mark}, {@code @Only}) as a typestate: a state parameter on the struct, the
     * mark consuming the object. A mark called on anything but a local variable is {@link #UNSUPPORTED}.
     */
    TYPESTATE(Severity.INFO),
    /** An enum constant with arguments or a body: printed as a plain variant. */
    ENUM_WITH_STATE(Severity.LOSS),
    /** A JDK type without a Rust counterpart in the mapping: printed by its simple name, which will not resolve. */
    UNMAPPED_JDK(Severity.ERROR),
    /** A JDK method without a Rust counterpart in the mapping: printed by its snake-case name. */
    UNMAPPED_JDK_CALL(Severity.ERROR),
    /** A statement or expression the printer does not translate yet: printed as {@code todo!()}. */
    UNSUPPORTED(Severity.ERROR),
    /** A field the constructor leaves unassigned, of a type without an obvious empty value: {@code Default::default()}. */
    FIELD_DEFAULT(Severity.INFO),
    /** {@code throw}: a {@code panic!}, which a caller cannot catch as Java's {@code catch} would. */
    THROW_AS_PANIC(Severity.BEHAVIOUR_CHANGE),
    /** {@code try}: only its body is printed; the handlers are dropped. */
    TRY_BODY_ONLY(Severity.LOSS),
    /** A {@code synchronized} block or method: printed without the lock. */
    SYNCHRONIZED(Severity.LOSS),
    /** A dereference of a value the nullability verdicts call nullable: {@code .unwrap()}, Java's NPE as a panic. */
    UNWRAP_AT_DEREFERENCE(Severity.INFO),
    /** A cast of an {@code Object}: {@code downcast_ref}, which panics where Java throws ClassCastException. */
    DOWNCAST(Severity.INFO),
    /** A reference cast: Rust has no downcast of a struct; the value is printed uncast. */
    REFERENCE_CAST(Severity.ERROR),
    /** A string's length or character index: Rust counts UTF-8 bytes where Java counts UTF-16 units. */
    STRING_UNITS(Severity.BEHAVIOUR_CHANGE),
    /** {@code BigDecimal}: an {@code f64}, binary floating point where Java computes in exact decimals. */
    BIGDECIMAL_AS_F64(Severity.BEHAVIOUR_CHANGE),
    /** {@code LocalDateTime}, {@code Instant}: a {@code SystemTime}, an instant without Java's calendar fields. */
    TIME_AS_SYSTEM_TIME(Severity.BEHAVIOUR_CHANGE),
    /** {@code List.subList}: a copy of the range, where Java's view writes through to the list. */
    SUBLIST_COPY(Severity.BEHAVIOUR_CHANGE),
    /** {@code String.format}: Java's specifiers as {@code format!}'s; one without a counterpart is printed as is. */
    FORMAT(Severity.INFO),
    /** {@code >>>}: printed as {@code >>} on the unsigned type. */
    UNSIGNED_SHIFT(Severity.INFO),
    /** {@code x == null} where the nullability verdicts call {@code x} non-null: the constant outcome. */
    NULL_CHECK_OF_NON_NULL(Severity.INFO);

    private final Severity severity;

    RustPrintCode(Severity severity) {
        this.severity = severity;
    }

    @Override
    public Severity severity() {
        return severity;
    }
}
