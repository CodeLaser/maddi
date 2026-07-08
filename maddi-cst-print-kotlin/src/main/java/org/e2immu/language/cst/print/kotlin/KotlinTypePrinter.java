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

package org.e2immu.language.cst.print.kotlin;

import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.info.CompilationUnitPrinterImpl;
import org.e2immu.language.cst.impl.info.TypeModifierEnum;
import org.e2immu.language.cst.impl.output.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Prints a {@link TypeInfo} as a Kotlin type declaration, mirroring the Java {@code TypePrinterImpl} and its
 * pluggable-printer seam: the method/field/enclosed-type printers are supplied by factories (defaulting to the
 * Kotlin printers), so callers can substitute their own — exactly as in the Java case.
 *
 * <p>Best-effort reconstruction (requires the analyzer's prepwork phase, which populates {@code getSetField}):
 * <ul>
 *   <li>getter/setter methods (a non-empty {@code getSetField}) are collapsed away — the backing field prints as
 *       a `val`/`var` property, avoiding the Kotlin platform-declaration clash of a property + its `getX()`;</li>
 *   <li>a single constructor whose parameters all name a field becomes the <b>primary constructor</b>
 *       (`class Foo(val id: Int)`); those fields and that constructor are then omitted from the body.</li>
 * </ul>
 * The class nature maps to `class`/`interface`/`enum class`; supertypes use Kotlin's `:` (a class parent as a
 * constructor call `Super()`). `public`/`final` are omitted (Kotlin defaults); a non-final class is `open`.
 */
public record KotlinTypePrinter(TypeInfo typeInfo, boolean formatter2) implements TypePrinter {

    @Override
    public List<TypeModifier> minimalModifiers(TypeInfo typeInfo) {
        return List.of();
    }

    @Override
    public OutputBuilder print(ImportComputer importComputer, Qualification qualification, boolean doTypeDeclaration) {
        CompilationUnitPrinterImpl printer = new CompilationUnitPrinterImpl(typeInfo.compilationUnit(), formatter2);
        CompilationUnitPrinter.ImportData importData = printer.computeImportData(importComputer, qualification);
        return print(importData, doTypeDeclaration);
    }

    @Override
    public OutputBuilder print(CompilationUnitPrinter.ImportData importData, boolean doTypeDeclaration) {
        return print(importData, doTypeDeclaration, KotlinMethodPrinter::new, KotlinFieldPrinter::new, KotlinTypePrinter::new);
    }

