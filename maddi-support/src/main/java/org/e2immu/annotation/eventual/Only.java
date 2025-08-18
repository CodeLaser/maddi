/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.annotation.eventual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is part of the eventual immutability computations. Preconditions govern whether
 * the method can be executed when the object is in the <em>before</em> state, or in the <em>after</em> state.
 * <p>
 * Contracting of this annotation is currently not implemented (see https://github.com/e2immu/e2immu/issues/48).
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
public @interface Only {

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
     * When the method can only be executed in the <em>before</em> state, this parameter shows the name of the mark (see {@link Mark}).
     * Note that the <code>before</code> and <code>after</code> parameters cannot be present at the same time.
     *
     * @return the name of the mark
     */
    String before() default "";

    /**
     * When the method can only be executed in the <em>after</em> state, this parameter shows the name of the mark (see {@link Mark}).
     * Note that the <code>before</code> and <code>after</code> parameters cannot be present at the same time.
     *
     * @return the name of the mark
     */
    String after() default "";

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
