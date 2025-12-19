package org.e2immu.analyzer.modification.link.io;

import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.impl.analysis.PropertyProviderImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.io.CodecImpl;

import java.util.Map;
import java.util.function.BiFunction;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION;

public class LinkCodec {

        private final Codec.TypeProvider typeProvider;
        private final Codec.DecoderProvider decoderProvider;
        private final Codec.PropertyProvider propertyProvider;
        private final Runtime runtime;
        private final SourceSet sourceSetOfRequest;

        public LinkCodec(Runtime runtime, SourceSet sourceSetOfRequest) {
            this.typeProvider = fqn -> runtime.getFullyQualified(fqn, true, sourceSetOfRequest);
            decoderProvider = new D();
            this.propertyProvider = new P();
            this.sourceSetOfRequest = sourceSetOfRequest;
            this.runtime = runtime;
        }

        public Codec codec() {
            return new C();
        }

        class C extends CodecImpl {
            public C() {
                super(runtime, propertyProvider, decoderProvider, typeProvider, sourceSetOfRequest);
            }
        }

        private static final Map<String, Property> PROPERTY_MAP = Map.of(
                PART_OF_CONSTRUCTION.key(), PART_OF_CONSTRUCTION,
                METHOD_LINKS.key(), METHOD_LINKS);

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
                if (MethodLinkedVariablesImpl.class.equals(clazz)) {
                    return (di, ev) -> MethodLinkedVariablesImpl.decode(di.codec(), di.context(), ev);
                }
                // part of construction uses "set of info", which is in ValueImpl.
                return ValueImpl.decoder(clazz);
            }
        }

    }

