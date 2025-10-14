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

package org.e2immu.analyzer.modification.linkedvariables.lv;

import org.e2immu.analyzer.modification.prepwork.variable.Indices;
import org.e2immu.analyzer.modification.prepwork.variable.LV;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.Links;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LinksImpl.NO_LINKS;
import static org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl.ALL_INDICES;

/*
initial delay: value can still become statically_assigned
 */
public class LVImpl implements LV {
    private static final int I_INITIAL_DELAY = -2;
    private static final int I_DELAY = -1;
    private static final int I_HC = 4;
    private static final int I_DEPENDENT = 2;

    public static final LV LINK_INITIAL_DELAY = new LVImpl(-2, NO_LINKS, "<initial delay>");
    public static final LV LINK_DELAYED = new LVImpl(-1, NO_LINKS, "<delayed>");
    public static final LV LINK_STATICALLY_ASSIGNED = new LVImpl(0, NO_LINKS, "-0-");
    public static final LV LINK_ASSIGNED = new LVImpl(1, NO_LINKS, "-1-");
    public static final LV LINK_DEPENDENT = new LVImpl(I_DEPENDENT, NO_LINKS, "-2-");

    // do not use for equality! Use LV.isCommonHC()
    public static final LV LINK_COMMON_HC = new LVImpl(I_HC, NO_LINKS, "-4-");
    public static final LV LINK_INDEPENDENT = new LVImpl(5, NO_LINKS, "-5-");

    private final int value;
    private final Links links;
    private final String label;

    private LVImpl(int value, Links links, String label) {
        this.value = value;
        this.links = links;
        this.label = Objects.requireNonNull(label);
        assert !label.isBlank();
    }

    public static LV delay() {
        return new LVImpl(I_DELAY, NO_LINKS, "delay");
    }

    @Override
    public boolean haveLinks() {
        return !links.map().isEmpty();
    }

    @Override
    public boolean isDependent() {
        return I_DEPENDENT == value;
    }

    @Override
    public boolean isCommonHC() {
        return I_HC == value;
    }

    @Override
    public int value() {
        return value;
    }

