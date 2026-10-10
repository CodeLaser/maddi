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
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java's annotations as C#'s attributes. An annotation type of the program is a sealed attribute class,
 * {@code ToolAttribute}: its elements are properties with their defaults, a {@code value} element also the
 * constructor's parameter, and {@code @Target} is {@code [AttributeUsage]}. Reading an element, {@code tool.name()},
 * is reading the property, {@code tool.Name}. A use of one, {@code @Tool(name = "x")}, is {@code [Tool(Name = "x")]};
 * {@code @Deprecated} is {@code [Obsolete]}. Other JDK annotations ({@code @Override}, {@code @FunctionalInterface})
 * have no C# counterpart, and those of other libraries are dropped.
 */
final class CSharpAttributes {

    private CSharpAttributes() {
    }

    /** An annotation type of the program: printed as an attribute class. */
    static boolean isAttribute(TypeInfo typeInfo) {
        return typeInfo.typeNature().isAnnotation() && CSharpNames.translated(typeInfo);
    }

    /** {@code tool.name()}: reading the property. */
    static boolean isElement(MethodInfo method) {
        return isAttribute(method.typeInfo()) && !method.isStatic() && method.parameters().isEmpty();
    }

    static String property(MethodInfo element) {
        return CSharpNames.pascal(element.name());
    }

    private static final Map<String, String> TARGETS = Map.of(
            "TYPE", "AttributeTargets.Class | AttributeTargets.Interface | AttributeTargets.Struct | AttributeTargets.Enum",
            "METHOD", "AttributeTargets.Method",
            "FIELD", "AttributeTargets.Field | AttributeTargets.Property",
            "PARAMETER", "AttributeTargets.Parameter",
            "CONSTRUCTOR", "AttributeTargets.Constructor",
            "ANNOTATION_TYPE", "AttributeTargets.Class",
            "TYPE_USE", "AttributeTargets.All");

    /** The attribute class of an annotation type. */
    static OutputBuilder declaration(TypeInfo annotation, String access, Qualification q) {
        CSharpContext.using("System");
        String name = CSharpNames.type(annotation);
        List<OutputBuilder> members = new ArrayList<>();
        MethodInfo valueElement = null;
        for (MethodInfo element : annotation.methods()) {
            if (element.isSynthetic() || element.isStatic()) continue;
            if ("value".equals(element.name())) valueElement = element;
            OutputBuilder b = new OutputBuilderImpl().add(new TextImpl("public " + CSharpTypeName.of(element.returnType(), q)
                                                                        + " " + property(element) + " { get; set; }"));
            Expression defaultValue = defaultValue(element);
            if (defaultValue != null) {
                b.add(SymbolEnum.assignment("=")).add(value(defaultValue, element.returnType(), q)).add(SymbolEnum.SEMICOLON);
            }
            members.add(b);
        }
        for (FieldInfo constant : annotation.fields()) {
            if (constant.isSynthetic()) continue;
            members.add(new CSharpFieldPrinter(constant, true).print(q, false));
        }
        List<OutputBuilder> constructors = new ArrayList<>();
        constructors.add(new OutputBuilderImpl().add(new TextImpl("public " + name + "()")).add(SpaceEnum.ONE)
                .add(SymbolEnum.LEFT_BRACE).add(SymbolEnum.RIGHT_BRACE));
        if (valueElement != null) {
            ParameterizedType type = valueElement.returnType();
            constructors.add(new OutputBuilderImpl().add(new TextImpl("public " + name + "("
                    + (type.arrays() > 0 ? "params " : "") + CSharpTypeName.of(type, q) + " value)"))
                    .add(SpaceEnum.ONE).add(new TextImpl("{ Value = value; }")));
        }
        constructors.addAll(members);
        OutputBuilder out = new OutputBuilderImpl();
        String usage = usage(annotation);
        if (usage != null) out.add(new TextImpl("[AttributeUsage(" + usage + ")]")).add(SpaceEnum.NEWLINE);
        out.add(new TextImpl((access == null ? "" : access + " ") + "sealed class " + name + " : Attribute"))
                .add(SpaceEnum.ONE).add(CSharpStatementPrinter.braces(constructors));
        return out;
    }

    /** An element's default: the single return statement of its body, as the CST keeps it. */
    private static Expression defaultValue(MethodInfo element) {
        if (element.methodBody() == null) return null;
        List<Statement> statements = element.methodBody().statements().stream().filter(s -> !s.isSynthetic()).toList();
        return statements.size() == 1 && statements.getFirst() instanceof ReturnStatement rs && !rs.hasNoValue()
                ? rs.expression() : null;
    }

