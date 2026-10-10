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

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.FieldPrinter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;

/**
 * Prints a {@link FieldInfo} as a C# field: {@code [access] [static] [const|readonly] [volatile] Type name [= init];}.
 * A static final primitive or String with a constant initializer is a {@code const}; another final field is
 * {@code readonly}. As a record component, {@code Type Name}: a positional record's parameter.
 */
public record CSharpFieldPrinter(FieldInfo fieldInfo, boolean formatter2) implements FieldPrinter {

    @Override
    public OutputBuilder print(Qualification q, boolean asParameterInRecordDeclaration) {
        String type = CSharpTypeName.of(fieldInfo.type(), q);
        String name = CSharpNames.field(fieldInfo);
        if (asParameterInRecordDeclaration) return new OutputBuilderImpl().add(new TextImpl(type + " " + name));

        StringBuilder modifiers = new StringBuilder();
        String access = CSharpModifiers.access(fieldInfo);
        if (access != null) modifiers.append(access).append(' ');
        boolean isConst = isConst(fieldInfo);
        if (isConst) {
            modifiers.append("const ");
        } else {
            if (fieldInfo.isStatic() || fieldInfo.owner().isInterface()) modifiers.append("static ");
            if (isFinal(fieldInfo)) modifiers.append("readonly ");
            else if (fieldInfo.isVolatile()) modifiers.append("volatile ");
        }
        OutputBuilder b = new OutputBuilderImpl().add(new TextImpl(modifiers + type)).add(SpaceEnum.ONE)
                .add(new TextImpl(name));
        if (fieldInfo.initializer() != null && !fieldInfo.initializer().isEmpty()) {
            b.add(SymbolEnum.assignment("=")).add(CSharpExpressionPrinter.initializer(fieldInfo.initializer(),
                    fieldInfo.type(), q));
        }
        return b.add(SymbolEnum.SEMICOLON);
    }

    /** Final as Java has it: an interface's field is implicitly static and final. */
    static boolean isFinal(FieldInfo fieldInfo) {
        return fieldInfo.isFinal() || fieldInfo.owner().isInterface();
    }

    /** A static final primitive or String with a constant initializer. */
    static boolean isConst(FieldInfo fieldInfo) {
        if (!fieldInfo.isStatic() && !fieldInfo.owner().isInterface() || !isFinal(fieldInfo)
            || fieldInfo.initializer() == null || fieldInfo.initializer().isEmpty()
            || !fieldInfo.initializer().isConstant()) {
            return false;
        }
        ParameterizedType t = fieldInfo.type();
        return t.arrays() == 0 && (t.isPrimitiveExcludingVoid() || t.isJavaLangString());
    }
}
