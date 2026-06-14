package org.e2immu.language.java.openjdk;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.jetbrains.annotations.Nullable;
import org.parsers.java.JavaParser;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

public final class SourceCodeScan {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceCodeScan.class);
    private final Runtime runtime;

    public SourceCodeScan(Runtime runtime) {
        this.runtime = runtime;
    }

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

        public Source findEndOfArgumentList(Source sourceOfMethodCallConstructorCall) {
            Map<Object, Object> map = argumentLists.get(sourceOfMethodCallConstructorCall);
            return map == null ? null : (Source) map.get(DetailedSources.END_OF_ARGUMENT_LIST);
        }

        @SuppressWarnings("unchecked")
        public List<Source> findArgumentCommas(Source sourceOfMethodCallConstructorCall) {
            Map<Object, Object> map = argumentLists.get(sourceOfMethodCallConstructorCall);
            if (map == null) return null;
            Object value = map.get(DetailedSources.ARGUMENT_COMMAS);
            return value == null ? null : ((List<Object>) value).stream().map(o -> (Source) o).toList();
        }

    }

    public Result go(CharSequence input, boolean isModule) {
        NavigableMap<Source, List<Comment>> comments = new TreeMap<>();
        NavigableMap<Source, List<Comment>> trailingComments = new TreeMap<>();
        NavigableMap<Source, String> keywords = new TreeMap<>();
        NavigableMap<Source, Map<Object, Object>> argumentLists = new TreeMap<>();
        Result result = new Result(comments, trailingComments, keywords, argumentLists);

        JavaParser p = new JavaParser(input);
        p.setParserTolerant(false);
        if (isModule) {
            p.ModularCompilationUnit();
            Node root = p.rootNode();
            if (root instanceof ModularCompilationUnit mcu) {
                for (Node child : mcu.children()) {
                    switch (child) {
                        case RequiresDirective _, ExportsDirective _, OpensDirective _, UsesDirective _,
                             ProvidesDirective _ -> scanModuleDirective(child, result);
                        default -> {
                        }
                    }
                }
            } else throw new UnsupportedOperationException("? expected module");
        } else {
            handleCompilationUnit(p, result);
        }
        return new Result(Collections.unmodifiableNavigableMap(comments),
                Collections.unmodifiableNavigableMap(trailingComments),
                Collections.unmodifiableNavigableMap(keywords),
                Collections.unmodifiableNavigableMap(argumentLists));
    }

    private void addComments(Result result, Node node) {
        List<Comment> comments = comments(node);
        if (!comments.isEmpty()) {
            result.comments.put(source(node), comments);
        }
    }

    private void addTrailingComments(Result result, Source sourceOwner, Node lastChild) {
        List<Comment> trailingComments = comments(lastChild);
        if (!trailingComments.isEmpty()) {
            result.trailingComments.put(sourceOwner, trailingComments);
        }
    }

    private void handleCompilationUnit(JavaParser p, Result result) {
        CompilationUnit cu = p.CompilationUnit();
        PackageDeclaration packageDeclaration = cu.getPackageDeclaration();
        if (packageDeclaration != null) {
            addComments(result, packageDeclaration);
            Node pkgDeclaration0 = packageDeclaration.getFirst();
            result.keywords.put(source(pkgDeclaration0), pkgDeclaration0.getSource());
        }
        for (ImportDeclaration id : cu.childrenOfType(ImportDeclaration.class)) {
            addComments(result, id);
            result.keywords.put(source(id.getFirst()), id.getFirst().toString());
        }
        Source classSource = null;

        for (Node node : cu) {
            if (node instanceof TypeDeclaration td && !(node instanceof EmptyDeclaration)) {
                scanTypeDeclaration(td, result);
                classSource = source(td);
            }
        }

        Node lastChild = cu.getLastChild();
        if (lastChild != null && lastChild.getType().isEOF() && classSource != null) {
            addTrailingComments(result, classSource, lastChild);
        }
    }

    private void scanModuleDirective(Node md, Result result) {
        addComments(result, md);
        for (Node node : md) {
            switch (node) {
                case KeyWord _, Name _ -> result.keywords.put(source(node), node.getSource());
                case Token t -> {
                    switch (t.getType()) {
                        case REQUIRES, PROVIDES, USES, EXPORTS, OPENS ->
                                result.keywords.put(source(node), node.getSource());
                        default -> {
                        }
                    }
                }
                default -> {
                }
            }
        }
    }

    private void scanTypeDeclaration(TypeDeclaration td, Result result) {
        addComments(result, td);

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
            } else if (node instanceof ClassOrInterfaceBody || node instanceof EnumBody) {
                for (Node node2 : node.children()) {
                    String string2 = node2.getSource();
                    switch (node2) {
                        case TypeDeclaration sub -> scanTypeDeclaration(sub, result);
                        case ConstructorDeclaration cd -> scanMethodDeclaration(cd, result);
                        case MethodDeclaration md -> scanMethodDeclaration(md, result);
                        case FieldDeclaration fd -> scanFieldDeclaration(fd, result);
                        default -> {
                        }
                    }
                }
                Node lastChild = node.getLastChild();
                if (lastChild != null) {
                    addTrailingComments(result, source(td), lastChild);
                }
            }
        }
    }

    private void scanFieldDeclaration(Node fd, Result result) {
        addComments(result, fd);
    }

    private void scanMethodDeclaration(Node md, Result result) {
        addComments(result, md);
        for (Node node : md.children()) {
            String string = node.getSource();
            LOGGER.debug("In MD: {}: {}", node.getClass(), limit(string));
            switch (node) {
                case KeyWord _ -> result.keywords.put(source(node), string);
                case FormalParameters fps -> {
                    for (Node param : fps.children()) {
                        if (param instanceof FormalParameter fp) {
                            addComments(result, fp);

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
                }
                case ExplicitConstructorInvocation eci -> scanCodeBlock(eci, result);
                case Statement st -> scanCodeBlock(st, result);
                default -> {
                }
            }
        }
    }

    private static String limit(String s) {
        if (s.length() < 100) return s;
        return s.substring(0, 99) + "...";
    }

    private void scanCodeBlock(Node cb, Result result) {
        LOGGER.debug("Scan {}: {}", cb.getClass(), limit(cb.getSource()));
        visit(cb, child -> {
            if (child instanceof Statement st) {
                addComments(result, st);
                if (st instanceof CodeBlock sub && !sub.isEmpty()) {
                    addTrailingComments(result, source(sub), sub.getLastChild());
                }
            }
            if (child instanceof TypeDeclaration td) {
                scanTypeDeclaration(td, result);
                return false;
            }
            if (child instanceof MethodCall || child instanceof AllocationExpression
                || child instanceof ExplicitConstructorInvocation) {
                scanCall(result, child);
            }
            return true;
        });
    }

    private void scanCall(Result result, Node child) {
        Source source;
        if (child instanceof ExplicitConstructorInvocation) {
            // NOTE: specific code to exclude the ';' because OpenJDK sees an ECI as an expression (method call)
            // rather than a statement.
            source = source(child.getFirst(), child.get(child.size() - 2));
        } else {
            source = source(child);
        }
        LOGGER.debug("*** scan call {}: {}: {}", source.compact2(), child.getClass(), limit(child.getSource()));
        InvocationArguments ia = child.firstChildOfType(InvocationArguments.class);
        if (ia != null) {
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
            LOGGER.debug("*** ... result is {}", argList);
            result.argumentLists.put(source, Map.copyOf(argList));
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


    private void visit(Node node, Predicate<Node> test) {
        if (test.test(node)) {
            for (Node child : node.children()) {
                visit(child, test);
            }
        }
    }

    /*
    This is the only provider of Comment objects. All visitors must use this method.

    Note: we're not using Node.getAllTokens(), because that method recurses down unconditionally
     */
    public List<Comment> comments(Node node) {
        Node.TerminalNode tn = firstTerminal(node);
        if (tn != null) {
            return tn.precedingUnparsedTokens().stream()
                    .map(this::commentsFromTerminal)
                    .filter(Objects::nonNull)
                    .toList();
        }
        return List.of();
    }

    private @Nullable Comment commentsFromTerminal(Node.TerminalNode t) {
        if (t instanceof SingleLineComment slc) {
            return runtime.newSingleLineComment(source(slc), slc.getSource());
        }
        if (t instanceof MultiLineComment multiLineComment) {
            if (multiLineComment.getSource().startsWith("/**")) {
                return null;
            }
            boolean addNewline = true; // FIXME
            return runtime.newMultilineComment(source(multiLineComment), multiLineComment.getSource(),
                    addNewline);
        }
        return null;
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

    public Runtime runtime() {
        return runtime;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SourceCodeScan) obj;
        return Objects.equals(this.runtime, that.runtime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runtime);
    }

    @Override
    public String toString() {
        return "SourceCodeScan[" +
               "runtime=" + runtime + ']';
    }


}
