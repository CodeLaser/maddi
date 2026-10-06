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

package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.FieldPrinter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.impl.output.*;

/**
 * Prints a {@link FieldInfo} as a Kotlin property: `[visibility] val|var name: Type [= initializer]`. A final
 * field becomes `val`, otherwise `var`. When printed as a primary-constructor parameter, the initializer is
 * omitted. Static fields print as plain properties for now (a Kotlin `companion object`/top-level `const` is a
 * refinement). The method body / initializer expression reuses the shared expression printing.
 */
public record KotlinFieldPrinter(FieldInfo fieldInfo, boolean formatter2) implements FieldPrinter {

    @Override
    public OutputBuilder print(Qualification qualification, boolean asParameterInPrimaryConstructor) {
        boolean hasInitializer = fieldInfo.initializer() != null && !fieldInfo.initializer().isEmpty();
        // a Kotlin property is initialized where it is declared; Java's is zero/false/null until assigned
        // a final field the secondary constructors assign cannot be a val: Kotlin assigns those in an init block only
        boolean isVal = fieldInfo.isFinal() && (hasInitializer || fieldInfo.isStatic()
                                                || !KotlinTypePrinter.finalFieldsAssignedInSecondaryConstructors(fieldInfo.owner()));
        boolean needsDefault = !asParameterInPrimaryConstructor && !hasInitializer && !isVal
                               && !fieldInfo.owner().isInterface();
        String zero = needsDefault ? defaultValue(fieldInfo.type()) : null;

        OutputBuilder builder = new OutputBuilderImpl();
        KotlinModifiers.visibility(fieldInfo.access(), fieldInfo.owner()).ifPresent(v -> builder.add(v).add(SpaceEnum.ONE));
        if (needsDefault && zero == null) builder.add(new TextImpl("lateinit")).add(SpaceEnum.ONE);
        if (isConst()) builder.add(new TextImpl("const")).add(SpaceEnum.ONE);
        builder.add(isVal ? KotlinKeyword.VAL : KotlinKeyword.VAR)
                .add(SpaceEnum.ONE)
                .add(new TextImpl(KotlinNames.name(fieldInfo.name())))
                .add(SymbolEnum.COLON_LABEL) // Kotlin type ascription: no leading space, one trailing
                .add(new TextImpl(KotlinTypeName.of(fieldInfo.type(), qualification)));
        if (asParameterInPrimaryConstructor) return builder;
        if (hasInitializer) {
            builder.add(SpaceEnum.ONE).add(KotlinSymbols.assignment("=")).add(SpaceEnum.ONE)
                    .add(KotlinExpressionPrinter.widened(fieldInfo.initializer(), fieldInfo.type(), qualification));
        } else if (zero != null) {
            builder.add(SpaceEnum.ONE).add(KotlinSymbols.assignment("=")).add(SpaceEnum.ONE).add(new TextImpl(zero));
        }
        return builder;
    }

    /** A static final primitive or String with a constant initializer: {@code const val}, usable in annotations. */
    private boolean isConst() {
        if (!fieldInfo.isStatic() || !fieldInfo.isFinal() || fieldInfo.initializer() == null
            || !fieldInfo.initializer().isConstant() || KotlinTypePrinter.fromKotlinSource(fieldInfo.owner())) {
            return false;
        }
        io.codelaser.maddi.cst.api.type.ParameterizedType t = fieldInfo.type();
        return t.arrays() == 0 && (t.isPrimitiveExcludingVoid() || t.isJavaLangString());
    }

    /** Java's default value of a primitive field, as Kotlin writes it; null for a reference type. */
    static String defaultValue(io.codelaser.maddi.cst.api.type.ParameterizedType type) {
        if (type.arrays() > 0 || type.typeInfo() == null) return null;
        return switch (type.typeInfo().fullyQualifiedName()) {
            case "boolean" -> "false";
            case "char" -> "'\\u0000'";
            case "byte", "short", "int" -> "0";
            case "long" -> "0L";
            case "float" -> "0f";
            case "double" -> "0.0";
            default -> null;
        };
    }
}
