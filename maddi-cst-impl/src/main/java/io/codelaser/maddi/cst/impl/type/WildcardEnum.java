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

package io.codelaser.maddi.cst.impl.type;

import io.codelaser.maddi.cst.api.type.Wildcard;

public enum WildcardEnum implements Wildcard {
    EXTENDS, EXTENDS_INTERSECTION, SUPER, UNBOUND,
    ;

    @Override
    public boolean isUnbound() {
        return this == UNBOUND;
    }

    @Override
    public boolean isExtends() {
        return this == EXTENDS || this == EXTENDS_INTERSECTION;
    }

    @Override
    public boolean isExtendsNoIntersection() {
        return this == EXTENDS;
    }

    @Override
    public boolean isExtendsIntersection() {
        return this == EXTENDS_INTERSECTION;
    }

    @Override
    public boolean isSuper() {
        return this == SUPER;
    }
}
