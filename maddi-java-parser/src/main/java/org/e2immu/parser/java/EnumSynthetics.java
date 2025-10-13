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

import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.parser.Context;

import java.util.List;

class EnumSynthetics {
    private final Runtime runtime;
    private final TypeInfo typeInfo;
    private final TypeInfo.Builder builder;

    EnumSynthetics(Runtime runtime, TypeInfo typeInfo, TypeInfo.Builder builder) {
        this.runtime = runtime;
        this.typeInfo = typeInfo;
        this.builder = builder;
    }

    public void create(Context context, List<FieldInfo> enumFields) {

        // name() returns String

        MethodInfo name = runtime.newMethod(typeInfo, "name", runtime.methodTypeMethod());
        name.builder()
                .setSynthetic(true)
                .setAccess(runtime.accessPublic())
                .addMethodModifier(runtime.methodModifierPublic())
                .setReturnType(runtime.stringParameterizedType())
                .setMethodBody(runtime.emptyBlock())
                .setMissingData(runtime.methodMissingMethodBody())
                .commitParameters()
                .commit();
        builder.addMethod(name);

        // values() returns E[]

        MethodInfo values = runtime.newMethod(typeInfo, "values", runtime.methodTypeStaticMethod());
        values.builder()
                .setSynthetic(true)
                .setAccess(runtime.accessPublic())
                .addMethodModifier(runtime.methodModifierPublic())
                .addMethodModifier(runtime.methodModifierStatic())
                .setReturnType(runtime.newParameterizedType(typeInfo, 1))
                .setMethodBody(runtime.emptyBlock())
                .setMissingData(runtime.methodMissingMethodBody())
                .commitParameters()
                .commit();
        builder.addMethod(values);

        // valueOf(String) returns E

        MethodInfo valueOf = runtime.newMethod(typeInfo, "valueOf", runtime.methodTypeStaticMethod());
        valueOf.builder()
                .setSynthetic(true)
                .setAccess(runtime.accessPublic())
                .addMethodModifier(runtime.methodModifierStatic())
                .addMethodModifier(runtime.methodModifierPublic())
                .setReturnType(runtime.newParameterizedType(typeInfo, 0))
                .setMethodBody(runtime.emptyBlock())
                .setMissingData(runtime.methodMissingMethodBody())
                .addParameter("name", runtime.stringParameterizedType());
        valueOf.builder()
                .commitParameters()
                .commit();
        builder.addMethod(valueOf);
    }
}
