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

import io.codelaser.maddi.cst.api.output.element.ElementarySpace;

import java.util.Objects;

public enum ElementarySpaceEnum implements ElementarySpace {
    ONE(" "), NICE(" "), NONE(""), RELAXED_NONE(""), NEWLINE("\n"),
    ;

    private final String space;

    ElementarySpaceEnum(String space) {
        this.space = Objects.requireNonNull(space);
    }

    @Override
    public boolean isNone() {
        return this == NONE;
    }

    @Override
    public boolean isOne() {
        return this == ONE;
    }

    @Override
    public boolean isRelaxedNone() {
        return this == RELAXED_NONE;
    }

    @Override
    public String write() {
        return space;
    }
}
