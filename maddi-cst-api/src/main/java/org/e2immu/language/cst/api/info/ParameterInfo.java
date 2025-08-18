package org.e2immu.language.cst.api.info;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;

public interface ParameterInfo extends Variable, Info {
    int index();

    String name();

    boolean isVarArgs();

    boolean isFinal();

    Builder builder();

    List<ParameterInfo> translate(TranslationMap translationMap);

    boolean isUnnamed();

    interface Builder extends Info.Builder<Builder> {

        @Fluent
        Builder setIsFinal(boolean isFinal);

        @Fluent
        Builder setVarArgs(boolean varArgs);
    }

    default boolean isModified() { return !isUnmodified(); }

    // result of analysis
    boolean isUnmodified();

    boolean isIgnoreModifications();

    Value.AssignedToField assignedToField();

    MethodInfo methodInfo();
}
