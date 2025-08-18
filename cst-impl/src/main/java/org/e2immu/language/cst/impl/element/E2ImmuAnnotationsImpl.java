package org.e2immu.language.cst.impl.element;

import org.e2immu.annotation.*;
import org.e2immu.annotation.eventual.BeforeMark;
import org.e2immu.annotation.eventual.Mark;
import org.e2immu.annotation.eventual.Only;
import org.e2immu.annotation.eventual.TestMark;
import org.e2immu.annotation.method.GetSet;
import org.e2immu.annotation.rare.AllowsInterrupt;
import org.e2immu.annotation.rare.Finalizer;
import org.e2immu.annotation.rare.IgnoreModifications;
import org.e2immu.annotation.type.ExtensionClass;
import org.e2immu.annotation.type.Singleton;
import org.e2immu.annotation.type.UtilityClass;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.impl.expression.AnnotationExpressionImpl;
import org.e2immu.language.cst.impl.info.TypeInfoImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class E2ImmuAnnotationsImpl {
    public final static String IMPLIED = "implied";
    public final static String ABSENT = "absent";
    public final static String AFTER = "after";
    public final static String BEFORE = "before";
    public final static String CONSTRUCTION = "construction";
    public final static String CONTENT = "content";
    public final static String CONTRACT = "contract";
    public final static String HIDDEN_CONTENT = "hc";
    public final static String INCONCLUSIVE = "inconclusive";
    public final static String HC_PARAMETERS = "hcParameters";
    public final static String VALUE = "value";
    public final static String PAR = "par";
    public final static String SEQ = "seq";
    public final static String MULTI = "multi";

    public final AnnotationExpression allowsInterrupt = create(AllowsInterrupt.class);
    public final AnnotationExpression beforeMark = create(BeforeMark.class);
    public final AnnotationExpression commutable = create(Commutable.class);
    public final AnnotationExpression container = create(Container.class);
    public final AnnotationExpression extensionClass = create(ExtensionClass.class);
    public final AnnotationExpression effectivelyFinal = create(Final.class);
    public final AnnotationExpression finalFields = create(FinalFields.class);
    public final AnnotationExpression finalizer = create(Finalizer.class);
    public final AnnotationExpression fluent = create(Fluent.class);
    public final AnnotationExpression getSet = create(GetSet.class);
    public final AnnotationExpression identity = create(Identity.class);
    public final AnnotationExpression ignoreModifications = create(IgnoreModifications.class);
    public final AnnotationExpression immutable = create(Immutable.class);
    public final AnnotationExpression immutableContainer = create(ImmutableContainer.class);
    public final AnnotationExpression independent = create(Independent.class);
    public final AnnotationExpression mark = create(Mark.class);
    public final AnnotationExpression modified = create(Modified.class);
    public final AnnotationExpression notModified = create(NotModified.class);
    public final AnnotationExpression notNull = create(NotNull.class);
    public final AnnotationExpression nullable = create(Nullable.class);
    public final AnnotationExpression only = create(Only.class);
    public final AnnotationExpression singleton = create(Singleton.class);
    public final AnnotationExpression testMark = create(TestMark.class);
    public final AnnotationExpression utilityClass = create(UtilityClass.class);

    @ImmutableContainer // result of Map.copyOf
    private final Map<String, AnnotationExpression> annotationTypes;

    public E2ImmuAnnotationsImpl() {
        Map<String, AnnotationExpression> builder = new HashMap<>();
        add(builder, allowsInterrupt, beforeMark, commutable, container, independent,
                immutableContainer, extensionClass, finalFields, getSet, immutable,
                effectivelyFinal, fluent, finalizer, identity, ignoreModifications, mark, modified);
        add(builder, notModified, notNull, nullable, only, singleton, testMark, utilityClass);
        annotationTypes = Map.copyOf(builder);
    }

    private static void add(Map<String, AnnotationExpression> builder, AnnotationExpression... aes) {
        for (AnnotationExpression ae : aes) {
            builder.put(ae.typeInfo().fullyQualifiedName(), ae);
        }
    }

    /**
     * create an annotation for a given class, without parameters (contract=false, absent=false)
     *
     * @param clazz must have a method called type of Enum type AnnotationType
     * @return an annotation expression
     */
    private AnnotationExpression create(Class<?> clazz) {
        CompilationUnit cu = new CompilationUnitImpl.Builder().setPackageName(clazz.getPackageName()).build();
        return new AnnotationExpressionImpl(List.of(), null, new TypeInfoImpl(cu, clazz.getSimpleName()), List.of());
    }

    public AnnotationExpression get(String name) {
        return Objects.requireNonNull(annotationTypes.get(name), name);
    }

    public Stream<AnnotationExpression> streamTypes() {
        return annotationTypes.values().stream();
    }
}
