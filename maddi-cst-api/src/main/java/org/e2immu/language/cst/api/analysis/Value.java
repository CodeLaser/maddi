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

package org.e2immu.language.cst.api.analysis;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.util.ParSeq;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Value extends Comparable<Value> {
    Codec.EncodedValue encode(Codec codec, Codec.Context context);

    @Override
    default int compareTo(Value o) {
        throw new UnsupportedOperationException();
    }

    boolean isDefault();

    default boolean lt(Value other) {
        return compareTo(other) < 0;
    }

    default Value min(Value v) {
        return v == null || compareTo(v) <= 0 ? this : v;
    }

    /**
     * Produce the equivalent of this value in the rewired world: every {@link org.e2immu.language.cst.api.info.Info}
     * and {@link Variable} reference must be mapped through {@code infoMap}, which returns its argument unchanged for
     * anything that was not rewired.
     * <p>
     * The default throws rather than returning {@code this}. A value that holds Info or Variable references and
     * silently returns {@code this} smuggles objects of the previous generation into the new CST, where they are
     * indistinguishable from the real ones -- {@code Info} equality is structural (fqn + source set), so nothing
     * downstream will notice. That bug class is expensive to find; failing here is not. So choose explicitly:
     * {@code return this} when the value is plain (ints, strings), a real mapping when it is not, or throw when the
     * mapping has not been written yet.
     */
    default Value rewire(InfoMapView infoMap) {
        throw new UnsupportedOperationException("No rewire() on " + getClass());
    }

    default boolean overwriteAllowed(Value newValue) {
        return false;
    }

    interface Bool extends Value {
        boolean isTrue();

        boolean isFalse();

        boolean hasAValue();

        Bool or(Bool bool);
    }


    interface Message extends Value {
        String message();

        boolean isEmpty();
    }

    interface Immutable extends Value {
        boolean isAtLeastImmutableHC();

        boolean isFinalFields();

        boolean isImmutable();

        boolean isImmutableHC();

        boolean isMutable();

        Immutable max(Immutable other);

        Immutable min(Immutable other);

        Independent toCorrespondingIndependent();
    }


    interface Independent extends Value {

        boolean isAtLeastIndependentHc();

        boolean isDependent();

        boolean isIndependent();

        Independent min(Independent other);

        Independent max(Independent other);

        boolean isIndependentHc();

        Map<Integer, Integer> linkToParametersReturnValue();

        List<MethodInfo> dependentMethods();
    }

    interface NotNullProperty extends Value {

        boolean isAtLeastNotNull();

        boolean isNullable();

        NotNullProperty max(NotNullProperty other);
    }

    /*
    the strings are arbitrary labels.
    at least two methods should have the same label, of the same kind (seq, par, multi).
     */
    interface CommutableData extends Value {
        // different from isDefault, which is a generic method to determine absence of annotation
        default boolean isBlankMultiParSeq() {
            return !isNone() && par().isBlank() && seq().isBlank() && multi().isBlank();
        }

        boolean isNone();

        default boolean isParallel() {
            return !isNone() && !par().isBlank() && seq().isBlank();
        }

        default boolean isSequential() {
            return !isNone() && par().isBlank() && !seq().isBlank();
        }

        String multi();

        String par();

        String seq();
    }

    // meant for the "GetSetField" property
    interface FieldValue extends Value {
        Variable createVariable(Runtime runtime, Expression object, Expression indexOrNull);

        boolean list();

        default int parameterIndexOfValue() {
            int i = parameterIndexOfIndex();
            return i < 0 ? 0 : 1 - i;
        }

        boolean setter();

        int parameterIndexOfIndex();

        default boolean hasIndex() {
            return parameterIndexOfIndex() >= 0;
        }

        FieldInfo field();
    }

    interface FieldBooleanMap extends Value {
        Map<FieldInfo, Boolean> map();
    }

    interface VariableBooleanMap extends Value {

        boolean isEmpty();

        Map<Variable, Boolean> map();
    }

    // meant for the "GetSetEquivalent" property
    interface GetSetEquivalent extends Value {
        Set<ParameterInfo> convertToGetSet();

        MethodInfo methodWithoutParameters();
    }

    // meant for parallel parameter groups
    interface ParameterParSeq extends Value {
        ParSeq<ParameterInfo> parSeq();
    }

    interface PostConditions extends Value {
        Map<String, Expression> byIndex();
    }

    interface Precondition extends Value {
        Expression expression();
    }

    /**
     * Eventual immutability at the level of a method or parameter: the {@code @Mark}, {@code @Only} and
     * {@code @TestMark} family (road to immutability, §060). One value type carries all three, because a method is
     * at most one of them and they share the mark label.
     * <p>
     * The label is the name, or comma-separated names, of the field(s) whose state transition the type is built
     * around. We keep <em>names</em> rather than {@link FieldInfo} objects: a mark is frequently inherited, and the
     * field is then not visible in the type carrying the annotation (e.g. {@code Freezable.frozen} seen from a
     * subclass). Names also make the value trivially rewireable and language-agnostic.
     */
    interface Eventual extends Value {
        /** the names of the fields carrying the state transition; empty when this element is not eventual */
        Set<String> fields();

        /** {@code @Mark}: this method effects the transition from 'before' to 'after' */
        boolean mark();

        /** {@code @Only(after=)} true, {@code @Only(before=)} false, null when this is not an {@code @Only} */
        Boolean after();

        /**
         * {@code @TestMark}: true when the method returns true in the 'after' state, false for the inverted
         * ({@code before=true}) sense; null when the method is not a test.
         */
        Boolean test();

        default boolean isEventual() {
            return !fields().isEmpty();
        }

        default boolean isMark() {
            return mark();
        }

        default boolean isOnly() {
            return isEventual() && !mark() && test() == null;
        }

        default boolean isTestMark() {
            return test() != null;
        }

        /** the {@code value=}/{@code before=}/{@code after=} string of the annotation */
        default String markLabel() {
            return fields().stream().sorted().collect(java.util.stream.Collectors.joining(","));
        }

        /** two eventual values belong to the same state transition */
        default boolean consistentWith(Eventual other) {
            return fields().equals(other.fields());
        }
    }

    /**
     * Eventual immutability at the level of a type: the {@code after="…"} parameter of {@code @Immutable},
     * {@code @ImmutableContainer} and {@code @FinalFields}. The type is mutable until the mark, and reaches
     * {@link #immutableAfterMark()} once marked.
     * <p>
     * Deliberately separate from {@code IMMUTABLE_TYPE}: the immutability lattice is combined with min/max
     * throughout the analyzer, so an eventual value inside it would silently change independence and guard
     * decisions. This property carries the promise; the lattice keeps carrying what holds unconditionally.
     */
    interface EventuallyImmutable extends Value {
        /** the name(s) of the field(s) carrying the state transition; blank when the type is not eventual */
        String markLabel();

        /** the immutability level the type reaches once it has been marked */
        Immutable immutableAfterMark();

        default boolean isEventual() {
            return !markLabel().isBlank();
        }
    }

    // for parameters

    interface AssignedToField extends Value {
        Set<FieldInfo> fields();
    }

    interface IndicesOfEscapes extends Value {
        Set<String> indices();
    }

    // general
    interface SetOfInfo extends Value {
        Set<? extends Info> infoSet();
    }

    interface SetOfTypeInfo extends Value {
        Set<TypeInfo> typeInfoSet();
    }

    interface VariableToTypeInfoSet extends Value {
        Map<Variable, Set<TypeInfo>> variableToTypeInfoSet();
    }

    interface SetOfMethodInfo extends Value {
        Iterable<MethodInfo> methodInfoSet();

        boolean isEmpty();

        boolean add(MethodInfo methodInfo);
    }

    interface SetOfStrings extends Value {
        Set<String> set();
    }

    interface Scope extends Value {
        boolean isEmpty();

        String scope();
    }
}
