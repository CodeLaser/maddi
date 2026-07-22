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

package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EXPERIMENTAL, gated on {@code EVENTUALCLUSTER=1}. Breaks the circular recognition that stops the {@code Info}
 * family from being certified eventually immutable (see {@code docs/eventual-info-hierarchy.md}).
 * <p>
 * The {@code Info} types mutually reference each other ({@code MethodInfoImpl.typeInfo},
 * {@code ParameterInfoImpl.methodInfo}, …) and share the abstract base {@code InfoImpl}, which has no mark of
 * its own. Each one's eventual verdict needs the others', so a monotone least-fixpoint concludes nothing. This
 * oracle supplies the optimistic half of a greatest fixpoint: it recognises the {@code cluster} of
 * <em>candidates</em> and lets the two type analyzers treat a candidate cross-reference or supertype as
 * eventually immutable before its verdict has actually been proven.
 * <p>
 * A type is a candidate when it shows eventual intent -- it has a {@code @Mark}/{@code @Only}/{@code @TestMark}
 * method, or a computed {@code EVENTUALLY_NON_MODIFYING_METHOD}, or an {@code EVENTUALLY_IMMUTABLE_TYPE} -- plus
 * the upward hierarchy closure: the supertypes of a candidate are candidates too, which is the only way the
 * interfaces and {@code InfoImpl} (no eventual method of their own) enter the set. The closure is accumulated as
 * {@link #noteCandidate} is called for each direct candidate the eventual analyzer meets; by the second full
 * iteration the set is complete.
 * <p>
 * Only the recognition is optimistic; every genuine field/method check in the two analyzers stays strict, so a
 * candidate with a real mutable field still fails. Soundness of the resulting coinductive verdict is not yet
 * enforced by a removal pass (that is the follow-up) -- the gate keeps it out of the default corpus meanwhile.
 */
public class EventualCluster {

    public static final boolean ENABLED = System.getenv("EVENTUALCLUSTER") != null;

    // supertypes of direct candidates: the only members with no eventual method of their own
    private final Set<TypeInfo> inheritedCandidates = ConcurrentHashMap.newKeySet();
    private final Set<TypeInfo> directCandidateCache = ConcurrentHashMap.newKeySet();
    private final Set<TypeInfo> notDirectCandidateCache = ConcurrentHashMap.newKeySet();

    /** Does the type carry eventual intent of its own (independent of the hierarchy closure)? */
    public boolean isDirectCandidate(TypeInfo typeInfo) {
        if (directCandidateCache.contains(typeInfo)) return true;
        if (notDirectCandidateCache.contains(typeInfo)) return false;
        boolean direct = typeInfo.analysis()
                .getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE, ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL)
                .isEventual()
                || typeInfo.methods().stream().anyMatch(this::methodShowsEventualIntent);
        (direct ? directCandidateCache : notDirectCandidateCache).add(typeInfo);
        return direct;
    }

    private boolean methodShowsEventualIntent(MethodInfo methodInfo) {
        if (methodInfo.analysis().getOrDefault(PropertyImpl.EVENTUAL_METHOD, ValueImpl.EventualImpl.NOT_EVENTUAL)
                .isEventual()) return true;
        return !methodInfo.analysis()
                .getOrDefault(PropertyImpl.EVENTUALLY_NON_MODIFYING_METHOD, ValueImpl.SetOfStringsImpl.EMPTY_SET)
                .set().isEmpty();
    }

    /** Record a direct candidate and close upward: its supertypes join the cluster (this is how {@code InfoImpl}
     *  and the interfaces, which have no eventual method, get in). */
    public void noteCandidate(TypeInfo typeInfo) {
        if (!ENABLED) return;
        if (!isDirectCandidate(typeInfo)) return;
        for (ParameterizedType superType : typeInfo.parentAndInterfacesImplemented()) {
            TypeInfo st = superType.typeInfo();
            if (st != null && !st.isJavaLangObject()) inheritedCandidates.add(st);
        }
    }

    /** A cluster member: a direct candidate, or a supertype of one. */
    public boolean isCandidate(TypeInfo typeInfo) {
        return isDirectCandidate(typeInfo) || inheritedCandidates.contains(typeInfo);
    }

    /** Under the gate, a candidate may be treated as eventually immutable before its verdict is proven. */
    public boolean treatAsEventuallyImmutable(TypeInfo typeInfo, Value.EventuallyImmutable actual) {
        if (actual.isEventual()) return true;
        return ENABLED && isCandidate(typeInfo);
    }
}
