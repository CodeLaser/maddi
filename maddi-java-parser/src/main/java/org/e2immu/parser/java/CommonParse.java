package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.RecordPattern;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.SwitchEntry;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.ForwardType;
import org.e2immu.parser.java.util.JavaDocParser;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;
import org.parsers.java.ast.MultiLineComment;
import org.parsers.java.ast.SingleLineComment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class CommonParse {
    protected final Runtime runtime;
    protected final Parsers parsers;

    protected CommonParse(Runtime runtime, Parsers parsers) {
        this.runtime = runtime;
        this.parsers = parsers;
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
                            return runtime.newMultilineComment(source(multiLineComment), multiLineComment.getSource());
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

    // meant for detailed sources
    public Source source(Node node, int start, int end) {
        Node s = node.get(start);
        Node e = node.get(end);
        return runtime.newParserSource("", s.getBeginLine(), s.getBeginColumn(),
                e.getEndLine(), e.getEndColumn());
    }

    // also called for local type declarations
    protected Node handleTypeModifiers(TypeDeclaration td, TypeInfo typeInfo, boolean addDetailedSources) {
        DetailedSources.Builder detailedSourcesBuilder = addDetailedSources ? runtime.newDetailedSourcesBuilder() : null;

        Identifier identifier = null;
        Node sub = null;
        TypeNature typeNature = null;
        List<TypeModifier> typeModifiers = new ArrayList<>();
        for (Node child : td.children()) {
            if (child instanceof Modifiers modifiers) {
                for (Node child2 : modifiers) {
                    if (child2 instanceof KeyWord keyWord) {
                        TypeNature tn = getTypeNature(td, keyWord.getType());
                        if (tn != null) {
                            assert typeNature == null;
                            typeNature = tn;
                        }
                        TypeModifier tm = getTypeModifier(keyWord.getType());
                        if (tm != null) {
                            typeModifiers.add(tm);
                            if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(tm, source(keyWord));
                        }
                    }
                }
            } else if (child instanceof Identifier id) {
                identifier = id;
            } else if (child instanceof ClassOrInterfaceBody
                       || child instanceof AnnotationTypeBody
                       || child instanceof RecordBody
                       || child instanceof EnumBody) {
                sub = child;
                break;
            } else if (child instanceof Token keyWord) {
                TypeNature tn = getTypeNature(td, keyWord.getType());
                if (tn != null) {
                    assert typeNature == null;
                    typeNature = tn;
                    if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(typeNature, source(keyWord));
                }
                TypeModifier tm = getTypeModifier(keyWord.getType());
                if (tm != null) {
                    typeModifiers.add(tm);
                    if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(tm, source(keyWord));
                }
            }
        }
        assert identifier != null;

        Source source = source(td);
        typeInfo.builder()
                .setTypeNature(typeNature)
                .setSource(detailedSourcesBuilder == null ? source
                        : source.withDetailedSources(detailedSourcesBuilder.build()));
        typeModifiers.forEach(typeInfo.builder()::addTypeModifier);
        return sub;
    }


    private TypeNature getTypeNature(TypeDeclaration td, Token.TokenType tt) {
        return switch (tt) {
            case CLASS -> runtime.typeNatureClass();
            case INTERFACE -> td instanceof AnnotationTypeDeclaration
                    ? runtime.typeNatureAnnotation() : runtime.typeNatureInterface();
            case ENUM -> runtime.typeNatureEnum();
            case RECORD -> runtime.typeNatureRecord();
            default -> null;
        };
    }

    private TypeModifier getTypeModifier(Token.TokenType tt) {
        return switch (tt) {
            case PUBLIC -> runtime.typeModifierPublic();
            case PRIVATE -> runtime.typeModifierPrivate();
            case PROTECTED -> runtime.typeModifierProtected();
            case FINAL -> runtime.typeModifierFinal();
            case SEALED -> runtime.typeModifierSealed();
            case ABSTRACT -> runtime.typeModifierAbstract();
            case NON_SEALED -> runtime.typeModifierNonSealed();
            case STATIC -> runtime.typeModifierStatic();
            default -> null;
        };
    }


    protected TypeParameter parseTypeParameterDoNotInspect(Node node, Info owner, int typeParameterIndex) {
        String name;
        if (node instanceof Identifier) {
            name = node.getSource();
        } else if (node instanceof org.parsers.java.ast.TypeParameter tp) {
            Identifier identifier = tp.firstChildOfType(Identifier.class);
            name = identifier.getSource();
        } else throw new UnsupportedOperationException("Expected Identifier or TypeParameter");
        return runtime.newTypeParameter(typeParameterIndex, name, owner);
    }


    private record ParseTypeParameterResult(List<Node> unparsedTypeBounds, boolean doCommit,
                                            TypeParameter typeParameter,
                                            Source source,
                                            DetailedSources.Builder dsb) {
    }

    /*
    We have created the type parameter objects by now, but have not yet parsed and resolved their
    annotations, type bounds, source objects.

    We will commit the type parameter if it does not have annotations.
    If it has annotations, the resolver will do the commit.
     */
    protected void parseAndResolveTypeParameterBounds(List<Node> typeParametersToParse,
                                                      List<TypeParameter> typeParameters,
                                                      Context contextWithTP) {
        List<ParseTypeParameterResult> results = typeParameters.stream()
                .map(tp -> parseTypeParameter(typeParametersToParse.get(tp.getIndex()), tp, contextWithTP))
                .toList();
        int infiniteLoopProtection = typeParametersToParse.size() + 2;
        boolean resolved = false;
        while (infiniteLoopProtection >= 0 && !resolved) {
            resolved = true;
            for (ParseTypeParameterResult result : results) {
                if (result.unparsedTypeBounds.isEmpty()) {
                    result.typeParameter.builder().setTypeBounds(List.of());
                } else {
                    List<ParameterizedType> typeBounds = parseTypeBounds(result.unparsedTypeBounds, contextWithTP,
                            result.dsb);
                    if (typeBounds != null) {
                        result.typeParameter.builder().setTypeBounds(typeBounds);
                    } else {
                        resolved = false;
                    }
                }
            }
            infiniteLoopProtection--;
        }
        if (infiniteLoopProtection <= 0) {
            throw new UnsupportedOperationException("Hit infinite loop protection");
        }
        for (TypeParameter tp : typeParameters) {
            assert tp.typeBoundsAreSet();
        }
        for (ParseTypeParameterResult result : results) {
            result.typeParameter.builder().setSource(result.dsb == null ? result.source
                    : result.source.withDetailedSources(result.dsb.build()));
            if (result.doCommit) result.typeParameter.builder().commit();
        }
    }

    private List<ParameterizedType> parseTypeBounds(List<Node> unparsedTypeBounds, Context context,
                                                    DetailedSources.Builder dsb) {
        List<ParameterizedType> res = new ArrayList<>(unparsedTypeBounds.size());
        for (Node typeBound : unparsedTypeBounds) {
            ParameterizedType parsedTypeBound = parsers.parseType().parse(context, typeBound, false, null, dsb);
            if (parsedTypeBound != null) {
                res.add(parsedTypeBound);
            } else {
                return null;
            }
        }
        return res;
    }


    // code copied from ParseTypeDeclaration
    private ParseTypeParameterResult parseTypeParameter(Node node, TypeParameter typeParameter, Context context) {
        boolean doCommit;
        if (node instanceof org.parsers.java.ast.TypeParameter tp) {
            List<Annotation> annotations = tp.childrenOfType(Annotation.class);
            if (annotations.isEmpty()) {
                doCommit = true;
            } else {
                doCommit = !parseAnnotations(context, typeParameter.builder(), annotations);
            }
        } else {
            doCommit = true;
        }
        typeParameter.builder().addComments(comments(node));
        Source source = source(node);
        DetailedSources.Builder dsb = context.newDetailedSourcesBuilder();
        List<Node> unparsedTypeBounds = new ArrayList<>();
        if (node instanceof org.parsers.java.ast.TypeParameter tp) {
            TypeBound tb = tp.firstChildOfType(TypeBound.class);
            if (tb != null) {
                int i = 1;
                while (i < tb.size()) {
                    unparsedTypeBounds.add(tb.get(i));
                    i += 2; // skip the &
                }
            }
        }
        return new ParseTypeParameterResult(unparsedTypeBounds, doCommit, typeParameter, source, dsb);
    }

    // returns true when there are annotations to be resolved
    protected boolean parseAnnotations(Context context, Info.Builder<?> builder, List<Annotation> annotations) {
        int annotationIndex = 0;
        boolean toBeResolved = false;
        for (Annotation annotation : annotations) {
            ParseAnnotationExpression.Result result = parsers.parseAnnotationExpression().parse(context, builder,
                    annotation, annotationIndex++);
            builder.addAnnotation(result.annotationExpression());
            toBeResolved |= result.toBeResolved();
        }
        return toBeResolved;
    }


    protected void parseNewSwitchLabel(String index,
                                       NewSwitchLabel nsl,
                                       Context context,
                                       SwitchEntry.Builder entryBuilder,
                                       ForwardType selectorTypeFwd,
                                       Context enumContext) {
        Context newContext = Objects.requireNonNullElse(enumContext, context);
        org.e2immu.language.cst.api.expression.Expression whenExpression = runtime.newEmptyExpression();
        List<org.e2immu.language.cst.api.expression.Expression> conditions = new ArrayList<>();
        int j = 0;
        while (j < nsl.size()) {
            Node node = nsl.get(j);
            switch (node) {
                case TypePattern lvd -> {
                    RecordPattern recordPattern = parsers.parseRecordPattern()
                            .parseLocalVariableDeclaration(newContext, lvd, 0, null);
                    entryBuilder.setPatternVariable(recordPattern);
                }
                case org.parsers.java.ast.RecordPattern rp -> {
                    RecordPattern recordPattern = parsers.parseRecordPattern().parseRecordPattern(newContext, rp);
                    entryBuilder.setPatternVariable(recordPattern);
                }
                case WhenClause whenClause -> {
                    ForwardType booleanTypeFwd = newContext.newForwardType(runtime.booleanParameterizedType());
                    whenExpression = parsers.parseExpression().parse(newContext, index, booleanTypeFwd, whenClause.get(1));
                }
                case KeyWord kw -> {
                    if (Token.TokenType._DEFAULT.equals(kw.getType())) {
                        conditions.add(runtime.newEmptyExpression());
                    } // else: ignore: case, (, ), ...
                }
                default -> {
                    Expression c = parsers.parseExpression().parse(newContext, index, selectorTypeFwd, node);
                    conditions.add(c);
                }
            }
            Node next = nsl.get(j + 1);
            if (Token.TokenType.COMMA.equals(next.getType())) {
                j += 2;
            } else if (Token.TokenType.LAMBDA.equals(next.getType())) {
                break;
            } else {
                j++;
            }
        }
        entryBuilder.addConditions(conditions);
        entryBuilder.setWhenExpression(whenExpression);
    }


    // code structurally similar to code in ParseType.parseObjectType
    protected List<DetailedSources.Builder.TypeInfoSource> computeTypeInfoSources(List<? extends NamedType> nts, Node node) {
        List<DetailedSources.Builder.TypeInfoSource> list = new ArrayList<>(nts.size());
        int i = nts.size() - 2; // last one is the type itself, not a qualified type
        int j = node.size() - 3; // last one is the simple name of the type itself, then a delimiter
        while (i >= 0) {
            TypeInfo typeInfo = (TypeInfo) nts.get(i);
            Source source = source(node, 0, j);
            list.add(new DetailedSources.Builder.TypeInfoSource(typeInfo, source));
            if (typeInfo.isPrimaryType()) break;
            i--;
            j -= 2;
        }
        return List.copyOf(list);
    }

    protected Source sourceOfPrefix(Node node, int qualifications) {
        int i = node.size() - (1 + 2 * qualifications);
        if (i > 2 && node.get(i) instanceof Identifier
            && node.get(i - 1) instanceof Delimiter d && d.getType().equals(Token.TokenType.DOT)) {
            return source(node, 0, i - 2);
        }
        return null;
    }

    protected MethodModifier methodModifier(KeyWord keyWord) {
        return switch (keyWord.getType()) {
            case FINAL -> runtime.methodModifierFinal();
            case PRIVATE -> runtime.methodModifierPrivate();
            case PROTECTED -> runtime.methodModifierProtected();
            case PUBLIC -> runtime.methodModifierPublic();
            case STATIC -> runtime.methodModifierStatic();
            case SYNCHRONIZED -> runtime.methodModifierSynchronized();
            case ABSTRACT -> runtime.methodModifierAbstract();
            case _DEFAULT -> runtime.methodModifierDefault();
            default -> throw new UnsupportedOperationException("Have " + keyWord.getType());
        };
    }
}
