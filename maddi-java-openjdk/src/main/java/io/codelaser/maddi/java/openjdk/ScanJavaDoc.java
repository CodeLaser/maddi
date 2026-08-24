package io.codelaser.maddi.java.openjdk;

import com.sun.source.doctree.*;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreeScanner;
import com.sun.tools.javac.tree.DCTree;
import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.runtime.Runtime;

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
                case SeeTree st -> {
                    // G26(b): the reference of `@see a.b.C` is a ReferenceTree, not a TextTree — the
                    // child recursion never prints it, and the tag came out truncated to a bare `@see`
                    // (a doclint compile error once the comment is re-emitted). @throws writes its
                    // exception name explicitly, right above; @see gets the same treatment. A leading
                    // TextTree (e.g. `@see "text"`) still arrives via the recursion.
                    comment.append("@see ");
                    List<? extends DocTree> refs = st.getReference();
                    if (refs != null && !refs.isEmpty() && refs.getFirst() instanceof ReferenceTree rt) {
                        comment.append(rt.getSignature()).append(" ");
                    }
                }
                case ReturnTree _, DeprecatedTree _, SinceTree _, AuthorTree _, VersionTree _,
                     UnknownBlockTagTree _ -> {
                    comment.append("@").append(btt.getTagName()).append(" ");
                }
                default -> {
                    comment.append("@").append(btt.getKind().toString()
                            .toLowerCase().replace("_", "")).append(" ");
                }
            }
        }


        /*
        An EMPTY doc tag is legal input: `{@link }` compiles (javac warns), and so do `@throws` and `@see`
        with nothing after them. javac then hands us a tag whose reference is NULL, and dereferencing it threw
        a NullPointerException out of the whole parse -- one such comment in one file of 23,497 rejected an
        entire 354-source-set Elasticsearch parse, because a parse error anywhere refuses the ParseResult.
        The empty tag carries no information, so the content is empty; it is not an error to report.
         */
        private String content(DocTree docTree) {
            return switch (docTree) {
                case DCTree.DCAuthor a -> a.getName().toString();
                case DCTree.DCThrows t -> t.getExceptionName() == null ? "" : t.getExceptionName().getSignature();
                case DCTree.DCLink l -> l.getReference() == null ? "" : l.getReference().getSignature();
                case DCTree.DCParam p -> p.getName().toString();
                case DCTree.DCSee s -> s.getReference() == null ? "" : s.getReference().stream().map(this::content)
                        .collect(Collectors.joining("; "));
                // ⭐ without a case of its own, {@value #X} fell to the default and its content became the WHOLE
                // tag text -- so `tag.toString()` re-wrapped it into `{@value {@value #X}}`, and no consumer
                // could read the reference out of it. It is a member reference like any other (G30 arm (a):
                // {@value} inlines a constant, so it dangles the moment the constant's neighbour moves).
                case DCTree.DCValue v -> v.getReference() == null ? "" : v.getReference().getSignature();
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
                // the reference's own span, which is also what makes ResolveJavaDoc look at the tag at all
                case DCTree.DCValue v -> v.getReference() == null ? null : source(v.getReference());
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
            // an empty tag's reference is null (see content()); guarding here rather than at each of the four
            // call sites keeps them agreeing by construction
            if (docNode == null) return runtime.noSource();
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
