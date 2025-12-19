package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;

import java.util.List;

public class MethodLinkedVariablesImpl implements MethodLinkedVariables, Value {
    final static MethodLinkedVariables EMPTY = new MethodLinkedVariablesImpl(LinksImpl.EMPTY, List.of());
    public static final Property METHOD_LINKS = new PropertyImpl("methodLinks", EMPTY);


    private final Links ofReturnValue;
    private final List<Links> ofParameters;

    public MethodLinkedVariablesImpl(Links ofReturnValue, List<Links> ofParameters) {
        this.ofParameters = ofParameters;
        this.ofReturnValue = ofReturnValue;
    }

    public static Value decode(Codec codec, Codec.Context context, Codec.EncodedValue ev) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
        return null;
    }

    @Override
    public boolean isDefault() {
        return EMPTY.equals(this);
    }

    @Override
    public Links ofReturnValue() {
        return ofReturnValue;
    }

    @Override
    public List<Links> ofParameters() {
        return ofParameters;
    }

    @Override
    public String toString() {
        return ofParameters + " --> " + (ofReturnValue == null ? "-" : ofReturnValue.toString());
    }

    @Override
    public MethodLinkedVariables translate(TranslationMap translationMap) {
        if (translationMap == null || translationMap.isEmpty() || EMPTY.equals(this)) return this;
        return new MethodLinkedVariablesImpl(
                ofReturnValue == null ? null : ofReturnValue.translate(translationMap),
                ofParameters.stream().map(l -> l.translate(translationMap)).toList());
    }
}
