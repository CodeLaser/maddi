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
 * Annotation used to indicate that modifications on a field or parameter can be ignored.
 * The simple use case is <code>System.out</code>, where <code>println</code> is a modifying
 * method. To your application, however, this may not count as a modification but rather
 * as an action external to the system.
 * <p>
 * The use case for parameters is a <code>forEach</code> method which takes a consumer as a
 * parameter. Modifications inside the implementation of the <code>accept</code> method of the
 * consumer should typically not prevent the type of the <code>forEach</code> method to become a container.
 * <p>
 * This annotation is always contracted, never computed.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
public @interface IgnoreModifications {
    // contract: true, absent: false

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
