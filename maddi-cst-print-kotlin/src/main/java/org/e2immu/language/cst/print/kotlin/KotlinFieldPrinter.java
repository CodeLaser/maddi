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

package org.e2immu.language.cst.print.kotlin;

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.FieldPrinter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.impl.output.*;

/**
 * Prints a {@link FieldInfo} as a Kotlin property: `[visibility] val|var name: Type [= initializer]`. A final
 * field becomes `val`, otherwise `var`. When printed as a primary-constructor parameter, the initializer is
 * omitted. Static fields print as plain properties for now (a Kotlin `companion object`/top-level `const` is a
 * refinement). The method body / initializer expression reuses the shared expression printing.
 */
public record KotlinFieldPrinter(FieldInfo fieldInfo, boolean formatter2) implements FieldPrinter {

    @Override
    public OutputBuilder print(Qualification qualification, boolean asParameterInPrimaryConstructor) {
        OutputBuilder builder = new OutputBuilderImpl();
        KotlinModifiers.visibility(fieldInfo.access()).ifPresent(v -> builder.add(v).add(SpaceEnum.ONE));
        builder.add(fieldInfo.isFinal() ? KotlinKeyword.VAL : KotlinKeyword.VAR)
                .add(SpaceEnum.ONE)
                .add(new TextImpl(fieldInfo.name()))
                .add(SymbolEnum.COLON_LABEL) // Kotlin type ascription: no leading space, one trailing
                .add(new TextImpl(KotlinTypeName.of(fieldInfo.type())));
        if (!asParameterInPrimaryConstructor && fieldInfo.initializer() != null && !fieldInfo.initializer().isEmpty()) {
            builder.add(SpaceEnum.ONE).add(SymbolEnum.assignment("=")).add(SpaceEnum.ONE)
                    .add(fieldInfo.initializer().print(qualification));
        }
        return builder;
    }
}
