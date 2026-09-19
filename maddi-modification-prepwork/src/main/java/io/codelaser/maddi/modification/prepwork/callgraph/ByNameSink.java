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

package io.codelaser.maddi.modification.prepwork.callgraph;

import java.util.List;

/**
 * <b>A declared place where a string becomes a type.</b> {@code Class.forName(s)} is one; so is a resolver a
 * refactoring wrote, such as {@code LazyBinding.field("a.b.X", "instance")}.
 *
 * <h2>⛔ Sinks are DECLARED, never inferred</h2>
 * The tempting alternative — "any string literal that happens to look like a type name" — is a guess dressed as a
 * measurement, and it fails in both directions: {@code "java.lang.String"} in a log message is not a reference, and
 * a name assembled from a constant is one that no pattern will find. Worse, the name often does not reach
 * {@code Class.forName} in the same expression: in a generated {@code LazyBinding} it travels constructor → field
 * → {@code get()}, which no one-level look-through reaches and only full flow analysis would. The toolkit WROTE
 * that resolver, so it knows; it says so here instead of guessing later.
 * <p>
 * The cost of that choice is a blind spot, and the blind spot is counted rather than hidden:
 * {@code ComputeCallGraph.unresolvedSinkCalls()} is how many calls to a declared sink had a class argument that
 * could not be read as a name (a concatenation, a parameter, a method call).
 *
 * @param typeFqn        the declaring type of the sink method, e.g. {@code java.lang.Class}
 * @param methodName     its name, e.g. {@code forName}
 * @param arity          how many parameters it takes, or {@code -1} for any (overloads that agree on meaning)
 * @param classArgument  the 0-based index of the parameter holding the class's BINARY name
 * @param memberArgument the index of the parameter holding a member name, or {@code -1} when there is none
 * @param kind           what the sink does with what it resolves; see {@link Kind}
 */
public record ByNameSink(String typeFqn, String methodName, int arity, int classArgument, int memberArgument,
                         Kind kind) {

    /** What a sink does once it has resolved the name — the shape a verb has to preserve when it edits. */
    public enum Kind {
        /** The class itself: {@code Class.forName}, {@code ClassLoader.loadClass}. */
        TYPE,
        /** A static field's value. */
        FIELD,
        /** A static method, called for its result. */
        CALL,
        /** A constructor. */
        CONSTRUCT,
        /** A static method, called for its effect, with arguments. */
        RUN,
        /** A {@code MethodHandle}, bound to an exact signature. */
        HANDLE
    }

    public ByNameSink {
        if (classArgument < 0) throw new IllegalArgumentException("a sink must say which argument holds the name");
        if (arity >= 0 && (classArgument >= arity || memberArgument >= arity)) {
            throw new IllegalArgumentException("argument index outside the sink's arity: " + typeFqn + "."
                                               + methodName + "/" + arity);
        }
    }

    /**
     * The JDK's own by-name entry points. <b>Not on by default</b>: {@code ComputeCallGraph} recognises nothing
     * until a caller hands it a list, so maddi behaves for everyone else exactly as it did.
     * <p>
     * ⚠ {@code Class.getMethod}/{@code getField} are deliberately absent. They resolve a MEMBER of a class the
     * caller already holds, so the class does not arrive as a string here and the pair only means something across
     * two calls — a chain this version does not follow.
     */
    public static final List<ByNameSink> JDK = List.of(
            new ByNameSink("java.lang.Class", "forName", 1, 0, -1, Kind.TYPE),
            new ByNameSink("java.lang.Class", "forName", 3, 0, -1, Kind.TYPE),
            new ByNameSink("java.lang.ClassLoader", "loadClass", 1, 0, -1, Kind.TYPE));

    public boolean matches(String declaringTypeFqn, String name, int parameterCount) {
        return typeFqn.equals(declaringTypeFqn) && methodName.equals(name)
               && (arity < 0 || arity == parameterCount);
    }
}
