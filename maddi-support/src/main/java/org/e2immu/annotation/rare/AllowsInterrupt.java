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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that this method or constructor increases the statement time,
 * in other words, allows the execution to be interrupted.
 * <p>
 * Please see the <em>e2immu manual</em> for a discussion of statement times.
 * <p>
 * Default value is true. Methods can be annotated with <code>@AllowsInterrupt(false)</code> to explicitly
 * mark that they do not interrupt.
 * <p>
 * External methods not annotated will not interrupt.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface AllowsInterrupt {
    /**
     * Parameter to indicate whether the method allows for interrupts, or not.
     *
     * @return <code>true</code> when the method allows for interrupts.
     */
    boolean value() default true;

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
