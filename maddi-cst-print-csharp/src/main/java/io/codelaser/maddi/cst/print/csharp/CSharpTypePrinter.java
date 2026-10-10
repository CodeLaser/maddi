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
        if (enumClass) CSharpContext.message(CSharpPrintMessage.Code.ENUM_AS_CLASS, typeInfo, typeInfo.simpleName());
        TypeInfo enclosing = enclosingType(typeInfo);
        if (enclosing != null && !enclosing.typeParameters().isEmpty() && !CSharpNames.hoisted(typeInfo)) {
            CSharpContext.message(CSharpPrintMessage.Code.NESTED_IN_GENERIC, typeInfo, typeInfo.fullyQualifiedName());
        }

        OutputBuilder out = new OutputBuilderImpl();
        if (doTypeDeclaration) {
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
                    : declaredTypeParameters.stream().map(tp -> CSharpNames.name(tp.simpleName()))
                            .collect(Collectors.joining(", ", "<", ">"));
            out.add(new TextImpl(modifiers + keyword)).add(SpaceEnum.ONE)
                    .add(new TextImpl(CSharpNames.type(typeInfo) + typeParameters));
            if (record) {
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
        typeInfo.fields().stream()
                .filter(f -> !f.isSynthetic() && !CSharpNames.isEnumConstant(f) && !components.contains(f))
                .forEach(f -> members.add(fieldPrinterFactory.create(f, formatter2).print(q, false)));
        typeInfo.constructors().stream()
                .filter(c -> !c.isSynthetic() && !isImplicitDefaultConstructor(c) && !(staticClass && c.parameters().isEmpty()))
                .filter(c -> {
                    if (record && c.parameters().size() == components.size()) {
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
                    : outer.typeParameters().stream().map(tp -> CSharpNames.name(tp.simpleName()))
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

    /** The superclass, unless implicit (Object, Enum, Record), and the interfaces. */
    private List<String> superTypes(Qualification q) {
        List<String> supers = new ArrayList<>();
        ParameterizedType parent = typeInfo.parentClass();
        if (parent != null && !parent.isJavaLangObject() && parent.typeInfo() != null
            && !"java.lang.Enum".equals(parent.typeInfo().fullyQualifiedName())
            && !"java.lang.Record".equals(parent.typeInfo().fullyQualifiedName())) {
            supers.add(CSharpTypeName.of(parent, q));
        }
        typeInfo.interfacesImplemented().forEach(i -> supers.add(CSharpTypeName.of(i, q)));
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

    /** {@code public delegate int ExprentIterator(Exprent exprent);}: see {@link CSharpProgram}. */
    private OutputBuilder delegate(Qualification q) {
        MethodInfo sam = CSharpProgram.invoked(typeInfo);
        String access = typeAccess();
        String typeParameters = typeInfo.typeParameters().isEmpty() ? ""
                : typeInfo.typeParameters().stream().map(tp -> CSharpNames.name(tp.simpleName()))
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
        List<String> parameterTypes = sam.parameters().stream().map(p -> CSharpTypeName.argument(p.parameterizedType(), q))
                .toList();
        boolean isVoid = sam.returnType().isVoid();
        List<String> delegateArguments = new ArrayList<>(parameterTypes);
        if (!isVoid) delegateArguments.add(CSharpTypeName.argument(sam.returnType(), q));
        String delegate = (isVoid ? "Action" : "Func")
                          + (delegateArguments.isEmpty() ? "" : "<" + String.join(", ", delegateArguments) + ">");
        String self = CSharpNames.type(typeInfo) + (typeInfo.typeParameters().isEmpty() ? ""
                : typeInfo.typeParameters().stream().map(tp -> CSharpNames.name(tp.simpleName()))
                        .collect(Collectors.joining(", ", "<", ">")));
        List<String> names = sam.parameters().stream().map(p -> CSharpNames.name(p.name())).toList();
        String f = names.contains("f") ? "function" : "f";
        String parameters = IntStream.range(0, names.size())
                .mapToObj(i -> CSharpTypeName.of(sam.parameters().get(i).parameterizedType(), q) + " " + names.get(i))
                .collect(Collectors.joining(", "));
        CSharpContext.using("System");
        return new OutputBuilderImpl().add(new TextImpl("public sealed class Lambda(" + delegate + " " + f + ") : " + self
                + " { public " + CSharpTypeName.of(sam.returnType(), q) + " " + CSharpNames.method(sam) + "(" + parameters
                + ") => " + f + "(" + String.join(", ", names) + "); }"));
    }

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
