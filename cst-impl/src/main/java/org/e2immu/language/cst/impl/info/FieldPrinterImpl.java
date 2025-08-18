package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.info.Access;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.FieldModifier;
import org.e2immu.language.cst.api.info.FieldPrinter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.type.DiamondEnum;

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
        Stream<OutputBuilder> commentOBStream = commentStream.map(c -> c.print(qualification));
        return Stream.concat(Stream.concat(commentOBStream, annotationStream), Stream.of(outputBuilder))
                .collect(OutputBuilderImpl.joining(formatter2 ? SpaceEnum.NONE : SpaceEnum.ONE_IS_NICE_EASY_SPLIT,
                        GuideImpl.generatorForAnnotationList()));
    }

    private static FieldModifier toFieldModifier(Access access) {
        if (access.isPublic()) return FieldModifierEnum.PUBLIC;
        if (access.isPrivate()) return FieldModifierEnum.PRIVATE;
        if (access.isProtected()) return FieldModifierEnum.PROTECTED;
        throw new UnsupportedOperationException();
    }

    private List<FieldModifier> minimalModifiers() {
        Set<FieldModifier> modifiers = fieldInfo.modifiers();
        List<FieldModifier> list = new ArrayList<>();
        Access access = fieldInfo.access();
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
