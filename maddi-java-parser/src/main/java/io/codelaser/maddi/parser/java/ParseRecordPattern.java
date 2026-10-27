/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.RecordPattern;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.NamedType;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.inspection.api.parser.Context;
import io.codelaser.maddi.inspection.api.parser.GenericsHelper;
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
