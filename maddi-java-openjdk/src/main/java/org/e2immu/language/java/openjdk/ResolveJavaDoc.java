package org.e2immu.language.java.openjdk;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.ImportStatement;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.ArrayList;
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
            List<TypeInfo> parameterTypes = new ArrayList<>();
            Info resolvedReference = resolveReference(currentType, tag.content(), tag.sourceOfReference(), dsb,
                    parameterTypes);
            if (resolvedReference != null) {
                return tag.withResolvedReference(resolvedReference)
                        .withReferencedParameterTypes(parameterTypes)
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

    Info resolveReference(TypeInfo currentType, String signature, Source source, DetailedSources.Builder dsb,
                          List<TypeInfo> parameterTypesOut) {
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
                // overloaded ('{@link StreamOutput#write}'), inherited, or simply absent: the MEMBER cannot be
                // pinned down, but the TYPE is certain. Resolve to it rather than to nothing, so the caller keeps
                // the detailed sources of the type part -- a consumer that relocates the referring file must be
                // able to rewrite that token (ES server-base carve: the simple name stopped resolving otherwise).
                return type;
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
        resolveParameterTypes(currentType, signature, source, dsb, parameterTypesOut);
        MethodInfo method = type.methods().stream().filter(mi ->
                        methodName.equals(mi.name()) && mi.parameters().size() == paramTypes.size())
                .findFirst().orElse(null); // FIXME do actual param type check
        return method != null ? method : type; // fall back to the type, see above
    }

    /**
     * Resolve the types named in a member reference's parameter list — the {@code P} of {@code {@link T#m(P)}}.
     * The enclosing file needs {@code P} to resolve exactly as it needs {@code T}, so it is a genuine reference;
     * without this, a javadoc-ONLY import of P looks unused and gets dropped by a move (ES: Element.java's
     * {@code import …guice.Module} behind {@code Elements#getElements(Module[])}).
     * <p>
     * Offsets are computed on the ORIGINAL {@code signature} string, whose index 0 coincides with {@code source}'s
     * start, so each token is stamped where it is actually written rather than at the start of the reference. The
     * token is only stamped for a single-line reference: {@link Source#ofIndex} counts newlines in the string it is
     * given, and javac's signature text does not reproduce a line break's leading " * ", so a multi-line reference
     * would yield a span that is off — the shape of the old EditCollector overflow. Resolution still happens in that
     * case (which is what keeps the import), only the rewritable token is withheld.
     */
    private void resolveParameterTypes(TypeInfo currentType, String signature, Source source,
                                       DetailedSources.Builder dsb, List<TypeInfo> parameterTypesOut) {
        int hash = signature.indexOf('#');
        int open = signature.indexOf('(', hash < 0 ? 0 : hash);
        int close = signature.lastIndexOf(')');
        if (open < 0 || close < open) return;
        boolean singleLine = source != null && source.beginLine() == source.endLine();
        int from = open + 1;
        while (from < close) {
            int comma = signature.indexOf(',', from);
            int end = comma < 0 || comma > close ? close : comma;
            int start = from;
            while (start < end && Character.isWhitespace(signature.charAt(start))) start++;
            // the type name stops at '[' (array), '<' (generics), '.' is part of it, and whitespace introduces the
            // optional parameter NAME, as in "(String s)"
            int nameEnd = start;
            while (nameEnd < end && !Character.isWhitespace(signature.charAt(nameEnd))
                   && signature.charAt(nameEnd) != '[' && signature.charAt(nameEnd) != '<') nameEnd++;
            if (nameEnd > start) {
                String name = signature.substring(start, nameEnd);
                DetailedSources.Builder paramDsb = singleLine ? dsb : runtime.newDetailedSourcesBuilder();
                TypeInfo t = resolveType(currentType, name, source, paramDsb, start);
                if (t != null && !t.isPrimitive() && !parameterTypesOut.contains(t)) parameterTypesOut.add(t);
            }
            from = end + 1;
        }
    }

    private TypeInfo resolveType(TypeInfo currentType, String name, Source source, DetailedSources.Builder dsb) {
        return resolveType(currentType, name, source, dsb, 0);
    }

    private TypeInfo resolveType(TypeInfo currentType, String name, Source source, DetailedSources.Builder dsb,
                                 int offsetInSource) {
        if (name.isEmpty()) {
            // "#a()" with no type — member of the current class
            return currentType;
        }

        // 1. Fully qualified — direct lookup
        TypeInfo t = typeData.getType(name);
        if (t != null) {
            detailedSourcesOfType(name, source, dsb, t, offsetInSource);
            return t;
        }

        // 2. Simple name — check current package
        String pkg = currentType.packageName();
        String fqn = pkg + "." + name;
        t = typeData.getType(fqn);
        if (t != null) {
            // the source holds the (possibly partially-qualified) name as written, not the fqn we resolved to;
            // stamping the detailed source with the fqn's length overshoots the token and overflows the line.
            detailedSourcesOfType(name, source, dsb, t, offsetInSource);
            return t;
        }

        // 3. Check imports of current compilation unit
        for (ImportStatement imp : currentType.primaryType().compilationUnit().importStatements()) {
            String imported = imp.importString();
            if (imported.endsWith("." + name)) {
                t = typeData.getType(imported);
                if (t != null) {
                    detailedSourcesOfType(name, source, dsb, t, offsetInSource);
                    return t;
                }
            }
            // wildcard import
            if (imported.endsWith(".*")) {
                String qualified = imported.replace("*", name);
                t = typeData.getType(qualified);
                if (t != null) {
                    detailedSourcesOfType(name, source, dsb, t, offsetInSource);
                    return t;
                }
            }
        }

        // 4. Inner class of current type
        t = currentType.findSubType(name, false);
        if (t != null) {
            detailedSourcesOfType(name, source, dsb, t, offsetInSource);
            return t;
        }

        // 5. Sibling class of current type
        if (currentType.compilationUnitOrEnclosingType().isRight()) {
            t = currentType.compilationUnitOrEnclosingType().getRight().findSubType(name, false);
            if (t != null) {
                detailedSourcesOfType(name, source, dsb, t, offsetInSource);
                return t;
            }
        }

        // 6. java.lang implicit import
        t = typeData.getType("java.lang." + name);
        detailedSourcesOfType(name, source, dsb, t, offsetInSource);
        return t; // null if genuinely unresolvable
    }

    /**
     * The token of a name written {@code offsetInSource} characters into {@code source}. {@link Source#ofIndex}
     * derives the position by walking the string it is handed, counting newlines; padding the prefix therefore
     * shifts the token by that many columns. Only used with an offset for a single-line reference (see
     * {@link #resolveParameterTypes}), so no newline can hide in the padding.
     */
    private static Source tokenSource(Source source, String name, int offsetInSource) {
        if (offsetInSource <= 0) return source.ofIndex(name, 0, name.length());
        return source.ofIndex(" ".repeat(offsetInSource) + name, offsetInSource, name.length());
    }

    private static void detailedSourcesOfType(String nameIn, Source source, DetailedSources.Builder dsb, TypeInfo tIn,
                                              int offsetInSource) {
        String name = nameIn;
        TypeInfo t = tIn;
        while (t != null) {
            dsb.put(t, tokenSource(source, name, offsetInSource));
            int lastDot = name.lastIndexOf('.');
            if (lastDot <= 0) break;
            name = name.substring(0, lastDot);

            if (t.packageName().equals(name)) {
                dsb.put(t.packageName(), tokenSource(source, name, offsetInSource));
                break;
            }
            dsb.put(t, tokenSource(source, name, offsetInSource));
            if (t.compilationUnitOrEnclosingType().isRight()) {
                t = t.compilationUnitOrEnclosingType().getRight();
            } else {
                break;
            }
        }
    }
}
