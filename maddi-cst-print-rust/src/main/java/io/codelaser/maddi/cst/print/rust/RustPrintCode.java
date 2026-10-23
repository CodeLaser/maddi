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
    /** A reference cast: Rust has no downcast of a struct; the value is printed uncast. */
    REFERENCE_CAST(Severity.ERROR),
    /** A string's length or character index: Rust counts UTF-8 bytes where Java counts UTF-16 units. */
    STRING_UNITS(Severity.BEHAVIOUR_CHANGE),
    /** {@code BigDecimal}: an {@code f64}, binary floating point where Java computes in exact decimals. */
    BIGDECIMAL_AS_F64(Severity.BEHAVIOUR_CHANGE),
    /** {@code LocalDateTime}, {@code Instant}: a {@code SystemTime}, an instant without Java's calendar fields. */
    TIME_AS_SYSTEM_TIME(Severity.BEHAVIOUR_CHANGE),
    /** {@code String.format}: Java's specifiers as {@code format!}'s; one without a counterpart is printed as is. */
    FORMAT(Severity.INFO),
    /** {@code >>>}: printed as {@code >>} on the unsigned type. */
    UNSIGNED_SHIFT(Severity.INFO);

    private final Severity severity;

    RustPrintCode(Severity severity) {
        this.severity = severity;
    }

    @Override
    public Severity severity() {
        return severity;
    }
}
