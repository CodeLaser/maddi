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

package io.codelaser.maddi.cst.print.csharp;

/**
 * What the Java-to-C# printer did that the C# text does not say by itself: where the translation behaves differently
 * from the Java, loses something, or could not translate at all. One message per occurrence, at the Java source
 * position of the element it is about; {@link CSharpCompilationUnitPrinter#printWithMessages} returns them.
 *
 * @param code     what happened; it determines the {@link Severity}
 * @param type     the fully qualified name of the Java type being printed
 * @param line     the Java source line, -1 when the element has no source
 * @param position the column on that line, -1 when unknown
 * @param detail   the element concerned, briefly
 */
public record CSharpPrintMessage(Code code, String type, int line, int position, String detail) {

    public enum Severity {
        /** C# says it differently, with the same behaviour. */
        INFO,
        /** The C# can behave differently from the Java at run time. */
        BEHAVIOUR_CHANGE,
        /** Something of the Java's is not in the C#. */
        LOSS,
        /** Not translated: the file will not compile. */
        ERROR
    }

    public enum Code {
        /** {@code throws} clauses are dropped: C# has no checked exceptions. */
        THROWS_DROPPED(Severity.INFO),
        /** A wildcard type argument printed as its bound, or {@code object}: C# generics have no wildcards. */
        WILDCARD_AS_BOUND(Severity.LOSS),
        /** A raw generic type printed with {@code object} arguments: C# has no raw types. */
        RAW_TYPE(Severity.LOSS),
        /** A Java instance initializer {@code { … }}: C# has none (yet: to be moved into the constructors). */
        INSTANCE_INITIALIZER(Severity.ERROR),
        /** An anonymous class: C# has none (yet: to be hoisted into a nested class, or a lambda). */
        ANONYMOUS_CLASS(Severity.ERROR),
        /** A class declared in a method body: C# has none (yet: to be hoisted into a nested class). */
        LOCAL_CLASS(Severity.ERROR),
        /** {@code Outer.this}: a C# nested class has no enclosing instance (yet: to be passed explicitly). */
        OUTER_THIS(Severity.ERROR),
        /** An enum with fields, methods or constructors, printed as a class with static readonly instances. */
        ENUM_AS_CLASS(Severity.INFO),
        /** A record's canonical or compact constructor: a positional C# record has none (yet). */
        RECORD_CONSTRUCTOR(Severity.LOSS),
        /**
         * A static nested type of a generic type: C# nests it in every instantiation of its enclosing type, so its
         * name needs the enclosing type's arguments (yet: to be moved out of the enclosing type).
         */
        NESTED_IN_GENERIC(Severity.ERROR),
        /** A Java annotation type: not translated (yet: to become an attribute class). */
        ANNOTATION_TYPE(Severity.ERROR),
        /** {@code new int[a][b]}: a jagged array of more than one sized dimension has no C# creation expression. */
        MULTI_DIMENSIONAL_ARRAY(Severity.ERROR),
        /** A form of a switch C# cannot say (a pattern, a block arm of a switch expression). */
        SWITCH_FORM(Severity.ERROR),
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
