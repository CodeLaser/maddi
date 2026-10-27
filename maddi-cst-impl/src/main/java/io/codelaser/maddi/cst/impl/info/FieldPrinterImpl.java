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

package io.codelaser.maddi.cst.impl.info;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.Access;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.FieldModifier;
import io.codelaser.maddi.cst.api.info.FieldPrinter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.impl.element.ElementImpl;
import io.codelaser.maddi.cst.impl.output.*;
import io.codelaser.maddi.cst.impl.type.DiamondEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class FieldPrinterImpl implements FieldPrinter {
    private final boolean formatter2;
    private final FieldInfo fieldInfo;

    public FieldPrinterImpl(FieldInfo fieldInfo, boolean formatter2) {
        this.fieldInfo = fieldInfo;
        this.formatter2 = formatter2;
    }

    @Override
    public OutputBuilder print(Qualification qualification, boolean asParameterInRecordDeclaration) {
        Stream<OutputBuilder> annotationStream = Stream.concat(fieldInfo.annotations().stream(),
                        qualification.decorator() == null ? Stream.of()
                                : qualification.decorator().annotations(fieldInfo).stream())
                .map(ae -> ae.print(qualification));

        OutputBuilder outputBuilder = new OutputBuilderImpl();
        if (!asParameterInRecordDeclaration) {
            List<FieldModifier> fieldModifiers = minimalModifiers();
            outputBuilder.add(fieldModifiers.stream()
                    .map(mod -> new OutputBuilderImpl().add(mod.keyword()))
                    .collect(OutputBuilderImpl.joining(SpaceEnum.ONE)));
            if (!fieldModifiers.isEmpty()) outputBuilder.add(SpaceEnum.ONE);
        }
        outputBuilder
                .add(fieldInfo.type().print(qualification, false, DiamondEnum.SHOW_ALL))
                .add(SpaceEnum.ONE)
                .add(new TextImpl(fieldInfo.name()));
        if (!asParameterInRecordDeclaration && fieldInfo.initializer() != null && !fieldInfo.initializer().isEmpty()) {
            outputBuilder.add(SymbolEnum.assignment("=")).add(fieldInfo.initializer().print(qualification));
        }
        if (!asParameterInRecordDeclaration) {
            outputBuilder.add(SymbolEnum.SEMICOLON);
        }
        Stream<Comment> commentStream;
        if (qualification.decorator() != null) {
            commentStream = Stream.concat(fieldInfo.comments().stream(),
                    qualification.decorator().comments(fieldInfo).stream());
        } else {
            commentStream = fieldInfo.comments().stream();
        }
        // 'int a; // why': the comment stays after the declaration (#137)
        Source declaration = fieldInfo.source() == null || fieldInfo.source().detailedSources() == null ? null
                : fieldInfo.source().detailedSources().detail(DetailedSources.FIELD_DECLARATION);
        List<Comment> after = ElementImpl.commentsAfterOnLastLine(fieldInfo.comments(), declaration);
        after.forEach(c -> outputBuilder.add(SpaceEnum.ONE).add(c.print(qualification)));
        Stream<OutputBuilder> commentOBStream = commentStream.filter(c -> !after.contains(c))
                .map(c -> c.print(qualification));
        return Stream.concat(Stream.concat(commentOBStream, annotationStream), Stream.of(outputBuilder))
                .collect(OutputBuilderImpl.joining(formatter2 ? SpaceEnum.NONE : SpaceEnum.ONE_IS_NICE_EASY_SPLIT,
                        GuideImpl.generatorForAnnotationList()));
    }

    private static FieldModifier toFieldModifier(Access access) {
        if (access.isPublic()) return FieldModifierEnum.PUBLIC;
        if (access.isPrivate()) return FieldModifierEnum.PRIVATE;
        if (access.isProtected()) return FieldModifierEnum.PROTECTED;
        if (access.isInternal()) return FieldModifierEnum.INTERNAL;
        throw new UnsupportedOperationException();
    }

    private List<FieldModifier> minimalModifiers() {
        Set<FieldModifier> modifiers = fieldInfo.modifiers();
        List<FieldModifier> list = new ArrayList<>();
        Access access = fieldInfo.access();
        assert access != null : "No access for " + fieldInfo;
        Access ownerAccess = fieldInfo.owner().access();

        /*
        if the owner access is private, we don't write any modifier
         */
        if (access.le(ownerAccess) && !access.isPackage() && !ownerAccess.isPrivate()) {
            list.add(toFieldModifier(access));
        }
        // sorting... STATIC, FINAL, VOLATILE, TRANSIENT
        boolean inInterface = fieldInfo.owner().isInterface();
        if (!inInterface) {
            if (modifiers.contains(FieldModifierEnum.STATIC)) {
                list.add(FieldModifierEnum.STATIC);
            }
            if (modifiers.contains(FieldModifierEnum.FINAL)) {
                list.add(FieldModifierEnum.FINAL);
            }
        }
        if (modifiers.contains(FieldModifierEnum.VOLATILE)) {
            assert !inInterface;
            list.add(FieldModifierEnum.VOLATILE);
        }
        if (modifiers.contains(FieldModifierEnum.TRANSIENT)) {
            assert !inInterface;
            list.add(FieldModifierEnum.TRANSIENT);
        }

        return list;
    }

}
