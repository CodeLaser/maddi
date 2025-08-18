/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.language.cst.impl.output;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.TypeNameRequired;
import org.e2immu.language.cst.api.output.element.TypeName;
import org.e2immu.util.internal.util.StringUtil;

public record TypeNameImpl(String simpleName,
                           String fullyQualifiedName,
                           String fromPrimaryTypeDownwards,
                           TypeNameRequired required,
                           boolean annotation) implements TypeName {

    public enum Required implements TypeNameRequired {
        DOLLARIZED_FQN, // com.foo.Bar$Bar2
        FQN, // com.foo.Bar.Bar2
        QUALIFIED_FROM_PRIMARY_TYPE, // Bar.Bar2
        SIMPLE // Bar2
    }

    // for tests
    public TypeNameImpl(String simpleName) {
        this(simpleName, simpleName, simpleName, Required.SIMPLE, false);
    }

    public TypeNameImpl {
        assert simpleName != null;
        assert fullyQualifiedName != null;
        assert fromPrimaryTypeDownwards != null;
        assert required != null;
    }

    public static TypeName typeName(TypeInfo typeInfo, TypeNameRequired requiresQualifier, boolean annotation) {
        String simpleName = typeInfo.simpleName();
        String fqn = typeInfo.doesNotRequirePackage() ? simpleName : typeInfo.fullyQualifiedName();
        return new TypeNameImpl(simpleName, fqn, typeInfo.isPrimaryType() ? simpleName : typeInfo.fromPrimaryTypeDownwards(),
                requiresQualifier, annotation);
    }

    @Override
    public String minimal() {
        String addAnnotation = annotation ? "@" : "";
        return addAnnotation + switch ((Required) required) {
            case SIMPLE -> simpleName;
            case FQN -> fullyQualifiedName;
            case QUALIFIED_FROM_PRIMARY_TYPE -> fromPrimaryTypeDownwards;
            case DOLLARIZED_FQN ->
                    fullyQualifiedName.substring(0, fullyQualifiedName.length() - fromPrimaryTypeDownwards.length())
                    + fromPrimaryTypeDownwards.replace(".", "$");
        };
    }

    @Override
    public int length(FormattingOptions options) {
        return minimal().length();
    }

    @Override
    public String write(FormattingOptions options) {
        return minimal();
    }

    @Override
    public String generateJavaForDebugging() {
        return ".add(new TypeNameImpl(" + StringUtil.quote(simpleName) + "))";
    }
}
