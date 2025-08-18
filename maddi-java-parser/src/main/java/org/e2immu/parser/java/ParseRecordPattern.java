package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.RecordPattern;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.parsers.java.Token.TokenType.UNDERSCORE;

public class ParseRecordPattern extends CommonParse {
    protected ParseRecordPattern(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }


    public RecordPattern parseRecordPattern(Context context,
                                            org.parsers.java.ast.RecordPattern rp) {
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        ParameterizedType recordType = parsers.parseType().parse(context, rp.getFirst(), detailedSourcesBuilder);
        List<RecordPattern> list = new ArrayList<>();
        int recordFieldIndex = 0;
        for (int i = 2; i < rp.size(); i += 2) {
            Node node = rp.get(i);
            RecordPattern pattern = switch (node) {
                case org.parsers.java.ast.RecordPattern subRp -> parseRecordPattern(context, subRp);
                case TypePattern lvd ->
                        parseLocalVariableDeclaration(context, lvd, recordFieldIndex, recordType);
                case Token kw when UNDERSCORE.equals(kw.getType()) -> runtime.newRecordPatternBuilder()
                        .setSource(source(kw))
                        .setUnnamedPattern(true).build();
                case null, default -> {

                    throw new UnsupportedOperationException();
                }
            };
            list.add(pattern);
            if (detailedSourcesBuilder != null) {
                detailedSourcesBuilder.put(pattern, source(node));
            }
            ++recordFieldIndex;
        }
        Source source = source(rp);
        return runtime.newRecordPatternBuilder()
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .setRecordType(recordType)
                .setPatterns(List.copyOf(list))
                .build();
    }

    public RecordPattern parseLocalVariableDeclaration(Context context,
                                                       Node lvd,
                                                       int recordFieldIndex,
                                                       ParameterizedType recordType) {
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        ParameterizedType pt;
        if (lvd.getFirst() instanceof Type type) {
            pt = parsers.parseType().parse(context, type, detailedSourcesBuilder);
        } else if (lvd.getFirst() instanceof Token kw && Token.TokenType.VAR.equals(kw.getType())) {
            FieldInfo fieldInfo = recordType.typeInfo().fields().get(recordFieldIndex);
            if (fieldInfo.type().hasTypeParameters()) {
                pt = handleTypeParameters(fieldInfo, recordType);
            } else {
                pt = fieldInfo.type();
            }
        } else {
            throw new UnsupportedOperationException();
        }
        LocalVariable lv;
        if (lvd.get(1) instanceof Identifier identifier) {
            String name = identifier.getSource();
            if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(name, source(identifier));
            lv = runtime.newLocalVariable(name, pt);
        } else if (lvd.get(1) instanceof Token kw && UNDERSCORE.equals(kw.getType())) {
            lv = runtime.newUnnamedLocalVariable(pt, null);
        } else {
            throw new UnsupportedOperationException();
        }

        if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(lv, source(lvd));
        context.variableContext().add(lv);
        Source source = source(lvd);
        return runtime.newRecordPatternBuilder()
                .setLocalVariable(lv)
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .build();
    }

    private ParameterizedType handleTypeParameters(FieldInfo fieldInfo, ParameterizedType recordType) {
        // FIXME do we need recursion here, up the record hierarchy?
        Map<NamedType, ParameterizedType> map = recordType.initialTypeParameterMap();
        return fieldInfo.type().applyTranslation(runtime, map);
    }
}
