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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
 *
 * <h2>Witnessing the optimism (greatest-fixpoint, step 1)</h2>
 * A greatest fixpoint is <em>seed optimistically, then contract</em>: start with every candidate assumed
 * eventually immutable, then remove any member whose verdict does not hold once its dependencies are restricted
 * to the survivors, iterating to convergence. This oracle supplies the seed; the contraction is the follow-up.
 * For the contraction to have something to run on, every optimistic decision is now <em>recorded</em>: whenever
 * {@link #treatAsEventuallyImmutable} answers {@code true} only because of the seed (the candidate's own verdict
 * was not yet proven), it notes the edge <em>member &rarr; candidate</em> in {@link #assumptions()} -- "this
 * member concluded relying on that candidate". Recording is a pure side effect (read by nobody yet), so it
 * changes no verdict; it is the ledger the contraction pass will walk to retract any member that leaned on a
 * candidate which ultimately did not prove out.
 */
public class EventualCluster {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventualCluster.class);

    // non-final so a test can flip it; the env is the production default (mirrors StaticSideEffectAnalyzerImpl)
    public static boolean ENABLED = System.getenv("EVENTUALCLUSTER") != null;

    // supertypes of direct candidates: the only members with no eventual method of their own
    private final Set<TypeInfo> inheritedCandidates = ConcurrentHashMap.newKeySet();
    // positive cache only: eventual intent appears monotonically over the iterations, so a negative answer is
    // never final -- caching it once froze TypeInspectionImpl.Builder out of the cluster (queried before its
    // @TestMark was computed), which silently kept TypeInspection from ever becoming an inherited candidate
    private final Set<TypeInfo> directCandidateCache = ConcurrentHashMap.newKeySet();

    // greatest-fixpoint step 1: member -> the candidates it optimistically relied on (not yet proven when used).
    // The ledger the contraction pass walks; empty when the gate is off.
    private final Map<TypeInfo, Set<TypeInfo>> assumptions = new ConcurrentHashMap<>();

    // label provenance: abstract-method owner -> the implementation owners its EVENTUALLY_NON_MODIFYING labels
    // were inherited from. Under the commitLabels reframe a method label itself rests on seed assumptions, and
    // when an interface inherits the label it must also inherit the implementation's assumption edges -- the
    // implementation's own type verdict (whose retraction would otherwise carry the cascade) may legitimately
    // never exist. Folded into the ledger by {@link #effectiveAssumptions}.
    private final Map<TypeInfo, Set<TypeInfo>> labelProvenance = new ConcurrentHashMap<>();

    /** Does the type carry eventual intent of its own (independent of the hierarchy closure)? */
    public boolean isDirectCandidate(TypeInfo typeInfo) {
        if (directCandidateCache.contains(typeInfo)) return true;
        boolean direct = typeInfo.analysis()
                .getOrDefault(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE, ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL)
                .isEventual()
                || typeInfo.methods().stream().anyMatch(this::methodShowsEventualIntent);
        if (direct) directCandidateCache.add(typeInfo);
        return direct;
    }

    private boolean methodShowsEventualIntent(MethodInfo methodInfo) {
        if (methodInfo.analysis().getOrDefault(PropertyImpl.EVENTUAL_METHOD, ValueImpl.EventualImpl.NOT_EVENTUAL)
                .isEventual()) return true;
        return !methodInfo.analysis()
                .getOrDefault(PropertyImpl.EVENTUALLY_NON_MODIFYING_METHOD, ValueImpl.SetOfStringsImpl.EMPTY_SET)
                .set().isEmpty();
    }

    // Part A support: parent class -> ALL analyzed subclasses (candidate or not), for the abstract
    // subclass->parent mark inheritance (InfoImpl inherits the shared 'inspection' label from its subclasses)
    private final Map<TypeInfo, Set<TypeInfo>> subclassesByParent = new ConcurrentHashMap<>();

    /** Record the class-hierarchy edge of every analyzed type (not just candidates): the subclass->parent mark
     *  inheritance must see ALL subclasses -- one uncooperative subclass invalidates the shared label. */
    public void noteHierarchy(TypeInfo typeInfo) {
        if (!ENABLED) return;
        ParameterizedType parent = typeInfo.parentClass();
        TypeInfo parentType = parent == null ? null : parent.typeInfo();
        if (parentType != null && !parentType.isJavaLangObject()) {
            subclassesByParent.computeIfAbsent(parentType, k -> ConcurrentHashMap.newKeySet()).add(typeInfo);
        }
    }

    /** The analyzed direct subclasses of {@code parent} seen so far; empty when none (or off the gate). */
    public Set<TypeInfo> knownSubclasses(TypeInfo parent) {
        return subclassesByParent.getOrDefault(parent, Set.of());
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

    // downward interface closure (Element/Statement round): positive cache only, like directCandidateCache
    private final Set<TypeInfo> interfaceCandidateCache = ConcurrentHashMap.newKeySet();
    // setter-bearing types (the haveSetters MUTABLE exit, mirrored); getSet marks are prepwork-stable, so
    // caching both answers is safe. An abstract Builder interface carries no getset marks of its own --
    // only the impl builders' bodies are recognized -- so the abstract method's IMPLEMENTATIONS are
    // consulted too: an interface whose contract is implemented by setters is setter-bearing.
    private final Map<TypeInfo, Boolean> settersCache = new ConcurrentHashMap<>();

    private boolean hasSetters(TypeInfo typeInfo) {
        return settersCache.computeIfAbsent(typeInfo,
                t -> t.methodStream().anyMatch(EventualCluster::setterOrImplementedBySetter));
    }

    private static boolean setterOrImplementedBySetter(MethodInfo methodInfo) {
        if (isSetter(methodInfo)) return true;
        for (MethodInfo implementation : methodInfo.analysis()
                .getOrDefault(PropertyImpl.IMPLEMENTATIONS, ValueImpl.SetOfMethodInfoImpl.EMPTY)
                .methodInfoSet()) {
            if (isSetter(implementation)) return true;
        }
        return false;
    }

    private static boolean isSetter(MethodInfo methodInfo) {
        return methodInfo.getSetField() != null && methodInfo.getSetField().setter();
    }

    /**
     * A cluster member: a direct candidate, a supertype of one, or -- the downward interface closure -- an
     * INTERFACE whose superinterface is a member. A markless sub-interface of a candidate ({@code Block},
     * {@code Comment}, {@code LocalVariable} under {@code Element}) carries no eventual intent of its own,
     * but its content is the same promise space as its ancestor's: the statement/expression carriers hold
     * fields of exactly these types, and without membership every such field read bails the commit walk.
     * Implementations stay OUT unless they earn membership (intent or upward closure): only the recognition
     * is optimistic, every genuine field/method check stays strict, and each use is witnessed for the
     * contraction to retract.
     */
    public boolean isCandidate(TypeInfo typeInfo) {
        if (isDirectCandidate(typeInfo) || inheritedCandidates.contains(typeInfo)) return true;
        if (!typeInfo.isInterface()) return false;
        if (interfaceCandidateCache.contains(typeInfo)) return true;
        // a setter-bearing interface (the Builders) can NEVER prove: haveSetters is an unconditional MUTABLE
        // exit in computeImmutableType, before any after-mark relaxation. Admitting it is pure doomed mass
        // for the contraction -- the same criterion, mirrored.
        if (hasSetters(typeInfo)) return false;
        for (ParameterizedType superType : typeInfo.parentAndInterfacesImplemented()) {
            TypeInfo st = superType.typeInfo();
            if (st != null && !st.isJavaLangObject() && isCandidate(st)) {
                interfaceCandidateCache.add(typeInfo);
                return true;
            }
        }
        return false;
    }

    /**
     * May {@code candidate} be treated as eventually immutable here? {@code true} outright when its verdict is
     * genuinely proven ({@code actual.isEventual()}); otherwise, under the gate, {@code true} optimistically when
     * it is a cluster candidate whose verdict is still circular -- and that optimism is <em>witnessed</em>: the
     * edge {@code member -> candidate} is recorded in {@link #assumptions()} so the contraction pass can later
     * retract {@code member} if {@code candidate} does not prove out. {@code member} is the type whose analysis is
     * leaning on {@code candidate} (the subtype for a supertype contribution, the field owner for a field type).
     */
    public boolean treatAsEventuallyImmutable(TypeInfo member, TypeInfo candidate, Value.EventuallyImmutable actual) {
        if (actual.isEventual()) return true; // proven on its own merits: no assumption
        // optimism spent on a setter-bearing MEMBER is wasted, and optimism spent on a setter-bearing
        // CANDIDATE is doomed: haveSetters is an unconditional MUTABLE exit in computeImmutableType, before
        // any after-mark relaxation, so no verdict can ever form on either end. Refusing both keeps the
        // Builders' own typeLevel computations and the last consumer leans on Builder types out of the
        // ledger entirely. (Safe only since the Builder-lean quest: the flagship rewire chains no longer
        // need Builder candidacy -- receiverProvablyNotRoot and the freshness fixpoint carry them.)
        // the SELF-assumption (candidate == member) is always available: the computation in flight is the
        // very one that would make the type a candidate ("I will reach my own eventual verdict") -- this
        // breaks the leaf impls' bare-this chicken-and-egg (translationMap.translateExpression(this),
        // new CommonType(this): the owner-seed escape needed candidacy, candidacy needed a first enm).
        // Witnessed like any edge: a type that never forms retracts everything that consumed its labels.
        if (ENABLED && (candidate == member || isCandidate(candidate))
            && !hasSetters(member) && !hasSetters(candidate)) {
            java.util.ArrayDeque<java.util.List<TypeInfo[]>> stack = assumptionBuffers.get();
            if (!stack.isEmpty()) {
                // success-only witnessing: inside a buffered computation, the edge only reaches the ledger if
                // the computation lands its property -- a bailed attempt (retried next iteration, possibly
                // succeeding via a different path) must not leave vestigial edges for the contraction to
                // cascade on
                stack.peek().add(new TypeInfo[]{member, candidate});
            } else {
                record(member, candidate);
            }
            return true;
        }
        return false;
    }

    // per-thread stack of assumption buffers; the type loop runs computations in parallel, one per thread
    private final ThreadLocal<java.util.ArrayDeque<java.util.List<TypeInfo[]>>> assumptionBuffers =
            ThreadLocal.withInitial(java.util.ArrayDeque::new);

    // log-only diagnostic (MODREACH_EXPLAIN style): print DIRECT assumption edges whose candidate FQN
    // matches any comma-separated substring -- the ledger the contraction walks is otherwise only visible
    // after folding
    private static final String[] EC_ASSUME_DEBUG = System.getenv("EC_ASSUME_DEBUG") == null ? null
            : System.getenv("EC_ASSUME_DEBUG").split(",");

    private static boolean assumeDebugMatches(String name) {
        if (EC_ASSUME_DEBUG == null || name == null) return false;
        for (String s : EC_ASSUME_DEBUG) {
            if (!s.isBlank() && name.contains(s.trim())) return true;
        }
        return false;
    }
    // log-only diagnostic: trace commit-walk site decisions and property-write timing for computations whose
    // debug context (or method FQN) matches any comma-separated substring, e.g.
    // EC_SITE_DEBUG=rewirePhase1,handleMethodOrConstructor,builder()
    private static final String[] EC_SITE_DEBUG = System.getenv("EC_SITE_DEBUG") == null ? null
            : System.getenv("EC_SITE_DEBUG").split(",");
    public static final boolean SITE_DEBUG = EC_SITE_DEBUG != null;
    // the iterating analyzer's current iteration, stamped into ECASSUME/ECSITE lines; log-only
    public static volatile int ITERATION;
    // the computation (method/parameter/type) the eventual analyzer is currently running, for ECASSUME
    // site attribution; purely diagnostic, never read by any verdict path
    private final ThreadLocal<String> debugContext = new ThreadLocal<>();

    /** Diagnostic only: name the computation subsequent witnessed assumptions on this thread belong to. */
    public void setDebugContext(String context) {
        if (EC_ASSUME_DEBUG != null || SITE_DEBUG) debugContext.set(context);
    }

    /** Diagnostic only: the current computation's name, for ECSITE attribution. */
    public String debugContext() {
        return debugContext.get();
    }

    /** Diagnostic only: does {@code name} match any EC_SITE_DEBUG substring? */
    public static boolean siteDebugMatches(String name) {
        if (EC_SITE_DEBUG == null || name == null) return false;
        for (String s : EC_SITE_DEBUG) {
            if (!s.isBlank() && name.contains(s.trim())) return true;
        }
        return false;
    }

    /** Diagnostic only: an iteration-stamped ECSITE line. */
    public static void sitePrint(String message) {
        System.out.println("ECSITE it=" + ITERATION + " " + message);
    }

    private void record(TypeInfo member, TypeInfo candidate) {
        if (assumptions.computeIfAbsent(member, m -> ConcurrentHashMap.newKeySet()).add(candidate)) {
            if (assumeDebugMatches(candidate.fullyQualifiedName())) {
                System.out.println("ECASSUME it=" + ITERATION + " " + member.fullyQualifiedName()
                                   + " -> " + candidate.fullyQualifiedName()
                                   + " at " + debugContext.get());
            }
            LOGGER.debug("EC: {} optimistically assumes {} is eventually immutable", member, candidate);
        }
    }

    /**
     * MODREACH re-derivation: the eventual layer was just cleared alongside the immutability family, so the
     * witnessed edges, label provenance and candidacy caches -- all built by the cleared computations, some
     * on pre-cutover optimistic modification values -- restart with it. The hierarchy map and setter cache
     * are cheap, prepwork-stable facts, but clearing them too keeps the reset trivially complete.
     */
    public void resetForRederivation() {
        if (!ENABLED) return;
        assumptions.clear();
        labelProvenance.clear();
        directCandidateCache.clear();
        inheritedCandidates.clear();
        interfaceCandidateCache.clear();
        settersCache.clear();
        subclassesByParent.clear();
    }

    /** Open an assumption buffer for the computation that follows on this thread. Pair with exactly one
     *  {@link #commitAssumptionBuffer()} or {@link #discardAssumptionBuffer()}. No-op off the gate. */
    public void beginAssumptionBuffer() {
        if (!ENABLED) return;
        assumptionBuffers.get().push(new java.util.ArrayList<>());
    }

    /** The buffered computation landed its property: its optimistic edges enter the ledger. */
    public void commitAssumptionBuffer() {
        if (!ENABLED) return;
        for (TypeInfo[] edge : assumptionBuffers.get().pop()) record(edge[0], edge[1]);
    }

    /** The buffered computation bailed: its optimistic edges are dropped. */
    public void discardAssumptionBuffer() {
        if (!ENABLED) return;
        assumptionBuffers.get().pop();
    }

    /**
     * The optimistic assumptions witnessed so far: for each member, the set of candidates it concluded by relying
     * on (before those candidates' own verdicts were proven). The ledger the greatest-fixpoint contraction pass
     * walks; empty when the gate is off. The returned map is live (backed by the oracle); callers must not mutate.
     */
    public Map<TypeInfo, Set<TypeInfo>> assumptions() {
        return assumptions;
    }

    /** Record that {@code abstractOwner}'s abstract method inherited its eventually-non-modifying labels from an
     *  implementation declared on {@code implementationOwner}: the abstract owner's verdict then rests on
     *  whatever that implementation's excusals assumed. */
    public void noteLabelInheritance(TypeInfo abstractOwner, TypeInfo implementationOwner) {
        if (!ENABLED || abstractOwner == implementationOwner) return;
        labelProvenance.computeIfAbsent(abstractOwner, k -> ConcurrentHashMap.newKeySet()).add(implementationOwner);
    }

    /**
     * The contraction's view of the ledger: {@link #assumptions()} with label provenance folded in -- each member
     * also carries the assumption sets of the implementation owners its abstract methods inherited labels from,
     * transitively. A fresh snapshot; safe to keep while the underlying maps continue to grow.
     */
    public Map<TypeInfo, Set<TypeInfo>> effectiveAssumptions() {
        Map<TypeInfo, Set<TypeInfo>> effective = new HashMap<>();
        assumptions.forEach((member, set) -> effective.put(member, new HashSet<>(set)));
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<TypeInfo, Set<TypeInfo>> entry : labelProvenance.entrySet()) {
                Set<TypeInfo> mine = null;
                for (TypeInfo source : entry.getValue()) {
                    Set<TypeInfo> ofSource = effective.get(source);
                    if (ofSource != null && !ofSource.isEmpty()) {
                        if (mine == null) {
                            mine = effective.computeIfAbsent(entry.getKey(), k -> new HashSet<>());
                        }
                        changed |= mine.addAll(ofSource);
                    }
                }
            }
        }
        return effective;
    }
}
