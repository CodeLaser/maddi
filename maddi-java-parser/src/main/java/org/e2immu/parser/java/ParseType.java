package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.Wildcard;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.Summary;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ParseType extends CommonParse {

    public ParseType(Runtime runtime) {
        super(runtime, null);
    }

    /*
    KeyWord
    Identifier (1 child, e.g. nodes is a ReturnType)
    ObjectType (Identifier) (Identifier, TypeArguments)
    PrimitiveType (Primitive)
    ObjectType + Delimiter + Delimiter (3 children, nodes is a ReferenceType)

     */
    public ParameterizedType parse(Context context, List<Node> nodes, DetailedSources.Builder detailedSourcesBuilder) {
        return parse(context, nodes, true, null, detailedSourcesBuilder);
    }

    public ParameterizedType parse(Context context, List<Node> nodes, boolean complain, Node varargs,
                                   DetailedSources.Builder detailedSourcesBuilder) {
        Token.TokenType tt;
        ParameterizedType pt;
        if (nodes instanceof ReturnType rt) {
            return parse(context, rt.getFirst(), complain, varargs, detailedSourcesBuilder);
        }
        Arrays arrays = countArrays(nodes, varargs);
        Source source;
        switch (nodes) {
            case ClassLiteral cl -> {
                pt = parse(context, cl.getFirst(), complain, varargs, detailedSourcesBuilder);
                source = null; // will be set higher up
            }
            case PrimitiveArrayType pat -> {
                PrimitiveType primitiveType = pat.firstChildOfType(PrimitiveType.class);
                Primitive primitive = primitiveType.firstChildOfType(Primitive.class);
                tt = primitive.getType();
                source = source(primitive);
                pt = primitiveType(tt);
            }
            case ReferenceType rt -> {
                if (rt.getFirst() instanceof ObjectType ot) {
                    pt = parseObjectType(context, ot, complain, detailedSourcesBuilder);
                    source = source(ot);
                } else throw new UnsupportedOperationException();
            }
            case ObjectType ot -> {
                pt = parseObjectType(context, ot, complain, detailedSourcesBuilder);
                source = source(ot);
            }
            case KeyWord kw -> {
                pt = primitiveType(kw.getType());
                source = source(kw);
            }
            case PrimitiveType primitiveType -> {
                if (primitiveType.getFirst() instanceof Primitive p) {
                    pt = primitiveType(p.getType());
                    source = source(p);
                } else throw new UnsupportedOperationException();
            }
            case Name name -> {
                pt = parseObjectType(context, name, complain, detailedSourcesBuilder);
                source = source(name);
            }
            case TypeArgument ta -> {
                if (ta.getFirst() instanceof Operator o && Token.TokenType.HOOK.equals(o.getType())) {
                    // ?, ? super T ==
                    if (nodes.size() > 1 && nodes.get(1) instanceof WildcardBounds bounds) {
                        pt = parseWildcardBounds(context, bounds, detailedSourcesBuilder);
                        source = null; // do not set!
                    } else {
                        // simply ?
                        pt = runtime.parameterizedTypeWildcard();
                        source = source(o);
                    }
                } else throw new UnsupportedOperationException();
            }
            default -> throw new UnsupportedOperationException();
        }
        if (pt == null) {
            if (complain) {
                throw new UnsupportedOperationException();
            }
            return null;
        }
        if (detailedSourcesBuilder != null && source != null) {
            detailedSourcesBuilder.put(pt, source);
        }
        if (arrays == null) {
            return pt;
        }
        ParameterizedType withArrays = pt.copyWithArrays(arrays.count);
        if (detailedSourcesBuilder != null && source != null) {
            detailedSourcesBuilder.putWithArrayToWithoutArray(withArrays, pt);
            Source sourceWithVarargs = source.withEndPos(arrays.endPos);
            detailedSourcesBuilder.put(withArrays, sourceWithVarargs);
        }
        return withArrays;
    }

    private ParameterizedType parseWildcardBounds(Context context,
                                                  WildcardBounds bounds,
                                                  DetailedSources.Builder detailedSourcesBuilder) {
        Wildcard wildcard;
        Node node = bounds.getFirst();
        Node.NodeType type = node.getType();
        if (Token.TokenType.EXTENDS.equals(type)) {
            wildcard = runtime.wildcardExtends();
        } else if (Token.TokenType.SUPER.equals(type)) {
            wildcard = runtime.wildcardSuper();
        } else throw new Summary.ParseException(context, "Expect super or extends");
        if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(wildcard, source(node));
        ParameterizedType pt = parse(context, bounds.get(1), detailedSourcesBuilder);
        return pt.withWildcard(wildcard);
    }

    private ParameterizedType primitiveType(Token.TokenType tt) {
        return switch (tt) {
            case INT -> runtime.intParameterizedType();
            case DOUBLE -> runtime.doubleParameterizedType();
            case BYTE -> runtime.byteParameterizedType();
            case BOOLEAN -> runtime.booleanParameterizedType();
            case FLOAT -> runtime.floatParameterizedType();
            case CHAR -> runtime.charParameterizedType();
            case VOID -> runtime.voidParameterizedType();
            case SHORT -> runtime.shortParameterizedType();
            case LONG -> runtime.longParameterizedType();
            default -> null;
        };
    }

    private ParameterizedType parseObjectType(Context context, Node ot, boolean complain,
                                              DetailedSources.Builder detailedSourcesBuilder) {
        int i = 0;
        StringBuilder sb = new StringBuilder();
        int startNamedType = -1;
        Stack<Source> details = detailedSourcesBuilder == null ? null : new Stack<>();
        while (i < ot.size()) {
            if (ot.get(i) instanceof Annotation) {
                // annotations have been processed in CatchClause, we'll skip
                ++i;
            } else {
                if (ot.get(i) instanceof Identifier id) {
                    if (startNamedType == -1) startNamedType = i;
                    sb.append(id.getSource());
                    if (details != null) {
                        // source always starts at the beginning of the type, ends incrementally further
                        details.add(source(ot, id));
                    }
                } else {
                    throw new Summary.ParseException(context, "Expected an identifier");
                }
                i++;
                if (i < ot.size() && ot.get(i) instanceof Delimiter d && Token.TokenType.DOT.equals(d.getType())) {
                    i++;
                    sb.append(".");
                } else break;
            }
        }
        String qualifiedName = sb.toString();
        if (qualifiedName.isBlank()) throw new Summary.ParseException(context, "Expected a qualified name");
        List<? extends NamedType> nts = context.typeContext().getWithQualification(qualifiedName, complain);
        if (nts == null) {
            if (complain) throw new Summary.ParseException(context, "Expected non-null");
            return null;
        }
        ParameterizedType withoutTypeParameters = nts.getLast().asSimpleParameterizedType();
        if (detailedSourcesBuilder != null) {
            if (withoutTypeParameters.typeParameter() != null) {
                detailedSourcesBuilder.put(withoutTypeParameters.typeParameter(), details.pop());
            } else {
                detailedSourcesBuilder.put(withoutTypeParameters.typeInfo(), details.pop());
                List<DetailedSources.Builder.TypeInfoSource> associatedList = new ArrayList<>();
                for (int j = nts.size() - 2; j >= 0; --j) {
                    if (nts.get(j) instanceof TypeInfo qualifier) {
                        Source source = details.pop();
                        associatedList.add(new DetailedSources.Builder.TypeInfoSource(qualifier, source));
                    } else if (nts.get(j) instanceof TypeParameter tp) {
                        // TODO dubbo OSS project
                    }
                }
                if (!details.isEmpty()) {
                    detailedSourcesBuilder.put(withoutTypeParameters.typeInfo().packageName(), details.pop());
                }
                if (!associatedList.isEmpty()) {
                    detailedSourcesBuilder.putTypeQualification(withoutTypeParameters.typeInfo(), List.copyOf(associatedList));
                }
            }
        }
        if (ot.size() > i && ot.get(i) instanceof TypeArguments tas) {
            List<ParameterizedType> typeArguments = parseTypeArguments(context, tas, complain, detailedSourcesBuilder);
            if (typeArguments == null) {
                if (complain) throw new Summary.ParseException(context, "Expected type arguments");
                return null;
            }
            if (!typeArguments.isEmpty()) {
                return withoutTypeParameters.withParameters(List.copyOf(typeArguments));
            }
        }
        return withoutTypeParameters;
    }

    public List<ParameterizedType> parseTypeArguments(Context context, TypeArguments tas, boolean complain,
                                                      DetailedSources.Builder detailedSourcesBuilder) {
        List<ParameterizedType> typeArguments = new ArrayList<>();
        int j = 1;
        while (j < tas.size()) {
            switch (tas.get(j)) {
                case TypeArgument ta -> {
                    ParameterizedType arg = parse(context, ta, complain, null, detailedSourcesBuilder);
                    typeArguments.add(arg);
                }
                case Operator o when Token.TokenType.HOOK.equals(o.getType()) ->
                        typeArguments.add(runtime.parameterizedTypeWildcard());
                case Type type -> {
                    ParameterizedType arg = parse(context, type, complain, null, detailedSourcesBuilder);
                    if (arg == null) {
                        if (complain) {
                            throw new Summary.ParseException(context, "Expected to know type argument");
                        }
                        return null;
                    }
                    typeArguments.add(arg);
                }
                case null, default -> throw new UnsupportedOperationException();
            }
            j += 2;
        }
        return typeArguments;
    }

    private record Arrays(int count, int endPos) {
    }

    private Arrays countArrays(List<Node> nodes, Node varargs) {
        int arrays = varargs == null ? 0 : 1;
        int endPos = varargs == null ? 0 : varargs.getEndColumn();
        for (Node child : nodes) {
            if (child instanceof Delimiter d && "]".equals(d.getSource())) {
                arrays++;
                endPos = Math.max(d.getEndColumn(), endPos);
            }
        }
        if (arrays == 0) return null;
        return new Arrays(arrays, endPos);
    }
}
