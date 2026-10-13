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

import io.codelaser.maddi.cst.print.translate.PrintMessage;
import io.codelaser.maddi.cst.print.translate.PrintMessage.Severity;

/**
 * What the Java-to-C# printer did that the text does not say by itself, one {@link PrintMessage} per occurrence: the codes, each
 * with its {@link Severity}. CSharpCompilationUnitPrinter#printWithMessages returns the messages.
 */
public enum CSharpPrintCode implements PrintMessage.Code {
    /** {@code throws} clauses are dropped: C# has no checked exceptions. */
    THROWS_DROPPED(Severity.INFO),
    /** A wildcard type argument printed as its bound, or {@code object}: C# generics have no wildcards. */
    WILDCARD_AS_BOUND(Severity.LOSS),
    /** A raw generic type printed with {@code object} arguments: C# has no raw types. */
    RAW_TYPE(Severity.LOSS),
    /** A Java instance initializer {@code { … }}: C# has none (yet: to be moved into the constructors). */
    INSTANCE_INITIALIZER(Severity.ERROR),
    /** An enum constant's body, an anonymous subclass of its enum: not translated (yet). */
    ANONYMOUS_CLASS(Severity.ERROR),
    /** A class declared in a method body that captures its locals or its instance: not lifted (yet). */
    LOCAL_CLASS(Severity.ERROR),
    /** {@code Outer.this}: a C# nested class has no enclosing instance (yet: to be passed explicitly). */
    OUTER_THIS(Severity.ERROR),
    /**
     * A class extending ArrayList, printed extending the compatibility library's JavaArrayList, whose Java methods
     * are virtual: a call through a C# List-typed reference does not reach its overrides.
     */
    LIST_SUBCLASS(Severity.BEHAVIOUR_CHANGE),
    /**
     * A field JAXB binds whose type or annotation XmlSerializer has no counterpart of (a map without adapter, an
     * {@code @XmlIDREF}): left out of the XML ({@link JaxbXmlSerialization}).
     */
    XML_UNBOUND(Severity.LOSS),
    /** An enum with fields, methods or constructors, printed as a class with static readonly instances. */
    ENUM_AS_CLASS(Severity.INFO),
    /** A record's canonical or compact constructor: a positional C# record has none (yet). */
    RECORD_CONSTRUCTOR(Severity.LOSS),
    /**
     * An inner (non-static) class of a generic type: C# names it with the enclosing type's arguments,
     * {@code Outer<E>.Inner} (yet).
     */
    NESTED_IN_GENERIC(Severity.ERROR),
    /** A Java annotation type: not translated (yet: to become an attribute class). */
    ANNOTATION_TYPE(Severity.ERROR),
    /** {@code new int[a][b]}: a jagged array of more than one sized dimension has no C# creation expression. */
    MULTI_DIMENSIONAL_ARRAY(Severity.ERROR),
    /** A form of a switch C# cannot say (a pattern, a block arm of a switch expression). */
    SWITCH_FORM(Severity.ERROR),
    /**
     * A JDK type or member the BCL mapping does not translate (yet), printed with its Java name. One per use: the
     * counts per type and member are the mapping's work list.
     */
    UNMAPPED_JDK(Severity.ERROR),
    /** A statement or expression form the printer does not know, printed as Java. */
    JAVA_FALLBACK(Severity.ERROR);

    private final Severity severity;

    CSharpPrintCode(Severity severity) {
        this.severity = severity;
    }

    @Override
    public Severity severity() {
        return severity;
    }
}
