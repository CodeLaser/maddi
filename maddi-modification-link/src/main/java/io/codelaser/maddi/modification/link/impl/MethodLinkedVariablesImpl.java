package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.modification.link.impl.localvar.IntermediateVariable;
import io.codelaser.maddi.modification.link.impl.localvar.MarkerVariable;
import io.codelaser.maddi.modification.prepwork.variable.LinkNature;
import io.codelaser.maddi.modification.prepwork.variable.Links;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import io.codelaser.maddi.modification.prepwork.variable.impl.LinksImpl;
import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MethodLinkedVariablesImpl implements MethodLinkedVariables, Value {
    private final static MethodLinkedVariables EMPTY = new MethodLinkedVariablesImpl(LinksImpl.EMPTY, List.of(), Set.of());
    public static final Property METHOD_LINKS = new PropertyImpl("methodLinks", EMPTY);


    private final Links ofReturnValue;
    private final List<Links> ofParameters;
    private final Set<Variable> modified;
    private final Set<Variable> assigned;

    public MethodLinkedVariablesImpl(Links ofReturnValue, List<Links> ofParameters, Set<Variable> modified) {
        this(ofReturnValue, ofParameters, modified, Set.of());
    }

    public MethodLinkedVariablesImpl(Links ofReturnValue,
                                     List<Links> ofParameters,
                                     Set<Variable> modified,
                                     Set<Variable> assigned) {
        this.ofParameters = ofParameters;
        this.ofReturnValue = ofReturnValue;
        // canonical, sorted iteration order: callers hand in Set.of/Set.copyOf, whose iteration order is
        // per-JVM SALTED (java.util.ImmutableCollections) — consumers walking the modified set would
        // inherit run-to-run nondeterminism (the composed-dogfood 24 ↔ 10 bistability). Variable is
        // Comparable (FQN order, consistent with equals).
        this.modified = modified.isEmpty() ? modified
                : java.util.Collections.unmodifiableSortedSet(new java.util.TreeSet<>(modified));
        this.assigned = assigned.isEmpty() ? assigned
                : java.util.Collections.unmodifiableSortedSet(new java.util.TreeSet<>(assigned));
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MethodLinkedVariablesImpl that)) return false;
        // 'assigned' is deliberately NOT part of equality: equals is the retention KEY (LinksImpl equality
        // is primary-only), and TolerantWrite's canonical-order upgrade only runs on EQUAL values. Including
        // assigned would make pairs differing only in assigned UNEQUAL, sending them down the
        // plain-overwrite path — last-write retention of the whole summary, links content included,
        // resurrecting the arrival-order bistability the canonical order exists to kill. Assigned-richer
        // values win retention through strictlyRicherThan instead: contentCount and the tie-break
        // rendering both include assigned.
        return Objects.equals(ofReturnValue, that.ofReturnValue)
               && Objects.equals(ofParameters, that.ofParameters)
               && Objects.equals(modified, that.modified);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ofReturnValue, ofParameters, modified);
    }

    @Override
    public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
        List<Codec.EncodedValue> list = new ArrayList<>();
        list.add(ofReturnValue.encode(codec, context));
        list.add(codec.encodeList(context,
                ofParameters.stream().map(l -> l.encode(codec, context)).toList()));
        modified.stream().sorted().forEach(v -> list.add(codec.encodeVariable(context, v)));
        if (!assigned.isEmpty()) {
            // trailing "A"-tagged list; a variable encodes as a list whose first element is one of the
            // single-letter kind markers (see CodecImpl.encodeVariable), so the tag is unambiguous and
            // old files (no such entry) keep decoding — same back-compat style as decodeLink's element 4
            List<Codec.EncodedValue> aList = new ArrayList<>();
            aList.add(codec.encodeString(context, "A"));
            assigned.stream().sorted().forEach(v -> aList.add(codec.encodeVariable(context, v)));
            list.add(codec.encodeList(context, aList));
        }
        return codec.encodeList(context, list);
    }

    public static Value decode(Codec codec, Codec.Context context, Codec.EncodedValue ev) {
        List<Codec.EncodedValue> list = codec.decodeList(context, ev);
        Links ofRv = decodeLinks(codec, context, list.getFirst());
        Codec.EncodedValue evParams = list.get(1);
        List<Codec.EncodedValue> encodedParams = codec.decodeList(context, evParams);
        List<Links> ofParams = encodedParams.stream()
                .map(e -> decodeLinks(codec, context, e))
                .toList();
        Set<Variable> modifiedVariables = new java.util.HashSet<>();
        Set<Variable> assignedVariables = new java.util.HashSet<>();
        for (int i = 2; i < list.size(); i++) {
            Codec.EncodedValue e = list.get(i);
            List<Codec.EncodedValue> sub = codec.decodeList(context, e);
            if (!sub.isEmpty() && "A".equals(codec.decodeString(context, sub.getFirst()))) {
                sub.stream().skip(1).forEach(a -> assignedVariables.add(codec.decodeVariable(context, a)));
            } else {
                modifiedVariables.add(codec.decodeVariable(context, e));
            }
        }
        return new MethodLinkedVariablesImpl(ofRv, ofParams, Set.copyOf(modifiedVariables),
                Set.copyOf(assignedVariables));
    }


    public static Links decodeLinks(Codec codec, Codec.Context context, Codec.EncodedValue encodedValue) {
        List<Codec.EncodedValue> list = codec.decodeList(context, encodedValue);
        if (list.isEmpty()) return LinksImpl.EMPTY;
        Variable primary = codec.decodeVariable(context, list.getFirst());
        LinksImpl.Builder builder = new LinksImpl.Builder(primary);
        list.stream().skip(1).forEach(ev -> decodeLink(codec, context, ev, builder));
        return builder.build();
    }

    private static void decodeLink(Codec codec,
                                   Codec.Context context,
                                   Codec.EncodedValue ev,
                                   LinksImpl.Builder builder) {
        List<Codec.EncodedValue> list = codec.decodeList(context, ev);
        Variable from = codec.decodeVariable(context, list.getFirst());
        LinkNature linkNature;
        Codec.EncodedValue natureEv = list.get(1);
        if (codec.isList(natureEv)) {
            // pass-carrying ≡ variant (☷): [symbol, methodInfo...] — see LinksImpl.encodeLink
            List<Codec.EncodedValue> natureList = codec.decodeList(context, natureEv);
            assert "☷".equals(codec.decodeString(context, natureList.getFirst()));
            java.util.Set<io.codelaser.maddi.cst.api.info.MethodInfo> pass = new java.util.HashSet<>();
            for (int i = 1; i < natureList.size(); i++) {
                pass.add(codec.decodeMethodInfo(context, natureList.get(i)));
            }
            linkNature = LinkNatureImpl.makeIdenticalTo(pass);
        } else {
            linkNature = LinkNatureImpl.decode(codec.decodeString(context, natureEv));
        }
        Variable to = codec.decodeVariable(context, list.get(2));
        // optional 4th element: mediation provenance (task #39); absent = unmediated (also old files)
        boolean mediated = list.size() > 3 && codec.decodeBoolean(context, list.get(3));
        builder.add(from, linkNature, to, mediated);
    }

    @Override
    public boolean isDefault() {
        return EMPTY.equals(this);
    }

    @Override
    public Links ofReturnValue() {
        return ofReturnValue;
    }

    @Override
    public Set<Variable> modified() {
        return modified;
    }

    @Override
    public Set<Variable> assigned() {
        return assigned;
    }

    @Override
    public List<Links> ofParameters() {
        return ofParameters;
    }

    @Override
    public String toString() {
        return ofParameters.stream().map(p -> p.toString(modified))
                       .collect(Collectors.joining(", ", "[", "]"))
               + " --> " + (ofReturnValue == null ? "-" : ofReturnValue.toString(modified));
    }

    @Override
    public MethodLinkedVariables translate(TranslationMap translationMap) {
        if (translationMap == null || translationMap.isEmpty() || EMPTY.equals(this)) return this;
        return new MethodLinkedVariablesImpl(
                ofReturnValue == null ? null : ofReturnValue.translate(translationMap),
                ofParameters.stream().map(l -> l.translate(translationMap)).toList(),
                modified.stream().map(translationMap::translateVariableRecursively)
                        .collect(Collectors.toUnmodifiableSet()),
                assigned.stream().map(translationMap::translateVariableRecursively)
                        .collect(Collectors.toUnmodifiableSet()));
    }

    @Override
    public boolean virtual() {
        return ofReturnValue != null && ofReturnValue.containsVirtualFields()
               || ofParameters.stream().anyMatch(Links::containsVirtualFields);
    }

    @Override
    public MethodLinkedVariables removeSomeValue() {
        return new MethodLinkedVariablesImpl(
                ofReturnValue.isEmpty()
                        ? ofReturnValue
                        : ofReturnValue.removeIfTo(v -> v instanceof MarkerVariable mv && mv.isSomeValue()),
                ofParameters, modified, assigned);
    }

    @Override
    public boolean overwriteAllowed(Value newValue) {
        MethodLinkedVariables nv = (MethodLinkedVariables) newValue;
        // TODO currently not implementing restrictions on linking; pretty complicated
        Set<Variable> newModified = excludeInternal(nv.modified());
        return modified.containsAll(newModified); // can only shrink
    }

    /** Total number of link entries plus modified/assigned variables: the "content mass" of this value. */
    private int contentCount() {
        return (ofReturnValue == null ? 0 : (int) ofReturnValue.stream().count())
               + ofParameters.stream().mapToInt(l -> (int) l.stream().count()).sum()
               + modified.size()
               + assigned.size();
    }

    /**
     * Equality of method links is keyed on the primary variables (LinksImpl equality is primary-only),
     * so two values with the same primaries but DIFFERENT content are EQUAL — and whichever arrives
     * first freezes under first-arrival retention. Measured on the composed dogfood
     * (docs/eventual-info-hierarchy.md §"The retention round"): the all-empty {@code [-] --> -} vs the
     * rich derivation of {@code Statement.translate} split the 24↔10 worlds, and after fixing only the
     * empty case, rich-vs-richer pairs still split 39↔53. This predicate therefore imposes a TOTAL
     * canonical order on equal-keyed pairs: more content wins; equal content mass falls back to the
     * lexicographically smaller rendering. TolerantWrite replaces an equal current value whenever the
     * incoming one is canonically greater — the retained value is a function of the value SET, not
     * the arrival order.
     */
    @Override
    public boolean strictlyRicherThan(Value other) {
        if (!(other instanceof MethodLinkedVariablesImpl o)) return false;
        int c = Integer.compare(contentCount(), o.contentCount());
        if (c != 0) return c > 0;
        // assigned participates in the canonical rendering (toString deliberately omits it): two values
        // differing only in assigned content must still order totally
        String mine = toString() + "|" + sortedAssignedString();
        String theirs = o.toString() + "|" + o.sortedAssignedString();
        return mine.compareTo(theirs) < 0; // equal mass: smaller canonical rendering wins, arbitrarily but totally
    }

    private static @NotNull Set<Variable> excludeInternal(Set<Variable> variables) {
        return variables.stream()
                .filter(v -> !(v instanceof IntermediateVariable) && !(v instanceof MarkerVariable))
                .collect(Collectors.toUnmodifiableSet());
    }

    /*
    Holds Links (for the return value and per parameter) and a set of modified Variables. Like LinksImpl, this is
    derived across types, so a REWIRE type's method links are stale by construction and should be recomputed rather
    than carried; hence not implemented. See docs/rewiring.md.
     */
    @Override
    public Value rewire(InfoMapView infoMap) {
        // carryOnRewire (METHOD_LINKS): re-point the return-value links, the per-parameter links (positional), and
        // the modified- and assigned-variable sets through the infoMap.
        return new MethodLinkedVariablesImpl(
                (Links) ofReturnValue.rewire(infoMap),
                ofParameters.stream().map(l -> (Links) l.rewire(infoMap)).toList(),
                modified.stream().map(v -> v.rewire(infoMap)).collect(Collectors.toUnmodifiableSet()),
                assigned.stream().map(v -> v.rewire(infoMap)).collect(Collectors.toUnmodifiableSet()));
    }
}
