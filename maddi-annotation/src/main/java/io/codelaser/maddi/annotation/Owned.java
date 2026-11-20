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
package io.codelaser.maddi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The object the field holds can only be reached through the object holding the field: it lives and dies with its
 * owner, and in Rust it is a plain field, moved in, not a shared handle. Computed by the ownership pass (maddi-mod
 * docs/roadmap/ownership.md): the field is private, every value assigned to it is created for it (or handed over by
 * every caller of the method that stores it), and its object never leaves.
 * <p>
 * {@link #elements()}: the field holds a collection, a map or another generic object whose elements are owned too,
 * each reachable only through it ({@code Vec<T>} of plain values in Rust, rather than shared handles).
 * <p>
 * Not computed for a static or a primitive field. A field that is not owned only because its object, which is
 * immutable, is also held elsewhere is shared-immutable: no annotation, as {@code @Immutable} on its type says it.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Owned {

    /**
     * Parameter to mark that the annotation should be absent, or present.
     * In verification mode, <code>absent=true</code> means that an error will be raised
     * if the analyzer computes the annotation. In contract mode, it guarantees absence of the annotation.
     *
     * @return <code>true</code> when the annotation should be absent (verification mode) or must be absent (contract mode).
     */
    boolean absent() default false;

    /**
     * Parameter to set contract mode, even if the annotation occurs in a context
     * where verification mode is normal. Use <code>contract=true</code>
     * to override the computation of the analyzer.
     *
     * @return <code>true</code> when switching to contract mode.
     */
    boolean contract() default false;

    /**
     * The elements the field's object holds (a collection's, a map's, a generic object's hidden content) are owned
     * too: each arrives created for it, and none leaves.
     *
     * @return <code>true</code> when the elements are owned as well.
     */
    boolean elements() default false;

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
