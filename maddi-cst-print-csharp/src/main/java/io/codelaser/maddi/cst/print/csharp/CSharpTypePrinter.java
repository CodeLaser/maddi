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
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
                CSharpContext.message(CSharpPrintCode.ANNOTATION_TYPE, typeInfo, typeInfo.simpleName());
                return new OutputBuilderImpl();
            }
            if (typeInfo.typeNature().isEnum() && CSharpNames.simpleEnum(typeInfo)) return simpleEnum(importData.insideType());
            if (CSharpContext.program().delegate(typeInfo)) return delegate(importData.insideType());
            OutputBuilder type = printType(importData, doTypeDeclaration, methodPrinterFactory, fieldPrinterFactory,
                    enclosedTypePrinterFactory);
            if (doTypeDeclaration && CSharpNames.hasCompanion(typeInfo)) {
                type.add(SpaceEnum.NEWLINE).add(companion(importData.insideType(), methodPrinterFactory,
                        fieldPrinterFactory));
            }
            return type;
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
        if (enumClass) CSharpContext.message(CSharpPrintCode.ENUM_AS_CLASS, typeInfo, typeInfo.simpleName());
        TypeInfo enclosing = CstQueries.enclosing(typeInfo);
        if (enclosing != null && !enclosing.typeParameters().isEmpty() && !CSharpNames.hoisted(typeInfo)) {
            CSharpContext.message(CSharpPrintCode.NESTED_IN_GENERIC, typeInfo, typeInfo.fullyQualifiedName());
        }

        // what a library gives the type beyond its annotations: JAXB's binding (LibraryMapping#additions)
        LibraryMapping.Additions additions = self == null && !enumClass && !typeInfo.isInterface()
                ? CSharpContext.libraries().additions(typeInfo, q) : null;
        if (additions == null) additions = LibraryMapping.Additions.NONE;
        additions.namespaces().forEach(CSharpContext::using);
        OutputBuilder out = new OutputBuilderImpl();
        if (doTypeDeclaration) {
            out.add(CSharpAttributes.uses(typeInfo.annotations(), LibraryMapping.Target.TYPE, q, true));
            for (String attribute : additions.attributes()) {
                out.add(SymbolEnum.LEFT_BRACKET).add(new TextImpl(attribute)).add(SymbolEnum.RIGHT_BRACKET)
                        .add(SpaceEnum.NEWLINE);
            }
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
            String typeParameters = CSharpNames.typeParameters(declaredTypeParameters);
            out.add(new TextImpl(modifiers + keyword)).add(SpaceEnum.ONE)
                    .add(new TextImpl(CSharpNames.type(typeInfo) + typeParameters));
            if (record && canonical == null) {
                FieldPrinterFactory componentPrinter = CSharpFieldPrinter::new;
                out.add(components.stream().map(f -> componentPrinter.create(f, formatter2).print(q, true))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                                SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration())));
            }
            List<String> supers = new ArrayList<>(superTypes(q));
            // the wildcard interface of a self-bounded type (CSharpWildcardInterfaces)
            if (CSharpWildcardInterfaces.has(typeInfo)) supers.add(CSharpWildcardInterfaces.selfReference(typeInfo));
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
        // an inner class's enclosing instance (CSharpInner)
        boolean outer = CSharpInner.withOuter(typeInfo);
        boolean inConstructor = self == null && CSharpInner.initializedInConstructor(typeInfo);
        if (outer) {
            members.add(new OutputBuilderImpl().add(new TextImpl("private readonly " + CSharpInner.outerTypeName(typeInfo)
                                                                 + " outer;")));
        }
        if (canonical != null) {
            components.forEach(f -> members.add(new OutputBuilderImpl().add(new TextImpl("public "
                    + CSharpTypeName.of(f.type(), q) + " " + CSharpNames.field(f) + " { get; }"))));
        }
        typeInfo.fields().stream()
                .filter(f -> !f.isSynthetic() && !CSharpNames.isEnumConstant(f) && !components.contains(f)
                             && !CSharpNames.inCompanion(f))
                .forEach(f -> {
                    CSharpProperties.Property property = CSharpContext.program().property(f);
                    members.add(property != null ? CSharpFieldPrinter.property(property, q)
                            : fieldPrinterFactory.create(f, formatter2).print(q, false));
                });
        for (LibraryMapping.AddedMember added : additions.members()) {
            OutputBuilder b = new OutputBuilderImpl();
            for (String attribute : added.attributes()) {
                b.add(SymbolEnum.LEFT_BRACKET).add(new TextImpl(attribute)).add(SymbolEnum.RIGHT_BRACKET)
                        .add(SpaceEnum.NEWLINE);
            }
            members.add(b.add(new TextImpl(added.declaration())));
        }
        typeInfo.constructors().stream()
                .filter(c -> !c.isSynthetic() && (inConstructor || !isImplicitDefaultConstructor(c))
                             && !(staticClass && c.parameters().isEmpty())
                             && !CSharpNames.inCompanion(c))
                .filter(c -> {
                    if (record && c != canonical && c.parameters().size() == components.size()) {
                        CSharpContext.message(CSharpPrintCode.RECORD_CONSTRUCTOR, c, CSharpContext.describe(c));
                        return false;
                    }
                    return true;
                })
                .forEach(c -> members.add(methodPrinterFactory.create(typeInfo, c, formatter2).print(q)));
        if (inConstructor && typeInfo.constructors().stream().allMatch(MethodInfo::isSynthetic)) {
            // Java's default constructor, which takes the enclosing instance, and initializes the fields
            ParameterizedType parent = typeInfo.parentClass();
            String base = outer && parent != null && CSharpInner.withOuter(parent.typeInfo()) ? " : base(outer)" : "";
            List<OutputBuilder> body = CSharpInner.prologue(typeInfo, outer, q);
            String access = outer ? "internal " : typeInfo.isAbstract() ? "protected " : "public ";
            members.add(new OutputBuilderImpl().add(new TextImpl(access + CSharpNames.type(typeInfo) + "("
                    + (outer ? CSharpInner.outerTypeName(typeInfo) + " outer" : "") + ")" + base)).add(SpaceEnum.ONE)
                    .add(CSharpStatementPrinter.braces(body)));
        }
        typeInfo.methods().stream()
                .filter(m -> !m.isSynthetic() && !(record && CstQueries.isRecordAccessor(m)) && !CSharpNames.inCompanion(m)
                             && CSharpContext.program().accessorOf(m) == null)
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
        if (CSharpWildcardInterfaces.has(typeInfo)) members.addAll(CSharpWildcardInterfaces.bridges(typeInfo, q));
        anonymous.forEach(h -> members.add(new CSharpTypePrinter(h.type(), formatter2).print(importData, true)));

        List<OutputBuilder> nonEmpty = members.stream().filter(m -> !m.isEmpty()).toList();
        if (record && nonEmpty.isEmpty() && doTypeDeclaration) return out.add(SymbolEnum.SEMICOLON);
        out.add(SpaceEnum.ONE).add(CSharpStatementPrinter.braces(nonEmpty));
        if (doTypeDeclaration && CSharpWildcardInterfaces.has(typeInfo)) {
            out.add(SpaceEnum.NEWLINE).add(CSharpWildcardInterfaces.declaration(typeInfo, typeAccess(), q));
        }
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
            ParameterizedType superArgument = h.superArguments().get(i);
            // Holder<int>'s T is int, not int?
            parameters.add((h.typeArgument(i) ? CSharpTypeName.argument(superArgument, q)
                    : CSharpTypeName.of(superArgument, q)) + " p" + i);
        }
        if (h.outer()) {
            String outerType = CSharpInner.outerTypeName(typeInfo);
            members.add(new OutputBuilderImpl().add(new TextImpl("private readonly " + outerType + " outer;")));
            parameters.add(outerType + " outer");
            body.add(CSharpInner.outerAssignment());
        }
        for (CSharpAnonymous.Capture c : h.captures()) {
            String type = CSharpTypeName.of(c.type(), q);
            String name = CSharpNames.name(c.name());
            members.add(new OutputBuilderImpl().add(new TextImpl("private readonly " + type + " " + name + ";")));
            parameters.add(type + " " + name);
            body.add(new OutputBuilderImpl().add(new TextImpl("this." + name + " = " + name + ";")));
        }
        body.addAll(CSharpInner.fieldInitializers(typeInfo, q));
        if (parameters.isEmpty() && body.isEmpty()) return members;
        String baseCall = h.superArguments().isEmpty() ? "" : " : base(" + IntStream
                .range(0, h.superArguments().size()).mapToObj(i -> "p" + i).collect(Collectors.joining(", ")) + ")";
        members.add(new OutputBuilderImpl().add(new TextImpl("internal " + h.name() + "(" + String.join(", ", parameters)
                + ")" + baseCall)).add(SpaceEnum.ONE).add(CSharpStatementPrinter.braces(body)));
        return members;
    }

    private String typeAccess() {
        if (typeInfo.isAnonymous() && CSharpContext.hoisted(typeInfo) != null) return "private";
        if (isLocal(typeInfo)) return "private"; // lifted into the enclosing type
        // a top-level type is public or internal
        // a base of a public type is public too (CSharpProgram#exposedBase)
        if (typeInfo.isPrimaryType()) {
            return typeInfo.typeModifiers().stream().anyMatch(TypeModifier::isPublic)
                   || CSharpContext.program().exposedBase(typeInfo) ? "public" : "internal";
        }
        // a hoisted type is in the namespace, where C# has no private or protected
        if (CSharpNames.hoisted(typeInfo)) {
            return "public".equals(CSharpModifiers.access(typeInfo, CstQueries.enclosing(typeInfo))) ? "public" : "internal";
        }
        return CSharpModifiers.access(typeInfo, CstQueries.enclosing(typeInfo));
    }

    /** The superclass, unless implicit (Object, Enum, Record), and the interfaces. */
    private List<String> superTypes(Qualification q) {
        List<String> supers = new ArrayList<>();
        ParameterizedType parent = typeInfo.parentClass();
        if (parent != null && !parent.isJavaLangObject() && parent.typeInfo() != null
            && !"java.lang.Enum".equals(parent.typeInfo().fullyQualifiedName())
            && !"java.lang.Record".equals(parent.typeInfo().fullyQualifiedName())) {
            if (CSharpBcl.LIST_BASES.contains(parent.typeInfo().fullyQualifiedName())) {
                // C#'s List has no virtual methods: the compatibility library's JavaArrayList, whose Java methods are
                CSharpContext.using(CSharpCompat.NAMESPACE);
                if (typeInfo.methods().stream().anyMatch(m -> m.overrides().stream()
                        .anyMatch(o -> CSharpBcl.LIST_BASES.contains(o.typeInfo().fullyQualifiedName())))) {
                    CSharpContext.message(CSharpPrintCode.LIST_SUBCLASS, typeInfo, typeInfo.simpleName());
                }
                supers.add("JavaArrayList<" + parent.parameters().stream().map(p -> CSharpTypeName.argument(p, q))
                        .collect(Collectors.joining(", ")) + ">");
            } else {
                supers.add(CSharpTypeName.of(parent, q));
            }
        }
        // Java's marker interfaces have no C# counterpart: cloning is MemberwiseClone, serialization is opt-in
        typeInfo.interfacesImplemented().stream()
                .filter(i -> i.typeInfo() == null || !CSharpBcl.MARKERS.contains(i.typeInfo().fullyQualifiedName()))
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

    private OutputBuilder simpleEnum(Qualification q) {
        String access = typeAccess();
        OutputBuilder out = new OutputBuilderImpl().add(new TextImpl((access == null ? "" : access + " ") + "enum"))
                .add(SpaceEnum.ONE).add(new TextImpl(CSharpNames.type(typeInfo))).add(SpaceEnum.ONE);
        List<OutputBuilder> constants = typeInfo.fields().stream().filter(CSharpNames::isEnumConstant)
                .map(f -> new OutputBuilderImpl().add(CSharpFieldPrinter.libraryAttributes(f, SpaceEnum.ONE))
                        .add(new TextImpl(CSharpNames.field(f)))).map(o -> (OutputBuilder) o)
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
                    CSharpContext.message(CSharpPrintCode.ANONYMOUS_CLASS, f, CSharpContext.describe(f));
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
        // set by each constant's initializer, which C#'s nullable analysis does not see from the constructor (CS8618)
        String notNull = CSharpContext.program().facts().nullabilityAnalysed() ? " = null!;" : "";
        out.add(new OutputBuilderImpl().add(new TextImpl("public string Name { get; private init; }" + notNull)));
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
        ParameterizedType iterable = typeInfo.interfacesImplemented().stream()
                .filter(i -> i.typeInfo() != null && "java.lang.Iterable".equals(i.typeInfo().fullyQualifiedName()))
                .findFirst().orElse(null);
        List<OutputBuilder> out = new ArrayList<>();
        // an abstract class that leaves iterator() to its subclasses declares it, for the GetEnumerator of its own or
        // its interface's
        boolean abstractIterator = CSharpHierarchy.needsAbstractIterator(typeInfo);
        if (abstractIterator) {
            ParameterizedType seen = CSharpHierarchy.iterableAsSeen(typeInfo);
            String element = seen == null || seen.parameters().isEmpty() ? "object"
                    : CSharpTypeName.argument(seen.parameters().getFirst(), q);
            CSharpContext.using(CSharpCompat.NAMESPACE);
            out.add(new OutputBuilderImpl().add(new TextImpl("public abstract IJavaIterator<" + element + "> Iterator();")));
        }
        if (iterable == null) return out;
        MethodInfo iterator = typeInfo.methods().stream()
                .filter(m -> !m.isStatic() && "iterator".equals(m.name()) && m.parameters().isEmpty()).findFirst()
                .orElse(null);
        String element = iterable.parameters().isEmpty() ? "object" : CSharpTypeName.argument(iterable.parameters().getFirst(), q);
        CSharpContext.using(CSharpBcl.GENERIC);
        CSharpContext.using(CSharpCompat.NAMESPACE);
        if (typeInfo.isInterface()) {
            // an interface's implementations are enumerable through its default implementations, over its Iterator()
            String name = iterator == null ? "Iterator" : CSharpNames.method(iterator);
            String call = name + "()" + (iterator == null ? "" : CSharpNullability.resultForgiven(iterator));
            if (iterator == null) {
                out.add(new OutputBuilderImpl().add(new TextImpl("IJavaIterator<" + element + "> Iterator();")));
            }
            out.add(new OutputBuilderImpl().add(new TextImpl("IEnumerator<" + element + "> IEnumerable<" + element
                                                             + ">.GetEnumerator() => " + call + ".AsEnumerator();")));
            out.add(new OutputBuilderImpl().add(new TextImpl("System.Collections.IEnumerator System.Collections.IEnumerable"
                                                             + ".GetEnumerator() => " + call + ".AsEnumerator();")));
            return out;
        }
        if (iterator == null && !abstractIterator) return out;
        String iteratorCall = iterator == null ? "Iterator()"
                : CSharpNames.method(iterator) + "()" + CSharpNullability.resultForgiven(iterator);
        out.addAll(List.of(
                new OutputBuilderImpl().add(new TextImpl("public IEnumerator<" + element + "> GetEnumerator() => "
                                                         + iteratorCall + ".AsEnumerator();")),
                new OutputBuilderImpl().add(new TextImpl(
                        "System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator() => GetEnumerator();"))));
        return out;
    }

    /** {@code public delegate int ExprentIterator(Exprent exprent);}: see {@link CSharpProgram}. */
    private OutputBuilder delegate(Qualification q) {
        MethodInfo sam = CSharpProgram.invoked(typeInfo);
        String access = typeAccess();
        String typeParameters = CSharpNames.typeParameters(typeInfo.typeParameters());
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
        ParameterizedType asSeen = sam.typeInfo().equals(typeInfo) ? null
                : CSharpHierarchy.implementedAs(typeInfo, sam.typeInfo());
        Map<TypeParameter, ParameterizedType> map = asSeen == null ? Map.of() : CSharpHierarchy.typeArguments(asSeen);
        List<ParameterizedType> samParameters = sam.parameters().stream().map(p -> CSharpHierarchy.substitute(p.parameterizedType(), map))
                .toList();
        ParameterizedType samReturn = CSharpHierarchy.substitute(sam.returnType(), map);
        List<String> parameterTypes = samParameters.stream().map(p -> CSharpTypeName.argument(p, q)).toList();
        boolean isVoid = samReturn.isVoid();
        List<String> delegateArguments = new ArrayList<>(parameterTypes);
        if (!isVoid) delegateArguments.add(CSharpTypeName.argument(samReturn, q));
        String delegate = (isVoid ? "Action" : "Func")
                          + (delegateArguments.isEmpty() ? "" : "<" + String.join(", ", delegateArguments) + ">");
        String self = CSharpNames.type(typeInfo) + CSharpNames.typeParameters(typeInfo.typeParameters());
        List<String> names = sam.parameters().stream().map(p -> CSharpNames.name(p.name())).toList();
        String f = names.contains("f") ? "function" : "f";
        String parameters = IntStream.range(0, names.size())
                .mapToObj(i -> CSharpTypeName.of(samParameters.get(i), q) + " " + names.get(i))
                .collect(Collectors.joining(", "));
        CSharpContext.using("System");
        // a method that is a property is implemented as one
        String property = CSharpContext.program().computedProperty(sam);
        String member = property != null ? property : CSharpNames.method(sam) + "(" + parameters + ")";
        return new OutputBuilderImpl().add(new TextImpl("public sealed class Lambda(" + delegate + " " + f + ") : " + self
                + " { public " + CSharpTypeName.of(samReturn, q) + " " + member + " => " + f + "("
                + String.join(", ", names) + "); }"));
    }

    /** The record's canonical constructor, compact or not, when written; null when C#'s positional one serves. */
    private MethodInfo canonicalConstructor(List<FieldInfo> components) {
        return typeInfo.constructors().stream().filter(c -> !c.isSynthetic() && c.parameters().size() == components.size())
                .filter(c -> IntStream.range(0, components.size()).allMatch(i ->
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
            Map<TypeParameter, ParameterizedType> map = CSharpHierarchy.typeArguments(i);
            for (MethodInfo m : iface.methods()) {
                if (m.isStatic() || !m.isAbstract() || !m.parameters().isEmpty()) continue;
                components.stream().filter(f -> f.name().equals(m.name())).findFirst().ifPresent(f ->
                        out.add(new OutputBuilderImpl().add(new TextImpl(CSharpTypeName.of(CSharpHierarchy.substitute(m.returnType(), map), q)
                                + " " + CSharpTypeName.of(i, q) + "." + CSharpNames.method(m) + "() => "
                                + CSharpNames.field(f) + ";"))));
            }
        }
        return out;
    }

    /**
     * The static members of a generic type, in a non-generic static class of the same name beside it
     * ({@link CSharpNames#hasCompanion}): {@code public static class Response { public static Response<T> From<T>(T
     * content) => …; }}, which {@code Response.From(x)} calls, as Java does.
     */
    private OutputBuilder companion(Qualification q, MethodPrinterFactory methodPrinterFactory,
                                    FieldPrinterFactory fieldPrinterFactory) {
        TypeInfo previous = CSharpContext.companion();
        CSharpContext.companion(typeInfo);
        try {
            List<OutputBuilder> members = new ArrayList<>();
            typeInfo.fields().stream().filter(CSharpNames::inCompanion)
                    .forEach(f -> members.add(fieldPrinterFactory.create(f, formatter2).print(q, false)));
            Stream.concat(typeInfo.constructors().stream(), typeInfo.methods().stream())
                    .filter(CSharpNames::inCompanion)
                    .forEach(m -> members.add(methodPrinterFactory.create(typeInfo, m, formatter2).print(q)));
            String access = typeAccess();
            return new OutputBuilderImpl()
                    .add(new TextImpl((access == null ? "" : access + " ") + "static class " + CSharpNames.type(typeInfo)))
                    .add(SpaceEnum.ONE).add(CSharpStatementPrinter.braces(members.stream().filter(m -> !m.isEmpty()).toList()));
        } finally {
            CSharpContext.companion(previous);
        }
    }

    /**
     * Java lets a method implement an interface's method with a narrower return type, {@code Failure
     * withGuardrailClass(…)} for {@code IFailure withGuardrailClass(…)}; C# does not, for an interface. The interface's
     * method is implemented explicitly, and calls the method: {@code IFailure IFailure.WithGuardrailClass(Type g) =>
     * WithGuardrailClass(g);}.
     */
    private List<OutputBuilder> covariantImplementations(Qualification q) {
        List<OutputBuilder> out = new ArrayList<>();
        Set<TypeInfo> seen = new HashSet<>();
        for (ParameterizedType i : typeInfo.interfacesImplemented()) {
            covariantImplementations(i, Map.of(), seen, out, q);
        }
        return out;
    }

    private void covariantImplementations(ParameterizedType i, Map<TypeParameter, ParameterizedType> outer,
                                          Set<TypeInfo> seen, List<OutputBuilder> out, Qualification q) {
        TypeInfo iface = i.typeInfo();
        if (iface == null || !CSharpNames.translated(iface) || !seen.add(iface)) return;
        ParameterizedType implemented = CSharpHierarchy.substitute(i, outer);
        Map<TypeParameter, ParameterizedType> map = CSharpHierarchy.typeArguments(implemented);
        for (MethodInfo m : iface.methods()) {
            if (m.isStatic() || !m.typeParameters().isEmpty() || m.isSynthetic() || CSharpWildcards.mayCapture(m)) continue;
            MethodInfo own = typeInfo.methods().stream()
                    .filter(o -> !o.isStatic() && !o.isSynthetic() && o != m && o.overrides().contains(m))
                    .findFirst().orElse(null);
            if (own == null || own.typeInfo().typeNature().isRecord() && CstQueries.isRecordAccessor(own)) continue;
            String returnType = CSharpTypeName.of(CSharpHierarchy.substitute(m.returnType(), map), q);
            if (returnType.equals(CSharpTypeName.of(own.returnType(), q))) continue;
            List<String> names = own.parameters().stream().map(p -> CSharpNames.name(p.name())).toList();
            StringBuilder sb = new StringBuilder(returnType).append(' ').append(CSharpTypeName.of(implemented, q))
                    .append('.').append(CSharpNames.method(m)).append('(');
            for (int k = 0; k < names.size(); k++) {
                if (k > 0) sb.append(", ");
                sb.append(CSharpTypeName.of(CSharpHierarchy.substitute(m.parameters().get(k).parameterizedType(), map), q))
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
        Set<String> declared = new HashSet<>();
        for (CSharpHierarchy.LeftAbstract left : CSharpHierarchy.leftAbstract(typeInfo)) {
            MethodInfo m = left.method();
            String property = CSharpContext.program().computedProperty(m);
            if (property != null) {
                // the interface's property, left to the subclasses
                if (declared.add(property)) {
                    out.add(new OutputBuilderImpl().add(new TextImpl("public abstract "
                            + CSharpTypeName.of(CSharpHierarchy.substitute(m.returnType(), left.typeArguments()), q) + " " + property
                            + " { get; }")));
                }
                continue;
            }
            String name = CSharpNames.method(m);
            StringBuilder sb = new StringBuilder("public abstract ")
                    .append(CSharpTypeName.of(CSharpHierarchy.substitute(m.returnType(), left.typeArguments()), q)).append(' ')
                    .append(name).append('(');
            StringBuilder signature = new StringBuilder(name);
            for (int k = 0; k < m.parameters().size(); k++) {
                if (k > 0) sb.append(", ");
                String type = CSharpTypeName.of(CSharpHierarchy.substitute(m.parameters().get(k).parameterizedType(),
                        left.typeArguments()), q);
                sb.append(type).append(' ').append(CSharpNames.name(m.parameters().get(k).name()));
                signature.append(',').append(type);
            }
            if (declared.add(signature.toString())) out.add(new OutputBuilderImpl().add(new TextImpl(sb + ");")));
        }
        return out;
    }

    // ---------------------------------------------------------------- helpers

    /**
     * The no-arg, empty-body constructor, the only one of its type, that C# gives the class by default (an enum's
     * constructor is private, and a C# enum has none).
     */
    static boolean isImplicitDefaultConstructor(MethodInfo c) {
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

    /** A class declared in a method body. */
    private static boolean isLocal(TypeInfo typeInfo) {
        return !typeInfo.isPrimaryType() && typeInfo.enclosingMethod() != null;
    }
}
