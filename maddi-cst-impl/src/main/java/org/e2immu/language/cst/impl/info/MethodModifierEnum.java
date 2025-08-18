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

package org.e2immu.language.cst.impl.info;


import org.e2immu.language.cst.api.info.MethodModifier;
import org.e2immu.language.cst.api.output.element.Keyword;
import org.e2immu.language.cst.impl.output.KeywordImpl;

public enum MethodModifierEnum implements MethodModifier {
    PUBLIC(KeywordImpl.PUBLIC),
    PRIVATE(KeywordImpl.PRIVATE),
    PROTECTED(KeywordImpl.PROTECTED),
    ABSTRACT(KeywordImpl.ABSTRACT),
    DEFAULT(KeywordImpl.DEFAULT),
    FINAL(KeywordImpl.FINAL),
    STATIC(KeywordImpl.STATIC),
    SYNCHRONIZED(KeywordImpl.SYNCHRONIZED);

    public final Keyword keyword;

    MethodModifierEnum(Keyword keyword) {
        this.keyword = keyword;
    }

    @Override
    public boolean isPublic() {
        return this == PUBLIC;
    }

    @Override
    public boolean isPrivate() {
        return this == PRIVATE;
    }

    @Override
    public boolean isProtected() {
        return this == PROTECTED;
    }

    @Override
    public boolean isAbstract() {
        return this == ABSTRACT;
    }

    @Override
    public boolean isFinal() {
        return this == FINAL;
    }

    @Override
    public boolean isDefault() {
        return this == DEFAULT;
    }

    @Override
    public boolean isSynchronized() {
        return this == SYNCHRONIZED;
    }

    @Override
    public boolean isStatic() {
        return this == STATIC;
    }

    @Override
    public Keyword keyword() {
        return keyword;
    }
}
