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

package org.e2immu.util.internal.util;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ZipLists {
    public record Z<X, Y>(X x, Y y) {
    }

    public static <X, Y> Stream<Z<X, Y>> zip(List<X> lx, List<Y> ly) {
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
