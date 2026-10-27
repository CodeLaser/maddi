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

package io.codelaser.maddi.cst.impl.info;

import io.codelaser.maddi.cst.api.output.element.Keyword;
import io.codelaser.maddi.cst.api.type.TypeNature;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;

public enum TypeNatureEnum implements TypeNature {

    ANNOTATION(KeywordImpl.AT_INTERFACE),
    CLASS(KeywordImpl.CLASS),
    ENUM(KeywordImpl.ENUM),
    INTERFACE(KeywordImpl.INTERFACE),
    PRIMITIVE(null),
    RECORD(KeywordImpl.RECORD),
    STUB(null),
    PACKAGE_INFO(null);

    private final Keyword keyword;

    TypeNatureEnum(Keyword keyword) {
        this.keyword = keyword;
    }

    public boolean isFinal() {
        return this != CLASS && this != INTERFACE;
    }

    @Override
    public boolean isClass() {
        return this == CLASS;
    }

    @Override
    public boolean isInterface() {
        return this == INTERFACE;
    }

    @Override
    public boolean isRecord() {
        return this == RECORD;
    }

    @Override
    public boolean isEnum() {
        return this == ENUM;
    }

    @Override
    public boolean isStatic() {
        return this != CLASS;
    }

    @Override
    public Keyword keyword() {
        return keyword;
    }

    @Override
    public boolean isAnnotation() {
        return this == ANNOTATION;
    }

    @Override
    public boolean isPackageInfo() {
        return this == PACKAGE_INFO;
    }

    @Override
    public boolean isStub() {
        return this == STUB;
    }
}
