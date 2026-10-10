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

import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.info.*;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.info.CompilationUnitPrinterImpl;
import io.codelaser.maddi.cst.impl.output.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Prints a {@link TypeInfo} as a C# type declaration, with the same pluggable-printer seam as the Java
 * {@code TypePrinterImpl}: the method, field and enclosed-type printers come from factories, defaulting to the C#
 * ones.
 * <ul>
 *   <li>a final class is {@code sealed}; a class of static members only, whose one constructor is private and
 *   empty, is a {@code static class};</li>
 *   <li>a nested class never has a {@code static} modifier: C# nested classes have no enclosing instance;</li>
 *   <li>an enum of constants only is a C# {@code enum}; one with fields, methods or constructors is a sealed class
 *   with a {@code static readonly} instance per constant;</li>
 *   <li>a record is a positional {@code sealed record}, its components properties;</li>
 *   <li>superclass and interfaces follow {@code :}, type-parameter bounds are {@code where} constraints.</li>
 * </ul>
 */
public record CSharpTypePrinter(TypeInfo typeInfo, boolean formatter2) implements TypePrinter {

    @Override
    public List<TypeModifier> minimalModifiers(TypeInfo typeInfo) {
        return List.of();
    }

    @Override
    public OutputBuilder print(ImportComputer importComputer, Qualification qualification, boolean doTypeDeclaration) {
        CompilationUnitPrinterImpl printer = new CompilationUnitPrinterImpl(typeInfo.compilationUnit(), formatter2);
        return print(printer.computeImportData(importComputer, qualification), doTypeDeclaration);
    }

    @Override
    public OutputBuilder print(CompilationUnitPrinter.ImportData importData, boolean doTypeDeclaration) {
        return print(importData, doTypeDeclaration, CSharpMethodPrinter::new, CSharpFieldPrinter::new,
                CSharpTypePrinter::new);
    }

    @Override
    public OutputBuilder print(CompilationUnitPrinter.ImportData importData, boolean doTypeDeclaration,
                               MethodPrinterFactory methodPrinterFactory, FieldPrinterFactory fieldPrinterFactory,
                               EnclosedTypePrinterFactory enclosedTypePrinterFactory) {
        CSharpContext.pushType(typeInfo);
        try {
            if (CSharpAttributes.isAttribute(typeInfo)) {
                return CSharpAttributes.declaration(typeInfo, typeAccess(), importData.insideType());
            }
            if (typeInfo.typeNature().isAnnotation()) {
                CSharpContext.message(CSharpPrintMessage.Code.ANNOTATION_TYPE, typeInfo, typeInfo.simpleName());
                return new OutputBuilderImpl();
            }
            if (typeInfo.typeNature().isEnum() && simpleEnum()) return simpleEnum(importData.insideType());
            if (CSharpContext.program().delegate(typeInfo)) return delegate(importData.insideType());
            return printType(importData, doTypeDeclaration, methodPrinterFactory, fieldPrinterFactory,
                    enclosedTypePrinterFactory);
        } finally {
            CSharpContext.popType();
        }
    }

