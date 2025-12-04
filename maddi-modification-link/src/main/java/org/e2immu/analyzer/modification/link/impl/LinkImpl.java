package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;

public record LinkImpl(Variable from, LinkNature linkNature, Variable to) implements Link {
    public static final Link EMPTY = new LinkImpl(null, null, null);

    @Override
    public @NotNull String toString() {
        return (from == null ? "?" : from.toString()) + linkNature + to;
    }

    @Override
    public Link replaceThis(Runtime runtime, Variable variable, TypeInfo typeInfo) {
        This thisVar = runtime.newThis(typeInfo.asParameterizedType());
        TranslationMap tm = runtime.newTranslationMapBuilder()
                .put(thisVar, variable)
                .build();
        Variable translatedFrom = tm.translateVariableRecursively(from);
        Variable translatedTo = tm.translateVariableRecursively(to);
        return new LinkImpl(translatedFrom, linkNature, translatedTo);
    }
}
