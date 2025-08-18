package org.e2immu.language.cst.api.analysis;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.util.ParSeq;
import org.e2immu.language.cst.api.variable.Variable;

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

    default Value rewire(InfoMap infoMap) {
        return this;
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
    }

    interface NotNull extends Value {

        boolean isAtLeastNotNull();

        boolean isNullable();

        NotNull max(NotNull other);
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

    interface SetOfStrings extends Value {
        Set<String> set();
    }
}
