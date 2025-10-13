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

package org.e2immu.language.cst.impl.info;

import org.e2immu.language.cst.api.output.element.Keyword;
import org.e2immu.language.cst.api.type.TypeNature;
import org.e2immu.language.cst.impl.output.KeywordImpl;

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
