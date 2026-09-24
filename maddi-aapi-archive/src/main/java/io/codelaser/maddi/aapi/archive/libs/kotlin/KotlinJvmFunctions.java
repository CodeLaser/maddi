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
package io.codelaser.maddi.aapi.archive.libs.kotlin;

import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;

/**
 * The annotated API for {@code kotlin.jvm.functions}: the interfaces a Kotlin function type compiles to. A Kotlin
 * {@code (T) -> R} is one {@code Function1} where Java has {@code Function}, {@code Consumer}, {@code Predicate} and
 * the rest, so these take the contract of the GENERAL Java interface, {@code java.util.function.Function}: an
 * argument handed to an unknown function may be modified ({@code @Modified}), and the function object shares nothing
 * but hidden content with its caller ({@code @Independent(hc = true)}).
 * <p>
 * Without an entry, {@code f(b)} on a {@code (Box) -> Unit} left {@code b} unmodified where Java's
 * {@code Consumer.accept(b)} marks it modified: an unannotated library parameter reads unmodified.
 * {@code Predicate.test} is the one JDK interface whose argument is {@code @NotModified}; a Kotlin predicate has no
 * type of its own to carry that, so it is contracted as a function here, the conservative side.
 * <p>
 * Arities 0-3 cover what the corpora call; kotlinc's interfaces run to {@code Function22}.
 */
public class KotlinJvmFunctions {
    public static final String PACKAGE_NAME = "kotlin.jvm.functions";

    @Independent(hc = true)
    class Function0$<R> {
        R invoke() {
            return null;
        }
    }

    @Independent(hc = true)
    class Function1$<P1, R> {
        R invoke(@Modified P1 p1) {
            return null;
        }
    }

    @Independent(hc = true)
    class Function2$<P1, P2, R> {
        R invoke(@Modified P1 p1, @Modified P2 p2) {
            return null;
        }
    }

    @Independent(hc = true)
    class Function3$<P1, P2, P3, R> {
        R invoke(@Modified P1 p1, @Modified P2 p2, @Modified P3 p3) {
            return null;
        }
    }
}
