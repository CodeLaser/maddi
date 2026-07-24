package org.e2immu.language.java.openjdk;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.Arrays;
import java.util.List;

public record ResolveJavaDoc(Runtime runtime, TypeData typeData) {

    public JavaDoc resolve(TypeInfo currentType, MethodInfo currentMethod, JavaDoc javaDoc) {
        List<JavaDoc.Tag> tags = javaDoc.tags();
        List<JavaDoc.Tag> newList = tags.stream().map(t -> resolve(currentType, currentMethod, t))
                .collect(TranslationMap.staticToList(tags));
        if (newList == tags) return javaDoc;
        return javaDoc.withTags(newList);
    }

    JavaDoc.Tag resolve(TypeInfo currentType, MethodInfo currentMethod, JavaDoc.Tag tag) {
        if (tag.sourceOfReference() != null) {
            if (JavaDoc.TagIdentifier.PARAM.equals(tag.identifier())) {
                TypeParameter tpResolved = resolveTypeParameter(currentType, currentMethod, tag.content());
                if (tpResolved != null) {
                    return tag.withResolvedReference(tpResolved);
                }

                if (currentMethod != null) {
                    ParameterInfo resolvedReference = resolveParameterInfo(currentMethod, tag.content());
                    return tag.withResolvedReference(resolvedReference);
                }
                return tag;
            }
            DetailedSources.Builder dsb = runtime.newDetailedSourcesBuilder();
            Info resolvedReference = resolveReference(currentType, tag.content(), tag.sourceOfReference(), dsb);
            if (resolvedReference != null) {
                return tag.withResolvedReference(resolvedReference)
                        .withSource(tag.source().withDetailedSources(dsb.build()));
            }
        }
        return tag;
    }

    TypeParameter resolveTypeParameter(TypeInfo currentType, MethodInfo currentMethod, String name) {
        if (currentMethod != null) {
            return currentMethod.typeParameters().stream()
                    .filter(tp -> name.equals(tp.simpleName())).findFirst().orElse(null);
        }
        return currentType.typeParameters().stream()
                .filter(tp -> name.equals(tp.simpleName())).findFirst().orElse(null);
    }

    ParameterInfo resolveParameterInfo(MethodInfo currentMethod, String name) {
        return currentMethod.parameters().stream()
                .filter(pi -> name.equals(pi.name()))
                .findFirst()
                .orElse(null);
    }

    Info resolveReference(TypeInfo currentType, String signature, Source source, DetailedSources.Builder dsb) {
        int hash = signature.indexOf('#');

        if (hash < 0) {
            // Type reference only — "D" or "java.util.List"
            String typeName = signature.trim();
            // look up in your type table by simple or qualified name
            return resolveType(currentType, typeName, source, dsb);
        }
        // Member reference — "D#a()" or "D#field"
        String typeName = signature.substring(0, hash).trim();
        String memberSig = signature.substring(hash + 1).trim();

        TypeInfo type = resolveType(currentType, typeName, source, dsb);
        if (type == null) return null;

        int paren = memberSig.indexOf('(');
        if (paren < 0) {
            // Field reference — "D#field"
            FieldInfo fi = type.getFieldByName(memberSig, false);
            if (fi == null) {
                // try method, but only accept when the name is unique in the type
                List<MethodInfo> methods = type.methods().stream()
                        .filter(m -> memberSig.equals(m.name())).toList();
                if (methods.size() == 1) return methods.getFirst();
                return null;
            }
            return fi;
        }
        // Method reference — "D#a()" or "D#a(String, int)"
        String methodName = memberSig.substring(0, paren);
        String paramsPart = memberSig.substring(paren + 1,
                memberSig.lastIndexOf(')'));
        List<String> paramTypes = paramsPart.isBlank()
                ? List.of()
                : Arrays.stream(paramsPart.split(","))
                .map(String::trim)
                .toList();
        return type.methods().stream().filter(mi ->
                        methodName.equals(mi.name()) && mi.parameters().size() == paramTypes.size())
                .findFirst().orElse(null); // FIXME do actual param type check
    }

    private TypeInfo resolveType(TypeInfo currentType, String name, Source source, DetailedSources.Builder dsb) {
        if (name.isEmpty()) {
            // "#a()" with no type — member of the current class
            return currentType;
        }

        // 1. Fully qualified — direct lookup
        TypeInfo t = typeData.getType(name);
        if (t != null) {
            detailedSourcesOfType(name, source, dsb, t);
            return t;
        }

        // 2. Simple name — check current package
        String pkg = currentType.packageName();
        String fqn = pkg + "." + name;
        t = typeData.getType(fqn);
        if (t != null) {
            // the source holds the (possibly partially-qualified) name as written, not the fqn we resolved to;
            // stamping the detailed source with the fqn's length overshoots the token and overflows the line.
            detailedSourcesOfType(name, source, dsb, t);
            return t;
        }

        // 3. Check imports of current compilation unit
        for (ImportStatement imp : currentType.primaryType().compilationUnit().importStatements()) {
            String imported = imp.importString();
            if (imported.endsWith("." + name)) {
                t = typeData.getType(imported);
                if (t != null) {
                    detailedSourcesOfType(name, source, dsb, t);
                    return t;
                }
            }
            // wildcard import
            if (imported.endsWith(".*")) {
                String qualified = imported.replace("*", name);
                t = typeData.getType(qualified);
                if (t != null) {
                    detailedSourcesOfType(name, source, dsb, t);
                    return t;
                }
            }
        }

        // 4. Inner class of current type
        t = currentType.findSubType(name, false);
        if (t != null) {
            detailedSourcesOfType(name, source, dsb, t);
            return t;
        }

        // 5. Sibling class of current type
        if (currentType.compilationUnitOrEnclosingType().isRight()) {
            t = currentType.compilationUnitOrEnclosingType().getRight().findSubType(name, false);
            if (t != null) {
                detailedSourcesOfType(name, source, dsb, t);
                return t;
            }
        }

        // 6. java.lang implicit import
        t = typeData.getType("java.lang." + name);
        detailedSourcesOfType(name, source, dsb, t);
        return t; // null if genuinely unresolvable
    }

    private static void detailedSourcesOfType(String nameIn, Source source, DetailedSources.Builder dsb, TypeInfo tIn) {
        String name = nameIn;
        TypeInfo t = tIn;
        while (t != null) {
            dsb.put(t, source.ofIndex(name, 0, name.length()));
            int lastDot = name.lastIndexOf('.');
            if (lastDot <= 0) break;
            name = name.substring(0, lastDot);

            if (t.packageName().equals(name)) {
                dsb.put(t.packageName(), source.ofIndex(name, 0, name.length()));
                break;
            }
            dsb.put(t, source.ofIndex(name, 0, name.length()));
            if (t.compilationUnitOrEnclosingType().isRight()) {
                t = t.compilationUnitOrEnclosingType().getRight();
            } else {
                break;
            }
        }
    }
}
