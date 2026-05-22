package org.e2immu.language.java.openjdk;

import com.sun.source.doctree.*;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.runtime.Runtime;

import java.util.List;

public record ScanJavaDoc(Runtime runtime) {

    public JavaDoc scan(DocCommentTree docCommentTree, SourceProvider sourceProvider) {
        String comment = "";
        Source source = runtime.noSource();
        List<JavaDoc.Tag> tags = docCommentTree.getBlockTags().stream()
                .map(this::convertTag)
                .toList();
        return runtime.newJavaDoc(source, comment, tags);
    }

    private JavaDoc.Tag convertTag(DocTree docTree) {
        String content = docTree.toString();
        JavaDoc.TagIdentifier tagId = identifier(docTree);
        Info ref = null;
        Source srcRef = null;
        Source src = null;
        boolean isBlock = docTree instanceof BlockTagTree;
        return runtime.newJavaDocTag(tagId, content, ref, srcRef, src, isBlock);
    }

    private JavaDoc.TagIdentifier identifier(DocTree docTree) {
        return JavaDoc.TagIdentifier.valueOf(docTree.getKind().name().toUpperCase());
    }
}
