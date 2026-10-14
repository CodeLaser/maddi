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
import io.codelaser.maddi.cst.api.expression.ClassExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.TypeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The part of the {@code jackson:system-text-json} mapping that a template cannot write: {@code @JsonSubTypes}, a
 * list of subtypes, is one {@code [JsonDerivedType(typeof(X), "x")]} per subtype. Its name is the one the list gives,
 * or the subtype's {@code @JsonTypeName}; without either, the subtype has no discriminator of its own.
 */
final class JacksonAttributes implements LibraryMapping {

    static final JacksonAttributes INSTANCE = new JacksonAttributes();

    private static final String SUB_TYPES = "com.fasterxml.jackson.annotation.JsonSubTypes";
    private static final String TYPE_NAME = "com.fasterxml.jackson.annotation.JsonTypeName";

    private JacksonAttributes() {
    }

    @Override
    public String id() {
        return "jackson:system-text-json";
    }

    @Override
    public AttributeMapping annotation(AnnotationExpression use, Target target, Function<Expression, String> print) {
        if (!SUB_TYPES.equals(use.typeInfo().fullyQualifiedName())) return null;
        List<String> attributes = new ArrayList<>();
        for (Expression t : CSharpAttributes.elements(use, "value")) {
            if (!(CSharpExpressionPrinter.unwrap(t) instanceof AnnotationExpression type)) continue;
            Expression subtype = CSharpAttributes.element(type, "value");
            Expression name = CSharpAttributes.element(type, "name");
            if (subtype == null) continue;
            String discriminator = name != null ? print.apply(name) : typeName(subtype);
            attributes.add("JsonDerivedType(" + print.apply(subtype)
                           + (discriminator == null ? "" : ", " + discriminator) + ")");
        }
        return new AttributeMapping(attributes, List.of("System.Text.Json.Serialization"));
    }

    /** The subtype's {@code @JsonTypeName}, as a C# string literal; null without one. */
    private static String typeName(Expression subtype) {
        if (!(CSharpExpressionPrinter.unwrap(subtype) instanceof ClassExpression ce)) return null;
        TypeInfo t = ce.type().typeInfo();
        if (t == null) return null;
        for (AnnotationExpression a : t.annotations()) {
            if (TYPE_NAME.equals(a.typeInfo().fullyQualifiedName())) {
                String value = a.extractString("value", null);
                if (value != null) return CSharpExpressionPrinter.stringLiteral(value);
            }
        }
        return null;
    }
}
