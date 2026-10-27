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
import io.codelaser.maddi.cst.api.output.element.Qualifier;
import io.codelaser.maddi.cst.api.output.element.ThisName;
import io.codelaser.maddi.util.StringUtil;

public record ThisNameImpl(boolean isSuper, Qualifier qualifier, boolean qualifierRequired) implements ThisName {

    private String thisOrSuper() {
        return (isSuper ? KeywordImpl.SUPER : KeywordImpl.THIS).minimal();
    }

    @Override
    public String minimal() {
        return qualifierRequired ? qualifier.minimal() + "." + thisOrSuper() : thisOrSuper();
    }

    @Override
    public String fullyQualifiedName() {
        return qualifier.fullyQualifiedName() + "." + thisOrSuper();
    }

    @Override
    public String write(FormattingOptions options) {
        return qualifierRequired ? qualifier.write(options) + "." + thisOrSuper() : thisOrSuper();
    }

    @Override
    public String generateJavaForDebugging() {
        String q = qualifier == null ? "null" : StringUtil.quote(qualifier.minimal());
        return ".add(new ThisNameImpl(" + isSuper + ", " + q + ", " + qualifierRequired + "))";
    }
}
