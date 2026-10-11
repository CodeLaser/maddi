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

import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * What the types, members and fields of a library the program uses are in C#: the JDK's ({@link CSharpBcl#JDK}),
 * slf4j's, Jackson's. The translation asks an ordered list of mappings ({@link CSharpLibraries}), and the first that
 * answers wins: a client chooses one mapping per library, and may put one of its own in front to override a single
 * rule. Every method answers null for what the mapping does not know.
 */
public interface LibraryMapping {

    /** {@code jdk}, {@code slf4j:ms-logging}: the library, and after the colon the option. */
    String id();

    /**
     * A type in C#. {@code template}: the name, which gets the Java type arguments, or a pattern of them: {@code {0}}
     * is the first, as a type argument ({@code int}), {@code {0?}} as a type ({@code int?}). {@code compat}: a class
     * of the compatibility library, whose members are the Java ones in PascalCase. {@code dropArguments}: a generic
     * Java type whose C# counterpart is not generic ({@code Class<?>} is {@code Type}).
     */
    record TypeMapping(String template, String namespace, boolean compat, boolean dropArguments) {
    }

    /**
     * A member's translation: {@code value} for a call whose value is used, {@code statement} for one whose is not
     * (null: {@code value}). In a template, {@code $0} is the receiver, {@code $1}, {@code $2}, … the arguments,
     * {@code $*} all of them, {@code $+} all but the first, {@code @1} an argument as an operand, {@code $1.2} the
     * second argument of the first, a call, {@code {R}} the call's return type and {@code {R0}} its type arguments,
     * {@code {T0}} the receiver's.
     */
    record Rule(String value, String statement, List<String> namespaces) {
        /** A template that writes a generic collection, {@code new List<{R0}> { $* }}, uses its namespace. */
        private static final Pattern COLLECTION = Pattern.compile(
                "\\b(List|Dictionary|HashSet|SortedSet|SortedDictionary|LinkedList|Queue|Stack|KeyValuePair"
                + "|IList|IDictionary|ISet|ICollection|IEnumerable|IReadOnlyList|IReadOnlyCollection|Comparer"
                + "|EqualityComparer)<");
        private static final String GENERIC = "System.Collections.Generic";

        public Rule {
            if (!namespaces.contains(GENERIC) && (value != null && COLLECTION.matcher(value).find()
                                                  || statement != null && COLLECTION.matcher(statement).find())) {
                namespaces = Stream.concat(namespaces.stream(), Stream.of(GENERIC)).toList();
            }
        }

        public String template(boolean asStatement) {
            return asStatement && statement != null ? statement : value;
        }
    }

    /** A field in C#, {@code Console.Out}, and the namespace it needs (null: none). */
    record FieldMapping(String value, String namespace) {
    }

    /** A type test in C#, {@code Type { IsConstructedGenericType: true }}, and the namespace it needs. */
    record PatternMapping(String value, String namespace) {
    }

    /** A NuGet package the translated code references: {@code Microsoft.Extensions.Logging.Abstractions 10.0.0}. */
    record NuGetPackage(String id, String version) {
    }

    /** A C# source that goes with the translated code, beside it: {@code JavaCompat.cs}. */
    record Source(String fileName, Supplier<String> content) {
    }

    /** What the translated code needs when it uses the mapping: packages to reference, sources to compile with it. */
    record Requirements(List<NuGetPackage> packages, List<Source> sources) {
        public static final Requirements NONE = new Requirements(List.of(), List.of());
    }

    /** What the translated code needs: the packages it references, the sources that go with it. */
    default Requirements requirements() {
        return Requirements.NONE;
    }

    /** The C# counterpart of {@code typeInfo}, a type of the library. */
    default TypeMapping type(TypeInfo typeInfo) {
        return null;
    }

    /** The rule of a call of {@code method}; {@code call} is null for a method reference. */
    default Rule call(MethodInfo method, MethodCall call) {
        return null;
    }

    /** The rule of a call of the constructor {@code constructor}. */
    default Rule constructor(MethodInfo constructor) {
        return null;
    }

    default FieldMapping field(FieldInfo fieldInfo) {
        return null;
    }

    /**
     * The type test {@code x instanceof tested}, when it is not the test against the type's name;
     * {@code expression}, the type of {@code x}, is null where there is none (a switch's pattern).
     */
    default PatternMapping pattern(ParameterizedType tested, ParameterizedType expression) {
        return null;
    }

    /**
     * The C# name of a translated method that overrides a method of the library, when the C# member has another name
     * than the PascalCase of the Java one: {@code close()} of an {@code AutoCloseable} is {@code Dispose()}.
     */
    default String overrideName(MethodInfo method) {
        return null;
    }

    /** The simple names of the C# types the mapping writes, which a type of the program must not hide. */
    default Set<String> typeNames() {
        return Set.of();
    }
}
