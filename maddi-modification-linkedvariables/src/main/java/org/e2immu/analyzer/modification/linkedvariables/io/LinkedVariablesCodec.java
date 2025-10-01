package org.e2immu.analyzer.modification.linkedvariables.io;

import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.analysis.PropertyProviderImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.io.CodecImpl;

import java.util.Map;
import java.util.function.BiFunction;

import static org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl.*;
import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_METHOD;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_PARAMETER;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;

public class LinkedVariablesCodec {

    private final Codec.TypeProvider typeProvider;
    private final Codec.DecoderProvider decoderProvider;
    private final Codec.PropertyProvider propertyProvider;
    private final Runtime runtime;

    public LinkedVariablesCodec(Runtime runtime, SourceSet sourceSetOfRequest) {
        this.typeProvider = fqn -> runtime.getFullyQualified(fqn, true, sourceSetOfRequest);
        decoderProvider = new D();
        this.propertyProvider = new P();
        this.runtime = runtime;
    }

    public Codec codec() {
        return new C();
    }

    class C extends CodecImpl {
        public C() {
            super(runtime, propertyProvider, decoderProvider, typeProvider);
        }
    }

    private static final Map<String, Property> PROPERTY_MAP = Map.of(
            HIDDEN_CONTENT_TYPES.key(), HIDDEN_CONTENT_TYPES,
            HCS_METHOD.key(), HCS_METHOD,
            HCS_PARAMETER.key(), HCS_PARAMETER,
            PART_OF_CONSTRUCTION.key(), PART_OF_CONSTRUCTION,
            STATIC_VALUES_PARAMETER.key(), STATIC_VALUES_PARAMETER,
            STATIC_VALUES_METHOD.key(), STATIC_VALUES_METHOD,
            STATIC_VALUES_FIELD.key(), STATIC_VALUES_FIELD);

    static class P implements Codec.PropertyProvider {
        @Override
        public Property get(String propertyName) {
            Property inMap = PROPERTY_MAP.get(propertyName);
            if (inMap != null) return inMap;
            return PropertyProviderImpl.get(propertyName);
        }
    }

    static class D implements Codec.DecoderProvider {

        @Override
        public BiFunction<Codec.DI, Codec.EncodedValue, Value> decoder(Class<? extends Value> clazz) {
            if (HiddenContentTypes.class.equals(clazz)) {
                return (di, ev) -> HiddenContentTypes.decode(di.codec(), di.context(), ev);
            }
            if (HiddenContentSelector.class.equals(clazz)) {
                return (di, ev) -> HiddenContentSelector.decode(di.codec(), di.context(), ev);
            }
            if (StaticValuesImpl.class.equals(clazz)) {
                return (di, ev) -> StaticValuesImpl.decode(di.codec(), di.context(), ev);
            }
            // part of construction uses "set of info", which is in ValueImpl.
            return ValueImpl.decoder(clazz);
        }
    }

}

