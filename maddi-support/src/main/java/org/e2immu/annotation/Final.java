/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
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

package org.e2immu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that a field is effectively or eventually final: it is never assigned to
 * outside constructors or methods only reachable from constructors. In the eventual case, this restriction is
 * only reached after discarding some modifying methods with the use of a precondition.
 * <p>
 * The annotation is implicitly present but is not written when the field is explicitly final
 * (i.e., it has the <code>final</code> keyword.)
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Final {

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
     * Used when a field is eventually final.
     *
     * @return the name of the mark
     */
    String after() default "";

    /**
     * implied: if true, the annotation is generated internally for verification or educational purposes,
     * but not output, because it is implied.
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
