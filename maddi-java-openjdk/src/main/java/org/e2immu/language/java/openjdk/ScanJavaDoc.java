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
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.runtime.Runtime;

import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

public record ScanJavaDoc(Runtime runtime, TypeData typeData, DocSourcePositions docSourcePositions,
                          CompilationUnitTree compilationUnitTree, LineMap lineMap) {

    public JavaDoc scan(DocCommentTree docCommentTree) {
        DCTree.DCDocComment docComment = (DCTree.DCDocComment) docCommentTree;

        StringBuilder comment = new StringBuilder();
        Source source = source(docCommentTree, docCommentTree);
        List<JavaDoc.Tag> tags = new ArrayList<>();
        for (DocTree dt : docCommentTree.getFullBody()) {
            if (dt.getKind() != DocTree.Kind.TEXT) {
                JavaDoc.Tag tag = convertTag(docCommentTree, dt);
                tags.add(tag);
            }
            comment.append(dt);
        }
        for (DocTree dt : docComment.tags) {
            JavaDoc.Tag tag = convertTag(docCommentTree, dt);
            tags.add(tag);
            comment.append("\n").append(dt);
        }
        return runtime.newJavaDoc(source, comment.toString(), List.copyOf(tags));
    }

    private JavaDoc.Tag convertTag(DocCommentTree docCommentTree, DocTree docTree) {
        String content = docTree.toString();
        JavaDoc.TagIdentifier tagId = identifier(docTree);
        Info ref = reference(docTree);
        Source srcRef = null;
        Source src = source(docCommentTree, docTree);
        boolean isBlock = docTree instanceof BlockTagTree;
        return runtime.newJavaDocTag(tagId, content, ref, src, srcRef, isBlock);
    }

    private Info reference(DocTree docTree) {
        return null; // FIXME
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
}
