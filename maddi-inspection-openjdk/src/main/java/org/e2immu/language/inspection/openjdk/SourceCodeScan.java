package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.parser.java.util.JavaDocParser;
import org.parsers.java.JavaParser;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;

import java.util.*;
import java.util.function.Predicate;

public record SourceCodeScan(Runtime runtime) {


    public record Result(NavigableMap<Source, List<Comment>> comments,
                         NavigableMap<Source, List<Comment>> trailingComments,
                         NavigableMap<Source, String> keywords,
                         NavigableMap<Source, Map<Object, Object>> argumentLists) {
        public Source find(String keyword, Source source) {
            Map.Entry<Source, String> entry = keywords.floorEntry(source);
            while (entry != null) {
                if (keyword.equals(entry.getValue())) {
                    return entry.getKey();
                }
                entry = keywords.lowerEntry(entry.getKey());
            }
            throw new UnsupportedOperationException("Cannot find keyword " + keyword);
        }

        public List<Comment> findComments(Source source) {
            return findComments(source, comments);
        }

        public List<Comment> findTrailingComments(Source source) {
            return findComments(source, trailingComments);
        }

        private static List<Comment> findComments(Source source, NavigableMap<Source, List<Comment>> map) {
            Map.Entry<Source, List<Comment>> entry = map.floorEntry(source);
            if (entry == null) return List.of();
            Source s = entry.getKey();
            boolean accept = s.beginLine() == source.beginLine() && s.beginPos() == source.beginPos();
            return accept ? entry.getValue() : List.of();
        }
    }

    public Result go(CharSequence input) {
        NavigableMap<Source, List<Comment>> comments = new TreeMap<>();
        NavigableMap<Source, List<Comment>> trailingComments = new TreeMap<>();
        NavigableMap<Source, String> keywords = new TreeMap<>();
        NavigableMap<Source, Map<Object, Object>> argumentLists = new TreeMap<>();
        Result result = new Result(comments, trailingComments, keywords, argumentLists);

        JavaParser p = new JavaParser(input);
        p.setParserTolerant(false);

        CompilationUnit cu = p.CompilationUnit();
        PackageDeclaration packageDeclaration = cu.getPackageDeclaration();
        if (packageDeclaration != null) {
            List<Comment> pkgDeclarationComments = comments(packageDeclaration);
            if (!pkgDeclarationComments.isEmpty()) comments.put(source(packageDeclaration), pkgDeclarationComments);
            Node pkgDeclaration0 = packageDeclaration.getFirst();
            keywords.put(source(pkgDeclaration0), pkgDeclaration0.getSource());
        }
        for (ImportDeclaration id : cu.childrenOfType(ImportDeclaration.class)) {
            List<Comment> importComments = comments(id);
            if (!importComments.isEmpty()) comments.put(source(id), importComments);
            keywords.put(source(id.getFirst()), id.getFirst().toString());
        }
        Source classSource = null;

        for (Node node : cu) {
            if (node instanceof TypeDeclaration td && !(node instanceof EmptyDeclaration)) {
                scanTypeDeclaration(td, result);
                classSource = source(td);
            }
        }

        Node lastChild = cu.getLastChild();
        if (lastChild.getType().isEOF() && classSource != null) {
            List<Comment> trailingClassComments = comments(lastChild);
            if (!trailingClassComments.isEmpty()) {
                trailingComments.put(classSource, trailingClassComments);
            }
        }

        return new Result(Collections.unmodifiableNavigableMap(comments),
                Collections.unmodifiableNavigableMap(trailingComments),
                Collections.unmodifiableNavigableMap(keywords),
                Collections.unmodifiableNavigableMap(argumentLists));
    }

