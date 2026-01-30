package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.impl.localvar.IntermediateVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MethodLinkedVariablesImpl implements MethodLinkedVariables, Value {
    private final static MethodLinkedVariables EMPTY = new MethodLinkedVariablesImpl(LinksImpl.EMPTY, List.of(), Set.of());
    public static final Property METHOD_LINKS = new PropertyImpl("methodLinks", EMPTY);


    private final Links ofReturnValue;
    private final List<Links> ofParameters;
    private final Set<Variable> modified;

    public MethodLinkedVariablesImpl(Links ofReturnValue, List<Links> ofParameters, Set<Variable> modified) {
        this.ofParameters = ofParameters;
        this.ofReturnValue = ofReturnValue;
        this.modified = modified;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MethodLinkedVariablesImpl that)) return false;
        return Objects.equals(ofReturnValue, that.ofReturnValue)
               && Objects.equals(ofParameters, that.ofParameters)
               && Objects.equals(modified, that.modified);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ofReturnValue, ofParameters, modified);
    }

    @Override
    public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
        List<Codec.EncodedValue> list = new ArrayList<>();
        list.add(ofReturnValue.encode(codec, context));
        list.add(codec.encodeList(context,
                ofParameters.stream().map(l -> l.encode(codec, context)).toList()));
        modified.stream().sorted().forEach(v -> list.add(codec.encodeVariable(context, v)));
        return codec.encodeList(context, list);
    }

    public static Value decode(Codec codec, Codec.Context context, Codec.EncodedValue ev) {
        List<Codec.EncodedValue> list = codec.decodeList(context, ev);
        Links ofRv = decodeLinks(codec, context, list.getFirst());
        Codec.EncodedValue evParams = list.get(1);
        List<Codec.EncodedValue> encodedParams = codec.decodeList(context, evParams);
        List<Links> ofParams = encodedParams.stream()
                .map(e -> decodeLinks(codec, context, e))
                .toList();
        Set<Variable> modifiedVariables = list.stream().skip(2).map(e ->
                codec.decodeVariable(context, e)).collect(Collectors.toUnmodifiableSet());
        return new MethodLinkedVariablesImpl(ofRv, ofParams, modifiedVariables);
    }


    public static Links decodeLinks(Codec codec, Codec.Context context, Codec.EncodedValue encodedValue) {
        List<Codec.EncodedValue> list = codec.decodeList(context, encodedValue);
        if (list.isEmpty()) return LinksImpl.EMPTY;
        Variable primary = codec.decodeVariable(context, list.getFirst());
        LinksImpl.Builder builder = new LinksImpl.Builder(primary);
        list.stream().skip(1).forEach(ev -> decodeLink(codec, context, ev, builder));
        return builder.build();
    }

    private static void decodeLink(Codec codec,
                                   Codec.Context context,
                                   Codec.EncodedValue ev,
                                   LinksImpl.Builder builder) {
        List<Codec.EncodedValue> list = codec.decodeList(context, ev);
        Variable from = codec.decodeVariable(context, list.getFirst());
        LinkNature linkNature = LinkNatureImpl.decode(codec.decodeString(context, list.get(1)));
        Variable to = codec.decodeVariable(context, list.getLast());
        builder.add(from, linkNature, to);
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

    @Override
    public boolean overwriteAllowed(Value newValue) {
        MethodLinkedVariables nv = (MethodLinkedVariables) newValue;
        // TODO currently not implementing restrictions on linking; pretty complicated
        Set<Variable> newModified = excludeInternal(nv.modified());
        return modified.containsAll(newModified); // can only shrink
    }

    private static @NotNull Set<Variable> excludeInternal(Set<Variable> variables) {
        return variables.stream()
                .filter(v -> !(v instanceof IntermediateVariable) && !(v instanceof MarkerVariable))
                .collect(Collectors.toUnmodifiableSet());
    }
}
