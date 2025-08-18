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
