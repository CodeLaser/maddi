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


import io.codelaser.maddi.cst.api.info.FieldModifier;
import io.codelaser.maddi.cst.api.output.element.Keyword;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;

public enum FieldModifierEnum implements FieldModifier {
    PUBLIC(KeywordImpl.PUBLIC),
    PRIVATE(KeywordImpl.PRIVATE),
    PROTECTED(KeywordImpl.PROTECTED),
    INTERNAL(KeywordImpl.INTERNAL),
    VOLATILE(KeywordImpl.VOLATILE),
    TRANSIENT(KeywordImpl.TRANSIENT),
    FINAL(KeywordImpl.FINAL),
    STATIC(KeywordImpl.STATIC);

    public final Keyword keyword;

    FieldModifierEnum(Keyword keyword) {
        this.keyword = keyword;
    }

    @Override
    public Keyword keyword() {
        return keyword;
    }

    @Override
    public boolean isStatic() {
        return this == STATIC;
    }

    @Override
    public boolean isFinal() {
        return this == FINAL;
    }

    @Override
    public boolean isVolatile() {
        return this == VOLATILE;
    }

    @Override
    public boolean isTransient() {
        return this == TRANSIENT;
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
    public boolean isPublic() {
        return this == PUBLIC;
    }

    @Override
    public boolean isInternal() {
        return this == INTERNAL;
    }
}
