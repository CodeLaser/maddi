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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.FieldPrinter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.List;

/**
 * Prints a {@link FieldInfo} as a C# field: {@code [access] [static] [const|readonly] [volatile] Type name [= init];}.
 * A static final primitive or String with a constant initializer is a {@code const}; another final field is
 * {@code readonly}. As a record component, {@code Type Name}: a positional record's parameter.
 */
public record CSharpFieldPrinter(FieldInfo fieldInfo, boolean formatter2) implements FieldPrinter {

    @Override
    public OutputBuilder print(Qualification q, boolean asParameterInRecordDeclaration) {
        String type = CSharpTypeName.declared(fieldInfo.type(), false,
                CSharpContext.program().facts().nullable(fieldInfo), q);
        String name = CSharpNames.field(fieldInfo);
        if (asParameterInRecordDeclaration) return new OutputBuilderImpl().add(new TextImpl(type + " " + name));

        OutputBuilder attributes = CSharpAttributes.uses(fieldInfo.annotations(), LibraryMapping.Target.FIELD, q, true);
        StringBuilder modifiers = new StringBuilder();
        String access = CSharpModifiers.access(fieldInfo);
        if ("public".equals(access)) attributes.add(libraryAttributes(fieldInfo, SpaceEnum.NEWLINE));
        if (access != null) modifiers.append(access).append(' ');
        boolean isConst = isConst(fieldInfo);
        if (isConst) {
            modifiers.append("const ");
        } else {
            if (fieldInfo.isStatic() || fieldInfo.owner().isInterface()) modifiers.append("static ");
            if (isFinal(fieldInfo)) modifiers.append("readonly ");
            else if (fieldInfo.isVolatile()) modifiers.append("volatile ");
        }
        OutputBuilder b = new OutputBuilderImpl().add(attributes).add(new TextImpl(modifiers + type)).add(SpaceEnum.ONE)
                .add(new TextImpl(name));
        // a hoisted anonymous class initialises its instance fields in its constructor, after its captures
        // and an inner class's, whose initializers may use its enclosing instance (CSharpInner)
        boolean inConstructor = !fieldInfo.isStatic() && (fieldInfo.owner().isAnonymous()
                                                          && CSharpContext.hoisted(fieldInfo.owner()) != null
                                                          || CSharpInner.initializedInConstructor(fieldInfo.owner()));
        if (fieldInfo.initializer() != null && !fieldInfo.initializer().isEmpty() && !inConstructor) {
            b.add(SymbolEnum.assignment("=")).add(CSharpExpressionPrinter.initializer(fieldInfo.initializer(),
                    fieldInfo.type(), q));
        }
        return b.add(SymbolEnum.SEMICOLON);
    }

    private static final List<String> WIDENING = List.of("private", "internal", "protected internal", "public");

    private static String widest(String a, String b) {
        return WIDENING.indexOf(a) >= WIDENING.indexOf(b) ? a : b;
    }

    /**
     * A field that a property takes the place of ({@link CSharpProperties}): {@code public string Name { get; }},
     * with the field's initializer. The property is as visible as its getter, its {@code set} as its setter; the
     * field's own reads and writes go through them, so neither is less visible than the field was. An accessor less
     * visible than the property says so.
     */
    static OutputBuilder property(CSharpProperties.Property property, Qualification q) {
        FieldInfo f = property.field();
        String fieldAccess = CSharpModifiers.access(f);
        String getAccess = widest(CSharpModifiers.access(property.getter(), f.owner()), fieldAccess);
        String setAccess = property.setter() == null ? fieldAccess
                : widest(CSharpModifiers.access(property.setter(), f.owner()), fieldAccess);
        String access = property.settable() ? widest(getAccess, setAccess) : getAccess;
        // the getter's annotations are the property's, or its get accessor's when only a method may have them
        List<io.codelaser.maddi.cst.api.expression.AnnotationExpression> getterAnnotations = property.getter().annotations();
        OutputBuilder b = new OutputBuilderImpl()
                .add(CSharpAttributes.uses(f.annotations(), LibraryMapping.Target.FIELD, q, true))
                .add("public".equals(access) ? libraryAttributes(f, SpaceEnum.NEWLINE) : new OutputBuilderImpl())
                .add(CSharpAttributes.uses(getterAnnotations.stream().filter(CSharpAttributes::onProperty).toList(),
                        LibraryMapping.Target.FIELD, q, true));
        String onGet = CSharpAttributes.uses(getterAnnotations.stream().filter(a -> !CSharpAttributes.onProperty(a))
                .toList(), LibraryMapping.Target.METHOD, q, false).toString();
        StringBuilder accessors = new StringBuilder("{ ");
        if (!onGet.isEmpty()) accessors.append(onGet.endsWith(" ") ? onGet : onGet + " ");
        if (!getAccess.equals(access)) accessors.append(getAccess).append(' ');
        accessors.append("get; ");
        if (property.settable()) {
            if (!setAccess.equals(access)) accessors.append(setAccess).append(' ');
            accessors.append("set; ");
        }
        accessors.append('}');
        // a getter that code outside the program may override, as the policy has it: a virtual property
        String inheritance = CSharpModifiers.inheritance(property.getter(), f.owner());
        String modifiers = inheritance == null ? access : access + " " + inheritance;
        b.add(new TextImpl(modifiers + " " + CSharpTypeName.declared(f.type(), false,
                        CSharpContext.program().facts().nullable(f), q))).add(SpaceEnum.ONE)
                .add(new TextImpl(property.name() + " " + accessors));
        if (f.initializer() != null && !f.initializer().isEmpty() && (f.isStatic() || !CSharpInner.initializedInConstructor(f.owner()))) {
            b.add(SymbolEnum.assignment("=")).add(CSharpExpressionPrinter.initializer(f.initializer(), f.type(), q))
                    .add(SymbolEnum.SEMICOLON);
        }
        return b;
    }

    /**
     * {@code [XmlIgnore]}: what a library has to say about a public field or property, or an enum's constant
     * ({@link LibraryMapping#memberAttributes}); each attribute followed by {@code separator}.
     */
    static OutputBuilder libraryAttributes(FieldInfo f, SpaceEnum separator) {
        OutputBuilder b = new OutputBuilderImpl();
        LibraryMapping.AttributeMapping mapped = CSharpContext.libraries().memberAttributes(f);
        if (mapped == null) return b;
        mapped.namespaces().forEach(CSharpContext::using);
        for (String attribute : mapped.attributes()) {
            b.add(SymbolEnum.LEFT_BRACKET).add(new TextImpl(attribute)).add(SymbolEnum.RIGHT_BRACKET).add(separator);
        }
        return b;
    }

    /** Final as Java has it: an interface's field is implicitly static and final. */
    static boolean isFinal(FieldInfo fieldInfo) {
        return fieldInfo.isFinal() || fieldInfo.owner().isInterface();
    }

    /** A static final primitive or String with a constant initializer. */
    static boolean isConst(FieldInfo fieldInfo) {
        return (fieldInfo.isStatic() || fieldInfo.owner().isInterface()) && isFinal(fieldInfo)
               && CstQueries.constantInitializer(fieldInfo);
    }
}
