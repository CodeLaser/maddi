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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.expression.ArrayInitializer;
import io.codelaser.maddi.cst.api.expression.ClassExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.StringConstant;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * JAXB as System.Xml.Serialization's XmlSerializer. JAXB binds a type's fields, private ones too; XmlSerializer binds
 * public properties. So each field JAXB binds gets a public property, {@code XmlName}, that carries the attributes of
 * its JAXB annotations and reads and writes the field:
 * <pre>
 * [XmlElement("termination", Order = 11)]
 * public TerminationConfig XmlTerminationConfig { get => terminationConfig; set => terminationConfig = value; }
 * </pre>
 * The annotations themselves are dropped; the type's ({@code @XmlRootElement}, {@code @XmlType},
 * {@code @XmlSeeAlso}) become {@code [XmlRoot]}, {@code [XmlType]}, {@code [XmlInclude]}. The namespace of a type is
 * its {@code @XmlType}'s, or its package's {@code @XmlSchema}'s when that qualifies its elements; {@code propOrder} is
 * the elements' {@code Order}.
 * <p>
 * A type is bound when it, a field of it, or a superclass has a JAXB annotation; with {@code @XmlAccessorType(FIELD)}
 * (inherited, or its package's), every instance field that is not {@code transient} or {@code @XmlTransient} is
 * bound, otherwise the annotated and the public ones. The property has the field's type, or what XmlSerializer can
 * write:
 * <ul>
 *   <li>an enum of the program that is a C# enum is itself, its members {@code [XmlEnum("NAME")]}; one that is a
 *       class in C# is its name, {@code string};</li>
 *   <li>an interface, a self-bounded class's companion, or one of several {@code @XmlElements}, is {@code object},
 *       with an {@code [XmlElement(name, typeof(T))]} per type;</li>
 *   <li>{@code @XmlJavaTypeAdapter(A.class)} is the adapter's value type, through {@code new A().Marshal(…)} and
 *       {@code Unmarshal(…)} (the compatibility library's JavaXmlAdapter);</li>
 *   <li>a collection of what is converted so, or one of an interface type ({@code ISet<T>}), is an array, which
 *       XmlSerializer assigns when it has read it all.</li>
 * </ul>
 * A boxed primitive, {@code int?}, is written when it holds a value ({@code ShouldSerializeXmlName}). A field of a
 * type XmlSerializer cannot write (a map without adapter, an {@code @XmlIDREF}) is left out
 * ({@link CSharpPrintCode#XML_UNBOUND}). A public field or property of a bound type is {@code [XmlIgnore]}: its
 * property takes its place.
 */
final class JaxbXmlSerialization implements LibraryMapping {

    static final JaxbXmlSerialization INSTANCE = new JaxbXmlSerialization();

    private static final String XML = "System.Xml.Serialization";
    private static final List<String> PACKAGES = List.of("jakarta.xml.bind.annotation.", "javax.xml.bind.annotation.");

    /** What is known about the program being printed: which types JAXB binds, which enums it writes. */
    private record Known(CSharpProgram program, Map<TypeInfo, Boolean> bound, Set<TypeInfo> enums) {
    }

    private volatile Known known;

    private JaxbXmlSerialization() {
    }

    @Override
    public String id() {
        return "jaxb:xml-serialization";
    }

    private static boolean jaxb(TypeInfo annotationType) {
        String fqn = annotationType.fullyQualifiedName();
        return PACKAGES.stream().anyMatch(fqn::startsWith);
    }

    /** {@code XmlElement}: the JAXB annotation of that simple name on {@code annotations}, null without. */
    private static AnnotationExpression find(List<AnnotationExpression> annotations, String simpleName) {
        for (AnnotationExpression ae : annotations) {
            if (jaxb(ae.typeInfo()) && simpleName.equals(ae.typeInfo().simpleName())) return ae;
        }
        return null;
    }

    private static Expression element(AnnotationExpression ae, String key) {
        for (AnnotationExpression.KV kv : ae.keyValuePairs()) {
            if (key.equals(kv.key()) || "value".equals(key) && kv.keyIsDefault()) return kv.value();
        }
        return null;
    }

    private static List<Expression> elements(AnnotationExpression ae, String key) {
        Expression e = element(ae, key);
        if (e == null) return List.of();
        return e instanceof ArrayInitializer ai ? ai.expressions() : List.of(e);
    }

    /** A string element's value when it is a literal; null otherwise (a constant is printed instead). */
    private static String literal(Expression e) {
        return e != null && CSharpExpressionPrinter.unwrap(e) instanceof StringConstant sc ? sc.constant() : null;
    }

    /** JAXB's {@code "##default"}, which means: not given. */
    private static boolean given(Expression e) {
        return e != null && !"##default".equals(literal(e));
    }

    /** An enum constant element's name, {@code FIELD} of {@code XmlAccessType.FIELD}. */
    private static String constant(Expression e) {
        return e != null && CSharpExpressionPrinter.unwrap(e) instanceof VariableExpression ve
               && ve.variable() instanceof FieldReference fr ? fr.fieldInfo().name() : null;
    }

    private static TypeInfo classOf(Expression e) {
        return e != null && CSharpExpressionPrinter.unwrap(e) instanceof ClassExpression ce ? ce.type().typeInfo() : null;
    }

    @Override
    public AttributeMapping annotation(AnnotationExpression use, Target target, Function<Expression, String> print) {
        // what JAXB's annotations say is in the additions
        return jaxb(use.typeInfo()) ? AttributeMapping.DROPPED : null;
    }

    private Known known() {
        CSharpProgram program = CSharpContext.program();
        Known k = known;
        if (k == null || k.program() != program) {
            k = new Known(program, new ConcurrentHashMap<>(), ConcurrentHashMap.newKeySet());
            known = k;
            for (TypeInfo t : program.types()) {
                if (t.typeNature().isEnum() && t.annotations().stream().anyMatch(a -> jaxb(a.typeInfo()))) k.enums().add(t);
                if (!bound(t, k)) continue;
                for (FieldInfo f : t.fields()) {
                    ParameterizedType type = f.type();
                    for (ParameterizedType pt : java.util.stream.Stream.concat(java.util.stream.Stream.of(type),
                            type.parameters().stream()).toList()) {
                        if (pt.typeInfo() != null && pt.typeInfo().typeNature().isEnum()) k.enums().add(pt.typeInfo());
                    }
                }
            }
        }
        return k;
    }

    /** JAXB binds the type: it, a field of it, or a superclass has a JAXB annotation. An enum is its name. */
    boolean bound(TypeInfo t) {
        return bound(t, known());
    }

    private boolean bound(TypeInfo t, Known k) {
        Map<TypeInfo, Boolean> bound = k.bound();
        Boolean known = bound.get(t);
        if (known != null) return known;
        boolean b = !t.typeNature().isEnum() && !t.isInterface() && CSharpNames.translated(t)
                    && (t.annotations().stream().anyMatch(a -> jaxb(a.typeInfo()))
                        || t.fields().stream().anyMatch(f -> f.annotations().stream().anyMatch(a -> jaxb(a.typeInfo())))
                        || t.parentClass() != null && t.parentClass().typeInfo() != null
                           && bound(t.parentClass().typeInfo(), k));
        bound.put(t, b);
        return b;
    }

    /** {@code @XmlAccessorType(FIELD)} on the type, a superclass, or the package of one. */
    private static boolean fieldAccess(TypeInfo t) {
        for (TypeInfo s = t; s != null && CSharpNames.translated(s);
             s = s.parentClass() == null ? null : s.parentClass().typeInfo()) {
            AnnotationExpression a = find(s.annotations(), "XmlAccessorType");
            if (a == null) a = find(packageAnnotations(s), "XmlAccessorType");
            if (a != null) return "FIELD".equals(constant(element(a, "value")));
        }
        return false;
    }

    private static List<AnnotationExpression> packageAnnotations(TypeInfo t) {
        TypeInfo info = CSharpContext.program().packageInfo(t.primaryType().packageName());
        return info == null ? List.of() : info.annotations();
    }

    /** The type's namespace, as a C# expression: its {@code @XmlType}'s, or its package's qualified one. */
    private static String typeNamespace(TypeInfo t, Function<Expression, String> print) {
        AnnotationExpression type = find(t.annotations(), "XmlType");
        if (type != null && given(element(type, "namespace"))) return print.apply(element(type, "namespace"));
        AnnotationExpression schema = find(packageAnnotations(t), "XmlSchema");
        if (schema == null || !given(element(schema, "namespace"))
            || !"QUALIFIED".equals(constant(element(schema, "elementFormDefault")))) {
            return null;
        }
        return print.apply(element(schema, "namespace"));
    }

    @Override
    public AttributeMapping memberAttributes(FieldInfo field) {
        if (CSharpNames.isEnumConstant(field)) {
            // a C# enum's member is written by its C# name; JAXB writes the Java one
            String name = CSharpNames.field(field);
            if (name.equals(field.name()) || !CSharpTypePrinter.simpleEnum(field.owner())
                || !known().enums().contains(field.owner())) {
                return null;
            }
            return new AttributeMapping(List.of("XmlEnum(" + CSharpExpressionPrinter.stringLiteral(field.name()) + ")"),
                    List.of(XML));
        }
        return !field.isStatic() && bound(field.owner()) ? new AttributeMapping(List.of("XmlIgnore"), List.of(XML)) : null;
    }

    @Override
    public Additions additions(TypeInfo t, Qualification q) {
        if (!bound(t)) return null;
        Function<Expression, String> print = e -> CSharpExpressionPrinter.print(e, q).toString();
        List<String> namespaces = new ArrayList<>(List.of(XML));
        List<String> attributes = new ArrayList<>();
        String namespace = typeNamespace(t, print);
        AnnotationExpression root = find(t.annotations(), "XmlRootElement");
        if (root != null) {
            Expression name = element(root, "name");
            Expression ns = element(root, "namespace");
            AnnotationExpression schema = find(packageAnnotations(t), "XmlSchema");
            String rootNamespace = given(ns) ? print.apply(ns)
                    : schema != null && given(element(schema, "namespace")) ? print.apply(element(schema, "namespace"))
                    : null;
            attributes.add("XmlRoot(" + (given(name) ? print.apply(name)
                    : CSharpExpressionPrinter.stringLiteral(decapitalize(t.simpleName())))
                           + (rootNamespace == null ? "" : ", Namespace = " + rootNamespace) + ")");
        }
        AnnotationExpression type = find(t.annotations(), "XmlType");
        Expression typeName = type == null ? null : element(type, "name");
        boolean named = given(typeName) && !"".equals(literal(typeName));
        if (named || namespace != null) {
            attributes.add("XmlType(" + (named ? print.apply(typeName) : "")
                           + (namespace == null ? "" : (named ? ", " : "") + "Namespace = " + namespace) + ")");
        }
        AnnotationExpression seeAlso = find(t.annotations(), "XmlSeeAlso");
        if (seeAlso != null) elements(seeAlso, "value").forEach(e -> attributes.add("XmlInclude(" + print.apply(e) + ")"));

        List<String> order = new ArrayList<>();
        if (type != null) {
            for (Expression e : elements(type, "propOrder")) {
                String s = literal(e);
                if (s != null && !s.isEmpty()) order.add(s);
            }
        }
        boolean allFields = fieldAccess(t);
        List<AddedMember> members = new ArrayList<>();
        for (FieldInfo f : t.fields()) {
            if (f.isStatic() || f.isSynthetic() || f.isTransient() || find(f.annotations(), "XmlTransient") != null) {
                continue;
            }
            boolean annotated = f.annotations().stream().anyMatch(a -> jaxb(a.typeInfo()));
            if (!allFields && !annotated && !f.access().isPublic()) continue;
            members.addAll(bind(f, order.indexOf(f.name()), print, q, namespaces));
        }
        return new Additions(attributes, members, namespaces);
    }

    private static String decapitalize(String s) {
        if (s.length() > 1 && Character.isUpperCase(s.charAt(1)) && Character.isUpperCase(s.charAt(0))) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /** What XmlSerializer writes for a value of the field: its type, and how the field's value is it and back. */
    private record Conversion(String type, UnaryOperator<String> get, UnaryOperator<String> set) {
        boolean identity() {
            return type == null;
        }
    }

    private static final Conversion IDENTITY = new Conversion(null, x -> x, x -> x);

    /** The property of field {@code f}; nothing when XmlSerializer cannot write its type. */
    private List<AddedMember> bind(FieldInfo f, int order, Function<Expression, String> print, Qualification q,
                                   List<String> namespaces) {
        if (find(f.annotations(), "XmlIDREF") != null || find(f.annotations(), "XmlAnyElement") != null
            || find(f.annotations(), "XmlAnyAttribute") != null || find(f.annotations(), "XmlMixed") != null) {
            return unbound(f, "an @XmlIDREF, @XmlAny… or @XmlMixed field");
        }
        AnnotationExpression elementsAnnotation = find(f.annotations(), "XmlElements");
        List<AnnotationExpression> alternatives = elementsAnnotation == null ? List.of()
                : elements(elementsAnnotation, "value").stream()
                        .map(CSharpExpressionPrinter::unwrap).filter(AnnotationExpression.class::isInstance)
                        .map(AnnotationExpression.class::cast).toList();
        ParameterizedType type = f.type();
        boolean collection = type.arrays() == 0 && isCollection(type.typeInfo());
        ParameterizedType valueType = collection && type.parameters().size() == 1 ? type.parameters().getFirst() : type;
        if (collection && type.parameters().size() != 1) return unbound(f, "a raw collection");

        AnnotationExpression adapterAnnotation = find(f.annotations(), "XmlJavaTypeAdapter");
        if (adapterAnnotation == null && valueType.typeInfo() != null) {
            adapterAnnotation = find(valueType.typeInfo().annotations(), "XmlJavaTypeAdapter");
        }
        Conversion conversion;
        if (adapterAnnotation != null) {
            TypeInfo adapter = classOf(element(adapterAnnotation, "value"));
            ParameterizedType adapted = adapter == null ? null : adaptedValueType(adapter);
            if (adapted == null) return unbound(f, "an adapter without its value type");
            String a = CSharpTypeName.name(adapter, q);
            conversion = new Conversion(CSharpTypeName.of(adapted, q), x -> "new " + a + "().Marshal(" + x + ")",
                    x -> "new " + a + "().Unmarshal(" + x + ")");
        } else if (!alternatives.isEmpty() || objectTyped(valueType)) {
            String cast = CSharpTypeName.of(valueType, q);
            conversion = new Conversion("object", x -> x, x -> "(" + cast + ") " + x);
        } else if (valueType.arrays() == 0 && valueType.typeInfo() != null && valueType.typeInfo().typeNature().isEnum()
                   && CSharpNames.translated(valueType.typeInfo())
                   && CSharpTypePrinter.simpleEnum(valueType.typeInfo())) {
            // a C# enum, whose members are [XmlEnum("NAME")]
            conversion = IDENTITY;
        } else if (valueType.arrays() == 0 && valueType.typeInfo() != null && valueType.typeInfo().typeNature().isEnum()
                   && CSharpNames.translated(valueType.typeInfo())) {
            String e = CSharpTypeName.name(valueType.typeInfo(), q);
            conversion = new Conversion("string", x -> x + "?.Name", x -> x + " == null ? null : " + e + ".ValueOf(" + x + ")");
        } else if (writable(valueType)) {
            conversion = IDENTITY;
        } else {
            return unbound(f, "of type " + CSharpTypeName.of(type, q));
        }
        if (!conversion.identity() && type.arrays() > 0) return unbound(f, "an array of a converted type");
        if (conversion.identity() && collection && CSharpTypeName.of(type, q).matches("I[A-Z][A-Za-z]*<.*")) {
            // XmlSerializer cannot write a member of an interface type, ISet<T>: its elements as an array
            conversion = new Conversion(CSharpTypeName.of(valueType, q), x -> x, x -> x);
        }

        CSharpProperties.Property property = CSharpContext.program().property(f);
        String field = "this." + (property != null ? property.name() : CSharpNames.field(f));
        boolean settable = property != null ? property.settable() : !CSharpFieldPrinter.isFinal(f);
        boolean nullableContext = CSharpContext.program().facts().nullabilityAnalysed();
        NullableState nullable = CSharpContext.program().facts().nullable(f);
        String propertyType;
        String get;
        String set;
        if (conversion.identity()) {
            propertyType = CSharpTypeName.declared(type, false, nullable, q);
            get = field;
            set = field + " = value";
        } else if (collection) {
            namespaces.add("System.Linq");
            propertyType = conversion.type() + "[]" + (nullableContext ? "?" : "");
            String getElement = conversion.get().apply("e_");
            String setElement = conversion.set().apply("e_");
            get = field + "?" + ("e_".equals(getElement) ? "" : ".Select(e_ => " + getElement + ")") + ".ToArray()";
            String elements = "e_".equals(setElement) ? "value" : "value.Select(e_ => " + setElement + ")";
            set = field + " = value == null ? null" + (nullableContext && nullable != NullableState.NULLABLE ? "!" : "")
                  + " : " + rebuilt(CSharpTypeName.of(type, q), elements);
        } else {
            propertyType = conversion.type() + (nullableContext ? "?" : "");
            get = conversion.get().apply(field);
            String value = conversion.set().apply("value");
            set = field + " = " + value + (nullableContext && nullable != NullableState.NULLABLE ? "!" : "");
        }

        String name = "Xml" + CSharpNames.pascal(f.name());
        String orderPart = order < 0 ? "" : "Order = " + order;
        List<String> attributes = new ArrayList<>();
        AnnotationExpression attribute = find(f.annotations(), "XmlAttribute");
        AnnotationExpression wrapper = find(f.annotations(), "XmlElementWrapper");
        AnnotationExpression single = find(f.annotations(), "XmlElement");
        if (attribute != null) {
            if (type.isBoxedExcludingVoid()) return unbound(f, "an attribute of a boxed type");
            attributes.add("XmlAttribute(" + named(attribute, f, print, "") + ")");
        } else if (find(f.annotations(), "XmlValue") != null) {
            attributes.add("XmlText");
        } else if (wrapper != null) {
            attributes.add("XmlArray(" + named(wrapper, f, print, orderPart) + ")");
            if (alternatives.isEmpty()) {
                if (single != null) attributes.add("XmlArrayItem(" + named(single, null, print, "") + ")");
            } else {
                alternatives.forEach(alt -> attributes.add("XmlArrayItem(" + named(alt, null, print, typeOf(alt, print))
                                                           + ")"));
            }
        } else if (!alternatives.isEmpty()) {
            alternatives.forEach(alt -> attributes.add("XmlElement(" + named(alt, f, print,
                    join(typeOf(alt, print), orderPart)) + ")"));
        } else {
            String typed = single == null ? "" : typeOf(single, print);
            attributes.add("XmlElement(" + (single == null
                    ? join(CSharpExpressionPrinter.stringLiteral(f.name()), orderPart)
                    : named(single, f, print, join(typed, orderPart))) + ")");
        }
        List<AddedMember> members = new ArrayList<>();
        members.add(new AddedMember(attributes, "public " + propertyType + " " + name + " { get => " + get + ";"
                                                + (settable ? " set => " + set + ";" : "") + " }"));
        if (attribute == null && (type.isBoxedExcludingVoid() || conversion.identity() && propertyType.endsWith("?")
                                                                 && type.typeInfo() != null
                                                                 && type.typeInfo().typeNature().isEnum())) {
            // XmlSerializer writes a null int? as xsi:nil; JAXB leaves the element out
            members.add(new AddedMember(List.of(), "public bool ShouldSerialize" + name + "() => " + field + " != null;"));
        }
        return members;
    }

    /** {@code "name", Namespace = …, rest}: the annotation's name (the field's by default) and namespace. */
    private static String named(AnnotationExpression ae, FieldInfo f, Function<Expression, String> print, String rest) {
        Expression name = element(ae, "name");
        Expression ns = element(ae, "namespace");
        String n = given(name) ? print.apply(name) : f == null ? null : CSharpExpressionPrinter.stringLiteral(f.name());
        String args = n == null ? "" : n;
        if (rest.startsWith("typeof(")) {
            // the type is positional, after the name
            int comma = rest.indexOf(", ");
            args = join(args, comma < 0 ? rest : rest.substring(0, comma));
            rest = comma < 0 ? "" : rest.substring(comma + 2);
        }
        if (given(ns)) args = join(args, "Namespace = " + print.apply(ns));
        return join(args, rest);
    }

    private static String typeOf(AnnotationExpression ae, Function<Expression, String> print) {
        Expression type = element(ae, "type");
        TypeInfo t = classOf(type);
        // XmlElement.DEFAULT.class: none
        return t == null || "DEFAULT".equals(t.simpleName()) ? "" : print.apply(type);
    }

    private static String join(String a, String b) {
        return a.isEmpty() ? b : b.isEmpty() ? a : a + ", " + b;
    }

    /** {@code new List<T>(elements)}, or for an interface, the collection LINQ makes. */
    private static String rebuilt(String collectionType, String elements) {
        if (collectionType.matches("I(Set|ReadOnlySet)<.*")) return elements + ".ToHashSet()";
        if (collectionType.matches("I[A-Z][A-Za-z]*<.*")) return elements + ".ToList()";
        return "new " + collectionType + "(" + elements + ")";
    }

    private static List<AddedMember> unbound(FieldInfo f, String why) {
        CSharpContext.message(CSharpPrintCode.XML_UNBOUND, f, f.name() + ": " + why);
        return List.of();
    }

    private static boolean isCollection(TypeInfo t) {
        if (t == null) return false;
        if ("java.util.Collection".equals(t.fullyQualifiedName())) return true;
        return t.superTypesExcludingJavaLangObject().stream()
                .anyMatch(s -> "java.util.Collection".equals(s.fullyQualifiedName()));
    }

    /** An interface, or a self-bounded class's wildcard use, its companion interface: XmlSerializer needs a class. */
    private static boolean objectTyped(ParameterizedType pt) {
        TypeInfo t = pt.typeInfo();
        return pt.arrays() == 0 && t != null && CSharpNames.translated(t)
               && (t.isInterface() || CSharpCompanions.isWildcardUse(pt));
    }

    private static final Set<String> VALUES = Set.of("java.lang.String", "java.math.BigDecimal");

    /** XmlSerializer writes it as it is: a primitive, a string, a class of the program, an array of those. */
    private boolean writable(ParameterizedType pt) {
        TypeInfo t = pt.typeInfo();
        if (t == null) return false;
        if (t.isPrimitiveExcludingVoid() || t.isBoxedExcludingVoid() || VALUES.contains(t.fullyQualifiedName())) {
            return true;
        }
        return CSharpNames.translated(t) && !t.isInterface() && !t.typeNature().isEnum() && t.typeParameters().isEmpty();
    }

    /** {@code V} of the adapter's {@code XmlAdapter<V, B>}. */
    private static ParameterizedType adaptedValueType(TypeInfo adapter) {
        for (ParameterizedType p = adapter.parentClass(); p != null && p.typeInfo() != null;
             p = p.typeInfo().parentClass()) {
            if (p.typeInfo().fullyQualifiedName().endsWith(".adapters.XmlAdapter")) {
                return p.parameters().isEmpty() ? null : p.parameters().getFirst();
            }
        }
        return null;
    }
}
