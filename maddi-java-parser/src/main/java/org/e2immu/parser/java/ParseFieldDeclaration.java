/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.FieldModifier;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.ForwardType;
import org.e2immu.language.inspection.api.parser.Lombok;
import org.parsers.java.Node;
import org.parsers.java.ast.*;

import java.util.ArrayList;
import java.util.List;

public class ParseFieldDeclaration extends CommonParse {

    public ParseFieldDeclaration(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public List<FieldInfo> parse(Context context, FieldDeclaration fd, Lombok.Data lombokData) {
        int i = 0;
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        List<Annotation> annotations = new ArrayList<>();
        List<FieldModifier> fieldModifiers = new ArrayList<>();
        Node fdi;
        while (!((fdi = fd.get(i)) instanceof Type)) {
            if (fdi instanceof Annotation a) {
                annotations.add(a);
            } else if (fdi instanceof Modifiers modifiers) {
                for (Node node : modifiers.children()) {
                    if (node instanceof Annotation a) {
                        annotations.add(a);
                    } else if (node instanceof KeyWord keyWord) {
                        FieldModifier m = modifier(keyWord);
                        fieldModifiers.add(m);
                        if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(m, source(keyWord));
                    }
                }
            } else if (fd.get(i) instanceof KeyWord keyWord) {
                FieldModifier m = modifier(keyWord);
                fieldModifiers.add(m);
                if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(m, source(keyWord));
            }
            i++;
        }
        boolean isStatic = fieldModifiers.stream().anyMatch(FieldModifier::isStatic);
        TypeInfo owner = context.enclosingType();

        ParameterizedType parameterizedType;
        Node typeNode;
        if (fd.get(i) instanceof Type type) {
            parameterizedType = parsers.parseType().parse(context, type, detailedSourcesBuilder);
            i++;
            typeNode = type;
        } else throw new UnsupportedOperationException();
        List<FieldInfo> fields = new ArrayList<>();
        boolean first = true;
        while (i < fd.size() && fd.get(i) instanceof VariableDeclarator vd) {
            fields.add(makeField(context, fd, vd, typeNode, isStatic, parameterizedType, owner, detailedSourcesBuilder,
                    fieldModifiers, annotations, lombokData, first));
            i += 2;
            first = false;
        }
        return fields;
    }

    private FieldInfo makeField(Context context,
                                FieldDeclaration fd,
                                VariableDeclarator vd,
                                Node typeNode,
                                boolean isStatic,
                                ParameterizedType parameterizedType,
                                TypeInfo owner,
                                DetailedSources.Builder detailedSourcesBuilderMaster,
                                List<FieldModifier> fieldModifiers,
                                List<Annotation> annotations,
                                Lombok.Data lombokData,
                                boolean first) {
        ParameterizedType type;
        Node vd0 = vd.getFirst();
        Identifier identifier;
        DetailedSources.Builder detailedSourcesBuilder = detailedSourcesBuilderMaster == null ? null :
                detailedSourcesBuilderMaster.copy();

        if (vd0 instanceof VariableDeclaratorId vdi) {
            identifier = (Identifier) vdi.getFirst();
            int arrays = (vdi.size() - 1) / 2;
            type = parameterizedType.copyWithArrays(arrays);
            if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(type, source(typeNode));
        } else {
            identifier = (Identifier) vd0;
            type = parameterizedType;
        }
        String name = identifier.getSource();
        if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(name, source(identifier));
        Expression expression;
        if (vd.children().size() >= 3 && vd.get(2) instanceof Expression e) {
            expression = e;
        } else {
            expression = null;
        }

        FieldInfo fieldInfo = runtime.newFieldInfo(name, isStatic, type, owner);
        FieldInfo.Builder builder = fieldInfo.builder();
        fieldModifiers.forEach(builder::addFieldModifier);
        builder.computeAccess();
        addPrecedingSucceedingComma(vd, detailedSourcesBuilder);
        if (detailedSourcesBuilder != null) {
            detailedSourcesBuilder.put(DetailedSources.FIELD_DECLARATION, source(fd));
        }
        Source source = source(vd);
        builder.setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()));
        if (first) {
            builder.addComments(comments(fd, context, fieldInfo, builder));
        } // else: comment is only on the first field in the sequence, see e.g. TestFieldComments in java-parser
        builder.addComments(comments(vd, context, fieldInfo, builder));

        // now that there is a builder, we can parse the annotations
        parseAnnotations(context, builder, annotations);
        if (context.isLombok()) {
            context.lombok().handleField(lombokData, fieldInfo);
        }
        FieldReference fieldReference = runtime.newFieldReference(fieldInfo);
        context.variableContext().add(fieldReference);
        if (expression != null) {
            ForwardType fwd = context.newForwardType(fieldInfo.type());
            context.resolver().add(fieldInfo, builder, fwd, null, expression, context,
                    null);
        } else {
            builder.setInitializer(runtime.newEmptyExpression()).commit();
        }
        return fieldInfo;
    }

    private FieldModifier modifier(KeyWord keyWord) {
        return switch (keyWord.getType()) {
            case FINAL -> runtime.fieldModifierFinal();
            case PRIVATE -> runtime.fieldModifierPrivate();
            case PROTECTED -> runtime.fieldModifierProtected();
            case PUBLIC -> runtime.fieldModifierPublic();
            case STATIC -> runtime.fieldModifierStatic();
            case TRANSIENT -> runtime.fieldModifierTransient();
            case VOLATILE -> runtime.fieldModifierVolatile();
            default -> throw new UnsupportedOperationException("Have " + keyWord.getType());
        };
    }
}
