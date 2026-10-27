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

package io.codelaser.maddi.util;

import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.NotModified;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ZipLists {
    public record Z<X, Y>(X x, Y y) {
    }

    // zip only iterates its inputs, and the returned stream shares nothing but their elements (hidden
    // content). Contracted like ListUtil: as a jar on maddi's own dogfood, an unannotated zip() blocks
    // every withBlocks-style walk that hands a container field to it (docs/design/eventual-info-hierarchy.md).
    @NotModified
    @Independent(hc = true)
    public static <X, Y> Stream<Z<X, Y>> zip(@NotModified @Independent(hc = true) List<X> lx,
                                             @NotModified @Independent(hc = true) List<Y> ly) {
        Iterator<Z<X, Y>> it = new Iterator<>() {
            private final Iterator<X> ix = lx.iterator();
            private final Iterator<Y> iy = ly.iterator();

            @Override
            public boolean hasNext() {
                return ix.hasNext() && iy.hasNext();
            }

            @Override
            public Z<X, Y> next() {
                return new Z<>(ix.next(), iy.next());
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, 0), false);
    }
}
