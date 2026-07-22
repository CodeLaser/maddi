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

package org.e2immu.annotation.eventual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that this modifying method transitions an eventually immutable object from the <em>before</em>
 * state to the <em>after</em> state.
 * <p>
 * The annotation is computed on the basis of its precondition, and assignment or modification afterwards, which breaks
 * this condition.
 * <p>
 * The contracted form of this annotation is currently not implemented, but it will be in the near future
 * (https://github.com/e2immu/e2immu/issues/48)
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface Mark {

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
     * The name of the mark, which currently is computed as the comma-separated value of the fields
     * involved in the precondition that decides on eventual immutability.
     *
     * @return The name of the mark.
     */
    String value();

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
