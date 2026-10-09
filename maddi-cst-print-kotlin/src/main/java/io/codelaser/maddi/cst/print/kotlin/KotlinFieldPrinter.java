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

package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.FieldPrinter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.impl.output.*;

/**
 * Prints a {@link FieldInfo} as a Kotlin property: `[visibility] val|var name: Type [= initializer]`. A final
 * field becomes `val`, otherwise `var`. When printed as a primary-constructor parameter, the initializer is
 * omitted. Static fields print as plain properties for now (a Kotlin `companion object`/top-level `const` is a
 * refinement). The method body / initializer expression reuses the shared expression printing.
 */
public record KotlinFieldPrinter(FieldInfo fieldInfo, boolean formatter2) implements FieldPrinter {

    @Override
    public OutputBuilder print(Qualification qualification, boolean asParameterInPrimaryConstructor) {
        boolean hasInitializer = fieldInfo.initializer() != null && !fieldInfo.initializer().isEmpty();
        // a Kotlin property is initialized where it is declared; Java's is zero/false/null until assigned
        // a final field the secondary constructors assign cannot be a val: Kotlin assigns those in an init block only
        boolean isVal = isVal(fieldInfo, hasInitializer, asParameterInPrimaryConstructor);
        boolean needsDefault = !asParameterInPrimaryConstructor && !hasInitializer && !isVal
                               && !fieldInfo.owner().isInterface();
        io.codelaser.maddi.cst.api.type.ParameterizedType type = KotlinNullability.fieldType(fieldInfo);
        // Java's default for a reference is null: written out when the verdict allows it, else lateinit
        String zero = !needsDefault ? null : KotlinNullability.isNullable(type) ? "null" : defaultValue(type);

        OutputBuilder builder = new OutputBuilderImpl();
        java.util.Optional<io.codelaser.maddi.cst.api.output.element.Keyword> visibility =
                KotlinModifiers.visibility(fieldInfo.access(), fieldInfo.owner(), fieldInfo);
        boolean lateinit = needsDefault && zero == null;
        builder.add(KotlinAnnotations.print(fieldInfo.annotations(), qualification));
        if (!isConst() && !visibility.equals(java.util.Optional.of(KeywordImpl.PRIVATE))) {
            boolean clash = accessorClash(fieldInfo, isVal);
            if (lateinit && clash) {
                // a lateinit property cannot be a @JvmField: its accessors get names no Java method has
                builder.add(new TextImpl("@get:JvmName(\"" + fieldInfo.name() + "\\$get\") @set:JvmName(\""
                                         + fieldInfo.name() + "\\$set\")")).add(SpaceEnum.ONE);
            } else if (!lateinit && (clash || javaField(fieldInfo, visibility))) {
                builder.add(new TextImpl("@JvmField")).add(SpaceEnum.ONE);
            }
        }
        visibility.ifPresent(v -> builder.add(v).add(SpaceEnum.ONE));
        if (lateinit) builder.add(new TextImpl("lateinit")).add(SpaceEnum.ONE);
        if (isConst()) builder.add(new TextImpl("const")).add(SpaceEnum.ONE);
        builder.add(isVal ? KotlinKeyword.VAL : KotlinKeyword.VAR)
                .add(SpaceEnum.ONE)
                .add(new TextImpl(KotlinNames.name(fieldInfo.name())))
                .add(SymbolEnum.COLON_LABEL) // Kotlin type ascription: no leading space, one trailing
                .add(new TextImpl(KotlinTypeName.of(type, qualification)));
        if (asParameterInPrimaryConstructor) return builder;
        if (hasInitializer) {
            builder.add(SpaceEnum.ONE).add(KotlinSymbols.assignment("=")).add(SpaceEnum.ONE)
                    .add(KotlinNullability.toTarget(fieldInfo.initializer(), type,
                            KotlinNullability.translated(fieldInfo.owner()),
                            KotlinExpressionPrinter.widened(fieldInfo.initializer(), type, qualification), qualification));
        } else if (zero != null) {
            builder.add(SpaceEnum.ONE).add(KotlinSymbols.assignment("=")).add(SpaceEnum.ONE).add(new TextImpl(zero));
        }
        return builder;
    }

    private static boolean isVal(FieldInfo fieldInfo, boolean hasInitializer, boolean asParameterInPrimaryConstructor) {
        return isFinal(fieldInfo) && (hasInitializer || fieldInfo.isStatic()
                                       || !KotlinTypePrinter.finalFieldsAssignedInSecondaryConstructors(fieldInfo.owner()))
               || hasInitializer && !asParameterInPrimaryConstructor && KotlinAssignments.neverReassigned(fieldInfo);
    }

