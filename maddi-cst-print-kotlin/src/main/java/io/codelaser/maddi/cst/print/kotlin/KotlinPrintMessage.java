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

/**
 * What the Java-to-Kotlin printer did that the Kotlin text does not say by itself: where the translation behaves
 * differently from the Java, loses something, or could not translate at all. One message per occurrence, at the Java
 * source position of the element it is about; {@link KotlinCompilationUnitPrinter#printWithMessages} returns them.
 *
 * @param code     what happened; it determines the {@link Severity}
 * @param type     the fully qualified name of the Java type being printed
 * @param line     the Java source line, -1 when the element has no source
 * @param position the column on that line, -1 when unknown
 * @param detail   the element concerned, briefly
 */
public record KotlinPrintMessage(Code code, String type, int line, int position, String detail) {

    public enum Severity {
        /** Kotlin says it differently, with the same behaviour. */
        INFO,
        /** The Kotlin can behave differently from the Java at run time. */
        BEHAVIOUR_CHANGE,
        /** Something of the Java's is not in the Kotlin. */
        LOSS,
        /** Not translated: the Java text was printed, and the file will not compile. */
        ERROR
    }

    public enum Code {
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
        /** A type parameter with more than one bound is printed without them: Kotlin needs a {@code where} clause. */
        BOUNDS_DROPPED(Severity.LOSS),
        /** A statement or expression form the printer does not know, printed as Java. */
        JAVA_FALLBACK(Severity.ERROR);

        private final Severity severity;

        Code(Severity severity) {
            this.severity = severity;
        }

        public Severity severity() {
            return severity;
        }
    }

    public Severity severity() {
        return code.severity();
    }

    @Override
    public String toString() {
        return severity() + " " + code + " " + type + (line < 0 ? "" : ":" + line + (position < 0 ? "" : ":" + position))
               + (detail == null || detail.isEmpty() ? "" : " " + detail);
    }
}
