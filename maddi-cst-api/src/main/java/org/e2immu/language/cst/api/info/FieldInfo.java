package org.e2immu.language.cst.api.info;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;
import java.util.Set;

public interface FieldInfo extends Info {

    PropertyValueMap analysisOfInitializer();

    boolean hasBeenInspected();

    default int indexInType() {
        int count = 0;
        for (FieldInfo f : owner().fields()) {
            if (f == this) return count;
            ++count;
        }
        throw new UnsupportedOperationException();
    }

    default boolean isModified() { return !isUnmodified(); }

    // result of analysis
    boolean isUnmodified();

    String name();

    TypeInfo owner();

    void rewirePhase3(InfoMap infoMap);

    List<FieldInfo> translate(TranslationMap translationMap);

    ParameterizedType type();

    // inspection

    boolean isStatic();

    boolean isFinal();

    boolean isTransient();

    boolean isVolatile();


    // analysis

    boolean isPropertyNotNull();

    // as opposed to isFinal, which is the modifier
    boolean isPropertyFinal();

    boolean isIgnoreModifications();

    default CompilationUnit compilationUnit() {
        return owner().compilationUnit();
    }

    OutputBuilder print(Qualification qualification, boolean asParameter);

    Builder builder();

    Set<FieldModifier> modifiers();

    Expression initializer();

    FieldInfo withOwnerVariableBuilder(TypeInfo newOwner);

    interface Builder extends Info.Builder<Builder> {

        @Fluent
        Builder addFieldModifier(FieldModifier fieldModifier);

        @Fluent
        Builder setInitializer(Expression initializer);
    }
}
