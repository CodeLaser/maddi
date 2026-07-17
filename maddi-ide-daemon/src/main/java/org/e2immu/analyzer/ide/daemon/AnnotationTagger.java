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

package org.e2immu.analyzer.ide.daemon;

import org.e2immu.analyzer.modification.prepwork.io.DecoratorImpl;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Renders the full set of annotations for one element and tags each with polarity and context-default-ness,
 * so the plugin's inline-hints filter can hide clutter without losing signal.
 * <p>
 * The <em>positive</em> (proven-stronger) and neutral annotations come from {@link DecoratorImpl} (keeping its
 * exact formatting, e.g. {@code @Immutable(hc=true)}); the <em>negative</em> (baseline) ones — {@code @Modified},
 * {@code @Mutable}, {@code @Dependent}, {@code @Nullable} — which the decorator omits, are computed here from
 * the analysis values. They are mutually exclusive per concept, so nothing is double-rendered.
 */
public class AnnotationTagger {
    public static final String POSITIVE = "POSITIVE";
    public static final String NEGATIVE = "NEGATIVE";
    public static final String NEUTRAL = "NEUTRAL";

    // annotation simple names by polarity (the core safety concepts)
    private static final Set<String> POSITIVE_NAMES = Set.of(
            "NotModified", "Immutable", "ImmutableContainer", "Container", "Independent", "NotNull", "Final");
    private static final Set<String> NEGATIVE_NAMES = Set.of("Modified", "Mutable", "Dependent", "Nullable");

    private final DecoratorImpl decorator;
    private final Qualification simpleNames;

    public AnnotationTagger(Runtime runtime, SourceSet sourceSetOfRequest) {
        this.decorator = new DecoratorImpl(runtime, sourceSetOfRequest);
        this.simpleNames = runtime.qualificationSimpleNames();
    }

    public List<DaemonProtocol.Annotation> tag(Info info) {
        List<DaemonProtocol.Annotation> out = new ArrayList<>();
        for (AnnotationExpression ae : decorator.annotations(info)) {
            String text = ae.print(simpleNames).toString();
            String name = nameOf(text);
            out.add(new DaemonProtocol.Annotation(text, polarity(name), positiveContextDefault(info, name)));
        }
        addNegatives(info, out);
        return out;
    }

    private void addNegatives(Info info, List<DaemonProtocol.Annotation> out) {
        PropertyValueMap a = info.analysis();
        switch (info) {
            case MethodInfo mi -> {
                if (!mi.isConstructor() && isModified(a, PropertyImpl.NON_MODIFYING_METHOD)) {
                    out.add(neg("@Modified", true)); // methods modify by default
                }
                if (!mi.isConstructor() && mi.hasReturnValue()) {
                    if (isMutable(a, PropertyImpl.IMMUTABLE_METHOD)) out.add(neg("@Mutable", true));
                    if (isDependent(a, PropertyImpl.INDEPENDENT_METHOD)) out.add(neg("@Dependent", true));
                    if (isNullable(a, PropertyImpl.NOT_NULL_METHOD)) out.add(neg("@Nullable", true));
                }
            }
            case FieldInfo fi -> {
                if (!fi.type().isPrimitiveStringClass() && isModified(a, PropertyImpl.UNMODIFIED_FIELD)) {
                    out.add(neg("@Modified", false)); // a modified field is always informative
                }
                if (isMutable(a, PropertyImpl.IMMUTABLE_FIELD)) out.add(neg("@Mutable", true));
                if (isDependent(a, PropertyImpl.INDEPENDENT_FIELD)) out.add(neg("@Dependent", true));
                if (isNullable(a, PropertyImpl.NOT_NULL_FIELD)) out.add(neg("@Nullable", true));
            }
            case ParameterInfo pi -> {
                if (!pi.parameterizedType().isPrimitiveStringClass() && isModified(a, PropertyImpl.UNMODIFIED_PARAMETER)) {
                    // a modified parameter — "this method changes your argument" — is always worth showing
                    out.add(neg("@Modified", false));
                }
                if (isMutable(a, PropertyImpl.IMMUTABLE_PARAMETER)) out.add(neg("@Mutable", true));
                if (isDependent(a, PropertyImpl.INDEPENDENT_PARAMETER)) out.add(neg("@Dependent", true));
                if (isNullable(a, PropertyImpl.NOT_NULL_PARAMETER)) out.add(neg("@Nullable", true));
            }
            case TypeInfo ti -> {
                if (isMutable(a, PropertyImpl.IMMUTABLE_TYPE)) out.add(neg("@Mutable", true));
                if (isDependent(a, PropertyImpl.INDEPENDENT_TYPE)) out.add(neg("@Dependent", true));
            }
            default -> {
            }
        }
    }

    /** A positive annotation is a context default when the enclosing declaration already implies it. */
    private boolean positiveContextDefault(Info info, String name) {
        TypeInfo owner = ownerType(info);
        if (owner == null) return false; // a type's own @Immutable/@Container etc. is always informative
        boolean container = containerType(owner);
        boolean immutable = immutableType(owner);
        return switch (name) {
            // immutable ⇒ all methods non-modifying; container/immutable ⇒ params non-modified
            case "NotModified" -> info instanceof MethodInfo ? immutable : (container || immutable);
            case "Independent" -> immutable; // immutable ⇒ independent
            default -> false;
        };
    }

    private static TypeInfo ownerType(Info info) {
        return switch (info) {
            case ParameterInfo pi -> pi.methodInfo().typeInfo();
            case MethodInfo mi -> mi.typeInfo();
            case FieldInfo fi -> fi.owner();
            default -> null;
        };
    }

    private static DaemonProtocol.Annotation neg(String text, boolean contextDefault) {
        return new DaemonProtocol.Annotation(text, NEGATIVE, contextDefault);
    }

    private static String polarity(String name) {
        if (POSITIVE_NAMES.contains(name)) return POSITIVE;
        if (NEGATIVE_NAMES.contains(name)) return NEGATIVE;
        return NEUTRAL;
    }

    /** "@Immutable(hc=true)" → "Immutable". */
    private static String nameOf(String text) {
        int start = text.startsWith("@") ? 1 : 0;
        int paren = text.indexOf('(', start);
        return text.substring(start, paren < 0 ? text.length() : paren);
    }

    // ---- value predicates (decided values only; absent = undecided = no negative) ----

    private static boolean isModified(PropertyValueMap a, Property property) {
        Value.Bool v = a.getOrNull(property, ValueImpl.BoolImpl.class);
        return v != null && v.isFalse(); // "unmodified/non-modifying" decided FALSE ⇒ modified
    }

    private static boolean isMutable(PropertyValueMap a, Property property) {
        Value.Immutable v = a.getOrNull(property, ValueImpl.ImmutableImpl.class);
        return v != null && v.isMutable();
    }

    private static boolean isDependent(PropertyValueMap a, Property property) {
        Value.Independent v = a.getOrNull(property, ValueImpl.IndependentImpl.class);
        return v != null && v.isDependent();
    }

    private static boolean isNullable(PropertyValueMap a, Property property) {
        Value.NotNullProperty v = a.getOrNull(property, ValueImpl.NotNullImpl.class);
        return v != null && v.isNullable();
    }

    private static boolean containerType(TypeInfo type) {
        return type.analysis().getOrDefault(PropertyImpl.CONTAINER_TYPE, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    private static boolean immutableType(TypeInfo type) {
        Value.Immutable v = type.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
        return v != null && !v.isMutable();
    }
}
