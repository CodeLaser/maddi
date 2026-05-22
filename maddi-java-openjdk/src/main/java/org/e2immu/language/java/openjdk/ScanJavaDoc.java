package org.e2immu.language.java.openjdk;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.DocSourcePositions;
import com.sun.tools.javac.tree.DCTree;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;

import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record ScanJavaDoc(Runtime runtime,
                          TypeData typeData,
                          DocSourcePositions docSourcePositions,
                          CompilationUnitTree compilationUnitTree,
                          LineMap lineMap) {

    public JavaDoc scan(DocCommentTree docCommentTree, TypeInfo currentType) {
        DCTree.DCDocComment docComment = (DCTree.DCDocComment) docCommentTree;

        StringBuilder comment = new StringBuilder();
        Source source = source(docCommentTree, docCommentTree);
        List<JavaDoc.Tag> tags = new ArrayList<>();
        for (DocTree dt : docCommentTree.getFullBody()) {
            if (dt.getKind() != DocTree.Kind.TEXT) {
                JavaDoc.Tag tag = convertTag(currentType, docCommentTree, dt);
                tags.add(tag);
            }
            comment.append(dt);
        }
        for (DocTree dt : docComment.tags) {
            JavaDoc.Tag tag = convertTag(currentType, docCommentTree, dt);
            tags.add(tag);
            comment.append("\n").append(dt);
        }
        return runtime.newJavaDoc(source, comment.toString(), List.copyOf(tags));
    }

    private String content(DocTree docTree) {
        return switch (docTree) {
            case DCTree.DCAuthor a -> a.getName().toString();
            case DCTree.DCThrows t -> t.getExceptionName().getSignature();
            case DCTree.DCLink l -> l.getReference().getSignature();
            case DCTree.DCParam p -> p.getName().toString();
            case DCTree.DCSee s -> s.getReference().stream().map(this::content)
                    .collect(Collectors.joining("; "));
            default -> docTree.toString();
        };
    }

    private JavaDoc.Tag convertTag(TypeInfo currentType, DocCommentTree docCommentTree, DocTree docTree) {
        String content = content(docTree);
        JavaDoc.TagIdentifier tagId = identifier(docTree);
        Source srcRef = switch (docTree) {
            case DCTree.DCLink l -> source(docCommentTree, l.getReference());
            case DCTree.DCThrows t -> source(docCommentTree, t.getExceptionName());
            default -> null;
        };
        Source src = source(docCommentTree, docTree);
        boolean isBlock = docTree instanceof BlockTagTree;
        Info resolvedReference = resolveReference(currentType, content);
        return runtime.newJavaDocTag(tagId, content, resolvedReference, src, srcRef, isBlock);
    }


    private JavaDoc.TagIdentifier identifier(DocTree docTree) {
        return JavaDoc.TagIdentifier.valueOf(docTree.getKind().name().toUpperCase());
    }

    private Source source(DocCommentTree docComment, DocTree docNode) {
        long startPos = docSourcePositions.getStartPosition(compilationUnitTree, docComment, docNode);
        long endPos = docSourcePositions.getEndPosition(compilationUnitTree, docComment, docNode);
        if (startPos == Diagnostic.NOPOS) return runtime.noSource(); // no position available
        long startLine = lineMap.getLineNumber(startPos);
        long startCol = lineMap.getColumnNumber(startPos);
        long endLine = lineMap.getLineNumber(endPos);
        long endCol = lineMap.getColumnNumber(endPos) - 1; // inclusive
        return runtime().newParserSource("-", (int) startLine, (int) startCol, (int) endLine, (int) endCol);
    }

    Info resolveReference(TypeInfo currentType, String signature) {
        int hash = signature.indexOf('#');

        if (hash < 0) {
            // Type reference only — "D" or "java.util.List"
            String typeName = signature.trim();
            // look up in your type table by simple or qualified name
            return resolveType(currentType, typeName);
        }
        // Member reference — "D#a()" or "D#field"
        String typeName = signature.substring(0, hash).trim();
        String memberSig = signature.substring(hash + 1).trim();

        TypeInfo type = resolveType(currentType, typeName);
        if (type == null) return null;

        int paren = memberSig.indexOf('(');
        if (paren < 0) {
            // Field reference — "D#field"
            return type.getFieldByName(memberSig, false);
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
                .findFirst().orElseThrow(); // FIXME do actual param type check
    }

    private TypeInfo resolveType(TypeInfo currentType, String name) {
        if (name.isEmpty()) {
            // "#a()" with no type — member of the current class
            return currentType;
        }

        // 1. Fully qualified — direct lookup
        TypeInfo t = typeData.getType(name);
        if (t != null) return t;

        // 2. Simple name — check current package
        String pkg = compilationUnitTree.getPackageName().toString();
        t = typeData.getType(pkg + "." + name);
        if (t != null) return t;

        // 3. Check imports of current compilation unit
        for (ImportTree imp : compilationUnitTree.getImports()) {
            String imported = imp.getQualifiedIdentifier().toString();
            if (imported.endsWith("." + name)) {
                t = typeData.getType(imported);
                if (t != null) return t;
            }
            // wildcard import
            if (imported.endsWith(".*")) {
                String qualified = imported.replace("*", name);
                t = typeData.getType(qualified);
                if (t != null) return t;
            }
        }

        // 4. Inner class of current type
        t = currentType.findSubType(name, false);
        if (t != null) return t;

        // 5. java.lang implicit import
        t = typeData.getType("java.lang." + name);
        return t; // null if genuinely unresolvable
    }
}
