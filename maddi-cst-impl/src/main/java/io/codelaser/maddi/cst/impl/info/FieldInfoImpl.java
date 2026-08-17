/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.cst.impl.info;

import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.cst.api.analysis.PropertyValueMap;
import io.codelaser.maddi.cst.api.element.*;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.*;

import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.support.EventuallyFinal;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FieldInfoImpl extends InfoImpl implements FieldInfo {

    private final String name;
    private final boolean isStatic;
    private final ParameterizedType type;
    private final String fullyQualifiedName;
    private final TypeInfo owner;
    private final EventuallyFinal<FieldInspection> inspection;

    public FieldInfoImpl(String name, boolean isStatic, ParameterizedType type, TypeInfo owner) {
        this.name = name;
        this.isStatic = isStatic;
        this.type = type;
        this.fullyQualifiedName = owner.fullyQualifiedName() + "." + name;
        this.owner = owner;
        inspection = new EventuallyFinal<>();
        inspection.setVariable(new FieldInspectionImpl.Builder(this));
    }

    @Override
    public String info() {
        return "field";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldInfoImpl fieldInfo)) return false;
        return fullyQualifiedName.equals(fieldInfo.fullyQualifiedName)
               // note: the primitive types have no source set
               && Objects.equals(owner.compilationUnit().sourceSet(), fieldInfo.owner.compilationUnit().sourceSet());
    }

    @Override
    public String toString() {
        return fullyQualifiedName;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fullyQualifiedName);
    }

    public boolean hasBeenCommitted() {
        return inspection.isFinal();
    }

    @Override
    public TypeInfo typeInfo() {
        return owner;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String simpleName() {
        return name;
    }

    @Override
    public TypeInfo owner() {
        return owner;
    }

    @Override
    public ParameterizedType type() {
        return type;
    }

    @Override
    public String fullyQualifiedName() {
        return fullyQualifiedName;
    }

    @Override
    public String descriptor() {
        return owner.descriptor() + ":" + name;
    }

    /**
     * As recorded when the field was created. Deliberately <b>not</b> "or the owner is an interface", though JLS
     * 9.3 makes every real interface field implicitly {@code public static final}: maddi also models synthetic
     * <i>instance</i> fields on interface types — {@code CreateSyntheticFieldsForGetSet} attaches
     * {@code _synthetic_list} to {@code java.util.List} with {@code isStatic == false} on purpose, so that a
     * {@link io.codelaser.maddi.cst.api.variable.FieldReference} to it keeps its scope and two lists' elements
     * stay different variables. An accessor cannot tell that apart from a constant, so the implicit-static rule is
     * applied where the declaration is read instead ({@code ScanCompilationUnit.field}).
     */
    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public boolean isFinal() {
        return inspection.get().fieldModifiers().contains(FieldModifierEnum.FINAL);
    }

    @Override
    public boolean isTransient() {
        return inspection.get().fieldModifiers().contains(FieldModifierEnum.TRANSIENT);
    }

    @Override
    public boolean isVolatile() {
        return inspection.get().fieldModifiers().contains(FieldModifierEnum.VOLATILE);
    }

    @Override
    public List<AnnotationExpression> annotations() {
        return inspection.get().annotations();
    }

    @Override
    public JavaDoc javaDoc() {
        return inspection.get().javaDoc();
    }

    @Override
    public boolean isPropertyNotNull() {
        if (type.isPrimitiveExcludingVoid()) return true;
        return analysis().getOrDefault(PropertyImpl.NOT_NULL_FIELD, ValueImpl.NotNullImpl.NULLABLE).isAtLeastNotNull();
    }

    @Override
    public Access access() {
        return inspection.get().access();
    }

    @Override
    public boolean isPropertyFinal() {
        if (isFinal()) return true;
        // JLS 9.3: every field declaration in the body of an interface is implicitly public, static, final --
        // the modifier list only carries what was written, so an interface constant without the keyword
        // (String CONSTRUCTOR_NAME = "<init>") must not read as assignable
        if (owner.isInterface()) return true;
        return analysis().getOrDefault(PropertyImpl.FINAL_FIELD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public boolean isIgnoreModifications() {
        return analysis().getOrDefault(PropertyImpl.IGNORE_MODIFICATIONS_FIELD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public int complexity() {
        return 1 + inspection.get().initializer().complexity();
    }

    @Override
    public void visit(Predicate<Element> predicate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    // a reader over the inspection face: any receiver modification happens before the inspection commit
    @NotModified(after = "inspection")
    @Override
    public OutputBuilder print(Qualification qualification) {
        return print(qualification, false);
    }

    @Override
    public OutputBuilder print(Qualification qualification, boolean asParameter) {
        return new FieldPrinterImpl(this, false).print(qualification, asParameter);
    }

    @Override
    public Stream<Variable> variables(DescendMode descendMode) {
        throw new UnsupportedOperationException();
    }

    // a reader over the inspection face: any receiver modification happens before the inspection commit
    @NotModified(after = "inspection")
    @Override
    public Stream<TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        Expression initializer = inspection.get().initializer();
        Stream<TypeReference> fromAnnotations = annotations().stream().flatMap(annotationExpression -> annotationExpression.typesReferenced(predicate));
        Stream<TypeReference> fromInitializer = initializer == null ? Stream.of() : initializer.typesReferenced(predicate);
        Stream<TypeReference> fromJavaDoc = javaDoc() == null ? Stream.of() : javaDoc().typesReferenced(predicate);
        return Stream.concat(fromAnnotations,
                Stream.concat(type.typesReferenced(TypeReferenceNature.EXPLICIT,
                                source() == null ? null : source().detailedSources()),
                        Stream.concat(fromJavaDoc, fromInitializer)));
    }

    @Override
    public List<Comment> comments() {
        return inspection.get().comments();
    }

    @Override
    public Source source() {
        return inspection.get().source();
    }

    @Override
    public FieldInfo.Builder builder() {
        assert inspection.isVariable();
        return (FieldInfo.Builder) inspection.get();
    }

    public void commit(FieldInspectionImpl fieldInspection) {
        inspection.setFinal(fieldInspection);
    }

    @Override
    public Set<FieldModifier> modifiers() {
        return inspection.get().fieldModifiers();
    }

    @Override
    public Expression initializer() {
        return inspection.get().initializer();
    }

    @Override
    public PropertyValueMap analysisOfInitializer() {
        return inspection.get().analysisOfInitializer();
    }

    @Override
    public boolean isSynthetic() {
        return inspection.get().isSynthetic();
    }

    @Override
    public boolean hasBeenInspected() {
        return inspection.isFinal();
    }

    @Override
    public List<FieldInfo> translate(TranslationMap translationMap) {
        Expression init = initializer();
        Expression tInit = init.translate(translationMap);
        TypeInfo tOwner = translationMap.translateType(owner.asSimpleParameterizedType()).typeInfo();
        ParameterizedType tType = translationMap.translateType(type);

        if (tOwner != owner || tInit != init || tType != type || !analysis().isEmpty() && translationMap.isClearAnalysis()) {
            FieldInfoImpl newField = new FieldInfoImpl(name, isStatic, tType, tOwner);
            newField.builder()
                    .setInitializer(tInit)
                    .setSynthetic(isSynthetic());
            modifiers().forEach(newField.builder()::addFieldModifier);
            newField.builder().computeAccess();
            newField.builder().commit();
            if (!translationMap.isClearAnalysis()) {
                newField.analysis().setAll(analysis());
            }
            return translationMap.postTranslationHandler(this, List.of(newField));
        }
        return List.of(this);
    }

    @Override
    public FieldInfo withOwnerVariableBuilder(TypeInfo newOwner) {
        FieldInfoImpl fi = new FieldInfoImpl(name, isStatic, type, newOwner);
        fi.inspection.setVariable(new FieldInspectionImpl.Builder(fi, inspection.get()));
        return fi;
    }

    @Override
    public FieldInfo withOwner(TypeInfo newOwner) {
        if (owner != newOwner) {
            FieldInfoImpl fi = new FieldInfoImpl(name, isStatic, type, newOwner);
            if (inspection.isFinal()) {
                fi.inspection.setFinal(inspection.get());
            } else {
                fi.inspection.setVariable(inspection.get());
            }
            return fi;
        }
        return this;
    }

    @Override
    public boolean isUnmodified() {
        return analysis().getOrDefault(PropertyImpl.UNMODIFIED_FIELD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public void rewirePhase3(InfoMap infoMap) {
        FieldInfo rewiredField = infoMap.fieldInfo(this);
        rewiredField.builder()
                .addAnnotations(annotations().stream()
                        .map(a -> (AnnotationExpression) a.rewire(infoMap)).toList())
                .addComments(comments().stream().map(c -> c.rewire(infoMap)).toList())
                .setSource(source())
                .setInitializer(initializer().rewire(infoMap)).commit();
        // carry the opted-in analysis (see Property.carryOnRewire); inert until a property opts in
        rewiredField.analysis().setAll(analysis().rewire(infoMap));
    }

    @Override
    public Element rewire(InfoMapView infoMap) {
        throw new UnsupportedOperationException("Must use the infoMap.fieldInfo() method");
    }
}