    /** {@code @Target({METHOD, FIELD})}: the attribute's targets; null without a @Target. */
    private static String usage(TypeInfo annotation) {
        for (AnnotationExpression ae : annotation.annotations()) {
            if (!"java.lang.annotation.Target".equals(ae.typeInfo().fullyQualifiedName())) continue;
            Set<String> targets = new LinkedHashSet<>();
            for (AnnotationExpression.KV kv : ae.keyValuePairs()) {
                List<Expression> values = kv.value() instanceof ArrayInitializer ai ? ai.expressions() : List.of(kv.value());
                for (Expression v : values) {
                    if (v instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
                        String target = TARGETS.get(fr.fieldInfo().name());
                        if (target != null) targets.add(target);
                    }
                }
            }
            return targets.isEmpty() ? null : String.join(" | ", targets);
        }
        return null;
    }

    /** A value of an element: an array element given one value is an array of one, as Java allows. */
    private static OutputBuilder value(Expression value, ParameterizedType type, Qualification q) {
        if (type.arrays() > 0 && !(value instanceof ArrayInitializer)) {
            return new OutputBuilderImpl().add(new TextImpl("new " + CSharpTypeName.of(type, q))).add(SpaceEnum.ONE)
                    .add(SymbolEnum.LEFT_BRACE).add(CSharpExpressionPrinter.print(value, q)).add(SymbolEnum.RIGHT_BRACE);
        }
        if (type.arrays() > 0) {
            return new OutputBuilderImpl().add(new TextImpl("new " + CSharpTypeName.of(type, q))).add(SpaceEnum.ONE)
                    .add(CSharpExpressionPrinter.initializer(value, type, q));
        }
        return CSharpExpressionPrinter.print(value, q);
    }

    /** {@code [Tool(Name = "x")] }: the uses of program annotations, and @Deprecated; empty when there are none. */
    static OutputBuilder uses(List<AnnotationExpression> annotations, Qualification q, boolean newline) {
        OutputBuilder out = new OutputBuilderImpl();
        for (AnnotationExpression ae : annotations) {
            OutputBuilder use = use(ae, q);
            if (use == null) continue;
            out.add(use).add(newline ? SpaceEnum.NEWLINE : SpaceEnum.ONE);
        }
        return out;
    }

    private static OutputBuilder use(AnnotationExpression ae, Qualification q) {
        TypeInfo type = ae.typeInfo();
        if ("java.lang.Deprecated".equals(type.fullyQualifiedName())) {
            CSharpContext.using("System");
            return new OutputBuilderImpl().add(new TextImpl("[Obsolete]"));
        }
        if (!isAttribute(type)) return null;
        String name = CSharpTypeName.name(type, q);
        if (name.endsWith("Attribute")) name = name.substring(0, name.length() - "Attribute".length());
        OutputBuilder b = new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACKET).add(new TextImpl(name));
        if (!ae.keyValuePairs().isEmpty()) {
            b.add(SymbolEnum.LEFT_PARENTHESIS);
            boolean first = true;
            // positional arguments come before named ones
            List<AnnotationExpression.KV> ordered = new ArrayList<>(ae.keyValuePairs());
            ordered.sort(java.util.Comparator.comparing(kv -> !(kv.keyIsDefault() || "value".equals(kv.key()))));
            for (AnnotationExpression.KV kv : ordered) {
                if (!first) b.add(SymbolEnum.COMMA);
                first = false;
                MethodInfo element = type.methods().stream().filter(m -> m.name().equals(kv.key())).findFirst().orElse(null);
                ParameterizedType elementType = element == null ? null : element.returnType();
                boolean positional = kv.keyIsDefault() || "value".equals(kv.key());
                if (positional && elementType != null && elementType.arrays() > 0) {
                    // the params constructor: [Tool("a", "b")]
                    List<Expression> values = kv.value() instanceof ArrayInitializer ai ? ai.expressions() : List.of(kv.value());
                    for (int i = 0; i < values.size(); i++) {
                        if (i > 0) b.add(SymbolEnum.COMMA);
                        b.add(CSharpExpressionPrinter.print(values.get(i), q));
                    }
                    continue;
                }
                if (!positional) {
                    b.add(new TextImpl(CSharpNames.pascal(kv.key()))).add(SymbolEnum.assignment("="));
                }
                b.add(elementType == null ? CSharpExpressionPrinter.print(kv.value(), q) : value(kv.value(), elementType, q));
            }
            b.add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        return b.add(SymbolEnum.RIGHT_BRACKET);
    }
}
