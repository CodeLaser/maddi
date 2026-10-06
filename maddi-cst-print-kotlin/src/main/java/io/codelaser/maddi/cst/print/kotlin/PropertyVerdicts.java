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

import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

/**
 * The verdicts the analysis stored on the declarations: {@code NULLABILITY_FIELD}, {@code _PARAMETER} and
 * {@code _METHOD}, which maddi-mod's analyzer writes when it runs with nullability. Where nothing was stored (no
 * analysis ran), every answer is "no verdict", and the declared types print as they are. Local variables are not
 * {@link Info}s and have no property: a caller with the analysis's report supplies them, see
 * {@link #withLocals}.
 */
public final class PropertyVerdicts implements NullabilityVerdicts {

    public static final PropertyVerdicts INSTANCE = new PropertyVerdicts();

    private PropertyVerdicts() {
    }

    @Override
    public ParameterizedType field(FieldInfo fieldInfo) {
        return verdict(fieldInfo, PropertyImpl.NULLABILITY_FIELD, fieldInfo.type());
    }

    @Override
    public ParameterizedType parameter(ParameterInfo parameterInfo) {
        return verdict(parameterInfo, PropertyImpl.NULLABILITY_PARAMETER, parameterInfo.parameterizedType());
    }

    @Override
    public ParameterizedType returnType(MethodInfo methodInfo) {
        return verdict(methodInfo, PropertyImpl.NULLABILITY_METHOD, methodInfo.returnType());
    }

    private static ParameterizedType verdict(Info info, Property property, ParameterizedType declared) {
        Value.Nullability n = info.analysis().getOrDefault(property, ValueImpl.NullabilityImpl.UNSPECIFIED);
        return n.isDefault() ? null : n.applyTo(declared);
    }

    /** These verdicts for fields, parameters and returns, {@code locals} for local variables. */
    public static NullabilityVerdicts withLocals(NullabilityVerdicts locals) {
        return new NullabilityVerdicts() {
            @Override
            public ParameterizedType field(FieldInfo fieldInfo) {
                return INSTANCE.field(fieldInfo);
            }

            @Override
            public ParameterizedType parameter(ParameterInfo parameterInfo) {
                return INSTANCE.parameter(parameterInfo);
            }

            @Override
            public ParameterizedType returnType(MethodInfo methodInfo) {
                return INSTANCE.returnType(methodInfo);
            }

            @Override
            public ParameterizedType local(MethodInfo method, io.codelaser.maddi.cst.api.element.Element declaration,
                                           io.codelaser.maddi.cst.api.variable.LocalVariable variable) {
                return locals.local(method, declaration, variable);
            }
        };
    }
}
