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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.ClassExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.StringConstant;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.ArrayList;
import java.util.HashSet;
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
 *   <li>an interface, a self-bounded class's wildcard interface, or one of several {@code @XmlElements}, is {@code object},
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
            // filled before it is published: another thread printing the same program sees all of it or none
            k = new Known(program, new ConcurrentHashMap<>(), ConcurrentHashMap.newKeySet());
            for (TypeInfo t : program.types()) {
                if (t.typeNature().isEnum() && t.annotations().stream().anyMatch(a -> jaxb(a.typeInfo()))) k.enums().add(t);
                if (!bound(t, k)) continue;
                // every enum a field's type mentions, at any depth (Map<String, List<E>>): an enum that is not
                // written as well is harmless
                for (FieldInfo f : t.fields()) enums(f.type(), k.enums());
            }
            known = k;
        }
        return k;
    }

    private static void enums(ParameterizedType pt, Set<TypeInfo> enums) {
        if (pt.typeInfo() != null && pt.typeInfo().typeNature().isEnum()) enums.add(pt.typeInfo());
        pt.parameters().forEach(p -> enums(p, enums));
    }

    /** JAXB binds the type: it, a field or method of it, or a superclass has a JAXB annotation. An enum is its name. */
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
                        || t.methods().stream().anyMatch(m -> m.annotations().stream().anyMatch(a -> jaxb(a.typeInfo())))
                        || t.parentClass() != null && t.parentClass().typeInfo() != null
                           && bound(t.parentClass().typeInfo(), k));
        bound.put(t, b);
        return b;
    }

    /**
     * JAXB's access type of {@code t}, {@code FIELD} of {@code @XmlAccessorType(FIELD)}: on the type, a superclass, or
     * the package of one; {@code PUBLIC_MEMBER} by default.
     */
    private static String accessType(TypeInfo t) {
        for (TypeInfo s : CstQueries.classChain(t)) {
            if (!CSharpNames.translated(s)) break;
            AnnotationExpression a = find(s.annotations(), "XmlAccessorType");
            if (a == null) a = find(packageAnnotations(s), "XmlAccessorType");
            if (a != null) {
                String constant = constant(CSharpAttributes.element(a, "value"));
                return constant == null ? "PUBLIC_MEMBER" : constant;
            }
        }
        return "PUBLIC_MEMBER";
    }

    /** A field's JavaBean property: the getter of the field's type and name, and its setter, null without. */
    private record Accessors(MethodInfo getter, MethodInfo setter) {
        boolean isPublic() {
            return getter.access().isPublic() && (setter == null || setter.access().isPublic());
        }

        List<AnnotationExpression> annotations() {
            List<AnnotationExpression> all = new ArrayList<>(getter.annotations());
            if (setter != null) all.addAll(setter.annotations());
            return all;
        }
    }

    /** The field's getter and setter; null when it has no getter. */
    private static Accessors accessors(FieldInfo f) {
        String capitalized = Character.toUpperCase(f.name().charAt(0)) + f.name().substring(1);
        MethodInfo getter = null;
        MethodInfo setter = null;
        for (MethodInfo m : f.owner().methods()) {
            if (m.isStatic()) continue;
            if (m.parameters().isEmpty() && f.type().equals(m.returnType())
                && (m.name().equals("get" + capitalized) || m.name().equals("is" + capitalized) && f.type().isBoolean())) {
                getter = m;
            } else if (m.parameters().size() == 1 && m.name().equals("set" + capitalized)
                       && f.type().equals(m.parameters().getFirst().parameterizedType())) {
                setter = m;
            }
        }
        return getter == null ? null : new Accessors(getter, setter);
    }

    /**
     * Binding the field is binding its property: the getter returns the field, and the setter, if any, only assigns
     * it. A getter or setter that does more, JAXB calls and XmlSerializer would not.
     */
    private static boolean plain(Accessors accessors, FieldInfo f) {
        if (accessors.getter().methodBody() == null) return false;
        Expression returned = CstQueries.returnedValue(CstQueries.own(accessors.getter().methodBody().statements()));
        if (!(returned != null && CSharpExpressionPrinter.unwrap(returned) instanceof VariableExpression ve
              && ve.variable() instanceof FieldReference fr && fr.fieldInfo().equals(f))) {
            return false;
        }
        MethodInfo setter = accessors.setter();
        if (setter == null) return true;
        if (setter.methodBody() == null) return false;
        var own = CstQueries.own(setter.methodBody().statements());
        return own.size() == 1 && own.getFirst() instanceof ExpressionAsStatement eas
               && CSharpExpressionPrinter.unwrap(eas.expression()) instanceof Assignment a
               && a.variableTarget() instanceof FieldReference target && target.fieldInfo().equals(f);
    }

    private static List<AnnotationExpression> packageAnnotations(TypeInfo t) {
        TypeInfo info = CSharpContext.program().packageInfo(t.primaryType().packageName());
        return info == null ? List.of() : info.annotations();
    }

    /** The type's namespace, as a C# expression: its {@code @XmlType}'s, or its package's qualified one. */
    private static String typeNamespace(TypeInfo t, Function<Expression, String> print) {
        AnnotationExpression type = find(t.annotations(), "XmlType");
        if (type != null && given(CSharpAttributes.element(type, "namespace"))) return print.apply(CSharpAttributes.element(type, "namespace"));
        AnnotationExpression schema = find(packageAnnotations(t), "XmlSchema");
        if (schema == null || !given(CSharpAttributes.element(schema, "namespace"))
            || !"QUALIFIED".equals(constant(CSharpAttributes.element(schema, "elementFormDefault")))) {
            return null;
        }
        return print.apply(CSharpAttributes.element(schema, "namespace"));
    }

    @Override
    public AttributeMapping memberAttributes(FieldInfo field) {
        if (CSharpNames.isEnumConstant(field)) {
            // a C# enum's member is written by its C# name; JAXB writes the Java one
            String name = CSharpNames.field(field);
            if (name.equals(field.name()) || !CSharpNames.simpleEnum(field.owner())
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
            Expression name = CSharpAttributes.element(root, "name");
            Expression ns = CSharpAttributes.element(root, "namespace");
            AnnotationExpression schema = find(packageAnnotations(t), "XmlSchema");
            String rootNamespace = given(ns) ? print.apply(ns)
                    : schema != null && given(CSharpAttributes.element(schema, "namespace")) ? print.apply(CSharpAttributes.element(schema, "namespace"))
                    : null;
            attributes.add("XmlRoot(" + (given(name) ? print.apply(name)
                    : CSharpExpressionPrinter.stringLiteral(decapitalize(t.simpleName())))
                           + (rootNamespace == null ? "" : ", Namespace = " + rootNamespace) + ")");
        }
        AnnotationExpression type = find(t.annotations(), "XmlType");
        Expression typeName = type == null ? null : CSharpAttributes.element(type, "name");
        boolean named = given(typeName) && !"".equals(literal(typeName));
        if (named || namespace != null) {
            attributes.add("XmlType(" + (named ? print.apply(typeName) : "")
                           + (namespace == null ? "" : (named ? ", " : "") + "Namespace = " + namespace) + ")");
        }
        AnnotationExpression seeAlso = find(t.annotations(), "XmlSeeAlso");
        if (seeAlso != null) CSharpAttributes.elements(seeAlso, "value").forEach(e -> attributes.add("XmlInclude(" + print.apply(e) + ")"));

        List<String> order = new ArrayList<>();
        if (type != null) {
            for (Expression e : CSharpAttributes.elements(type, "propOrder")) {
                String s = literal(e);
                if (s != null && !s.isEmpty()) order.add(s);
            }
        }
        // JAXB binds fields (FIELD), JavaBean properties (PROPERTY), the public ones of both (PUBLIC_MEMBER, the
        // default), or the annotated ones only (NONE); a property is bound through its field
        String access = accessType(t);
        Set<MethodInfo> boundAccessors = new HashSet<>();
        List<AddedMember> members = new ArrayList<>();
        for (FieldInfo f : t.fields()) {
            if (f.isStatic() || f.isSynthetic() || f.isTransient()) continue;
            Accessors accessors = "FIELD".equals(access) ? null : accessors(f);
            List<AnnotationExpression> annotations = new ArrayList<>(f.annotations());
            if (accessors != null) annotations.addAll(accessors.annotations());
            if (find(annotations, "XmlTransient") != null) continue;
            boolean annotated = annotations.stream().anyMatch(a -> jaxb(a.typeInfo()));
            boolean property = accessors != null
                               && (accessors.setter() != null || isCollection(f.type().typeInfo()));
            boolean included = switch (access) {
                case "FIELD" -> true;
                case "NONE" -> annotated;
                case "PROPERTY" -> annotated || property;
                default -> annotated || f.access().isPublic() || property && accessors.isPublic();
            };
            if (!included) continue;
            boolean viaProperty = accessors != null && !f.access().isPublic()
                                  && (property || accessors.annotations().stream().anyMatch(a -> jaxb(a.typeInfo())));
            if (accessors != null) {
                boundAccessors.add(accessors.getter());
                if (accessors.setter() != null) boundAccessors.add(accessors.setter());
            }
            if (viaProperty && !plain(accessors, f)) {
                members.addAll(unbound(accessors.getter(), accessors.getter().name()
                                                           + "(): a property whose getter or setter does more than access its field"));
                continue;
            }
            members.addAll(bind(f, annotations, order.indexOf(f.name()), print, q, namespaces));
        }
        // a JAXB annotation on any other method: a property without a field to bind
        for (MethodInfo m : t.methods()) {
            if (!boundAccessors.contains(m) && m.annotations().stream().anyMatch(a -> jaxb(a.typeInfo()))
                && find(m.annotations(), "XmlTransient") == null) {
                unbound(m, m.name() + "(): a property without a field of its name and type");
            }
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
    private List<AddedMember> bind(FieldInfo f, List<AnnotationExpression> annotations, int order,
                                   Function<Expression, String> print, Qualification q, List<String> namespaces) {
        if (find(annotations, "XmlIDREF") != null || find(annotations, "XmlAnyElement") != null
            || find(annotations, "XmlAnyAttribute") != null || find(annotations, "XmlMixed") != null) {
            return unbound(f, "an @XmlIDREF, @XmlAny… or @XmlMixed field");
        }
        AnnotationExpression elementsAnnotation = find(annotations, "XmlElements");
        List<AnnotationExpression> alternatives = elementsAnnotation == null ? List.of()
                : CSharpAttributes.elements(elementsAnnotation, "value").stream()
                        .map(CSharpExpressionPrinter::unwrap).filter(AnnotationExpression.class::isInstance)
                        .map(AnnotationExpression.class::cast).toList();
        ParameterizedType type = f.type();
        boolean collection = type.arrays() == 0 && isCollection(type.typeInfo());
        if (collection && type.parameters().size() != 1) return unbound(f, "a raw collection");
        Converted converted = conversion(type, collection, annotations, alternatives, q);
        if (converted.why() != null) return unbound(f, converted.why());
        Conversion conversion = converted.conversion();
        PropertyCode code = propertyCode(f, conversion, collection, q, namespaces);
        AnnotationExpression attribute = find(annotations, "XmlAttribute");
        if (attribute != null && CSharpTypeName.valueReference(type)) return unbound(f, "an attribute of a nullable value type");
        String name = "Xml" + CSharpNames.pascal(f.name());
        List<AddedMember> members = new ArrayList<>();
        members.add(new AddedMember(attributes(f, annotations, alternatives, order, print), "public "
                + code.type() + " " + name + " { get => " + code.get() + ";"
                + (code.set() != null ? " set => " + code.set() + ";" : "") + " }"));
        if (attribute == null && (CSharpTypeName.valueReference(type) || conversion.identity() && code.type().endsWith("?")
                                                                 && type.typeInfo() != null
                                                                 && type.typeInfo().typeNature().isEnum())) {
            // XmlSerializer writes a null int? as xsi:nil; JAXB leaves the element out
            members.add(new AddedMember(List.of(), "public bool ShouldSerialize" + name + "() => " + code.field()
                                                   + " != null;"));
        }
        return members;
    }

    /** A field's conversion, or why XmlSerializer cannot write the field. */
    private record Converted(Conversion conversion, String why) {
        static Converted unbound(String why) {
            return new Converted(null, why);
        }
    }

    /** How XmlSerializer writes the value of a field of {@code type}: as itself, through an adapter, as a string… */
    private Converted conversion(ParameterizedType type, boolean collection, List<AnnotationExpression> annotations,
                                        List<AnnotationExpression> alternatives, Qualification q) {
        ParameterizedType valueType = collection ? type.parameters().getFirst() : type;
        AnnotationExpression adapterAnnotation = find(annotations, "XmlJavaTypeAdapter");
        if (adapterAnnotation == null && valueType.typeInfo() != null) {
            adapterAnnotation = find(valueType.typeInfo().annotations(), "XmlJavaTypeAdapter");
        }
        Conversion conversion;
        if (adapterAnnotation != null) {
            TypeInfo adapter = classOf(CSharpAttributes.element(adapterAnnotation, "value"));
            ParameterizedType adapted = adapter == null ? null : adaptedValueType(adapter);
            if (adapted == null) return Converted.unbound("an adapter without its value type");
            String a = CSharpTypeName.name(adapter, q);
            // the adapter's Marshal(TimeSpan) of a TimeSpan?: JAXB marshals no null
            String value = !collection && CSharpTypeName.valueReference(valueType) && !valueType.isBoxedExcludingVoid()
                           && !CSharpTypeName.valueReference(adapted) && !adapted.isPrimitiveExcludingVoid()
                    ? CSharpTypeName.argument(valueType, q) : null;
            conversion = new Conversion(CSharpTypeName.of(adapted, q), x -> value == null
                    ? "new " + a + "().Marshal(" + x + ")"
                    : x + " == null ? null : new " + a + "().Marshal((" + value + ") " + x + ")",
                    x -> "new " + a + "().Unmarshal(" + x + ")");
        } else if (!alternatives.isEmpty() || objectTyped(valueType)) {
            // a single value may be null; a collection's elements are not
            String cast = CSharpTypeName.of(valueType, q)
                          + (!collection && CSharpContext.program().facts().nullabilityAnalysed() ? "?" : "");
            conversion = new Conversion("object", x -> x, x -> "(" + cast + ") " + x);
        } else if (valueType.arrays() == 0 && valueType.typeInfo() != null && valueType.typeInfo().typeNature().isEnum()
                   && CSharpNames.translated(valueType.typeInfo())
                   && CSharpNames.simpleEnum(valueType.typeInfo())) {
            // a C# enum, whose members are [XmlEnum("NAME")]
            conversion = IDENTITY;
        } else if (valueType.arrays() == 0 && valueType.typeInfo() != null && valueType.typeInfo().typeNature().isEnum()
                   && CSharpNames.translated(valueType.typeInfo())) {
            String e = CSharpTypeName.name(valueType.typeInfo(), q);
            conversion = collection ? new Conversion("string", x -> x + ".Name", x -> e + ".ValueOf(" + x + ")")
                    : new Conversion("string", x -> x + "?.Name", x -> x + " == null ? null : " + e + ".ValueOf(" + x + ")");
        } else if (writable(valueType)) {
            conversion = IDENTITY;
        } else {
            return Converted.unbound("of type " + CSharpTypeName.of(type, q));
        }
        if (!conversion.identity() && type.arrays() > 0) return Converted.unbound("an array of a converted type");
        if (conversion.identity() && collection && genericInterface(type)) {
            // XmlSerializer cannot write a member of an interface type, ISet<T>: its elements as an array
            conversion = new Conversion(CSharpTypeName.argument(valueType, q), x -> x, x -> x);
        }
        return new Converted(conversion, null);
    }

    /** The property's type, its getter's and setter's bodies (null: no setter), over {@code field}. */
    private record PropertyCode(String type, String get, String set, String field) {
    }

    /** The property's code: the field's value as XmlSerializer writes it, and back. */
    private static PropertyCode propertyCode(FieldInfo f, Conversion conversion, boolean collection, Qualification q,
                                       List<String> namespaces) {
        ParameterizedType type = f.type();
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
                  + " : " + rebuilt(type, CSharpTypeName.of(type, q), elements);
        } else {
            propertyType = conversion.type() + (nullableContext ? "?" : "");
            get = conversion.get().apply(field);
            String value = conversion.set().apply("value");
            set = field + " = " + (nullableContext && nullable != NullableState.NULLABLE ? "(" + value + ")!" : value);
        }
        return new PropertyCode(propertyType, get, settable ? set : null, field);
    }

    /** The property's XmlSerializer attributes: an attribute, the text, an array and its items, or its elements. */
    private static List<String> attributes(FieldInfo f, List<AnnotationExpression> annotations,
                                           List<AnnotationExpression> alternatives, int order,
                                           Function<Expression, String> print) {
        String orderPart = order < 0 ? "" : "Order = " + order;
        List<String> attributes = new ArrayList<>();
        AnnotationExpression attribute = find(annotations, "XmlAttribute");
        AnnotationExpression wrapper = find(annotations, "XmlElementWrapper");
        AnnotationExpression single = find(annotations, "XmlElement");
        if (attribute != null) {
            attributes.add("XmlAttribute(" + named(attribute, f, print, "") + ")");
        } else if (find(annotations, "XmlValue") != null) {
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
        return attributes;
    }

    /** {@code "name", Namespace = …, rest}: the annotation's name (the field's by default) and namespace. */
    private static String named(AnnotationExpression ae, FieldInfo f, Function<Expression, String> print, String rest) {
        Expression name = CSharpAttributes.element(ae, "name");
        Expression ns = CSharpAttributes.element(ae, "namespace");
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
        Expression type = CSharpAttributes.element(ae, "type");
        TypeInfo t = classOf(type);
        // XmlElement.DEFAULT.class: none
        return t == null || "DEFAULT".equals(t.simpleName()) ? "" : print.apply(type);
    }

    private static String join(String a, String b) {
        return a.isEmpty() ? b : b.isEmpty() ? a : a + ", " + b;
    }

    /** {@code new List<T>(elements)}, or for an interface, the collection LINQ makes. */
    private static String rebuilt(ParameterizedType type, String collectionType, String elements) {
        if (!genericInterface(type)) return "new " + collectionType + "(" + elements + ")";
        return collectionType.matches("I(Set|ReadOnlySet)<.*") ? elements + ".ToHashSet()" : elements + ".ToList()";
    }

    /** A collection type that is an interface in C#: {@code ISet<T>}, as its mapping has it. */
    private static boolean genericInterface(ParameterizedType type) {
        if (type.arrays() > 0 || type.typeInfo() == null) return false;
        LibraryMapping.TypeMapping mapping = CSharpContext.libraries().type(type.typeInfo());
        return mapping != null ? mapping.isInterface() : type.typeInfo().isInterface();
    }

    private static List<AddedMember> unbound(FieldInfo f, String why) {
        return unbound(f, f.name() + ": " + why);
    }

    private static List<AddedMember> unbound(Element subject, String what) {
        CSharpContext.message(CSharpPrintCode.XML_UNBOUND, subject, what);
        return List.of();
    }

    private static boolean isCollection(TypeInfo t) {
        if (t == null) return false;
        if ("java.util.Collection".equals(t.fullyQualifiedName())) return true;
        return t.superTypesExcludingJavaLangObject().stream()
                .anyMatch(s -> "java.util.Collection".equals(s.fullyQualifiedName()));
    }

    /** An interface, or a self-bounded class's wildcard use, its wildcard interface: XmlSerializer needs a class. */
    private static boolean objectTyped(ParameterizedType pt) {
        TypeInfo t = pt.typeInfo();
        return pt.arrays() == 0 && t != null && CSharpNames.translated(t)
               && (t.isInterface() || CSharpWildcardInterfaces.isWildcardUse(pt));
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
