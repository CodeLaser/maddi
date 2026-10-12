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
package io.codelaser.maddi.cst.print.translate;

import java.util.Objects;

/**
 * What a Java-to-X printer did that the target text does not say by itself: where the translation behaves differently
 * from the Java, loses something, or could not translate at all. One message per occurrence, at the Java source
 * position of the element it is about. Each target language has its own {@link Code}s (KotlinPrintCode,
 * CSharpPrintCode); the record, the {@link Severity} scale and the text are shared.
 *
 * @param code     what happened; it determines the {@link Severity}
 * @param type     the fully qualified name of the Java type being printed
 * @param line     the Java source line, -1 when the element has no source
 * @param position the column on that line, -1 when unknown
 * @param detail   the element concerned, briefly
 * @param <C>      the target language's codes
 */
public record PrintMessage<C extends PrintMessage.Code>(C code, String type, int line, int position, String detail) {

    public enum Severity {
        /** The target language says it differently, with the same behaviour. */
        INFO,
        /** The translation can behave differently from the Java at run time. */
        BEHAVIOUR_CHANGE,
        /** Something of the Java's is not in the translation. */
        LOSS,
        /** Not translated: the Java text was printed, or the file will not compile. */
        ERROR
    }

    /** A kind of message of one target language: an enum constant, whose name is the code's text. */
    public interface Code {
        Severity severity();

        String name();
    }

    public PrintMessage {
        Objects.requireNonNull(code);
    }

    public Severity severity() {
        return code.severity();
    }

    @Override
    public String toString() {
        return severity() + " " + code.name() + " " + type
               + (line < 0 ? "" : ":" + line + (position < 0 ? "" : ":" + position))
               + (detail == null || detail.isEmpty() ? "" : " " + detail);
    }
}
