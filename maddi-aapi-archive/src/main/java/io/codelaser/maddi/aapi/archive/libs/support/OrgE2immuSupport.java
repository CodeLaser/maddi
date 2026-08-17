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

package io.codelaser.maddi.aapi.archive.libs.support;

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.eventual.Mark;
import io.codelaser.maddi.annotation.eventual.Only;
import io.codelaser.maddi.annotation.eventual.TestMark;
import io.codelaser.maddi.support.SetOnce;

/**
 * The e2immu support library: the original eventually-immutable containers the CST leans on
 * (TypeInfoImpl.compilationUnitOrEnclosingType is an Either, the inspections sit in SetOnce /
 * EventuallyFinal / EventuallyFinalOnDemand). The annotations mirror the library sources verbatim
 * (maddi-support/src/main/java/io/codelaser/maddi/support) -- the jar carries them as class-file annotations,
 * but nothing materializes external-library bytecode annotations into analysis(), so without this
 * package the whole family reads as absent = MUTABLE and every exposure of an Either or an inspection
 * holder is DEPENDENT (the TypeInfo/TypeParameter cap of the 2026-08 climb, Quest T).
 */
public class OrgE2immuSupport {
    public static final String PACKAGE_NAME = "io.codelaser.maddi.support";

    //public class Either<A, B>
    @ImmutableContainer(hc = true)
    class Either$<A, B> {
        @NotModified A getLeft() { return null; }
        @NotModified B getRight() { return null; }
        @NotModified boolean isLeft() { return false; }
        @NotModified boolean isRight() { return false; }
        @NotModified static <L, R> io.codelaser.maddi.support.Either<L, R> right(@NotNull R right) { return null; }
        @NotModified static <L, R> io.codelaser.maddi.support.Either<L, R> left(@NotNull L left) { return null; }
        @NotModified A getLeftOrElse(@NotNull A orElse) { return null; }
        @NotModified B getRightOrElse(@NotNull B orElse) { return null; }
    }

    //public class SetOnce<T>
    @ImmutableContainer(after = "t", hc = true)
    class SetOnce$<T> {
        @Mark("t")
        void set(@NotNull T t) { }

        @Only(after = "t")
        @NotModified
        @NotNull
        T get() { return null; }

        @Only(after = "t")
        @NotModified
        @NotNull
        T get(String message) { return null; }

        @NotModified
        @Nullable
        T getOrDefaultNull() { return null; }

        @NotModified
        @NotNull
        T getOrDefault(@NotNull T alternative) { return null; }

        @NotModified
        @TestMark("t")
        boolean isSet() { return false; }

        @Modified
        @Mark("t")
        void copy(@NotNull @NotModified SetOnce<T> other) { }
    }

    //public class EventuallyFinal<T>
    @ImmutableContainer(after = "isFinal", hc = true)
    class EventuallyFinal$<T> {
        T get() { return null; }

        @Mark("isFinal")
        void setFinal(T value) { }

        @Only(before = "isFinal")
        void setVariable(T value) { }

        @TestMark("isFinal")
        boolean isFinal() { return false; }

        @TestMark(value = "isFinal", before = true)
        boolean isVariable() { return false; }
    }

    //public class EventuallyFinalOnDemand<T>
    @ImmutableContainer(after = "isFinal", hc = true)
    class EventuallyFinalOnDemand$<T> {
        @NotModified(after = "isFinal")
        T get() { return null; }

        @Mark("isFinal")
        void setFinal(T value) { }

        @Only(before = "isFinal")
        void setVariable(T value) { }

        @Only(before = "isFinal")
        void setOnDemand(Runnable onDemand) { }

        @TestMark("isFinal")
        boolean isFinal() { return false; }

        @TestMark(value = "isFinal", before = true)
        boolean isVariable() { return false; }

        boolean haveOnDemand() { return false; }
    }
}
