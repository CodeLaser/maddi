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

package org.e2immu.annotation.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that this method is a getter or a setter.
 * A setter can be a void method, simply executing the assignment, or a
 * {@link org.e2immu.annotation.Fluent} one, returning <code>this</code> after
 * the assignment.
 * <p>
 * When applied to a factory method or a constructor, the parameters whose name correspond to fields in the
 * factory return type or constructor type, are regarded as "being set"; if a corresponding factory method or
 * constructor without these parameters exist, there is an equivalence between calling the large factory method or
 * constructor, and calling the one with fewer parameters, followed by individual setters on the newly created object.
 * In this situation, the 'value' parameter can be used to override the automatic detection algorithm, by specifying
 * a comma-separated list of parameterName=fieldName instructions.
 *
 * @since 0.6.1 April 2023
 * @since 0.6.2 July 2023 on constructors and factory methods
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface GetSet {
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
     * The algorithm recognizes getX, isX, hasX, setX, X itself. When prefixed, the first
     * character of X is (de)capitalized.
     *
     * @return The field name, when it cannot be algorithmically derived from the method name.
     */
    String value() default "";

    /**
     * Allows to specify or express a default value that is different from the natural default value.
     * This parameter can be present only when the parameter 'naturalDefaultValue' is different from 'YES'.
     * <p>
     * e2immu will attempt to parse the value as either a primitive constant, or the name of a constant
     * (fully qualified, or locally accessible).
     */
    String defaultValue() default "";

    // see GetSetEquivalent
    boolean equivalent() default false;

    enum TrueFalseIgnore {TRUE, FALSE, IGNORE}

    /*
    Allows to specify or express a default value that is the natural default value for the type, i.e.,
    0 or 0.0 in its forms for primitives, and null for objects.
     */
    TrueFalseIgnore naturalDefaultValue() default TrueFalseIgnore.IGNORE;

    /**
     * Any explanation for the presence of this annotion in this particular place.
     */
    String comment() default "";
}
