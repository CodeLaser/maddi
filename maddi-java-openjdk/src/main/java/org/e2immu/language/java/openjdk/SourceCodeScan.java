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
import java.util.stream.Stream;

public final class SourceCodeScan {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceCodeScan.class);
    private final Runtime runtime;
    private final Set<Source> seen = new HashSet<>();
    private final NavigableMap<Source, List<Comment>> comments = new TreeMap<>();
    private final NavigableMap<Source, List<Comment>> trailingComments = new TreeMap<>();
    private final NavigableMap<Source, String> keywords = new TreeMap<>();
    private final NavigableMap<Source, Map<Object, Object>> argumentLists = new TreeMap<>();

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
            if (entry == null) {
                if (!map.isEmpty()) {
                    // see TestComments; javac's source of the whole compilation unit differs from the parser's.
                    // javac does not include the trailing comments, while the parser does.
                    entry = map.firstEntry();
                } else {
                    return List.of();
                }
            }
            Source s = entry.getKey();
            boolean accept = s.beginLine() == source.beginLine() && s.beginPos() == source.beginPos();
            return accept ? entry.getValue() : List.of();
        }

        public Source findEndOfArgumentList(Source sourceOfMethodCallConstructorCall) {
            Map<Object, Object> map = argumentLists.get(sourceOfMethodCallConstructorCall);
            return map == null ? null : (Source) map.get(DetailedSources.END_OF_ARGUMENT_LIST);
        }

        // keyed by the source of the method/constructor declaration
        public Source findEndOfParameterList(Source sourceOfMethodOrConstructor) {
            Map<Object, Object> map = argumentLists.get(sourceOfMethodOrConstructor);
            return map == null ? null : (Source) map.get(DetailedSources.END_OF_PARAMETER_LIST);
        }

        // keyed by the source of a list element: a formal parameter, type parameter, or field declarator
        public Source findPrecedingComma(Source elementSource) {
            Map<Object, Object> map = argumentLists.get(elementSource);
            return map == null ? null : (Source) map.get(DetailedSources.PRECEDING_COMMA);
        }

        public Source findSucceedingComma(Source elementSource) {
            Map<Object, Object> map = argumentLists.get(elementSource);
            return map == null ? null : (Source) map.get(DetailedSources.SUCCEEDING_COMMA);
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
        JavaParser p = new JavaParser(input);
        p.setParserTolerant(false);
        if (isModule) {
            p.ModularCompilationUnit();
            Node root = p.rootNode();
            if (root instanceof ModularCompilationUnit mcu) {
                for (Node child : mcu.children()) {
                    switch (child) {
                        case RequiresDirective _, ExportsDirective _, OpensDirective _, UsesDirective _,
                             ProvidesDirective _ -> scanModuleDirective(child);
                        default -> {
                        }
                    }
                }
            } else throw new UnsupportedOperationException("? expected module");
        } else {
            handleCompilationUnit(p);
        }
        return new Result(Collections.unmodifiableNavigableMap(comments),
                Collections.unmodifiableNavigableMap(trailingComments),
                Collections.unmodifiableNavigableMap(keywords),
                Collections.unmodifiableNavigableMap(argumentLists));
    }

    private void handleCompilationUnit(JavaParser p) {
        CompilationUnit cu = p.CompilationUnit();
        PackageDeclaration packageDeclaration = cu.getPackageDeclaration();
        if (packageDeclaration != null) {
            addComments(packageDeclaration, false);
            Node pkgDeclaration0 = packageDeclaration.getFirst();
            keywords.put(source(pkgDeclaration0), pkgDeclaration0.getSource());
        }
        for (ImportDeclaration id : cu.childrenOfType(ImportDeclaration.class)) {
            addComments(id, false);
            keywords.put(source(id.getFirst()), id.getFirst().toString());
            if (id.get(1) instanceof KeyWord kwStatic) {
                keywords.put(source(kwStatic), kwStatic.toString());
            }
        }
        for (Node node : cu) {
            if (node instanceof TypeDeclaration td && !(node instanceof EmptyDeclaration)) {
                scanTypeDeclaration(td);
            }
        }

        Node lastChild = cu.getLastChild();
        if (lastChild != null && lastChild.getType().isEOF()) {
            addTrailingComments(source(cu), lastChild);
        }
    }

    private void scanModuleDirective(Node md) {
        addComments(md, true);
        for (Node node : md) {
            switch (node) {
                case KeyWord _, Name _ -> keywords.put(source(node), node.getSource());
                case Token t -> {
                    switch (t.getType()) {
                        case REQUIRES, PROVIDES, USES, EXPORTS, OPENS -> keywords.put(source(node), node.getSource());
                        default -> {
                        }
                    }
                }
                default -> {
                }
            }
        }
    }

    private void scanTypeDeclaration(TypeDeclaration td) {
        addComments(td, false);

        for (Node node : td.children()) {
            String string = node.getSource();
            switch (node) {
                case Modifiers modifiers -> scanModifiers(modifiers);
                case KeyWord _ -> keywords.put(source(node), string);
                case Token _ when "record".equals(string) -> keywords.put(source(node), string);
                case ExtendsList el -> {
                    Node extendsKeyword = el.getFirst();
                    keywords.put(source(extendsKeyword), extendsKeyword.getSource());
                }
                case ImplementsList il -> {
                    Node implementsKeyword = il.getFirst();
                    keywords.put(source(implementsKeyword), implementsKeyword.getSource());
                }
                case PermitsList pl -> {
                    Node permitsKeyword = pl.getFirst();
                    keywords.put(source(permitsKeyword), permitsKeyword.getSource());
                }
                case TypeParameters tps -> {
                    scanTypeParameters(tps);
                }
                case ClassOrInterfaceBody _, EnumBody _, AnnotationTypeBody _ -> {
                    for (Node node2 : node.children()) {
                        String string2 = node2.getSource();
                        switch (node2) {
                            case TypeDeclaration sub -> scanTypeDeclaration(sub);
                            case ConstructorDeclaration cd -> scanMethodDeclaration(cd);
                            case MethodDeclaration _, AnnotationMethodDeclaration _ -> scanMethodDeclaration(node2);
                            case FieldDeclaration fd -> scanFieldDeclaration(fd);
                            default -> {
                            }
                        }
                    }
                    Node lastChild = node.getLastChild();
                    if (lastChild != null) {
                        addTrailingComments(source(td), lastChild);
                    }
                }
                default -> {
                }
            }
        }
    }

    private void scanModifiers(Modifiers modifiers) {
        for (Node node : modifiers.children()) {
            String string = node.getSource();
            switch (node) {
                case Annotation _ -> scanAnnotation(node);
                case KeyWord _ -> keywords.put(source(node), string);
                default -> {
                }
            }
        }
    }

    private void scanAnnotation(Node node) {
        // what to do?
    }

    private void scanTypeParameters(Node tps) {
        for (Node node : tps.children()) {
            String string = node.getSource();
            if (node instanceof TypeParameter || node instanceof Identifier) {
                addComments(node, true);
                Map<Object, Object> commaMap = new HashMap<>();
                Node preceding = node.previousSibling();
                if (preceding != null && preceding.getType() == Token.TokenType.COMMA) {
                    commaMap.put(DetailedSources.PRECEDING_COMMA, source(preceding));
                }
                Node succeeding = node.nextSibling();
                if (succeeding != null && succeeding.getType() == Token.TokenType.COMMA) {
                    commaMap.put(DetailedSources.SUCCEEDING_COMMA, source(succeeding));
                }
                Source source = source(node);
                argumentLists.put(source, Map.copyOf(commaMap));
            }
        }
    }


    private void scanFieldDeclaration(Node fd) {
        addComments(fd, true);

        for (Node node : fd.children()) {
            String string = node.getSource();
            if (node instanceof VariableDeclarator vd) {
                Map<Object, Object> commaMap = new HashMap<>();
                Node preceding = vd.previousSibling();
                if (preceding != null && preceding.getType() == Token.TokenType.COMMA) {
                    commaMap.put(DetailedSources.PRECEDING_COMMA, source(preceding));
                }
                Node succeeding = vd.nextSibling();
                if (succeeding != null && succeeding.getType() == Token.TokenType.COMMA) {
                    commaMap.put(DetailedSources.SUCCEEDING_COMMA, source(succeeding));
                }
                Source source = source(vd);
                argumentLists.put(source, Map.copyOf(commaMap));
                for (Node n : vd.children()) {
                    if (n instanceof Identifier) {
                        Map<Object, Object> eqMap = new HashMap<>();
                        Node succeedingEq = n.nextSibling();
                        if (succeedingEq != null && succeedingEq.getType() == Token.TokenType.ASSIGN) {
                            eqMap.put(DetailedSources.SUCCEEDING_EQUALS, source(succeedingEq));
                            Source sourceId = source(n);
                            argumentLists.put(sourceId, Map.copyOf(eqMap));
                        }
                    }
                }
            }
        }
    }

    private void scanMethodDeclaration(Node md) {
        addComments(md, false);
        for (Node node : md.children()) {
            String string = node.getSource();
            LOGGER.debug("In MD: {}: {}", node.getClass(), limit(string));
            switch (node) {
                case DefaultValue _ -> {
                    if (node.getFirst() instanceof KeyWord kw) {
                        keywords.put(source(kw), kw.getSource());
                    }
                }
                case KeyWord _ -> keywords.put(source(node), string);
                case TypeParameters tps -> scanTypeParameters(tps);
                case FormalParameters fps -> {
                    for (Node param : fps.children()) {
                        if (param instanceof FormalParameter fp) {
                            addComments(fp, true);

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
                            argumentLists.put(formalSource, Map.copyOf(commaMap));
                        } else if (param instanceof Delimiter d && d.getType() == Token.TokenType.RPAREN) {
                            Source methodSource = source(md);
                            argumentLists.put(methodSource, Map.of(DetailedSources.END_OF_PARAMETER_LIST, source(d)));
                        }
                    }
                }
                case ExplicitConstructorInvocation eci -> scanCodeBlock(eci);
                case Statement st -> scanCodeBlock(st);
                default -> {
                }
            }
        }
    }

    private static String limit(String s) {
        if (s.length() < 100) return s;
        return s.substring(0, 99) + "...";
    }

    private void scanCodeBlock(Node cb) {
        LOGGER.debug("Scan {}: {}", cb.getClass(), limit(cb.getSource()));
        visit(cb, child -> {
            if (child instanceof Statement st) {
                addComments(st, true);
                if (st instanceof CodeBlock sub && !sub.isEmpty()) {
                    addTrailingComments(source(sub), sub.getLastChild());
                }
            }
            if (child instanceof TypeDeclaration td) {
                scanTypeDeclaration(td);
                return false;
            }
            if (child instanceof MethodCall || child instanceof AllocationExpression
                || child instanceof ExplicitConstructorInvocation) {
                scanCall(child);
            }
            return true;
        });
    }

    private void scanCall(Node child) {
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
            argumentLists.put(source, Map.copyOf(argList));
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
    All visitors must use this method.

    2 situations

    regular:

    // comment about max
    call(MAX)
    // comment about min
    call(MIN)

     alternative:

     call(MAX); // comment about max
     call(MIN); // (potentially trailing) comment about min
     */

    private void addComments(Node node, boolean lookahead) {
        Source source = source(node);
        if (lookahead) {
            Node nextSibling = node.nextSibling();
            if (nextSibling != null) {
                List<Comment> comments = comments(nextSibling)
                        .filter(c -> c.source().endLine() == c.source().beginLine()
                                     && c.source().beginLine() == source.endLine())
                        .filter(c -> seen.add(c.source()))
                        .toList();
                if (!comments.isEmpty()) {
                    List<Comment> prev = this.comments.put(source, comments);
                    assert prev == null;
                }
            }
        }
        List<Comment> comments = comments(node).filter(c -> seen.add(c.source())).toList();
        if (!comments.isEmpty()) {
            this.comments.merge(source, comments,
                    // note the order: the current one is the lookahead, it must come last
                    (c1, c2) -> Stream.concat(c2.stream(), c1.stream()).toList());
        }
    }

    private void addTrailingComments(Source sourceOwner, Node lastChild) {
        List<Comment> trailingComments = comments(lastChild).filter(c -> seen.add(c.source())).toList();
        if (!trailingComments.isEmpty()) {
            List<Comment> prev = this.trailingComments.put(sourceOwner, trailingComments);
            assert prev == null;
        }
    }

    /*
    This is the only provider of Comment objects.

    Note: we're not using Node.getAllTokens(), because that method recurses down unconditionally
     */
    private Stream<Comment> comments(Node node) {
        Node.TerminalNode tn = firstTerminal(node);
        if (tn != null) {
            return tn.precedingUnparsedTokens().stream()
                    .map(this::commentsFromTerminal)
                    .filter(Objects::nonNull);
        }
        return Stream.empty();
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
