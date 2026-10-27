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

package io.codelaser.maddi.cst.impl.output;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.TypeNameRequired;
import io.codelaser.maddi.cst.api.output.element.TypeName;
import io.codelaser.maddi.util.StringUtil;

public record TypeNameImpl(String simpleName,
                           String fullyQualifiedName,
                           String descriptor,
                           String fromPrimaryTypeDownwards,
                           TypeNameRequired required,
                           boolean annotation) implements TypeName {

    public enum Required implements TypeNameRequired {
        DOLLARIZED_FQN, // com.foo.Bar$Bar2
        FQN, // com.foo.Bar.Bar2
        DESCRIPTOR, // source_set::com.foo.Bar.Bar2
        QUALIFIED_FROM_PRIMARY_TYPE, // Bar.Bar2
        QUALIFIED_FROM_PRIMARY_TYPE_FOLLOW_EXISTING, // when detailed sources matter
        SIMPLE // Bar2
    }

    // for tests
    public TypeNameImpl(String simpleName) {
        this(simpleName, simpleName, simpleName, simpleName, Required.SIMPLE, false);
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
        return new TypeNameImpl(simpleName, fqn, typeInfo.descriptor(),
                typeInfo.isPrimaryType() ? simpleName : typeInfo.fromPrimaryTypeDownwards(),
                requiresQualifier, annotation);
    }

    @Override
    public String minimal() {
        String addAnnotation = annotation ? "@" : "";
        return addAnnotation + switch ((Required) required) {
            case SIMPLE -> simpleName;
            case FQN -> fullyQualifiedName;
            case DESCRIPTOR -> descriptor;
            case QUALIFIED_FROM_PRIMARY_TYPE, QUALIFIED_FROM_PRIMARY_TYPE_FOLLOW_EXISTING -> fromPrimaryTypeDownwards;
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