    @Override
    public OutputBuilder print(CompilationUnitPrinter.ImportData importData, boolean doTypeDeclaration,
                               MethodPrinterFactory methodPrinterFactory, FieldPrinterFactory fieldPrinterFactory,
                               EnclosedTypePrinterFactory enclosedTypePrinterFactory) {
        Qualification insideType = importData.insideType();

        List<MethodInfo> constructors = typeInfo.constructors().stream().filter(c -> !c.isSynthetic()).toList();
        Map<String, FieldInfo> fieldByName = typeInfo.fields().stream()
                .collect(Collectors.toMap(FieldInfo::name, Function.identity(), (a, b) -> a));

        // a single constructor whose parameters each name a field => the primary constructor
        MethodInfo primary = null;
        if (constructors.size() == 1) {
            MethodInfo c = constructors.getFirst();
            if (!c.parameters().isEmpty() && c.parameters().stream().allMatch(p -> fieldByName.containsKey(p.name()))) {
                primary = c;
            }
        }
        Set<String> headerFields = primary == null ? Set.of()
                : primary.parameters().stream().map(ParameterInfo::name).collect(Collectors.toSet());
        MethodInfo primaryFinal = primary;

        OutputBuilder out = new OutputBuilderImpl();
        if (doTypeDeclaration) {
            KotlinModifiers.visibility(typeInfo.access()).ifPresent(v -> out.add(v).add(SpaceEnum.ONE));
            Set<TypeModifier> mods = typeInfo.typeModifiers();
            if (typeInfo.typeNature().isClass()) {
                if (mods.contains(TypeModifierEnum.ABSTRACT)) out.add(KeywordImpl.ABSTRACT).add(SpaceEnum.ONE);
                else if (mods.contains(TypeModifierEnum.SEALED)) out.add(KeywordImpl.SEALED).add(SpaceEnum.ONE);
                else if (!mods.contains(TypeModifierEnum.FINAL)) out.add(KotlinKeyword.OPEN).add(SpaceEnum.ONE);
            }
            if (typeInfo.typeNature().isEnum()) {
                out.add(KeywordImpl.ENUM).add(SpaceEnum.ONE).add(KeywordImpl.CLASS);
            } else if (typeInfo.typeNature().isInterface()) {
                out.add(KeywordImpl.INTERFACE);
            } else {
                out.add(KeywordImpl.CLASS);
            }
            out.add(SpaceEnum.ONE).add(new TextImpl(typeInfo.simpleName()));

            if (!typeInfo.typeParameters().isEmpty()) {
                out.add(SymbolEnum.LEFT_ANGLE_BRACKET);
                out.add(typeInfo.typeParameters().stream()
                        .map(tp -> new OutputBuilderImpl().add(new TextImpl(tp.simpleName())))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
                out.add(SymbolEnum.RIGHT_ANGLE_BRACKET);
            }
            if (primary != null) {
                out.add(primary.parameters().stream()
                        .map(p -> new OutputBuilderImpl()
                                .add(fieldByName.get(p.name()).isFinal() ? KotlinKeyword.VAL : KotlinKeyword.VAR)
                                .add(SpaceEnum.ONE).add(new TextImpl(p.name())).add(SymbolEnum.COLON_LABEL)
                                .add(new TextImpl(KotlinTypeName.of(p.parameterizedType()))))
                        .collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                                SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.generatorForParameterDeclaration())));
            }
            List<OutputBuilder> supers = superTypes();
            if (!supers.isEmpty()) {
                out.add(SpaceEnum.ONE).add(SymbolEnum.COLON).add(SpaceEnum.ONE)
                        .add(supers.stream().collect(OutputBuilderImpl.joining(SymbolEnum.COMMA)));
            }
        }

        Stream<OutputBuilder> propertyStream = typeInfo.fields().stream()
                .filter(f -> !f.isSynthetic() && !headerFields.contains(f.name()))
                .map(f -> fieldPrinterFactory.create(f, formatter2).print(insideType, false));
        Stream<OutputBuilder> constructorStream = constructors.stream()
                .filter(c -> c != primaryFinal)
                .map(c -> methodPrinterFactory.create(typeInfo, c, formatter2).print(insideType));
        Stream<OutputBuilder> methodStream = typeInfo.methods().stream()
                .filter(m -> !m.isSynthetic() && !isAccessor(m))
                .map(m -> methodPrinterFactory.create(typeInfo, m, formatter2).print(insideType));
        Stream<OutputBuilder> subTypeStream = typeInfo.subTypes().stream()
                .filter(st -> !st.isSynthetic())
                .map(st -> enclosedTypePrinterFactory.create(st, formatter2).print(importData, true));

        List<OutputBuilder> members = Stream.of(propertyStream, constructorStream, methodStream, subTypeStream)
                .flatMap(Function.identity()).toList();
        if (!members.isEmpty()) {
            out.add(SpaceEnum.ONE).add(members.stream().collect(OutputBuilderImpl.joining(SpaceEnum.NONE,
                    SymbolEnum.LEFT_BRACE, SymbolEnum.RIGHT_BRACE, GuideImpl.generatorForBlock())));
        }
        return out;
    }

    private static boolean isAccessor(MethodInfo methodInfo) {
        return methodInfo.getSetField() != null && methodInfo.getSetField().field() != null;
    }

    private List<OutputBuilder> superTypes() {
        List<OutputBuilder> supers = new ArrayList<>();
        ParameterizedType parent = typeInfo.parentClass();
        if (parent != null && !parent.isJavaLangObject() && parent.typeInfo() != null) {
            String fqn = parent.typeInfo().fullyQualifiedName();
            if (!"java.lang.Enum".equals(fqn) && !"java.lang.Record".equals(fqn)) {
                supers.add(new OutputBuilderImpl().add(new TextImpl(KotlinTypeName.of(parent) + "()")));
            }
        }
        typeInfo.interfacesImplemented().forEach(i ->
                supers.add(new OutputBuilderImpl().add(new TextImpl(KotlinTypeName.of(i)))));
        return supers;
    }
}
