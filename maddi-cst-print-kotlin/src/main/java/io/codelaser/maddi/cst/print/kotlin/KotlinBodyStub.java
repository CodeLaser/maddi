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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import io.codelaser.maddi.cst.api.info.MethodInfo;

/**
 * A method whose body {@link KotlinPrintOptions.Bodies#STUB} left as {@code TODO("maddi:<id>")}: what a translator
 * that writes the body (an LLM) gets to go on. KotlinTranslator writes them to {@code maddi-bodies.json}.
 *
 * @param id             the Java method's fully qualified name, as in the stub
 * @param kotlinFile     the Kotlin file holding the stub, relative to the translation's source directory
 * @param javaFile       the Java source file
 * @param javaBeginLine  the method's first line in {@code javaFile} (its Javadoc and annotations not included), or -1
 * @param javaEndLine    its last line, or -1
 * @param declaration    the Kotlin declaration, which the body must fit: the analysis's types, nullability included
 * @param printed        the printer's own translation of the method, body included: a reference, and the fallback
 * @param messages       the printer's messages about that translation ({@code BEHAVIOUR_CHANGE}, {@code INFO}, ...)
 * @param callees        the translated methods and constructors the body calls or references, with their
 *                       declarations: the Kotlin API the body is written against (library methods are not listed)
 * @param fields         the translated fields the body reads or writes, with their declarations as properties (their
 *                       nullability, val or var, @JvmField)
 */
public record KotlinBodyStub(String id, String kotlinFile, String javaFile, int javaBeginLine, int javaEndLine,
                             String declaration, String printed, List<String> messages, List<Callee> callees,
                             List<Callee> fields) {

    /** A translated member the body uses, by its fully qualified name, with its Kotlin declaration. */
    public record Callee(String id, String declaration) {
    }

    public KotlinBodyStub {
        Objects.requireNonNull(id);
        Objects.requireNonNull(declaration);
        messages = List.copyOf(messages);
        callees = List.copyOf(callees);
        fields = List.copyOf(fields);
    }

    /** The stub's id: the Java method's fully qualified name, its parameter types included. */
    public static String id(MethodInfo methodInfo) {
        return methodInfo.fullyQualifiedName();
    }

    /** The stubbed body's one expression: {@code TODO("maddi:<id>")}, a Kotlin string ({@code $} escaped). */
    static String todo(MethodInfo methodInfo) {
        String id = id(methodInfo).replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$");
        return "TODO(\"maddi:" + id + "\")";
    }

    /** {@code stubs} as a JSON array, one object per stub, in this record's terms. */
    public static void write(List<KotlinBodyStub> stubs, Path file) throws IOException {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < stubs.size(); i++) {
            KotlinBodyStub s = stubs.get(i);
            sb.append(i == 0 ? "\n" : ",\n").append("  {");
            field(sb, "id", s.id).append(',');
            field(sb, "kotlinFile", s.kotlinFile).append(',');
            field(sb, "javaFile", s.javaFile).append(',');
            sb.append("\n    \"javaLines\": [").append(s.javaBeginLine).append(", ").append(s.javaEndLine).append("],");
            field(sb, "declaration", s.declaration).append(',');
            field(sb, "printed", s.printed).append(',');
            sb.append("\n    \"messages\": [");
            for (int j = 0; j < s.messages.size(); j++) {
                sb.append(j == 0 ? "" : ", ");
                string(sb, s.messages.get(j));
            }
            sb.append("],");
            members(sb, "callees", s.callees).append(',');
            members(sb, "fields", s.fields).append("\n  }");
        }
        sb.append(stubs.isEmpty() ? "]\n" : "\n]\n");
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, sb);
    }

    private static StringBuilder members(StringBuilder sb, String name, List<Callee> members) {
        sb.append("\n    \"").append(name).append("\": [");
        for (int j = 0; j < members.size(); j++) {
            Callee c = members.get(j);
            sb.append(j == 0 ? "\n" : ",\n").append("      {\"id\": ");
            string(sb, c.id).append(", \"declaration\": ");
            string(sb, c.declaration).append('}');
        }
        return sb.append(members.isEmpty() ? "]" : "\n    ]");
    }

    private static StringBuilder field(StringBuilder sb, String name, String value) {
        sb.append("\n    \"").append(name).append("\": ");
        return string(sb, value);
    }

    private static StringBuilder string(StringBuilder sb, String value) {
        if (value == null) return sb.append("null");
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"');
    }
}