    public Links links() {
        return links;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public boolean isInitialDelay() {
        return value == I_INITIAL_DELAY;
    }

    @Override
    public boolean isDelayed() {
        return value == I_DELAY || value == I_INITIAL_DELAY;
    }

    private static String createLabel(Links links, int hc) {
        return links.toString(hc);
    }

    public static LV createHC(Links links) {
        Links newLinks = links.ensureNoModification();
        return new LVImpl(I_HC, newLinks, createLabel(newLinks, I_HC));
    }

    public static LV createDependent(Links links) {
        return new LVImpl(I_DEPENDENT, links, createLabel(links, I_DEPENDENT));
    }

    @Override
    public LV reverse() {
        if (isDependent()) {
            return createDependent(links.reverse());
        }
        if (isCommonHC()) {
            return createHC(links.reverse());
        }
        return this;
    }

    @Override
    public boolean lt(LV other) {
        return value < other.value();
    }

    @Override
    public LV min(LV other) {
        if (isDelayed()) return this;
        if (other.isDelayed()) return other;
        if (value > other.value()) return other;
        if (!links.map().isEmpty() && !other.links().map().isEmpty()) {
            // IMPORTANT: the union only "compacts" on the "to" side for now, see Linking_1A.f9m()
            Links union = union(other.links());
            return new LVImpl(value, union, createLabel(union, value));
        }
        return this;
    }

    // both this and other have links
    // we start off with ours
    private Links union(Links other) {
        Map<Indices, Link> res = new HashMap<>();
        List<Indices> keysThatLinkToAll = new ArrayList<>();
        AtomicBoolean linkToAllMutable = new AtomicBoolean();

        Stream.concat(links.map().entrySet().stream(), other.map().entrySet().stream()).forEach(e -> {
            res.merge(e.getKey(), e.getValue(), Link::merge);
            if (e.getValue().to().isAll()) {
                keysThatLinkToAll.add(e.getKey());
                if (e.getValue().mutable()) linkToAllMutable.set(true);
            }
        });
        if (keysThatLinkToAll.size() > 1) {
            Indices merged = keysThatLinkToAll.stream().skip(1).reduce(keysThatLinkToAll.getFirst(), Indices::merge);
            keysThatLinkToAll.forEach(res.keySet()::remove);
            res.put(merged, new LinkImpl(ALL_INDICES, linkToAllMutable.get()));
        }
        Indices maSource = links.modificationAreaSource().merge(other.modificationAreaSource());
        Indices maTarget = links.modificationAreaTarget().merge(other.modificationAreaTarget());
        return new LinksImpl(Map.copyOf(res), maSource, maTarget);
    }

    private boolean sameLinks(LV other) {
        return links.equals(other.links());
    }

    @Override
    public LV max(LV other) {
        if (isDelayed()) return this;
        if (other.isDelayed()) return other;
        if (value < other.value()) return other;
        assert value != I_HC || other.value() != I_HC || sameLinks(other);
        return this;
    }

    @Override
    public boolean isDone() {
        return value != I_DELAY;
    }

    @Override
    public int compareTo(LV o) {
        return value - o.value();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LV lv = (LV) o;
        if (value != lv.value()) return false;
        return Objects.equals(links, lv.links());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, links);
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public String minimal() {
        if (!links.map().isEmpty()) {
            return label;
        }
        return Integer.toString(value);
    }

    @Override
    public boolean isStaticallyAssignedOrAssigned() {
        return value == 0 || value == 1;
    }

    @Override
    public boolean mineIsAll() {
        return links.map().size() == 1 && links.map().containsKey(ALL_INDICES);
    }

    @Override
    public boolean theirsIsAll() {
        return links.map().size() == 1 && links.map().values().stream().findFirst().orElseThrow().to().isAll();
    }

    @Override
    public boolean theirsContainsAll() {
        return links.map().values().stream().anyMatch(l -> l.to().isAll());
    }

    @Override
    public boolean propagateModification() {
        return isDependent()
               || isStaticallyAssignedOrAssigned();
        // uncomment the following if you want to follow *-4-M
        //       || isCommonHC() && allToNonAllMod();
    }

    // *M-...0M
    @SuppressWarnings("unused")
    private boolean allToNonAllMod() {
        for (Map.Entry<Indices, Link> e : links.map().entrySet()) {
            if (!e.getKey().isAll()) return false;
            if (!e.getValue().mutable() || e.getValue().to().isAll()) return false;
        }
        return true;
    }

    @Override
    public LV prefixMine(int index) {
        if (isDelayed() || isStaticallyAssignedOrAssigned()) return this;
        if (links.map().isEmpty()) return this;
        Map<Indices, Link> newMap = links.map().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(e -> e.getKey().prefix(index), Map.Entry::getValue));
        Links newLinks = new LinksImpl(newMap);
        return isCommonHC() ? createHC(newLinks) : createDependent(newLinks);
    }

    @Override
    public LV changeToHc() {
        if (value == I_DEPENDENT) {
            return createHC(links);
        }
        return this;
    }

    @Override
    public boolean overwriteAllowed(LV newValue) {
        if (value == I_DEPENDENT) {
            assert newValue != LINK_STATICALLY_ASSIGNED && newValue != LINK_INITIAL_DELAY && newValue != LINK_DELAYED;
            return true; // we can always overwrite dependent, by 1, by 4
        }
        if (this == LINK_STATICALLY_ASSIGNED) {
            return newValue == LINK_STATICALLY_ASSIGNED;
        }
        if (this == LINK_ASSIGNED) {
            return newValue == LINK_ASSIGNED;
        }
        if (this.isCommonHC()) {
            return newValue.isCommonHC();
        }
        return true;
    }
}