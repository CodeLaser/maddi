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

package org.e2immu.annotation.type;

import org.e2immu.annotation.Container;
import org.e2immu.annotation.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that a type is an <em>extension class</em> of another type <code>E</code>.
 * The following criteria are used:
 * <ol>
 * <li>the class is immutable or constant;</li>
 * <li>all non-private static methods with parameters (and there must be at least one) must have a 1st parameter:
 * <ol>
 *     <li>of type <code>E</code>, the type being extended,</li>
 *     <li>which is {@link NotNull};</li>
 * </ol>
 * <li>non-private static methods without parameters must return a value of type <code>E</code>, and must
 * also be {@link NotNull}.</li>
 * </ol>
 * Extension classes will often not be {@link Container}, because a modification of the first parameter
 * is pretty common.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface ExtensionClass {

    /**
     * Parameter to mark that the annotation should be absent, or present.
     * In verification mode, <code>absent=true</code> means that an error will be raised
     * if the analyser computes the annotation. In contract mode, it guarantees absence of the annotation.
     *
     * @return <code>true</code> when the annotation should be absent (verification mode) or must be absent (contract mode).
     */
    boolean absent() default false;

    /**
     * Parameter to set contract mode, even if the annotation occurs in a context
     * where verification mode is normal. Use <code>contract=true</code>
     * to override the computation of the analyser.
     *
     * @return <code>true</code> when switching to contract mode.
     */
    boolean contract() default false;

    /**
     * The type being extended (<code>E</code>); currently for decorative use only.
     *
     * @return The type being extended.
     */
    Class<?> of();

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
