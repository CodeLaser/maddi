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

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The naming policy: Java's names, written the way C# code names things. Every name is a function of the declaration
 * alone, so a declaration and all its uses agree without a renaming pass:
 * <ul>
 *   <li>a namespace is the package, each segment PascalCase: {@code org.example.util} is {@code Org.Example.Util};</li>
 *   <li>a translated method is PascalCase ({@code getName} is {@code GetName}); {@code toString}, {@code equals} and
 *   {@code hashCode} are {@code ToString}, {@code Equals} and {@code GetHashCode}; an override has its overridden
 *   method's name;</li>
 *   <li>a translated interface is prefixed with {@code I} ({@code Visitor} is {@code IVisitor}), unless its name has
 *   that shape already;</li>
 *   <li>an enum constant, and a record component (a property in C#), is PascalCase: {@code NOT_FOUND} is
 *   {@code NotFound};</li>
 *   <li>other fields, parameters and locals keep their names;</li>
 *   <li>an identifier that is a C# keyword is escaped: {@code @params}.</li>
 * </ul>
 * A new name that would collide (with its enclosing type, which C# forbids, or another member) falls back to the Java
 * name. Library declarations keep their Java names: what the JDK's become is the BCL mapping's business.
 */
public final class CSharpNames {

    private static final Set<String> KEYWORDS = Set.of("abstract", "as", "base", "bool", "break", "byte", "case",
            "catch", "char", "checked", "class", "const", "continue", "decimal", "default", "delegate", "do", "double",
            "else", "enum", "event", "explicit", "extern", "false", "finally", "fixed", "float", "for", "foreach",
            "goto", "if", "implicit", "in", "int", "interface", "internal", "is", "lock", "long", "namespace", "new",
            "null", "object", "operator", "out", "override", "params", "private", "protected", "public", "readonly",
            "ref", "return", "sbyte", "sealed", "short", "sizeof", "stackalloc", "static", "string", "struct",
            "switch", "this", "throw", "true", "try", "typeof", "uint", "ulong", "unchecked", "unsafe", "ushort",
            "using", "virtual", "void", "volatile", "while");

    private CSharpNames() {
    }

    /** An identifier, escaped when it is a C# keyword. */
    public static String name(String identifier) {
        return KEYWORDS.contains(identifier) ? "@" + identifier : identifier;
    }

    /** The namespace of a translated package; a library package is left as it is. */
    public static String namespace(String packageName, boolean translated) {
        if (packageName == null || packageName.isEmpty()) return "";
        if (!translated) return packageName;
        CSharpProgram program = CSharpContext.program();
        return Arrays.stream(packageName.split("\\.")).map(CSharpNames::pascal).map(program::namespaceSegment)
                .collect(Collectors.joining("."));
    }

    /** The namespace a type is declared in. */
    public static String namespace(TypeInfo typeInfo) {
        return namespace(typeInfo.packageName(), translated(typeInfo));
    }

    /** The simple name of a type: {@code IVisitor} for a translated interface {@code Visitor}. */
    public static String type(TypeInfo typeInfo) {
        CSharpAnonymous.Hoisted hoisted = typeInfo.isAnonymous() ? CSharpContext.hoisted(typeInfo) : null;
        if (hoisted != null) return hoisted.name();
        String simple = typeInfo.simpleName();
        // a delegate is named as a class
        if (translated(typeInfo) && typeInfo.typeNature().isInterface() && !typeInfo.typeNature().isAnnotation()
            && !CSharpContext.program().delegate(typeInfo)
            && !(simple.length() > 1 && simple.charAt(0) == 'I' && Character.isUpperCase(simple.charAt(1)))) {
            String prefixed = "I" + simple;
            if (!clashesInEnclosingType(typeInfo, prefixed)) return prefixed;
        }
        return name(simple);
    }

    /** The name of a method in a declaration and in its calls. */
    public static String method(MethodInfo methodInfo) {
        String javaName = methodInfo.name();
        if (!methodInfo.isStatic() && !methodInfo.isConstructor()) {
            int n = methodInfo.parameters().size();
            if (n == 0 && "toString".equals(javaName)) return "ToString";
            if (n == 0 && "hashCode".equals(javaName)) return "GetHashCode";
            if (n == 1 && "equals".equals(javaName)) return "Equals";
        }
        if (!translated(methodInfo.typeInfo())) return name(javaName);
        String bcl = CSharpBcl.overrideName(methodInfo);
        if (bcl != null) return bcl;
        // an override is named as what it overrides, which may have kept its Java name
        for (MethodInfo overridden : methodInfo.overrides()) {
            if (overridden != methodInfo && translated(overridden.typeInfo())) return method(overridden);
        }
        String pascal = pascal(javaName);
        TypeInfo owner = methodInfo.typeInfo();
        if (pascal.equals(type(owner)) && !pascal.equals(javaName)) {
            // C# has no member named as its type: Metadata.metadata(k, v) is Metadata.Of(k, v)
            return methodInfo.isStatic() ? "Of" : pascal + "Value";
        }
        if (pascal.equals(javaName) || pascal.equals(type(owner))
            || owner.fields().stream().anyMatch(f -> pascal.equals(field(f)))
            || owner.subTypes().stream().anyMatch(st -> pascal.equals(type(st)))) {
            return name(javaName);
        }
        return pascal;
    }

    /** The name of a field: PascalCase for an enum constant or a record component, else the Java name. */
    public static String field(FieldInfo fieldInfo) {
        TypeInfo owner = fieldInfo.owner();
        if (!translated(owner)) return name(fieldInfo.name());
        String renamed = null;
        if (isEnumConstant(fieldInfo)) {
            renamed = pascalFromConstant(fieldInfo.name());
        } else if (owner.typeNature().isRecord() && !fieldInfo.isStatic()) {
            renamed = pascal(fieldInfo.name());
        }
        if (renamed == null || renamed.equals(fieldInfo.name())) return name(fieldInfo.name());
        String r = renamed;
        boolean clash = r.equals(type(owner))
                        || owner.fields().stream().anyMatch(f -> f != fieldInfo && r.equals(f.name()))
                        || owner.subTypes().stream().anyMatch(st -> r.equals(type(st)));
        return clash ? name(fieldInfo.name()) : r;
    }

    /** An enum constant: a static final field of the enum's own type. */
    static boolean isEnumConstant(FieldInfo f) {
        TypeInfo owner = f.owner();
        return owner.typeNature().isEnum() && f.isStatic() && f.isFinal() && f.type().typeInfo() == owner
               && f.type().arrays() == 0;
    }

    /** Declared in the sources being translated, not in a library. */
    /**
     * A static nested type of a generic type. C# makes a nested type generic in its enclosing types' parameters,
     * {@code Outer<E>.Inner}, which Java's static nested type is not: it is printed beside its primary type, in the
     * namespace, and named without its enclosing types.
     */
    static boolean hoisted(TypeInfo typeInfo) {
        TypeInfo enclosing = enclosing(typeInfo);
        return enclosing != null && typeInfo.enclosingMethod() == null && translated(typeInfo)
               && (typeInfo.isStatic() || typeInfo.isInterface() || typeInfo.typeNature().isEnum()
                   || typeInfo.typeNature().isRecord())
               && genericScope(enclosing);
    }

    /** Type parameters C# would give a type nested in this one: its own, and those of its non-hoisted enclosing types. */
    private static boolean genericScope(TypeInfo typeInfo) {
        if (!typeInfo.typeParameters().isEmpty()) return true;
        TypeInfo enclosing = enclosing(typeInfo);
        return enclosing != null && !hoisted(typeInfo) && genericScope(enclosing);
    }

    /** The type C# declares in the namespace that contains this one: its primary type, or a hoisted type. */
    static TypeInfo topLevel(TypeInfo typeInfo) {
        TypeInfo t = typeInfo;
        while (true) {
            TypeInfo enclosing = enclosing(t);
            if (enclosing == null || hoisted(t)) return t;
            t = enclosing;
        }
    }

    /**
     * {@code type} has {@code ancestor} as a superclass or superinterface, at any depth. Not
     * {@code recursiveSuperTypeStream()}, which includes the enclosing type of an inner class.
     */
    static boolean inherits(TypeInfo type, TypeInfo ancestor) {
        java.util.Deque<TypeInfo> todo = new java.util.ArrayDeque<>(List.of(type));
        java.util.Set<TypeInfo> seen = new java.util.HashSet<>();
        while (!todo.isEmpty()) {
            TypeInfo t = todo.pop();
            if (!seen.add(t)) continue;
            if (t != type && t.equals(ancestor)) return true;
            if (t.parentClass() != null && t.parentClass().typeInfo() != null) todo.push(t.parentClass().typeInfo());
            t.interfacesImplemented().stream().filter(i -> i.typeInfo() != null).forEach(i -> todo.push(i.typeInfo()));
        }
        return false;
    }

    static TypeInfo enclosing(TypeInfo typeInfo) {
        var cuOrEnclosing = typeInfo.compilationUnitOrEnclosingType();
        return cuOrEnclosing.isRight() ? cuOrEnclosing.getRight() : null;
    }

    /** A translated functional interface: its lambdas are printed with an adapter class, see CSharpTypePrinter. */
    static boolean lambdaAdapter(TypeInfo typeInfo) {
        if (!typeInfo.isInterface() || !translated(typeInfo) || CSharpContext.program().delegate(typeInfo)) return false;
        MethodInfo sam = singleAbstractMethod(typeInfo);
        return sam != null && sam.typeParameters().isEmpty();
    }

    /** The one abstract method of a functional interface, also when no lambda targets it. */
    static MethodInfo singleAbstractMethod(TypeInfo typeInfo) {
        if (!typeInfo.interfacesImplemented().isEmpty()) {
            return typeInfo.isFunctionalInterface() ? typeInfo.singleAbstractMethod() : null;
        }
        List<MethodInfo> abstractMethods = typeInfo.methods().stream()
                .filter(m -> !m.isSynthetic() && m.isAbstract() && !m.isStatic()).toList();
        return abstractMethods.size() == 1 ? abstractMethods.getFirst() : null;
    }

    static boolean translated(TypeInfo typeInfo) {
        return typeInfo.compilationUnit() != null && !typeInfo.compilationUnit().externalLibrary();
    }

    private static boolean clashesInEnclosingType(TypeInfo typeInfo, String name) {
        if (typeInfo.isPrimaryType()) return false;
        var cuOrEnclosing = typeInfo.compilationUnitOrEnclosingType();
        TypeInfo enclosing = cuOrEnclosing.isRight() ? cuOrEnclosing.getRight() : null;
        return enclosing != null && (enclosing.simpleName().equals(name)
                                     || enclosing.subTypes().stream().anyMatch(st -> st.simpleName().equals(name)));
    }

    /** {@code getName} is {@code GetName}. */
    static String pascal(String s) {
        if (s.isEmpty() || !Character.isLowerCase(s.charAt(0))) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** {@code NOT_FOUND} is {@code NotFound}; a name that is not all capitals is only capitalized. */
    static String pascalFromConstant(String s) {
        if (!s.equals(s.toUpperCase()) || s.chars().noneMatch(Character::isLetter)) return pascal(s);
        StringBuilder sb = new StringBuilder();
        for (String part : s.split("_")) {
            if (part.isEmpty()) continue;
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return sb.isEmpty() || !Character.isJavaIdentifierStart(sb.charAt(0)) ? s : sb.toString();
    }
}
