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

package io.codelaser.maddi.kotlin.k2

import io.codelaser.maddi.cst.api.element.Element
import io.codelaser.maddi.cst.api.expression.AnnotationExpression
import io.codelaser.maddi.cst.api.expression.Expression
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.runtime.Runtime
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * Kotlin annotations as CST [AnnotationExpression]s, in the shapes the Java front end builds for the same annotation
 * read from a class file (`ClassSymbolScanner.annotationExpression`): a constant for a constant, an
 * `ArrayInitializer` for an array, a field reference for an enum entry, a `ClassExpression` for a class literal, and
 * a nested annotation for a nested one. The contract reader (`AnnotationToProperty`) casts to exactly those shapes.
 *
 * ⭐ PLACEMENT IS K2's. An annotation is copied onto the CST element built from the K2 symbol that carries it, and
 * K2 has already applied Kotlin's use-site rules (measured, `AnnotationPlacementTest`): `@get:`/`@set:`/`@field:`/
 * `@setparam:` sit on the getter/setter/backing-field/setter-parameter symbol, a Java annotation on a class-body
 * property on its backing field, and a `PROPERTY`-only one on the property, which has no JVM element and so no
 * CST element to receive it — kotlinc too puts it where no Java reader looks.
 *
 * A value that cannot be represented drops its key/value pair, and an annotation whose class cannot be resolved is
 * dropped whole, both as the class-file reader does.
 */
internal class KotlinAnnotations(private val runtime: Runtime, private val typeMapper: KotlinTypeMapper) {

    /** Add [annotated]'s annotations to [builder]; [owner] is the type the element belongs to. */
    fun KaSession.annotate(builder: Element.Builder<*>, annotated: KaAnnotated?, owner: TypeInfo) {
        annotated ?: return
        annotated.annotations.forEach { a -> convert(a, owner)?.let { builder.addAnnotation(it) } }
    }

    private fun KaSession.convert(annotation: KaAnnotation, owner: TypeInfo): AnnotationExpression? {
        val typeInfo = typeOf(annotation.classId, owner) ?: return null
        val kvs = annotation.arguments.mapNotNull { argument ->
            value(argument.expression, owner)?.let {
                runtime.newAnnotationExpressionKeyValuePair(argument.name.asString(), it)
            }
        }
        return runtime.newAnnotationExpressionBuilder()
            .setTypeInfo(typeInfo)
            .setKeyValuesPairs(kvs)
            .setSource(runtime.noSource())
            .build()
    }

    private fun KaSession.value(value: KaAnnotationValue, owner: TypeInfo): Expression? = when (value) {
        is KaAnnotationValue.ConstantValue -> constantExpression(runtime, value.value.value)
        is KaAnnotationValue.EnumEntryValue -> value.callableId?.let { id ->
            val enumType = id.classId?.let { typeOf(it, owner) }
            enumType?.getFieldByName(id.callableName.asString(), false)?.let { field ->
                runtime.newVariableExpressionBuilder()
                    .setSource(runtime.noSource())
                    .setVariable(runtime.newFieldReference(field))
                    .build()
            }
        }
        is KaAnnotationValue.ClassLiteralValue ->
            runtime.newClassExpressionBuilder(with(typeMapper) { mapType(value.type, owner) }).build()
        is KaAnnotationValue.ArrayValue -> {
            val values = value.values.mapNotNull { value(it, owner) }
            runtime.newArrayInitializerBuilder()
                .setExpressions(values)
                .setCommonType(values.firstOrNull()?.parameterizedType() ?: runtime.objectParameterizedType())
                .build()
        }
        is KaAnnotationValue.NestedAnnotationValue -> convert(value.annotation, owner)
        else -> null
    }

    /** The annotation (or enum) class, resolved as every other type reference is: source, Java source, or library. */
    private fun KaSession.typeOf(classId: ClassId?, owner: TypeInfo): TypeInfo? {
        val symbol = classId?.let { findClass(it) } as? KaNamedClassSymbol ?: return null
        return with(typeMapper) { mapType(buildClassType(symbol), owner) }.typeInfo()
    }
}

/**
 * A compile-time constant as a CST constant expression, the one mapping both a constant in a body and one in an
 * annotation argument use. Null for a value kind with no CST constant.
 */
internal fun constantExpression(runtime: Runtime, value: Any?): Expression? = when (value) {
    is Int -> runtime.newInt(value)
    is Long -> runtime.newLong(value)
    is Short -> runtime.newShort(value)
    is Byte -> runtime.newByte(value)
    is Double -> runtime.newDouble(value)
    is Float -> runtime.newFloat(value)
    is Char -> runtime.newChar(value)
    is Boolean -> runtime.newBoolean(value)
    is String -> runtime.newStringConstant(value)
    null -> runtime.nullConstant()
    else -> null
}
