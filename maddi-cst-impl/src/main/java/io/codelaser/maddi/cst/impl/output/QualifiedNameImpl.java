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

import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.api.output.element.QualifiedName;
import io.codelaser.maddi.cst.api.output.element.Qualifier;
import io.codelaser.maddi.cst.api.output.element.ThisName;
import io.codelaser.maddi.cst.api.output.element.TypeName;
import io.codelaser.maddi.util.StringUtil;

public record QualifiedNameImpl(String name, Qualifier qualifier, Required qualifierRequired) implements QualifiedName {

    // for tests
    public QualifiedNameImpl(String name) {
        this(name, null, Required.NEVER);
    }

    public enum Required {
        YES, // always write
        NO_FIELD, // don't write unless a field-related option says so
        NO_METHOD, // don't write unless a method-related option says so
        NEVER // never write
    }

    @Override
    public String minimal() {
        return qualifierRequired == Required.YES ? qualifier.minimal() + "." + name : name;
    }

    @Override
    public String fullyQualifiedName() {
        return qualifier.fullyQualifiedName() + "." + name;
    }

    @Override
    public int length(FormattingOptions options) {
        return minimal().length();
    }

    @Override
    public String write(FormattingOptions options) {
        if (options.allFieldsRequireThis() && qualifierRequired == Required.NO_FIELD
            && qualifier instanceof ThisName) {
            return qualifier().write(options) + "." + name;
        }
        if (options.allStaticFieldsRequireType() && qualifierRequired != Required.NO_FIELD
            && qualifier instanceof TypeName) {
            return qualifier().write(options) + "." + name;
        }
        return minimal();
    }

    @Override
    public String generateJavaForDebugging() {
        String q = qualifier == null ? "null" : StringUtil.quote(qualifier.minimal());
        return ".add(new QualifiedNameImpl(" + StringUtil.quote(name) + ", " + q + ", " + qualifierRequired + "))";
    }
}
