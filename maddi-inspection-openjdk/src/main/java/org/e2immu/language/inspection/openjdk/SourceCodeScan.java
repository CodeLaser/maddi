package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.parser.java.util.JavaDocParser;
import org.parsers.java.JavaParser;
import org.parsers.java.Node;
import org.parsers.java.ast.CompilationUnit;
import org.parsers.java.ast.MultiLineComment;
import org.parsers.java.ast.PackageDeclaration;
import org.parsers.java.ast.SingleLineComment;

import java.util.*;

public record SourceCodeScan(Runtime runtime) {


    public record Result(NavigableMap<Source, List<Comment>> comments, Map<Source, String> keywords) {
    }

    public Result go(CharSequence input) {
        NavigableMap<Source, List<Comment>> comments = new TreeMap<>();
        NavigableMap<Source, String> keywords = new TreeMap<>();

        JavaParser p = new JavaParser(input);
        p.setParserTolerant(false);

        CompilationUnit cu = p.CompilationUnit();
        PackageDeclaration packageDeclaration = cu.getPackageDeclaration();
        List<Comment> pkgDeclarationComments = comments(packageDeclaration);
        if (!pkgDeclarationComments.isEmpty()) comments.put(source(packageDeclaration), pkgDeclarationComments);
        Node pkgDeclaration0 = packageDeclaration.getFirst();
        keywords.put(source(pkgDeclaration0), pkgDeclaration0.getSource());

        return new Result(Collections.unmodifiableNavigableMap(comments), Collections.unmodifiableNavigableMap(keywords));
    }


    /*
    Note: we're not using Node.getAllTokens(), because that method recurses down unconditionally
     */
    public List<Comment> comments(Node node) {
        return comments(node, null, null, null);
    }

    public List<Comment> comments(Node node, Context context, Info info, Info.Builder<?> infoBuilder) {
        Node.TerminalNode tn = firstTerminal(node);
        if (tn != null) {
            return tn.precedingUnparsedTokens().stream()
                    .map(t -> {
                        if (t instanceof SingleLineComment slc) {
                            return runtime.newSingleLineComment(source(slc), slc.getSource());
                        }
                        if (t instanceof MultiLineComment multiLineComment) {
                            if (multiLineComment.getSource().startsWith("/**")) {
                                return parseJavaDoc(multiLineComment, source(multiLineComment), context, info, infoBuilder);
                            }
                            boolean addNewline = true; // FIXME
                            return runtime.newMultilineComment(source(multiLineComment), multiLineComment.getSource(),
                                    addNewline);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }
        return List.of();
    }

    private Comment parseJavaDoc(MultiLineComment multiLineComment,
                                 Source source,
                                 Context context,
                                 Info info,
                                 Info.Builder<?> infoBuilder) {
        JavaDoc javaDoc = new JavaDocParser(runtime).extractTags(multiLineComment.getSource(), source);
        if (context != null) {
            context.resolver().addJavadoc(info, infoBuilder, context, javaDoc);
        }
        return javaDoc;
    }


    private Node.TerminalNode firstTerminal(Node node) {
        if (node instanceof Node.TerminalNode tn) return tn;
        for (Node child : node) {
            Node.TerminalNode tn = firstTerminal(child);
            if (tn != null) return tn;
        }
        return null;
    }

    /*
    this implementation gives an "imperfect" parent... See e.g. parseBlock: we cannot pass on the parent during
    parsing, because we still have the builder at that point in time.
     */
    public Source source(String index, Node node) {
        return runtime.newParserSource(index, node.getBeginLine(), node.getBeginColumn(), node.getEndLine(),
                node.getEndColumn());
    }

    // meant for detailed sources
    public Source source(Node node) {
        return runtime.newParserSource("", node.getBeginLine(), node.getBeginColumn(),
                node.getEndLine(), node.getEndColumn());
    }

    public Source source(Node beginNode, Node endNodeIncl) {
        return runtime.newParserSource("", beginNode.getBeginLine(), beginNode.getBeginColumn(),
                endNodeIncl.getEndLine(), endNodeIncl.getEndColumn());
    }

    public Source source(String index, Node beginNode, Node endNodeIncl) {
        return runtime.newParserSource(index, beginNode.getBeginLine(), beginNode.getBeginColumn(),
                endNodeIncl.getEndLine(), endNodeIncl.getEndColumn());
    }

    // meant for detailed sources
    public Source source(Node node, int start, int end) {
        Node s = node.get(start);
        Node e = node.get(end);
        return runtime.newParserSource("", s.getBeginLine(), s.getBeginColumn(),
                e.getEndLine(), e.getEndColumn());
    }

}