    private OutputBuilder printType(CompilationUnitPrinter.ImportData importData, boolean doTypeDeclaration,
                                    MethodPrinterFactory methodPrinterFactory, FieldPrinterFactory fieldPrinterFactory,
                                    EnclosedTypePrinterFactory enclosedTypePrinterFactory) {
        Qualification q = importData.insideType();
        List<CSharpAnonymous.Hoisted> anonymous = CSharpAnonymous.in(typeInfo);
        anonymous.forEach(CSharpContext::hoisted);
        CSharpAnonymous.Hoisted self = typeInfo.isAnonymous() ? CSharpContext.hoisted(typeInfo) : null;
        boolean record = typeInfo.typeNature().isRecord();
        boolean enumClass = typeInfo.typeNature().isEnum();
        boolean staticClass = staticClass();
        List<FieldInfo> components = record
                ? typeInfo.fields().stream().filter(f -> !f.isStatic() && !f.isSynthetic()).toList() : List.of();
        // a positional record has no constructor body: one with a canonical or compact constructor declares its
        // properties and that constructor
        MethodInfo canonical = record ? canonicalConstructor(components) : null;
        if (enumClass) CSharpContext.message(CSharpPrintMessage.Code.ENUM_AS_CLASS, typeInfo, typeInfo.simpleName());
        TypeInfo enclosing = enclosingType(typeInfo);
        if (enclosing != null && !enclosing.typeParameters().isEmpty() && !CSharpNames.hoisted(typeInfo)) {
            CSharpContext.message(CSharpPrintMessage.Code.NESTED_IN_GENERIC, typeInfo, typeInfo.fullyQualifiedName());
        }

        OutputBuilder out = new OutputBuilderImpl();
        if (doTypeDeclaration) {
            out.add(CSharpAttributes.uses(typeInfo.annotations(), q, true));
            StringBuilder modifiers = new StringBuilder();
            String access = typeAccess();
            if (access != null) modifiers.append(access).append(' ');
            if (staticClass) modifiers.append("static ");
            else if (typeInfo.isAbstract() && !typeInfo.isInterface()) modifiers.append("abstract ");
            else if (record || enumClass || typeInfo.typeNature().isClass()
                                            && (typeInfo.isFinal() || !CSharpContext.program().open(typeInfo))) {
                modifiers.append("sealed ");
            }
            String keyword = typeInfo.isInterface() ? "interface" : record ? "record" : "class";
            List<TypeParameter> declaredTypeParameters = self != null ? self.typeParameters() : typeInfo.typeParameters();
            String typeParameters = declaredTypeParameters.isEmpty() ? ""
                    : declaredTypeParameters.stream().map(CSharpNames::typeParameter)
                            .collect(Collectors.joining(", ", "<", ">"));
            out.add(new TextImpl(modifiers + keyword)).add(SpaceEnum.ONE)
                    .add(new TextImpl(CSharpNames.type(typeInfo) + typeParameters));
            if (record && canonical == null) {
                FieldPrinterFactory componentPrinter = CSharpFieldPrinter::new;
                out.add(components.stream().map(f -> componentPrinter.create(f, formatter2).print(q, true))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                                SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration())));
            }
            List<String> supers = superTypes(q);
            if (!supers.isEmpty()) {
                out.add(SymbolEnum.COLON).add(new TextImpl(String.join(", ", supers)));
            }
            for (String constraint : CSharpTypeName.constraints(declaredTypeParameters, q)) {
                out.add(SpaceEnum.ONE).add(new TextImpl(constraint));
            }
        }

        List<OutputBuilder> members = new ArrayList<>();
        if (enumClass) enumInstances(q).forEach(members::add);
        if (self != null) members.addAll(hoistedMembers(self, q));
        if (canonical != null) {
            components.forEach(f -> members.add(new OutputBuilderImpl().add(new TextImpl("public "
                    + CSharpTypeName.of(f.type(), q) + " " + CSharpNames.field(f) + " { get; }"))));
        }
        typeInfo.fields().stream()
                .filter(f -> !f.isSynthetic() && !CSharpNames.isEnumConstant(f) && !components.contains(f))
                .forEach(f -> members.add(fieldPrinterFactory.create(f, formatter2).print(q, false)));
        typeInfo.constructors().stream()
                .filter(c -> !c.isSynthetic() && !isImplicitDefaultConstructor(c) && !(staticClass && c.parameters().isEmpty()))
                .filter(c -> {
                    if (record && c != canonical && c.parameters().size() == components.size()) {
                        CSharpContext.message(CSharpPrintMessage.Code.RECORD_CONSTRUCTOR, c, CSharpContext.describe(c));
                        return false;
                    }
                    return true;
                })
                .forEach(c -> members.add(methodPrinterFactory.create(typeInfo, c, formatter2).print(q)));
        typeInfo.methods().stream()
                .filter(m -> !m.isSynthetic() && !(record && isRecordAccessor(m)))
                .forEach(m -> members.add(methodPrinterFactory.create(typeInfo, m, formatter2).print(q)));
        typeInfo.subTypes().stream().filter(st -> !st.isSynthetic() && !CSharpNames.hoisted(st))
                .forEach(st -> members.add(enclosedTypePrinterFactory.create(st, formatter2).print(importData, true)));
        CSharpLocalTypes.lifted(typeInfo)
                .forEach(lt -> members.add(new CSharpTypePrinter(lt, formatter2).print(importData, true)));
        if (CSharpNames.lambdaAdapter(typeInfo)) members.add(lambdaAdapter(q));
        members.addAll(enumerable(q));
        if (record) members.addAll(accessorImplementations(components, q));
        if (!typeInfo.isInterface()) members.addAll(covariantImplementations(q));
        if (!typeInfo.isInterface() && typeInfo.isAbstract()) members.addAll(abstractRedeclarations(q));
        anonymous.forEach(h -> members.add(new CSharpTypePrinter(h.type(), formatter2).print(importData, true)));

        List<OutputBuilder> nonEmpty = members.stream().filter(m -> !m.isEmpty()).toList();
        if (record && nonEmpty.isEmpty() && doTypeDeclaration) return out.add(SymbolEnum.SEMICOLON);
        out.add(SpaceEnum.ONE).add(CSharpStatementPrinter.braces(nonEmpty));
        return out;
    }

    /**
     * The fields of a hoisted anonymous class's captures, and its constructor: the superclass's arguments, the
     * enclosing instance, the captures, then the instance fields' initializers.
     */
    private List<OutputBuilder> hoistedMembers(CSharpAnonymous.Hoisted h, Qualification q) {
        List<OutputBuilder> members = new ArrayList<>();
        List<String> parameters = new ArrayList<>();
        List<OutputBuilder> body = new ArrayList<>();
        for (int i = 0; i < h.superArguments().size(); i++) {
            parameters.add(CSharpTypeName.of(h.superArguments().get(i), q) + " p" + i);
        }
        if (h.outer()) {
            TypeInfo outer = enclosingType(typeInfo);
            String outerType = CSharpNames.type(outer) + (outer.typeParameters().isEmpty() ? ""
                    : outer.typeParameters().stream().map(CSharpNames::typeParameter)
                            .collect(Collectors.joining(", ", "<", ">")));
            members.add(new OutputBuilderImpl().add(new TextImpl("private readonly " + outerType + " outer;")));
            parameters.add(outerType + " outer");
            body.add(new OutputBuilderImpl().add(new TextImpl("this.outer = outer;")));
        }
        for (CSharpAnonymous.Capture c : h.captures()) {
            String type = CSharpTypeName.of(c.type(), q);
            String name = CSharpNames.name(c.name());
            members.add(new OutputBuilderImpl().add(new TextImpl("private readonly " + type + " " + name + ";")));
            parameters.add(type + " " + name);
            body.add(new OutputBuilderImpl().add(new TextImpl("this." + name + " = " + name + ";")));
        }
        for (FieldInfo f : typeInfo.fields()) {
            if (f.isSynthetic() || f.isStatic() || f.initializer() == null || f.initializer().isEmpty()) continue;
            body.add(new OutputBuilderImpl().add(new TextImpl("this." + CSharpNames.field(f)))
                    .add(SymbolEnum.assignment("=")).add(CSharpExpressionPrinter.initializer(f.initializer(), f.type(), q))
                    .add(SymbolEnum.SEMICOLON));
        }
        if (parameters.isEmpty() && body.isEmpty()) return members;
        String baseCall = h.superArguments().isEmpty() ? "" : " : base(" + java.util.stream.IntStream
                .range(0, h.superArguments().size()).mapToObj(i -> "p" + i).collect(Collectors.joining(", ")) + ")";
        members.add(new OutputBuilderImpl().add(new TextImpl("internal " + h.name() + "(" + String.join(", ", parameters)
                + ")" + baseCall)).add(SpaceEnum.ONE).add(CSharpStatementPrinter.braces(body)));
        return members;
    }

    private String typeAccess() {
        if (typeInfo.isAnonymous() && CSharpContext.hoisted(typeInfo) != null) return "private";
        if (isLocal(typeInfo)) return "private"; // lifted into the enclosing type
        // a top-level type is public or internal
        if (typeInfo.isPrimaryType()) return typeInfo.typeModifiers().stream().anyMatch(TypeModifier::isPublic) ? "public" : "internal";
        // a hoisted type is in the namespace, where C# has no private or protected
        if (CSharpNames.hoisted(typeInfo)) {
            return "public".equals(CSharpModifiers.access(typeInfo, enclosingType(typeInfo))) ? "public" : "internal";
        }
        return CSharpModifiers.access(typeInfo, enclosingType(typeInfo));
    }

    private static final java.util.Set<String> LIST_BASES = java.util.Set.of("java.util.ArrayList",
            "java.util.AbstractList", "java.util.LinkedList");

    private static final java.util.Set<String> MARKERS = java.util.Set.of("java.lang.Cloneable", "java.io.Serializable",
            "java.util.RandomAccess");

    /** The superclass, unless implicit (Object, Enum, Record), and the interfaces. */
    private List<String> superTypes(Qualification q) {
        List<String> supers = new ArrayList<>();
        ParameterizedType parent = typeInfo.parentClass();
        if (parent != null && !parent.isJavaLangObject() && parent.typeInfo() != null
            && !"java.lang.Enum".equals(parent.typeInfo().fullyQualifiedName())
            && !"java.lang.Record".equals(parent.typeInfo().fullyQualifiedName())) {
            if (LIST_BASES.contains(parent.typeInfo().fullyQualifiedName())) {
                // C#'s List has no virtual methods: the compatibility library's JavaArrayList, whose Java methods are
                CSharpContext.using(CSharpCompat.NAMESPACE);
                if (typeInfo.methods().stream().anyMatch(m -> m.overrides().stream()
                        .anyMatch(o -> LIST_BASES.contains(o.typeInfo().fullyQualifiedName())))) {
                    CSharpContext.message(CSharpPrintMessage.Code.LIST_SUBCLASS, typeInfo, typeInfo.simpleName());
                }
                supers.add("JavaArrayList<" + parent.parameters().stream().map(p -> CSharpTypeName.argument(p, q))
                        .collect(Collectors.joining(", ")) + ">");
            } else {
                supers.add(CSharpTypeName.of(parent, q));
            }
        }
        // Java's marker interfaces have no C# counterpart: cloning is MemberwiseClone, serialization is opt-in
        typeInfo.interfacesImplemented().stream()
                .filter(i -> i.typeInfo() == null || !MARKERS.contains(i.typeInfo().fullyQualifiedName()))
                .forEach(i -> supers.add(CSharpTypeName.of(i, q)));
        return supers;
    }

    /**
     * A class of static members only, whose one constructor, if written, is private, parameterless and empty: Java's
     * idiom for a utility class, which C# says with {@code static class}.
     */
    private boolean staticClass() {
        if (!typeInfo.typeNature().isClass() || typeInfo.typeNature().isRecord() || typeInfo.isAbstract()) return false;
        if (typeInfo.parentClass() != null && !typeInfo.parentClass().isJavaLangObject()) return false;
        if (!typeInfo.interfacesImplemented().isEmpty()) return false;
        List<MethodInfo> constructors = typeInfo.constructors().stream().filter(c -> !c.isSynthetic()).toList();
        boolean privateNoArg = constructors.size() == 1 && constructors.getFirst().parameters().isEmpty()
                               && constructors.getFirst().access() != null && constructors.getFirst().access().isPrivate()
                               && emptyBody(constructors.getFirst());
        if (!privateNoArg) return false;
        boolean anyMember = false;
        for (FieldInfo f : typeInfo.fields()) {
            if (f.isSynthetic()) continue;
            if (!f.isStatic()) return false;
            anyMember = true;
        }
        for (MethodInfo m : typeInfo.methods()) {
            if (m.isSynthetic()) continue;
            if (!m.isStatic() && !m.isStaticInitializer()) return false;
            anyMember = true;
        }
        return anyMember;
    }

    // ---------------------------------------------------------------- enums

    /** Only constants, without arguments or bodies, and nothing else: a C# enum. */
    private boolean simpleEnum() {
        return simpleEnum(typeInfo);
    }

    static boolean simpleEnum(TypeInfo typeInfo) {
        if (typeInfo.methods().stream().anyMatch(m -> !m.isSynthetic())) return false;
        if (typeInfo.subTypes().stream().anyMatch(st -> !st.isSynthetic())) return false;
        if (typeInfo.constructors().stream().anyMatch(c -> !c.isSynthetic() && !isImplicitDefaultConstructor(c))) {
            return false;
        }
        if (!typeInfo.interfacesImplemented().isEmpty()) return false;
        return typeInfo.fields().stream().filter(f -> !f.isSynthetic()).allMatch(f -> CSharpNames.isEnumConstant(f)
                && (!(f.initializer() instanceof ConstructorCall cc)
                    || cc.parameterExpressions().isEmpty() && cc.anonymousClass() == null));
    }

    private OutputBuilder simpleEnum(Qualification q) {
        String access = typeAccess();
        OutputBuilder out = new OutputBuilderImpl().add(new TextImpl((access == null ? "" : access + " ") + "enum"))
                .add(SpaceEnum.ONE).add(new TextImpl(CSharpNames.type(typeInfo))).add(SpaceEnum.ONE);
        List<OutputBuilder> constants = typeInfo.fields().stream().filter(CSharpNames::isEnumConstant)
                .map(f -> new OutputBuilderImpl().add(new TextImpl(CSharpNames.field(f)))).map(o -> (OutputBuilder) o)
                .toList();
        if (constants.isEmpty()) return out.add(SymbolEnum.LEFT_BRACE).add(SymbolEnum.RIGHT_BRACE);
        return out.add(constants.stream().collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_BRACE,
                SymbolEnum.RIGHT_BRACE, GuideImpl.generatorForBlock())));
    }

    /**
     * {@code public static readonly Color Red = new Color(255, 0, 0) { Name = "Red", Ordinal = 0 };} per constant,
     * then Java's {@code name()}, {@code ordinal()}, {@code values()} and {@code valueOf(String)}.
     */
    private List<OutputBuilder> enumInstances(Qualification q) {
        String self = CSharpNames.type(typeInfo);
        List<FieldInfo> constants = typeInfo.fields().stream().filter(CSharpNames::isEnumConstant).toList();
        List<OutputBuilder> out = new ArrayList<>(constants.stream().map(f -> {
            OutputBuilder b = new OutputBuilderImpl().add(new TextImpl("public static readonly " + self + " "
                                                                       + CSharpNames.field(f)))
                    .add(SymbolEnum.assignment("="));
            if (f.initializer() instanceof ConstructorCall cc) {
                if (cc.anonymousClass() != null) {
                    CSharpContext.message(CSharpPrintMessage.Code.ANONYMOUS_CLASS, f, CSharpContext.describe(f));
                }
                b.add(KeywordImpl.NEW).add(SpaceEnum.ONE).add(new TextImpl(self))
                        .add(CSharpExpressionPrinter.arguments(cc.parameterExpressions(), q));
            } else {
                b.add(KeywordImpl.NEW).add(SpaceEnum.ONE).add(new TextImpl(self)).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
            }
            b.add(SymbolEnum.LEFT_BRACE).add(new TextImpl("Name")).add(SymbolEnum.assignment("="))
                    .add(new TextImpl("\"" + f.name() + "\"")).add(SymbolEnum.COMMA).add(new TextImpl("Ordinal"))
                    .add(SymbolEnum.assignment("=")).add(new TextImpl(Integer.toString(constants.indexOf(f))))
                    .add(SymbolEnum.RIGHT_BRACE);
            return (OutputBuilder) b.add(SymbolEnum.SEMICOLON);
        }).toList());
        out.add(new OutputBuilderImpl().add(new TextImpl("public string Name { get; private init; }")));
        out.add(new OutputBuilderImpl().add(new TextImpl("public int Ordinal { get; private init; }")));
        if (typeInfo.methods().stream().noneMatch(m -> "toString".equals(m.name()) && m.parameters().isEmpty())) {
            out.add(new OutputBuilderImpl().add(new TextImpl("public override string ToString() => Name;")));
        }
        String all = constants.stream().map(CSharpNames::field).collect(Collectors.joining(", "));
        out.add(new OutputBuilderImpl().add(new TextImpl("public static " + self + "[] Values() => new " + self
                                                         + "[] { " + all + " };")));
        out.add(new OutputBuilderImpl().add(new TextImpl("public static " + self + " ValueOf(string name) => Array.Find(Values(), v => v.Name == name) ?? throw new ArgumentException(name);")));
        CSharpContext.using("System");
        return out;
    }

    /**
     * A class implementing Java's Iterable is C#'s IEnumerable: its GetEnumerator walks its {@code iterator()}, so that
     * foreach and LINQ work on it.
     */
    private List<OutputBuilder> enumerable(Qualification q) {
        if (typeInfo.isInterface()) return List.of();
        ParameterizedType iterable = typeInfo.interfacesImplemented().stream()
                .filter(i -> i.typeInfo() != null && "java.lang.Iterable".equals(i.typeInfo().fullyQualifiedName()))
                .findFirst().orElse(null);
        if (iterable == null) return List.of();
        MethodInfo iterator = typeInfo.methods().stream()
                .filter(m -> !m.isStatic() && "iterator".equals(m.name()) && m.parameters().isEmpty()).findFirst()
                .orElse(null);
        if (iterator == null) return List.of();
        String element = iterable.parameters().isEmpty() ? "object" : CSharpTypeName.argument(iterable.parameters().getFirst(), q);
        CSharpContext.using(CSharpBcl.GENERIC);
        CSharpContext.using(CSharpCompat.NAMESPACE);
        return List.of(
                new OutputBuilderImpl().add(new TextImpl("public IEnumerator<" + element + "> GetEnumerator() => "
                                                         + CSharpNames.method(iterator) + "().AsEnumerator();")),
                new OutputBuilderImpl().add(new TextImpl(
                        "System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator() => GetEnumerator();")));
    }

    /** {@code public delegate int ExprentIterator(Exprent exprent);}: see {@link CSharpProgram}. */
    private OutputBuilder delegate(Qualification q) {
        MethodInfo sam = CSharpProgram.invoked(typeInfo);
        String access = typeAccess();
        String typeParameters = typeInfo.typeParameters().isEmpty() ? ""
                : typeInfo.typeParameters().stream().map(CSharpNames::typeParameter)
                        .collect(Collectors.joining(", ", "<", ">"));
        String parameters = sam.parameters().stream()
                .map(p -> CSharpTypeName.of(p.parameterizedType(), q) + " " + CSharpNames.name(p.name()))
                .collect(Collectors.joining(", "));
        StringBuilder sb = new StringBuilder(access == null ? "" : access + " ").append("delegate ")
                .append(CSharpTypeName.of(sam.returnType(), q)).append(' ').append(CSharpNames.type(typeInfo))
                .append(typeParameters).append('(').append(parameters).append(')');
        for (String constraint : CSharpTypeName.constraints(typeInfo.typeParameters(), q)) sb.append(' ').append(constraint);
        return new OutputBuilderImpl().add(new TextImpl(sb.toString())).add(SymbolEnum.SEMICOLON);
    }

    /**
     * {@code public sealed class Lambda(Func<Exprent, int> f) : IExprentIterator { public int ProcessExprent(Exprent
     * exprent) => f(exprent); }}: a lambda of a functional interface that is not a delegate, {@code new IExprentIterator.Lambda(
     * e => 0)}. An interface becomes a delegate only when nothing else implements it, which a file does not know.
     */
    private OutputBuilder lambdaAdapter(Qualification q) {
        MethodInfo sam = CSharpNames.singleAbstractMethod(typeInfo);
        // a method of a generic super-interface, Listener<T>.onEvent(T), is the interface's with its arguments
        java.util.Map<TypeParameter, ParameterizedType> arguments = sam.typeInfo().equals(typeInfo) ? java.util.Map.of()
                : superArguments(typeInfo, sam.typeInfo(), java.util.Map.of());
        if (arguments == null) arguments = java.util.Map.of();
        java.util.Map<TypeParameter, ParameterizedType> map = arguments;
        List<ParameterizedType> samParameters = sam.parameters().stream().map(p -> substitute(p.parameterizedType(), map))
                .toList();
        ParameterizedType samReturn = substitute(sam.returnType(), map);
        List<String> parameterTypes = samParameters.stream().map(p -> CSharpTypeName.argument(p, q)).toList();
        boolean isVoid = samReturn.isVoid();
        List<String> delegateArguments = new ArrayList<>(parameterTypes);
        if (!isVoid) delegateArguments.add(CSharpTypeName.argument(samReturn, q));
        String delegate = (isVoid ? "Action" : "Func")
                          + (delegateArguments.isEmpty() ? "" : "<" + String.join(", ", delegateArguments) + ">");
        String self = CSharpNames.type(typeInfo) + (typeInfo.typeParameters().isEmpty() ? ""
                : typeInfo.typeParameters().stream().map(CSharpNames::typeParameter)
                        .collect(Collectors.joining(", ", "<", ">")));
        List<String> names = sam.parameters().stream().map(p -> CSharpNames.name(p.name())).toList();
        String f = names.contains("f") ? "function" : "f";
        String parameters = IntStream.range(0, names.size())
                .mapToObj(i -> CSharpTypeName.of(samParameters.get(i), q) + " " + names.get(i))
                .collect(Collectors.joining(", "));
        CSharpContext.using("System");
        return new OutputBuilderImpl().add(new TextImpl("public sealed class Lambda(" + delegate + " " + f + ") : " + self
                + " { public " + CSharpTypeName.of(samReturn, q) + " " + CSharpNames.method(sam) + "(" + parameters
                + ") => " + f + "(" + String.join(", ", names) + "); }"));
    }

    /** The type arguments {@code from} gives the type parameters of its super-interface {@code target}. */
    private static java.util.Map<TypeParameter, ParameterizedType> superArguments(
            TypeInfo from, TypeInfo target, java.util.Map<TypeParameter, ParameterizedType> map) {
        for (ParameterizedType i : from.interfacesImplemented()) {
            TypeInfo t = i.typeInfo();
            if (t == null) continue;
            java.util.Map<TypeParameter, ParameterizedType> next = new java.util.HashMap<>();
            for (int k = 0; k < Math.min(t.typeParameters().size(), i.parameters().size()); k++) {
                next.put(t.typeParameters().get(k), substitute(i.parameters().get(k), map));
            }
            if (t.equals(target)) return next;
            java.util.Map<TypeParameter, ParameterizedType> found = superArguments(t, target, next);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * The method of an inherited interface that this default method of interface {@code iface} implements, when C#
     * needs it implemented explicitly: a C# interface's member never implements another's implicitly.
     */
    static MethodInfo interfaceMethodDefaulted(TypeInfo iface, MethodInfo m) {
        if (!iface.isInterface() || m.isStatic() || m.isAbstract() || m.methodBody() == null
            || !m.typeParameters().isEmpty() || m.isConstructor() || CSharpWildcards.mayCapture(m)) return null;
        return m.overrides().stream()
                .filter(o -> o != m && o.typeInfo() != iface && o.typeInfo().isInterface()
                             && CSharpNames.translated(o.typeInfo()) && o.isAbstract() && o.typeParameters().isEmpty())
                .filter(o -> implementedAs(iface, o.typeInfo()) != null)
                .findFirst().orElse(null);
    }

    /** {@code ancestor} as {@code type} extends or implements it, in {@code type}'s type parameters. */
    static ParameterizedType implementedAs(TypeInfo type, TypeInfo ancestor) {
        java.util.Deque<ParameterizedType> todo = new java.util.ArrayDeque<>();
        java.util.Set<TypeInfo> seen = new java.util.HashSet<>();
        if (type.parentClass() != null) todo.add(type.parentClass());
        todo.addAll(type.interfacesImplemented());
        while (!todo.isEmpty()) {
            ParameterizedType pt = todo.poll();
            TypeInfo t = pt.typeInfo();
            if (t == null || !seen.add(t)) continue;
            if (t.equals(ancestor)) return pt;
            java.util.Map<TypeParameter, ParameterizedType> map = typeArguments(pt);
            if (t.parentClass() != null) todo.add(substitute(t.parentClass(), map));
            t.interfacesImplemented().forEach(i -> todo.add(substitute(i, map)));
        }
        return null;
    }

    /** The type parameters of {@code pt}'s type, mapped to its arguments. */
    static java.util.Map<TypeParameter, ParameterizedType> typeArguments(ParameterizedType pt) {
        java.util.Map<TypeParameter, ParameterizedType> map = new java.util.HashMap<>();
        TypeInfo t = pt.typeInfo();
        for (int k = 0; k < Math.min(t.typeParameters().size(), pt.parameters().size()); k++) {
            map.put(t.typeParameters().get(k), pt.parameters().get(k));
        }
        return map;
    }

    static ParameterizedType substitute(ParameterizedType pt, java.util.Map<TypeParameter, ParameterizedType> map) {
        if (map.isEmpty()) return pt;
        if (pt.isTypeParameter() && map.containsKey(pt.typeParameter())) {
            ParameterizedType mapped = map.get(pt.typeParameter());
            return pt.arrays() == 0 ? mapped : mapped.copyWithArrays(mapped.arrays() + pt.arrays());
        }
        if (pt.parameters().isEmpty()) return pt;
        return pt.withParameters(pt.parameters().stream().map(p -> substitute(p, map)).toList());
    }

    /** The record's canonical constructor, compact or not, when written; null when C#'s positional one serves. */
    private MethodInfo canonicalConstructor(List<FieldInfo> components) {
        return typeInfo.constructors().stream().filter(c -> !c.isSynthetic() && c.parameters().size() == components.size())
                .filter(c -> java.util.stream.IntStream.range(0, components.size()).allMatch(i ->
                        c.parameters().get(i).parameterizedType().equals(components.get(i).type())))
                .filter(c -> c.methodBody() != null && c.methodBody().statements().stream().anyMatch(s -> !s.isSynthetic())
                             || c.methodType().isCompactConstructor())
                .findFirst().orElse(null);
    }

    /**
     * A record's accessor that implements an interface's method, {@code T response()}, is the property in C#: the
     * interface's method is implemented explicitly, {@code T IBatchItemResult<T>.Response() => Response;}.
     */
    private List<OutputBuilder> accessorImplementations(List<FieldInfo> components, Qualification q) {
        List<OutputBuilder> out = new ArrayList<>();
        for (ParameterizedType i : typeInfo.interfacesImplemented()) {
            TypeInfo iface = i.typeInfo();
            if (iface == null || !CSharpNames.translated(iface)) continue;
            java.util.Map<TypeParameter, ParameterizedType> map = new java.util.HashMap<>();
            for (int k = 0; k < Math.min(iface.typeParameters().size(), i.parameters().size()); k++) {
                map.put(iface.typeParameters().get(k), i.parameters().get(k));
            }
            for (MethodInfo m : iface.methods()) {
                if (m.isStatic() || !m.isAbstract() || !m.parameters().isEmpty()) continue;
                components.stream().filter(f -> f.name().equals(m.name())).findFirst().ifPresent(f ->
                        out.add(new OutputBuilderImpl().add(new TextImpl(CSharpTypeName.of(substitute(m.returnType(), map), q)
                                + " " + CSharpTypeName.of(i, q) + "." + CSharpNames.method(m) + "() => "
                                + CSharpNames.field(f) + ";"))));
            }
        }
        return out;
    }

    /**
     * Java lets a method implement an interface's method with a narrower return type, {@code Failure
     * withGuardrailClass(…)} for {@code IFailure withGuardrailClass(…)}; C# does not, for an interface. The interface's
     * method is implemented explicitly, and calls the method: {@code IFailure IFailure.WithGuardrailClass(Type g) =>
     * WithGuardrailClass(g);}.
     */
    private List<OutputBuilder> covariantImplementations(Qualification q) {
        List<OutputBuilder> out = new ArrayList<>();
        java.util.Set<TypeInfo> seen = new java.util.HashSet<>();
        for (ParameterizedType i : typeInfo.interfacesImplemented()) {
            covariantImplementations(i, java.util.Map.of(), seen, out, q);
        }
        return out;
    }

    private void covariantImplementations(ParameterizedType i, java.util.Map<TypeParameter, ParameterizedType> outer,
                                          java.util.Set<TypeInfo> seen, List<OutputBuilder> out, Qualification q) {
        TypeInfo iface = i.typeInfo();
        if (iface == null || !CSharpNames.translated(iface) || !seen.add(iface)) return;
        ParameterizedType implemented = substitute(i, outer);
        java.util.Map<TypeParameter, ParameterizedType> map = new java.util.HashMap<>();
        for (int k = 0; k < Math.min(iface.typeParameters().size(), implemented.parameters().size()); k++) {
            map.put(iface.typeParameters().get(k), implemented.parameters().get(k));
        }
        for (MethodInfo m : iface.methods()) {
            if (m.isStatic() || !m.typeParameters().isEmpty() || m.isSynthetic() || CSharpWildcards.mayCapture(m)) continue;
            MethodInfo own = typeInfo.methods().stream()
                    .filter(o -> !o.isStatic() && !o.isSynthetic() && o != m && o.overrides().contains(m))
                    .findFirst().orElse(null);
            if (own == null || own.typeInfo().typeNature().isRecord() && isRecordAccessor(own)) continue;
            String returnType = CSharpTypeName.of(substitute(m.returnType(), map), q);
            if (returnType.equals(CSharpTypeName.of(own.returnType(), q))) continue;
            List<String> names = own.parameters().stream().map(p -> CSharpNames.name(p.name())).toList();
            StringBuilder sb = new StringBuilder(returnType).append(' ').append(CSharpTypeName.of(implemented, q))
                    .append('.').append(CSharpNames.method(m)).append('(');
            for (int k = 0; k < names.size(); k++) {
                if (k > 0) sb.append(", ");
                sb.append(CSharpTypeName.of(substitute(m.parameters().get(k).parameterizedType(), map), q))
                        .append(' ').append(names.get(k));
            }
            sb.append(") => ").append(CSharpNames.method(own)).append('(').append(String.join(", ", names))
                    .append(");");
            out.add(new OutputBuilderImpl().add(new TextImpl(sb.toString())));
        }
        for (ParameterizedType sup : iface.interfacesImplemented()) {
            covariantImplementations(sup, map, seen, out, q);
        }
    }

    /**
     * An abstract Java class may leave an interface's method to its subclasses; C# makes it say so:
     * {@code public abstract Response<List<Embedding>> EmbedAll(List<TextSegment> textSegments);}.
     */
    private List<OutputBuilder> abstractRedeclarations(Qualification q) {
        List<OutputBuilder> out = new ArrayList<>();
        java.util.Set<String> declared = new java.util.HashSet<>();
        for (LeftAbstract left : leftAbstract(typeInfo)) {
            MethodInfo m = left.method();
            String name = CSharpNames.method(m);
            StringBuilder sb = new StringBuilder("public abstract ")
                    .append(CSharpTypeName.of(substitute(m.returnType(), left.typeArguments()), q)).append(' ')
                    .append(name).append('(');
            StringBuilder signature = new StringBuilder(name);
            for (int k = 0; k < m.parameters().size(); k++) {
                if (k > 0) sb.append(", ");
                String type = CSharpTypeName.of(substitute(m.parameters().get(k).parameterizedType(),
                        left.typeArguments()), q);
                sb.append(type).append(' ').append(CSharpNames.name(m.parameters().get(k).name()));
                signature.append(',').append(type);
            }
            if (declared.add(signature.toString())) out.add(new OutputBuilderImpl().add(new TextImpl(sb + ");")));
        }
        return out;
    }

    /** An interface's method an abstract class leaves to its subclasses, and the interface's type arguments. */
    record LeftAbstract(MethodInfo method, java.util.Map<TypeParameter, ParameterizedType> typeArguments) {}

    /** The interface methods abstract class {@code cls} redeclares abstract: no class or default implements them. */
    static List<LeftAbstract> leftAbstract(TypeInfo cls) {
        if (cls.isInterface() || !cls.isAbstract() || !CSharpNames.translated(cls)) return List.of();
        List<MethodInfo> implementations = new ArrayList<>();
        for (TypeInfo t = cls; t != null; t = t.parentClass() == null ? null : t.parentClass().typeInfo()) {
            implementations.addAll(t.methods());
        }
        java.util.Map<TypeInfo, ParameterizedType> interfaces = new java.util.LinkedHashMap<>();
        java.util.Deque<ParameterizedType> todo = new java.util.ArrayDeque<>();
        for (TypeInfo t = cls; t != null; t = t.parentClass() == null ? null : t.parentClass().typeInfo()) {
            ParameterizedType asSeen = t == cls ? null : implementedAs(cls, t);
            java.util.Map<TypeParameter, ParameterizedType> map = asSeen == null ? java.util.Map.of() : typeArguments(asSeen);
            t.interfacesImplemented().forEach(i -> todo.add(substitute(i, map)));
        }
        while (!todo.isEmpty()) {
            ParameterizedType i = todo.poll();
            TypeInfo iface = i.typeInfo();
            if (iface == null || interfaces.containsKey(iface)) continue;
            interfaces.put(iface, i);
            java.util.Map<TypeParameter, ParameterizedType> map = typeArguments(i);
            iface.interfacesImplemented().forEach(sup -> todo.add(substitute(sup, map)));
        }
        interfaces.keySet().forEach(iface -> implementations.addAll(iface.methods().stream()
                .filter(m -> !m.isAbstract() && !m.isStatic()).toList()));
        List<LeftAbstract> left = new ArrayList<>();
        interfaces.forEach((iface, i) -> {
            if (!CSharpNames.translated(iface)) return;
            for (MethodInfo m : iface.methods()) {
                if (m.isStatic() || !m.isAbstract() || !m.typeParameters().isEmpty() || m.isSynthetic()
                    || CSharpWildcards.mayCapture(m)) continue;
                if (OBJECT_METHODS.contains(m.name() + "/" + m.parameters().size())) continue;
                if (implementations.stream().anyMatch(x -> x != m && x.overrides().contains(m))) continue;
                left.add(new LeftAbstract(m, typeArguments(i)));
            }
        });
        return left;
    }

    private static final java.util.Set<String> OBJECT_METHODS = java.util.Set.of("equals/1", "hashCode/0", "toString/0");

    // ---------------------------------------------------------------- helpers

    /** {@code x()} of a record with component {@code x}: the record's property {@code X} takes its place. */
    static boolean isRecordAccessor(MethodInfo m) {
        TypeInfo owner = m.typeInfo();
        return owner.typeNature().isRecord() && !m.isStatic() && m.parameters().isEmpty()
               && owner.fields().stream().anyMatch(f -> !f.isStatic() && f.name().equals(m.name()));
    }

    /**
     * The no-arg, empty-body constructor, the only one of its type, that C# gives the class by default (an enum's
     * constructor is private, and a C# enum has none).
     */
    private static boolean isImplicitDefaultConstructor(MethodInfo c) {
        TypeInfo owner = c.typeInfo();
        return c.parameters().isEmpty() && emptyBody(c)
               && owner.constructors().stream().filter(x -> !x.isSynthetic()).count() == 1
               && (c.access() == null || c.access().isPublic() || owner.typeNature().isEnum()
                   || c.access().isPackage() && !owner.isPubliclyAccessible());
    }

    /** No statements; {@code Block.isEmpty()} is about the empty-block placeholder, not a written {@code { }}. */
    private static boolean emptyBody(MethodInfo m) {
        return m.methodBody() == null || m.methodBody().statements().stream().allMatch(s -> s.isSynthetic());
    }

    private static TypeInfo enclosingType(TypeInfo typeInfo) {
        var cuOrEnclosing = typeInfo.compilationUnitOrEnclosingType();
        return cuOrEnclosing.isRight() ? cuOrEnclosing.getRight() : null;
    }

    /** A class declared in a method body. */
    private static boolean isLocal(TypeInfo typeInfo) {
        return !typeInfo.isPrimaryType() && typeInfo.enclosingMethod() != null;
    }
}