    /**
     * Java's field and its getter are two members; a Kotlin property's accessors are methods with the getter's JVM
     * name: {@code val handlers} next to {@code fun getHandlers()} is a platform declaration clash. A property whose
     * accessor name a method of its class or a supertype has, is a {@code @JvmField}: a field, as in Java, without
     * accessors. A {@code lateinit} property cannot be one; its accessors are renamed ({@code @get:JvmName}).
     */
    static boolean accessorClash(FieldInfo fieldInfo, boolean isVal) {
        if (!KotlinContext.translatingJava()) return false;
        String name = fieldInfo.name();
        boolean isPrefix = name.length() > 2 && name.startsWith("is") && !Character.isLowerCase(name.charAt(2));
        String capitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        String getter = isPrefix ? name : "get" + capitalized;
        String setter = isPrefix ? "set" + name.substring(2) : "set" + capitalized;
        return accessorClash(fieldInfo.owner(), fieldInfo.isStatic(), getter, isVal ? null : setter,
                new java.util.HashSet<>());
    }

    private static boolean accessorClash(io.codelaser.maddi.cst.api.info.TypeInfo typeInfo, boolean isStatic,
                                         String getter, String setter,
                                         java.util.Set<io.codelaser.maddi.cst.api.info.TypeInfo> visited) {
        if (typeInfo == null || !visited.add(typeInfo)) return false;
        if (typeInfo.methods().stream().anyMatch(m -> !m.isSynthetic() && m.isStatic() == isStatic
                && (m.parameters().isEmpty() && getter.equals(m.name())
                    || m.parameters().size() == 1 && setter != null && setter.equals(m.name())))) {
            return true;
        }
        if (typeInfo.parentClass() != null
            && accessorClash(typeInfo.parentClass().typeInfo(), isStatic, getter, setter, visited)) return true;
        return typeInfo.interfacesImplemented().stream()
                .anyMatch(i -> accessorClash(i.typeInfo(), isStatic, getter, setter, visited));
    }

    /**
     * A public field of translated Java stays a field: Java code that is not translated (yet) reads {@code i.opcode}
     * and {@code C.FIELD}, not {@code i.getOpcode()} and {@code C.Companion.getFIELD()}. A lateinit property's field
     * is public already. In an interface's companion only when it has no {@code const val}: Kotlin requires all of
     * its properties to be {@code @JvmField}, then.
     */
    static boolean javaField(FieldInfo fieldInfo,
                             java.util.Optional<io.codelaser.maddi.cst.api.output.element.Keyword> visibility) {
        if (!KotlinContext.translatingJava() || !visibility.isEmpty()) return false;
        if (!fieldInfo.isStatic()) return true;
        // Kotlin: in an interface's companion, all properties are @JvmField, or none; a const val is not
        return !fieldInfo.owner().isInterface()
               || fieldInfo.owner().fields().stream().noneMatch(f -> !f.isSynthetic() && isConst(f));
    }

    /** The property is a {@code val}, which Kotlin can smart-cast; a {@code var} property it cannot. */
    static boolean printsAsVal(FieldInfo fieldInfo) {
        boolean hasInitializer = fieldInfo.initializer() != null && !fieldInfo.initializer().isEmpty();
        return isVal(fieldInfo, hasInitializer, false);
    }

    /** A static final primitive or String with a constant initializer: {@code const val}, usable in annotations. */
    private boolean isConst() {
        return isConst(fieldInfo);
    }

    /** Final as Java has it: an interface's field, and an annotation type's, is implicitly static and final. */
    static boolean isFinal(FieldInfo fieldInfo) {
        return fieldInfo.isFinal() || fieldInfo.owner().isInterface() || fieldInfo.owner().typeNature().isAnnotation();
    }

    private static boolean isConst(FieldInfo fieldInfo) {
        if (!fieldInfo.isStatic() || !isFinal(fieldInfo) || fieldInfo.initializer() == null
            || !fieldInfo.initializer().isConstant() || KotlinTypePrinter.fromKotlinSource(fieldInfo.owner())) {
            return false;
        }
        io.codelaser.maddi.cst.api.type.ParameterizedType t = fieldInfo.type();
        return t.arrays() == 0 && (t.isPrimitiveExcludingVoid() || t.isJavaLangString());
    }

    /** Java's default value of a primitive field, as Kotlin writes it; null for a reference type. */
    static String defaultValue(io.codelaser.maddi.cst.api.type.ParameterizedType type) {
        if (type.arrays() > 0 || type.typeInfo() == null) return null;
        return switch (type.typeInfo().fullyQualifiedName()) {
            case "boolean" -> "false";
            case "char" -> "'\\u0000'";
            case "byte", "short", "int" -> "0";
            case "long" -> "0L";
            case "float" -> "0f";
            case "double" -> "0.0";
            default -> null;
        };
    }
}
