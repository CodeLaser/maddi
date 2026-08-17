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
 * Indicates that no modifications took place in the method; that the parameter's content was not modified,
 * that no method modifies the content of the field.
 * <p>
 * Because constructors are meant to be modifying fields, this annotation is not relevant on constructors.
 * <p>
 * Please refer to the <em>e2immu</em> manual for an in-depth discussion of this concept.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface NotModified {

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
     * Used when a field is eventually not modified.
     *
     * @return the name of the mark
     */
    String after() default "";

    /**
     * implied: if true, the annotation is generated internally for verification or educational purposes,
     * but not output, because it is implied.
     * For example, there is no point in annotating every return value of type {@link java.lang.String} with
     * <code>@NotModified</code>...
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
