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

package org.e2immu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marking that the type's fields are effectively or eventually {@link Final}.
 * Java record types are always (effectively) {@link FinalFields}.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface FinalFields {

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
     * Marker for eventual immutability.
     *
     * @return when the type is effectively {@link FinalFields}, set the empty string.
     * When it is eventually so, return a comma-separated list of strings from <code>@Mark</code>
     * values on some modifying methods of the type. After these have been called, the
     * type will become effectively {@link FinalFields}.
     */
    String after() default "";

    /**
     * If present with value <code>true</code>, the decision-making process of this annotation was
     * not conclusive.
     *
     * @return <code>true</code> when the decision-making process was cut short, and this value was chosen based
     * on incomplete information.
     */
    boolean inconclusive() default false;

    /**
     * implied: if true, the annotation is generated internally for verification or educational purposes,
     * but not output, because it is implied.
     * For example, there is no point in annotating every array with  <code>@FinalFields</code>.
     * <p>
     * internal or demonstration use only!
     *
     * @return true when the annotation is not really necessary
     */
    boolean implied() default false;

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
