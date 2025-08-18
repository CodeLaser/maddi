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

package org.e2immu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opposite of {@link NotModified}. On a method, it indicates that the method modifies a field, either
 * by assigning to it, or calling another method which does.
 * On a field, it indicates that there exists a method which changes the object graph of the field.
 * On a parameter, it indicates that somehow through the class, the object represented by the parameter
 * is modified.
 * <p>
 * Non-modification is a requirement for fields of a level 2 immutable type.
 * Fields of implicitly immutable type cannot be modified in a type.
 * <p>
 * Please refer to the <em>e2immu</em> manual for an in-depth discussion of this concept.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
public @interface Modified {

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
     * For private methods only
     *
     * @return : true when the modification only occurs in the construction phase, or in factory methods.
     * As a consequence, the method can be ignored for the computation of immutability.
     */
    boolean construction() default false;

    /*
    names of components that are modified.

     */
    String[] value() default {};

    /**
     * Used when the formal type is mutable, with hidden content, and at least one of the hidden content types
     * has been substituted for a mutable type in the concrete type.
     * The parameter indicates that the mutable part of the variable has been modified, and also that the part
     * corresponding to the hidden content index has been modified.
     * <p>
     * E.g., <code>List&lt;E&gt;</code> as formal type, <code>List&lt;StringBuilder&gt;</code> as concrete type, and
     * <code>hcs={0}</code>. Index 0 corresponds to type parameter E, concretely, a mutable StringBuilder.
     *
     * @return the hidden content indices, starting from 0.
     */
    int[] alsoHcs() default {};

    /**
     * Used when the formal type is mutable, with hidden content, and at least one of the hidden content types
     * has been substituted for a mutable type in the concrete type.
     * The parameter indicates that the mutable part of the variable has <b>not</b> been modified; however, the part
     * corresponding to the hidden content index has been modified.
     * <p>
     * E.g., <code>List&lt;E&gt;</code> as formal type, <code>List&lt;StringBuilder&gt;</code> as concrete type, and
     * <code>hcs={0}</code>. Index 0 corresponds to type parameter E, concretely, a mutable StringBuilder.
     *
     * @return the hidden content indices, starting from 0.
     */
    int[] onlyHcs() default {};

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