    private void scanTypeDeclaration(TypeDeclaration td, Result result) {
        List<Comment> classComments = comments(td);
        if (!classComments.isEmpty()) result.comments.put(source(td), classComments);

        for (Node node : td.children()) {
            String string = node.getSource();
            if (node instanceof KeyWord || node instanceof Token && "record".equals(string)) {
                result.keywords.put(source(node), string);
            } else if (node instanceof ExtendsList el) {
                Node extendsKeyword = el.getFirst();
                result.keywords.put(source(extendsKeyword), extendsKeyword.getSource());
            } else if (node instanceof ImplementsList il) {
                Node implementsKeyword = il.getFirst();
                result.keywords.put(source(implementsKeyword), implementsKeyword.getSource());
            } else if (node instanceof ClassOrInterfaceBody body) {
                for (Node node2 : body.children()) {
                    if (node2 instanceof TypeDeclaration sub) {
                        scanTypeDeclaration(sub, result);
                    }
                    if (node2 instanceof MethodDeclaration md) {
                        scanMethodDeclaration(md, result);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void mergeList(Map<Object, Object> map, Object key, Object element) {
        List<Object> list = (List<Object>) map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(element);
    }

    private void scanMethodDeclaration(MethodDeclaration md, Result result) {
        List<Comment> methodComments = comments(md);
        if (!methodComments.isEmpty()) result.comments.put(source(md), methodComments);
        for (Node node : md.children()) {
            String string = node.getSource();
            if (node instanceof KeyWord || node instanceof Token && "record".equals(string)) {
                result.keywords.put(source(node), string);
            } else if (node instanceof FormalParameters fps) {
                for (Node param : fps.children()) {
                    if (param instanceof FormalParameter fp) {
                        List<Comment> fpComments = comments(fp);
                        if (!fpComments.isEmpty()) result.comments.put(source(fp), fpComments);

                        Map<Object, Object> commaMap = new HashMap<>();
                        Node preceding = param.previousSibling();
                        if (preceding != null && preceding.getType() == Token.TokenType.COMMA) {
                            commaMap.put(DetailedSources.PRECEDING_COMMA, source(preceding));
                        }
                        Node succeeding = param.nextSibling();
                        if (succeeding != null && succeeding.getType() == Token.TokenType.COMMA) {
                            commaMap.put(DetailedSources.SUCCEEDING_COMMA, source(succeeding));
                        }
                        Source formalSource = source(fp);
                        result.argumentLists.put(formalSource, Map.copyOf(commaMap));
                    } else if (param instanceof Delimiter d && d.getType() == Token.TokenType.RPAREN) {
                        Source methodSource = source(md);
                        result.argumentLists.put(methodSource, Map.of(DetailedSources.END_OF_PARAMETER_LIST, source(d)));
                    }
                }

            } else if (node instanceof CodeBlock cb) {
                scanCodeBlock(cb, result);
            }
        }
    }

    private void scanCodeBlock(CodeBlock cb, Result result) {
        visit(cb, child -> {
            if (child instanceof Statement st) {
                List<Comment> statementComments = comments(st);
                if (!statementComments.isEmpty()) {
                    result.comments.put(source(st), statementComments);
                }
                if (st instanceof CodeBlock sub) {
                    List<Comment> trailing = comments(sub.getLastChild());
                    if (!trailing.isEmpty()) {
                        result.trailingComments.put(source(sub), trailing);
                    }
                }
            }
            if (child instanceof TypeDeclaration td) {
                scanTypeDeclaration(td, result);
                return false;
            }
            if (child instanceof MethodCall || child instanceof AllocationExpression) {
                InvocationArguments ia = child.firstChildOfType(InvocationArguments.class);
                Map<Object, Object> argList = new HashMap<>();
                for (Node node : ia.children()) {
                    if (node instanceof Delimiter d) {
                        if (d.getType() == Token.TokenType.RPAREN) {
                            argList.put(DetailedSources.END_OF_ARGUMENT_LIST, source(d));
                        } else if (d.getType() == Token.TokenType.COMMA) {
                            mergeList(argList, DetailedSources.ARGUMENT_COMMAS, source(d));
                        }
                    }
                }
                result.argumentLists.put(source(child), Map.copyOf(argList));
            }
            return true;
        });
    }

    private void visit(Node node, Predicate<Node> test) {
        if (test.test(node)) {
            for (Node child : node.children()) {
                visit(child, test);
            }
        }
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
