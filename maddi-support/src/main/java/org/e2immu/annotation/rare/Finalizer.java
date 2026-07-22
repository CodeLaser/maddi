/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, Bart Naudts, https://github.com/CodeLaser/maddi
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

package org.e2immu.annotation.rare;

import org.e2immu.annotation.Final;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation on a method, always contracted, indicating that after calling this method,
 * no other methods may be called anymore on the object. After calling a finalizer, the
 * object has gone into a <em>final</em> state.
 * <p>
 * The analyser imposes strict rules for the life-cycle of objects with a finalizer method:
 * <ol>
 *     <li>
 *         Any field of a type with finalizers must be effectively final (marked with {@link Final}).
 *     </li>
 *     <li>
 *         A finalizer method can only be called on a field inside a method which is marked as a finalizer as well.
 *     </li>
 *     <li>
 *         A finalizer method can never be called on a parameter or any variable linked to it,
 *     </li>
 * </ol>
 * These rules allow the analyser to enforce the final state of the object.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Finalizer {

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
