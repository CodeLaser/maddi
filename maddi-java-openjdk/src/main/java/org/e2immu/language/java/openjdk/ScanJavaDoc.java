package org.e2immu.language.java.openjdk;

import com.sun.source.doctree.*;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreeScanner;
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
        MyScanner myScanner = new MyScanner(docCommentTree);
        Source source = myScanner.source(docCommentTree);
        myScanner.scan(docCommentTree, null);
        return runtime.newJavaDoc(source, myScanner.comment.toString(), List.copyOf(myScanner.tags));
    }

    class MyScanner extends DocTreeScanner<Void, Void> {
        final DocCommentTree docCommentTree;

        StringBuilder comment = new StringBuilder();
        List<JavaDoc.Tag> tags = new ArrayList<>();
        // the following 2 are needed to compute the gap between the initial text, and the block tags
        int countBlockTags;
        int lastTextLine;

        MyScanner(DocCommentTree docCommentTree) {
            this.docCommentTree = docCommentTree;
        }

        @Override
        public Void scan(DocTree node, Void unused) {
            switch (node) {
                case null -> {
                }
                case BlockTagTree btt -> {
                    if (countBlockTags == 0 && !comment.isEmpty()) {
                        Source source = source(node);
                        int diff = source.endLine() - lastTextLine;
                        comment.append("\n".repeat(diff));
                    }
                    ++countBlockTags;
                    JavaDoc.Tag tag = convertTag(node);
                    if (tag != null) tags.add(tag);
                    appendBlockTagInfo(btt, comment);
                    // Recurse into children to pick up text via TextTree case
                    super.scan(node, unused);
                    comment.append("\n");
                }
                case InlineTagTree _ -> {
                    JavaDoc.Tag tag = convertTag(node);
                    if (tag != null) tags.add(tag);
                    comment.append(node); // e.g. "{@link Foo}"
                }
                case TextTree tt -> {
                    comment.append(tt.getBody());
                    Source source = source(node);
                    lastTextLine = source.endLine();
                }
                default -> super.scan(node, unused);
            }
            return null;
        }

        private void appendBlockTagInfo(BlockTagTree btt, StringBuilder comment) {
            switch (btt) {
                case ParamTree pt -> {
                    comment.append("@param ");
                    if (pt.isTypeParameter()) comment.append("<");
                    comment.append(pt.getName());
                    if (pt.isTypeParameter()) comment.append(">");
                    if (!pt.getDescription().isEmpty()) comment.append(" ");
                }
                case ThrowsTree tt -> {
                    comment.append("@throws ");
                    comment.append(tt.getExceptionName());
                    if (!tt.getDescription().isEmpty()) comment.append(" ");
                }
                case ReturnTree _, DeprecatedTree _, SinceTree _, AuthorTree _, VersionTree _, SeeTree _,
                     UnknownBlockTagTree _ -> {
                    comment.append("@").append(btt.getTagName()).append(" ");
                }
                default -> {
                    comment.append("@").append(btt.getKind().toString()
                            .toLowerCase().replace("_", "")).append(" ");
                }
            }
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

        private JavaDoc.Tag convertTag(DocTree docTree) {
            String content = content(docTree);
            JavaDoc.TagIdentifier tagId = identifier(docTree);
            if (tagId == null) return null;
            Source srcRef = switch (docTree) {
                case DCTree.DCParam p -> source(p.name);
                case DCTree.DCLink l -> source(l.getReference());
                case DCTree.DCThrows t -> source(t.getExceptionName());
                case DCTree.DCSee s -> sourceOfList(s.getReference());
                default -> null;
            };
            Source src = source(docTree);
            boolean isBlock = docTree instanceof BlockTagTree;
            return runtime.newJavaDocTag(tagId, content, null, src, srcRef, isBlock);
        }

        private Source sourceOfList(List<? extends DocTree> reference) {
            if (reference != null) {
                for (DocTree dt : reference) {
                    if (dt instanceof DCTree.DCReference) {
                        return source(reference.getFirst());
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

        private Source source(DocTree docNode) {
            long startPos = docSourcePositions.getStartPosition(compilationUnitTree, docCommentTree, docNode);
            long endPos = docSourcePositions.getEndPosition(compilationUnitTree, docCommentTree, docNode);
            if (startPos == Diagnostic.NOPOS) return runtime.noSource(); // no position available
            long startLine = lineMap.getLineNumber(startPos);
            long startCol = lineMap.getColumnNumber(startPos);
            long endLine = lineMap.getLineNumber(endPos);
            long endCol = lineMap.getColumnNumber(endPos) - 1; // inclusive
            return runtime().newParserSource("-", (int) startLine, (int) startCol, (int) endLine, (int) endCol);
        }
    }
}
