package org.e2immu.language.cst.api.util;

import org.e2immu.language.cst.api.info.InfoMap;

import java.util.Comparator;
import java.util.List;

public interface ParSeq<T> {
    /**
     * Useful as a condition before calling <code>sortParallels</code>.
     *
     * @return false if the ParSeq is simply a sequence; true if it contains any parallel groups.
     */
    boolean containsParallels();

    /**
     * Sort a list of items, of a completely unrelated type, according to the ParSeq.
     * Inside parallel groups, use the comparator.
     *
     * @param items      the input
     * @param comparator the comparator for parallel groups
     * @param <X>        the type of the input
     * @return a new list of items, sorted accordingly.
     */
    <X> List<X> sortParallels(List<X> items, Comparator<X> comparator);
}
