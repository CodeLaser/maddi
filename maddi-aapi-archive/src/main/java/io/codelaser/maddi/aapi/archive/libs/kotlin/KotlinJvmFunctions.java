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
