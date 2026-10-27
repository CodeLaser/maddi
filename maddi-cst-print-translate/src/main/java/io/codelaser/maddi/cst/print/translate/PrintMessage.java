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
