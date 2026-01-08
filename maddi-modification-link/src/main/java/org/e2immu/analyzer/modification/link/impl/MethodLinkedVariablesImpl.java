package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MethodLinkedVariablesImpl implements MethodLinkedVariables, Value {
    public final static MethodLinkedVariables EMPTY = new MethodLinkedVariablesImpl(LinksImpl.EMPTY, List.of(), Set.of());
    public static final Property METHOD_LINKS = new PropertyImpl("methodLinks", EMPTY);


    private final Links ofReturnValue;
    private final List<Links> ofParameters;
    private final Set<Variable> modified;

    public MethodLinkedVariablesImpl(Links ofReturnValue, List<Links> ofParameters, Set<Variable> modified) {
        this.ofParameters = ofParameters;
        this.ofReturnValue = ofReturnValue;
        this.modified = modified;
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
    public Set<Variable> modified() {
        return modified;
    }

    @Override
    public List<Links> ofParameters() {
        return ofParameters;
    }

    @Override
    public String toString() {
        return ofParameters.stream().map(p -> p.toString(modified))
                       .collect(Collectors.joining(", ", "[", "]"))
               + " --> " + (ofReturnValue == null ? "-" : ofReturnValue.toString(modified));
    }

    @Override
    public MethodLinkedVariables translate(TranslationMap translationMap) {
        if (translationMap == null || translationMap.isEmpty() || EMPTY.equals(this)) return this;
        return new MethodLinkedVariablesImpl(
                ofReturnValue == null ? null : ofReturnValue.translate(translationMap),
                ofParameters.stream().map(l -> l.translate(translationMap)).toList(),
                modified.stream().map(translationMap::translateVariableRecursively)
                        .collect(Collectors.toUnmodifiableSet()));
    }

    @Override
    public boolean virtual() {
        return ofReturnValue != null && ofReturnValue.containsVirtualFields()
               || ofParameters.stream().anyMatch(Links::containsVirtualFields);
    }

    @Override
    public MethodLinkedVariables removeSomeValue() {
        return new MethodLinkedVariablesImpl(
                ofReturnValue.isEmpty()
                        ? ofReturnValue
                        : ofReturnValue.removeIfTo(v -> v instanceof MarkerVariable mv && mv.isSomeValue()),
                ofParameters, modified);
    }
}
