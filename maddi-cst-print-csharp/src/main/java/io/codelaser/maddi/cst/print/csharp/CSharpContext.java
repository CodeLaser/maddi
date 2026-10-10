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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The state of one file's printing, shared by the printers: they are created through the cst-api factories, whose
 * signatures carry no context. One per thread; {@link CSharpCompilationUnitPrinter} resets it per file.
 */
final class CSharpContext {

    private static final class State {
        final Deque<TypeInfo> types = new ArrayDeque<>();
        final Deque<MethodInfo> methods = new ArrayDeque<>();
        final Set<CSharpPrintMessage> messages = new LinkedHashSet<>();
        final Set<String> usings = new TreeSet<>();
        final Set<TypeInfo> referenced = new LinkedHashSet<>();
        Set<Object> privateReachedFromOutside = Set.of();
    }

    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);

    private CSharpContext() {
    }

    /** A new file: no types, no messages, no usings. */
    static void reset() {
        STATE.set(new State());
    }

    static void pushType(TypeInfo typeInfo) {
        STATE.get().types.push(typeInfo);
    }

    static void popType() {
        STATE.get().types.pop();
    }

    static TypeInfo currentType() {
        return STATE.get().types.peek();
    }

    static void pushMethod(MethodInfo methodInfo) {
        STATE.get().methods.push(methodInfo);
    }

    static void popMethod() {
        STATE.get().methods.pop();
    }

    static MethodInfo currentMethod() {
        return STATE.get().methods.peek();
    }

    /** A {@code using} directive the printed code needs: a BCL namespace a mapped type lives in. */
    static void using(String namespace) {
        STATE.get().usings.add(namespace);
    }

    static Set<String> usings() {
        return STATE.get().usings;
    }

    /** The private declarations of the file that code outside their owner reaches: see {@link CSharpAccess}. */
    static void privateReachedFromOutside(Set<Object> reached) {
        STATE.get().privateReachedFromOutside = reached;
    }

    static boolean reachedFromOutside(Object info) {
        return STATE.get().privateReachedFromOutside.contains(info);
    }

    /** A type the printed code names by its simple name: it must be in scope, through a {@code using}. */
    static void referenced(TypeInfo typeInfo) {
        STATE.get().referenced.add(typeInfo);
    }

    static Set<TypeInfo> referenced() {
        return STATE.get().referenced;
    }

    /** Records a message about {@code subject}, an element of the type being printed. */
    static void message(CSharpPrintMessage.Code code, Element subject, String detail) {
        TypeInfo type = currentType();
        Element located = subject != null ? subject : type;
        Source source = located == null ? null : located.source();
        boolean known = source != null && !source.isNoSource();
        STATE.get().messages.add(new CSharpPrintMessage(code, type == null ? "?" : type.fullyQualifiedName(),
                known ? source.beginLine() : -1, known ? source.beginPos() : -1, detail));
    }

    /** The messages recorded since the last reset. */
    static List<CSharpPrintMessage> messages() {
        return new ArrayList<>(STATE.get().messages);
    }

    /** An element in a message: its Java text, on one line, shortened. */
    static String describe(Element e) {
        if (e == null) return "";
        if (e instanceof MethodInfo m) return m.fullyQualifiedName();
        if (e instanceof FieldInfo f) return f.owner().simpleName() + "." + f.name();
        if (e instanceof TypeInfo t) return t.fullyQualifiedName();
        String text;
        try {
            text = e.toString().replaceAll("\\s+", " ").trim();
        } catch (RuntimeException re) {
            return e.getClass().getSimpleName();
        }
        return text.length() <= 80 ? text : text.substring(0, 77) + "...";
    }
}
