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

package io.codelaser.maddi.annotation.rare;

import io.codelaser.maddi.annotation.Final;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation on a method indicating that after calling this method, no other methods may be called anymore on the
 * object: the call consumes its receiver (in Rust, {@code self} by value). After calling a finalizer, the object has
 * gone into a <em>final</em> state.
 * <p>
 * A finalizer is a contract where it is written (in source, or in the annotated APIs: the stream operations), and it
 * is computed by the ownership pass (maddi-mod docs/roadmap/ownership.md, stage 6) for a method that both needs to be
 * one and may be one. It needs to be one when it moves an owned field of its object out (returns it, or hands it over
 * to an object that keeps it). It may be one when every call site is known (a private or package-private method, or
 * the whole application analysed) and every caller hands its receiver over: a new object, a fresh result, a fluent
 * chain on one, or a local that is not used after the call.
 * <p>
 * A call of a finalizer never counts as a modification of its receiver: nobody can observe the object afterwards.
 * <p>
 * The life-cycle rules, in ownership terms:
 * <ol>
 *     <li>
 *         Any field of a type with finalizers must be effectively final (marked with {@link Final}).
 *         <em>Not checked.</em>
 *     </li>
 *     <li>
 *         A finalizer method can only be called on a field inside a method which is marked as a finalizer as well:
 *         an owner consumes what it owns only while it is itself consumed.
 *     </li>
 *     <li>
 *         A finalizer method can never be called on a parameter or any variable linked to it: a borrowed object
 *         cannot be consumed. The exception is Rust's parameter by value: no caller uses its argument after the call.
 *     </li>
 * </ol>
 * The ownership pass checks every call of a contracted finalizer in the analysed sources for rules 2 and 3, and for
 * any use of the receiver after the call; a violation is a warning of category {@code finalizer-violation}.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Finalizer {

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
