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

import io.codelaser.maddi.cst.api.output.element.Split;

public enum SplitEnum implements Split {
    NEVER(0),
    EASY_L(1),
    EASY_R(2),
    EASY(3),
    BEGIN_END(4), // if you split at {, then also at }
    ALWAYS(5);

    private final int rank;

    SplitEnum(int rank) {
        this.rank = rank;
    }

    @Override
    public int rank() {
        return rank;
    }

    @Override
    public Split easiest(Split split) {
        return split.rank() > this.rank ? split : this;
    }

    @Override
    public boolean isNever() {
        return rank == 0;
    }
}
