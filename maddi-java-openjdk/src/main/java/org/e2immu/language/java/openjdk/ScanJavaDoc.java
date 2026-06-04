package org.e2immu.language.java.openjdk;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.DocSourcePositions;
import com.sun.tools.javac.tree.DCTree;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.runtime.Runtime;

import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record ScanJavaDoc(Runtime runtime,
                          TypeData typeData,
                          DocSourcePositions docSourcePositions,
                          CompilationUnitTree compilationUnitTree,
                          LineMap lineMap) {

    public JavaDoc scan(DocCommentTree docCommentTree) {
        DCTree.DCDocComment docComment = (DCTree.DCDocComment) docCommentTree;

        StringBuilder comment = new StringBuilder();
        Source source = source(docCommentTree, docCommentTree);
        List<JavaDoc.Tag> tags = new ArrayList<>();
        for (DocTree dt : docCommentTree.getFullBody()) {
            if (dt instanceof DCTree.DCInlineTag<?>) {
                JavaDoc.Tag tag = convertTag(docCommentTree, dt);
                if (tag != null) {
                    tags.add(tag);
                }
            }
            comment.append(dt);
        }
        for (DocTree dt : docComment.tags) {
            JavaDoc.Tag tag = convertTag(docCommentTree, dt);
            if (tag != null) tags.add(tag);
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

    private JavaDoc.Tag convertTag(DocCommentTree docCommentTree, DocTree docTree) {
        String content = content(docTree);
        JavaDoc.TagIdentifier tagId = identifier(docTree);
        if (tagId == null) return null;
        Source srcRef = switch (docTree) {
            case DCTree.DCParam p -> source(docCommentTree, p.name);
            case DCTree.DCLink l -> source(docCommentTree, l.getReference());
            case DCTree.DCThrows t -> source(docCommentTree, t.getExceptionName());
            case DCTree.DCSee s -> sourceOfList(docCommentTree, s.getReference());
            default -> null;
        };
        Source src = source(docCommentTree, docTree);
        boolean isBlock = docTree instanceof BlockTagTree;
        return runtime.newJavaDocTag(tagId, content, null, src, srcRef, isBlock);
    }

    private Source sourceOfList(DocCommentTree docCommentTree, List<? extends DocTree> reference) {
        if (reference != null) {
            for (DocTree dt : reference) {
                if (dt instanceof DCTree.DCReference) {
                    return source(docCommentTree, reference.getFirst());
                }
            }
        }
        return null;
    }

    private JavaDoc.TagIdentifier identifier(DocTree docTree) {
        String upperCase = docTree.getKind().name().toUpperCase();
        if ("ERRONEOUS".equals(upperCase)) return null;
        return JavaDoc.TagIdentifier.valueOf(upperCase);
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
}
