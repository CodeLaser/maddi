package org.e2immu.language.cst.impl.type;

import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DescendMode;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;
import org.e2immu.language.cst.impl.info.InfoImpl;
import org.e2immu.language.cst.impl.info.TypeParameterInspection;
import org.e2immu.language.cst.impl.info.TypeParameterInspectionImpl;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.support.Either;
import org.e2immu.support.EventuallyFinal;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TypeParameterImpl extends InfoImpl implements TypeParameter {
    private final int index;
    private final String name;
    private final Either<TypeInfo, MethodInfo> owner;
    private final EventuallyFinal<TypeParameterInspection> inspection = new EventuallyFinal<>();
    private final PropertyValueMap analysis = new PropertyValueMapImpl();

    public TypeParameterImpl(int index, String name, Either<TypeInfo, MethodInfo> owner) {
        this.index = index;
        this.name = name;
        this.owner = owner;
        this.inspection.setVariable(new TypeParameterInspectionImpl.Builder(this));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeParameterImpl that)) return false;
        return getIndex() == that.getIndex() && Objects.equals(getOwner(), that.getOwner());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIndex(), getOwner());
    }

    public void commit(TypeParameterInspection typeParameterInspection) {
        this.inspection.setFinal(typeParameterInspection);
    }

    @Override
    public boolean hasBeenInspected() {
        return this.inspection.isFinal();
    }

    @Override
    public int complexity() {
        return 0;
    }

    @Override
    public List<Comment> comments() {
        return inspection.get().comments();
    }

    @Override
    public TypeParameter rewire(InfoMap infoMap) {
        return rewire(infoMap, new HashMap<>());
    }

    public TypeParameter rewire(InfoMap infoMap, Map<TypeParameter, TypeParameter> done) {
        Either<TypeInfo, MethodInfo> rewiredOwner = owner.isLeft()
                ? Either.left(infoMap.typeInfo(owner.getLeft()))
                : Either.right(infoMap.methodInfo(owner.getRight()));
        List<AnnotationExpression> rewiredAnnotations = annotations().stream()
                .map(ae -> (AnnotationExpression) ae.rewire(infoMap)).toList();
        TypeParameter rewired = new TypeParameterImpl(index, name, rewiredOwner);
        done.put(this, rewired);
        List<ParameterizedType> rewiredTypeBounds = typeBounds().stream()
                .map(pt -> pt.rewire(infoMap, done)).toList();
        rewired.builder().setSource(source()).addAnnotations(rewiredAnnotations).setTypeBounds(rewiredTypeBounds).commit();
        return rewired;
    }

    @Override
    public Source source() {
        return inspection.get().source();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        predicate.test(this);
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.beforeElement(this);
        visitor.afterElement(this);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        return Stream.empty();
    }

    @Override
    public TypeParameter withOwnerVariableTypeBounds(MethodInfo methodInfo) {
        return withOwner(Either.right(methodInfo));
    }

    @Override
    public TypeParameter withOwnerVariableTypeBounds(TypeInfo typeInfo) {
        return withOwner(Either.left(typeInfo));
    }

    private TypeParameter withOwner(Either<TypeInfo, MethodInfo> owner) {
        TypeParameterImpl tpi = new TypeParameterImpl(getIndex(), simpleName(), owner);
        List<ParameterizedType> newBounds = typeBounds().stream()
                .map(pt -> pt.replaceTypeParameter(this, tpi)).toList();
        tpi.builder().setTypeBounds(newBounds).setSource(source()).addAnnotations(annotations());
        return tpi;
    }

    @Override
    public TypeParameter.Builder builder() {
        assert inspection.isVariable();
        return (TypeParameter.Builder) inspection.get();
    }

    @Override
    public boolean typeBoundsAreSet() {
        if (!inspection.get().typeBoundsAreSet()) return false;
        return inspection.get().typeBounds().stream().allMatch(pt -> pt.typeBoundsAreSet(this));
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public Either<TypeInfo, MethodInfo> getOwner() {
        return owner;
    }

    @Override
    public List<ParameterizedType> typeBounds() {
        return inspection.get().typeBounds();
    }

    @Override
    public ParameterizedType asParameterizedType() {
        // NOTE: we do not add the type bounds
        return new ParameterizedTypeImpl(this, 0);
    }

    @Override
    public ParameterizedType asSimpleParameterizedType() {
        return new ParameterizedTypeImpl(this, 0);
    }

    @Override
    public OutputBuilder print(Qualification qualification) {
        return print(qualification, true);
    }

    @Override
    public OutputBuilder print(Qualification qualification, boolean printTypeBounds) {
        OutputBuilder outputBuilder = new OutputBuilderImpl();
        List<AnnotationExpression> allAnnotations = Stream.concat(annotations().stream(),
                        qualification.decorator() == null ? Stream.of()
                                : qualification.decorator().annotations(this).stream())
                .toList();
        if (!allAnnotations.isEmpty()) {
            OutputBuilder ob = allAnnotations.stream().map(ae -> ae.print(qualification))
                    .collect(OutputBuilderImpl.joining(SpaceEnum.ONE));
            outputBuilder.add(ob).add(SpaceEnum.ONE);
        }
        outputBuilder.add(new TextImpl(name));
        if (!typeBounds().isEmpty() && printTypeBounds) {
            outputBuilder.add(SpaceEnum.ONE).add(KeywordImpl.EXTENDS).add(SpaceEnum.ONE);
            outputBuilder.add(typeBounds()
                    .stream()
                    .map(pt -> ParameterizedTypePrinter.print(qualification, pt, false,
                            DiamondEnum.SHOW_ALL, false, false))
                    .collect(OutputBuilderImpl.joining(SymbolEnum.AND_TYPES)));
        }
        return outputBuilder;
    }

    @Override
    public String info() {
        return "typeParameter";
    }

    @Override
    public Access access() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompilationUnit compilationUnit() {
        return typeInfo().primaryType().compilationUnit();
    }

    @Override
    public String simpleName() {
        return name;
    }

    @Override
    public String fullyQualifiedName() {
        String ownerFqn = owner.isLeft() ? owner.getLeft().fullyQualifiedName() : owner.getRight().fullyQualifiedName();
        return ownerFqn + ":TP" + index;
    }

    @Override
    public boolean isSynthetic() {
        return inspection.get().isSynthetic();
    }

    @Override
    public TypeInfo typeInfo() {
        return owner.isLeft() ? owner.getLeft() : owner.getRight().typeInfo();
    }

    @Override
    public JavaDoc javaDoc() {
       return null;
    }

    @Override
    public List<? extends Info> translate(TranslationMap translationMap) {
        return List.of();
    }

    @Override
    public String toString() {
        String shortOwner = owner.isLeft() ? owner.getLeft().simpleName()
                : owner.getRight().typeInfo().simpleName() + "." + owner.getRight().name();
        return simpleName() + "=TP#" + index + " in " + shortOwner;
    }

    @Override
    public String toStringWithTypeBounds() {
        return this + " " + typeBounds();
    }

    @Override
    public List<AnnotationExpression> annotations() {
        return inspection.get().annotations();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced(boolean explicit, Set<TypeParameter> visited) {
        if (visited.add(this)) {
            Stream<Element.TypeReference> s1 = annotations().stream().flatMap(Element::typesReferenced);
            Stream<Element.TypeReference> s2 = typeBounds().stream().flatMap(pt -> pt.typesReferenced(explicit, visited));
            return Stream.concat(s1, s2);
        }
        return Stream.of();
    }

    @Override
    public Stream<Element.TypeReference> typesReferenced() {
        return typesReferenced(true, new HashSet<>());
    }

    @Override
    public PropertyValueMap analysis() {
        return analysis;
    }
}
