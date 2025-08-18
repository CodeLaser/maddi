package org.e2immu.language.cst.impl.runtime;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.OutputElement;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.output.element.ElementarySpace;
import org.e2immu.language.cst.api.output.element.Split;
import org.e2immu.language.cst.api.output.element.TextBlockFormatting;
import org.e2immu.language.cst.api.runtime.Factory;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.Diamond;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;
import org.e2immu.language.cst.api.type.Wildcard;
import org.e2immu.language.cst.api.variable.*;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.element.*;
import org.e2immu.language.cst.impl.expression.*;
import org.e2immu.language.cst.impl.expression.util.PrecedenceEnum;
import org.e2immu.language.cst.impl.info.*;
import org.e2immu.language.cst.impl.output.*;
import org.e2immu.language.cst.impl.statement.*;
import org.e2immu.language.cst.impl.translate.TranslationMapImpl;
import org.e2immu.language.cst.impl.type.*;
import org.e2immu.language.cst.impl.variable.*;
import org.e2immu.support.Either;
import org.e2immu.util.internal.util.IntUtil;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;

public class FactoryImpl extends PredefinedImpl implements Factory {

    private final IntConstant zero;
    private final IntConstant one;
    private final IntConstant minusOne;
    private final BooleanConstant constantTrue;
    private final BooleanConstant constantFalse;
    private final Map<MethodInfo, Precedence> precedenceMap = new HashMap<>();

    public FactoryImpl() {
        zero = new IntConstantImpl(List.of(), noSource(), intParameterizedType(), 0);
        one = new IntConstantImpl(List.of(), noSource(), intParameterizedType(), 1);
        minusOne = new IntConstantImpl(List.of(), noSource(), intParameterizedType(), -1);
        constantFalse = new BooleanConstantImpl(List.of(), noSource(), booleanParameterizedType(), false);
        constantTrue = new BooleanConstantImpl(List.of(), noSource(), booleanParameterizedType(), true);

        precedenceMap.put(plusOperatorInt(), PrecedenceEnum.ADDITIVE);
        precedenceMap.put(minusOperatorInt(), PrecedenceEnum.ADDITIVE);
        precedenceMap.put(plusOperatorString(), PrecedenceEnum.ADDITIVE);

        precedenceMap.put(multiplyOperatorInt(), PrecedenceEnum.MULTIPLICATIVE);
        precedenceMap.put(divideOperatorInt(), PrecedenceEnum.MULTIPLICATIVE);
        precedenceMap.put(remainderOperatorInt(), PrecedenceEnum.MULTIPLICATIVE);
        precedenceMap.put(andOperatorInt(), PrecedenceEnum.AND);
        precedenceMap.put(orOperatorInt(), PrecedenceEnum.OR);
        precedenceMap.put(xorOperatorInt(), PrecedenceEnum.XOR);
        precedenceMap.put(leftShiftOperatorInt(), PrecedenceEnum.SHIFT);
        precedenceMap.put(signedRightShiftOperatorInt(), PrecedenceEnum.SHIFT);
        precedenceMap.put(unsignedRightShiftOperatorInt(), PrecedenceEnum.SHIFT);

        precedenceMap.put(lessEqualsOperatorInt(), PrecedenceEnum.RELATIONAL);
        precedenceMap.put(lessOperatorInt(), PrecedenceEnum.RELATIONAL);
        precedenceMap.put(greaterEqualsOperatorInt(), PrecedenceEnum.RELATIONAL);
        precedenceMap.put(greaterOperatorInt(), PrecedenceEnum.RELATIONAL);

        precedenceMap.put(equalsOperatorInt(), PrecedenceEnum.EQUALITY);
        precedenceMap.put(notEqualsOperatorInt(), PrecedenceEnum.EQUALITY);
        precedenceMap.put(equalsOperatorObject(), PrecedenceEnum.EQUALITY);
        precedenceMap.put(notEqualsOperatorObject(), PrecedenceEnum.EQUALITY);

        precedenceMap.put(andOperatorBool(), PrecedenceEnum.LOGICAL_AND);
        precedenceMap.put(orOperatorBool(), PrecedenceEnum.LOGICAL_OR);
        precedenceMap.put(xorOperatorBool(), PrecedenceEnum.XOR);
    }

    @Override
    public Access accessPackage() {
        return InspectionImpl.AccessEnum.PACKAGE;
    }

    @Override
    public Access accessPrivate() {
        return InspectionImpl.AccessEnum.PRIVATE;
    }

    @Override
    public Access accessProtected() {
        return InspectionImpl.AccessEnum.PROTECTED;
    }

    @Override
    public Access accessPublic() {
        return InspectionImpl.AccessEnum.PUBLIC;
    }

    @Override
    public ParameterizedType commonType(ParameterizedType pt1, ParameterizedType pt2) {
        return new CommonType(this).commonType(pt1, pt2);
    }

    @Override
    public BooleanConstant constantFalse() {
        return constantFalse;
    }

    @Override
    public BooleanConstant constantTrue() {
        return constantTrue;
    }

    @Override
    public DescendMode descendModeNo() {
        return DescendModeEnum.NO;
    }

    @Override
    public DescendMode descendModeYes() {
        return DescendModeEnum.YES;
    }

    @Override
    public Diamond diamondNo() {
        return DiamondEnum.NO;
    }

    @Override
    public Diamond diamondShowAll() {
        return DiamondEnum.SHOW_ALL;
    }

    @Override
    public Diamond diamondYes() {
        return DiamondEnum.YES;
    }

    @Override
    public ElementarySpace elementarySpaceNice() {
        return ElementarySpaceEnum.NICE;
    }

    @Override
    public ElementarySpace elementarySpaceRelaxedNone() {
        return ElementarySpaceEnum.RELAXED_NONE;
    }

    @Override
    public Block emptyBlock() {
        return new BlockImpl();
    }

    @Override
    public FieldModifier fieldModifierFinal() {
        return FieldModifierEnum.FINAL;
    }

    @Override
    public FieldModifier fieldModifierPrivate() {
        return FieldModifierEnum.PRIVATE;
    }

    @Override
    public FieldModifier fieldModifierProtected() {
        return FieldModifierEnum.PROTECTED;
    }

    @Override
    public FieldModifier fieldModifierPublic() {
        return FieldModifierEnum.PUBLIC;
    }

    @Override
    public FieldModifier fieldModifierStatic() {
        return FieldModifierEnum.STATIC;
    }

    @Override
    public FieldModifier fieldModifierTransient() {
        return FieldModifierEnum.TRANSIENT;
    }

    @Override
    public FieldModifier fieldModifierVolatile() {
        return FieldModifierEnum.VOLATILE;
    }

    @Override
    public IntConstant intMinusOne() {
        return minusOne;
    }

    @Override
    public IntConstant intOne() {
        return one;
    }

    @Override
    public IntConstant intOne(Source source) {
        return new IntConstantImpl(List.of(), source, intParameterizedType(), 1);
    }

    @Override
    public Numeric intOrDouble(double v) {
        if (IntUtil.isMathematicalInteger(v)) {
            long l = Math.round(v);
            if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
                return newLong(List.of(), noSource(), l);
            }
            return newInt(List.of(), noSource(), (int) l);
        }
        return newDouble(List.of(), noSource(), v);
    }

    @Override
    public IntConstant intZero() {
        return zero;
    }

    @Override
    public int isAssignableFromCovariantErasure(ParameterizedType target, ParameterizedType from) {
        return new IsAssignableFrom(this, target, from)
                .execute(false, false, IsAssignableFrom.Mode.COVARIANT_ERASURE);
    }

    @Override
    public int isNotAssignable() {
        return IsAssignableFrom.NOT_ASSIGNABLE;
    }

    @Override
    public Lambda.OutputVariant lambdaOutputVariantEmpty() {
        return LambdaImpl.OutputVariantImpl.EMPTY;
    }

    @Override
    public Lambda.OutputVariant lambdaOutputVariantTyped() {
        return LambdaImpl.OutputVariantImpl.TYPED;
    }

    @Override
    public Lambda.OutputVariant lambdaOutputVariantVar() {
        return LambdaImpl.OutputVariantImpl.VAR;
    }

    @Override
    public LocalVariableCreation.Modifier localVariableModifierFinal() {
        return LocalVariableCreationImpl.ModifierEnum.FiNAL;
    }

    @Override
    public LocalVariableCreation.Modifier localVariableModifierVar() {
        return LocalVariableCreationImpl.ModifierEnum.VAR;
    }

    @Override
    public MethodInfo.MissingData methodMissingMethodBody() {
        return new MethodInspectionImpl.MissingDataImpl(EnumSet.of(MethodInspectionImpl.MissingDataEnum.METHOD_BODY));
    }

    @Override
    public MethodModifier methodModifierAbstract() {
        return MethodModifierEnum.ABSTRACT;
    }

    @Override
    public MethodModifier methodModifierDefault() {
        return MethodModifierEnum.DEFAULT;
    }

    @Override
    public MethodModifier methodModifierFinal() {
        return MethodModifierEnum.FINAL;
    }

    @Override
    public MethodModifier methodModifierPrivate() {
        return MethodModifierEnum.PRIVATE;
    }

    @Override
    public MethodModifier methodModifierProtected() {
        return MethodModifierEnum.PROTECTED;
    }

    @Override
    public MethodModifier methodModifierPublic() {
        return MethodModifierEnum.PUBLIC;
    }

    @Override
    public MethodModifier methodModifierStatic() {
        return MethodModifierEnum.STATIC;
    }

    @Override
    public MethodModifier methodModifierSynchronized() {
        return MethodModifierEnum.SYNCHRONIZED;
    }

    @Override
    public MethodInfo.MethodType methodTypeAbstractMethod() {
        return MethodInfoImpl.MethodTypeEnum.ABSTRACT_METHOD;
    }

    @Override
    public MethodInfo.MethodType methodTypeCompactConstructor() {
        return MethodInfoImpl.MethodTypeEnum.COMPACT_CONSTRUCTOR;
    }

    @Override
    public MethodInfo.MethodType methodTypeConstructor() {
        return MethodInfoImpl.MethodTypeEnum.CONSTRUCTOR;
    }

    @Override
    public MethodInfo.MethodType methodTypeDefaultMethod() {
        return MethodInfoImpl.MethodTypeEnum.DEFAULT_METHOD;
    }

    @Override
    public MethodInfo.MethodType methodTypeMethod() {
        return MethodInfoImpl.MethodTypeEnum.METHOD;
    }

    @Override
    public MethodInfo.MethodType methodTypeStaticBlock() {
        return MethodInfoImpl.MethodTypeEnum.STATIC_BLOCK;
    }

    @Override
    public MethodInfo.MethodType methodTypeStaticMethod() {
        return MethodInfoImpl.MethodTypeEnum.STATIC_METHOD;
    }

    @Override
    public MethodInfo.MethodType methodTypeSyntheticArrayConstructor() {
        return MethodInfoImpl.MethodTypeEnum.SYNTHETIC_ARRAY_CONSTRUCTOR;
    }

    @Override
    public MethodInfo.MethodType methodTypeSyntheticConstructor() {
        return MethodInfoImpl.MethodTypeEnum.SYNTHETIC_CONSTRUCTOR;
    }

    @Override
    public And.Builder newAndBuilder() {
        return new AndImpl.Builder().setBooleanParameterizedType(booleanParameterizedType());
    }

    @Override
    public AnnotationExpression.Builder newAnnotationExpressionBuilder() {
        return new AnnotationExpressionImpl.Builder();
    }

    @Override
    public AnnotationExpression.KV newAnnotationExpressionKeyValuePair(String key, Expression value) {
        return new AnnotationExpressionImpl.KVI(key, value);
    }

    @Override
    public TypeInfo newAnonymousType(TypeInfo enclosingType, int index) {
        return new TypeInfoImpl(enclosingType, index);
    }

    @Override
    public MethodInfo newArrayCreationConstructor(ParameterizedType type) {
        MethodInfo mi = newMethod(type.typeInfo(), MethodInfoImpl.CONSTRUCTOR_NAME, methodTypeSyntheticArrayConstructor());
        mi.builder()
                .setReturnType(type)
                .addMethodModifier(methodModifierPublic())
                .setMethodBody(emptyBlock())
                .setMissingData(methodMissingMethodBody())
                .computeAccess();
        for (int i = 0; i < type.arrays(); i++) {
            mi.builder().addParameter("dim" + i, intParameterizedType());
        }
        mi.builder().commitParameters().commit();
        return mi;
    }

    @Override
    public ArrayInitializer.Builder newArrayInitializerBuilder() {
        return new ArrayInitializerImpl.Builder();
    }

    @Override
    public ArrayLength.Builder newArrayLengthBuilder() {
        return new ArrayLengthImpl.Builder(intParameterizedType());
    }

    @Override
    public AssertStatement.Builder newAssertBuilder() {
        return new AssertStatementImpl.Builder();
    }

    @Override
    public Assignment newAssignment(VariableExpression target, Expression value) {
        return new AssignmentImpl(target, value);
    }

    @Override
    public Assignment.Builder newAssignmentBuilder() {
        return new AssignmentImpl.Builder();
    }

    @Override
    public BinaryOperator.Builder newBinaryOperatorBuilder() {
        return new BinaryOperatorImpl.Builder();
    }

    @Override
    public BitwiseNegation newBitwiseNegation(List<Comment> comments, Source source, Expression value) {
        return new BitwiseNegationImpl(comments, source, bitWiseNotOperatorInt(), PrecedenceEnum.UNARY, value);
    }

    @Override
    public Block.Builder newBlockBuilder() {
        return new BlockImpl.Builder();
    }

    @Override
    public BooleanConstant newBoolean(List<Comment> comments, Source source, boolean value) {
        return new BooleanConstantImpl(comments, source, booleanParameterizedType(), value);
    }

    @Override
    public BooleanConstant newBoolean(boolean value) {
        return new BooleanConstantImpl(this, value);
    }

    @Override
    public BreakStatement.Builder newBreakBuilder() {
        return new BreakStatementImpl.Builder();
    }

    @Override
    public ByteConstant newByte(byte b) {
        return new ByteConstantImpl(this, b);
    }

    @Override
    public ByteConstant newByte(List<Comment> comments, Source source, byte b) {
        return new ByteConstantImpl(comments, source, byteParameterizedType(), b);
    }

    @Override
    public Cast newCast(Expression e, ParameterizedType parameterizedType) {
        return new CastImpl(List.of(), null, parameterizedType, e);
    }

    @Override
    public Cast.Builder newCastBuilder() {
        return new CastImpl.Builder();
    }

    @Override
    public TryStatement.CatchClause.Builder newCatchClauseBuilder() {
        return new TryStatementImpl.CatchClauseImpl.Builder();
    }

    @Override
    public CharConstant newChar(char c) {
        return new CharConstantImpl(this, c);
    }

    @Override
    public CharConstant newChar(List<Comment> comments, Source source, char c) {
        return new CharConstantImpl(comments, source, charParameterizedType(), c);
    }

    @Override
    public ClassExpression.Builder newClassExpressionBuilder(ParameterizedType pt) {
        // while int.class is legal, Class<int> is not, so we ensure boxed: Class<Integer>
        ParameterizedType classPt = newParameterizedType(classTypeInfo(), List.of(pt.ensureBoxed(this)));
        return new ClassExpressionImpl.Builder().setParameterizedType(pt).setClassType(classPt);
    }

    @Override
    public CommaExpression.Builder newCommaBuilder() {
        return new CommaExpressionImpl.Builder();
    }

    @Override
    public CompilationUnit.Builder newCompilationUnitBuilder() {
        return new CompilationUnitImpl.Builder();
    }

    @Override
    public CompilationUnit newCompilationUnitStub(String candidatePackageName) {
        return new CompilationUnitStub(candidatePackageName);
    }

    @Override
    public Source newCompiledClassSource(CompilationUnit compilationUnit) {
        return SourceImpl.forCompiledClass(compilationUnit);
    }

    @Override
    public MethodInfo newConstructor(TypeInfo owner) {
        return new MethodInfoImpl(owner);
    }

    @Override
    public MethodInfo newConstructor(TypeInfo owner, MethodInfo.MethodType methodType) {
        return new MethodInfoImpl(owner, methodType);
    }

    @Override
    public ConstructorCall.Builder newConstructorCallBuilder() {
        return new ConstructorCallImpl.Builder();
    }

    @Override
    public ContinueStatement.Builder newContinueBuilder() {
        return new ContinueStatementImpl.Builder();
    }

    @Override
    public DependentVariable newDependentVariable(Expression array, Expression index) {
        return DependentVariableImpl.create(array, index, null);
    }

    @Override
    public DependentVariable newDependentVariable(Expression arrayExpression,
                                                  Expression indexExpression,
                                                  ParameterizedType parameterizedType) {
        return DependentVariableImpl.create(arrayExpression, indexExpression, parameterizedType);
    }

    @Override
    public DetailedSources.Builder newDetailedSourcesBuilder() {
        return new DetailedSourcesImpl.BuilderImpl();
    }

    @Override
    public DoStatement.Builder newDoBuilder() {
        return new DoStatementImpl.Builder();
    }

    @Override
    public DoubleConstant newDouble(List<Comment> comments, Source source, double v) {
        return new DoubleConstantImpl(comments, source, doubleParameterizedType(), v);
    }

    @Override
    public DoubleConstant newDouble(double d) {
        return new DoubleConstantImpl(this, d);
    }

    @Override
    public EmptyExpression newEmptyExpression() {
        return new EmptyExpressionImpl(this, EmptyExpressionImpl.EMPTY_EXPRESSION);
    }

    @Override
    public EmptyExpression newEmptyExpression(String msg) {
        return new EmptyExpressionImpl(this, msg);
    }

    @Override
    public EmptyStatement.Builder newEmptyStatementBuilder() {
        return new EmptyStatementImpl.Builder();
    }

    @Override
    public EnclosedExpression.Builder newEnclosedExpressionBuilder() {
        return new EnclosedExpressionImpl.Builder();
    }

    @Override
    public Equals newEquals(Expression lhs, Expression rhs) {
        MethodInfo operator = lhs.isNumeric() && rhs.isNumeric() ? equalsOperatorInt()
                : equalsOperatorObject();
        return new EqualsImpl(List.of(), null, operator, PrecedenceEnum.EQUALITY, lhs, rhs, booleanParameterizedType());
    }

    @Override
    public ExplicitConstructorInvocation.Builder newExplicitConstructorInvocationBuilder() {
        return new ExplicitConstructorInvocationImpl.Builder();
    }

    @Override
    public ExpressionAsStatement newExpressionAsStatement(Expression expression) {
        return new ExpressionAsStatementImpl(expression);
    }

    @Override
    public ExpressionAsStatement.Builder newExpressionAsStatementBuilder() {
        return new ExpressionAsStatementImpl.Builder();
    }

    @Override
    public FieldInfo newFieldInfo(String name, boolean isStatic, ParameterizedType parameterizedType, TypeInfo owner) {
        return new FieldInfoImpl(name, isStatic, parameterizedType, owner);
    }

    @Override
    public FieldReference newFieldReference(FieldInfo fieldInfo) {
        return new FieldReferenceImpl(fieldInfo);
    }

    @Override
    public FieldReference newFieldReference(FieldInfo fieldInfo, Expression scope, ParameterizedType concreteReturnType) {
        // assert scope != null && scope.source() != null;
        return new FieldReferenceImpl(fieldInfo, scope, null, concreteReturnType);
    }

    @Override
    public FloatConstant newFloat(List<Comment> comments, Source source, float v) {
        return new FloatConstantImpl(comments, source, floatParameterizedType(), v);
    }

    @Override
    public FloatConstant newFloat(float f) {
        return new FloatConstantImpl(this, f);
    }

    @Override
    public ForStatement.Builder newForBuilder() {
        return new ForStatementImpl.Builder();
    }

    @Override
    public ForEachStatement.Builder newForEachBuilder() {
        return new ForEachStatementImpl.Builder();
    }

    @Override
    public GreaterThanZero newGreaterThanZero(Expression e, boolean allowEquals) {
        return new GreaterThanZeroImpl(booleanParameterizedType(), e, allowEquals);
    }

    @Override
    public IfElseStatement.Builder newIfElseBuilder() {
        return new IfElseStatementImpl.Builder();
    }

    @Override
    public ImportComputer newImportComputer(int minForAsterisk, Function<String, Collection<TypeInfo>> typesPerPackage) {
        return new ImportComputerImpl(minForAsterisk, typesPerPackage);
    }

    @Override
    public ImportStatement.Builder newImportStatementBuilder() {
        return new ImportStatementImpl.Builder();
    }

    @Override
    public InfoMap newInfoMap(Set<TypeInfo> primaryTypes) {
        return new InfoMapImpl(primaryTypes);
    }

    @Override
    public InlineConditional newInlineConditional(Expression condition, Expression ifTrue, Expression ifFalse) {
        return new InlineConditionalImpl(List.of(), null, condition, ifTrue, ifFalse,
                commonType(ifTrue.parameterizedType(), ifFalse.parameterizedType()));
    }

    @Override
    public InlineConditional.Builder newInlineConditionalBuilder() {
        return new InlineConditionalImpl.Builder();
    }

    @Override
    public InstanceOf.Builder newInstanceOfBuilder() {
        return new InstanceOfImpl.BuilderImpl(booleanParameterizedType());
    }

    @Override
    public IntConstant newInt(List<Comment> comments, Source source, int i) {
        return new IntConstantImpl(comments, source, intParameterizedType(), i);
    }

    @Override
    public IntConstant newInt(int i) {
        return new IntConstantImpl(this, i);
    }

    @Override
    public JavaDoc newJavaDoc(Source source, String comment, List<JavaDoc.Tag> tags) {
        return new JavaDocImpl(source, comment, tags);
    }

    @Override
    public JavaDoc.Tag newJavaDocTag(JavaDoc.TagIdentifier tagIdentifier, String content,
                                     Info resolvedReference, Source source, Source sourceOfReference,
                                     boolean blockTag) {
        return new JavaDocImpl.TagImpl(tagIdentifier, content, null, source, sourceOfReference,
                blockTag);
    }

    @Override
    public Lambda.Builder newLambdaBuilder() {
        return new LambdaImpl.Builder();
    }

    @Override
    public LocalTypeDeclaration.Builder newLocalTypeDeclarationBuilder() {
        return new LocalTypeDeclarationImpl.Builder();
    }

    @Override
    public LocalVariable newLocalVariable(ParameterizedType parameterizedType) {
        return new LocalVariableImpl(parameterizedType, null);
    }

    @Override
    public LocalVariable newLocalVariable(String name, ParameterizedType parameterizedType) {
        return new LocalVariableImpl(name, parameterizedType, null);
    }

    @Override
    public LocalVariable newLocalVariable(String name,
                                          ParameterizedType parameterizedType,
                                          Expression assignmentExpression) {
        return new LocalVariableImpl(name, parameterizedType, assignmentExpression);
    }

    @Override
    public LocalVariableCreation newLocalVariableCreation(LocalVariable localVariable) {
        return new LocalVariableCreationImpl(localVariable);
    }

    @Override
    public LocalVariableCreation.Builder newLocalVariableCreationBuilder() {
        return new LocalVariableCreationImpl.Builder();
    }

    @Override
    public LongConstant newLong(List<Comment> comments, Source source, long l) {
        return new LongConstantImpl(comments, source, longParameterizedType(), l);
    }

    @Override
    public LongConstant newLong(long l) {
        return new LongConstantImpl(this, l);
    }

    @Override
    public MethodInfo newMethod(TypeInfo owner, String name, MethodInfo.MethodType methodType) {
        return new MethodInfoImpl(methodType, name, owner);
    }

    @Override
    public MethodCall.Builder newMethodCallBuilder() {
        return new MethodCallImpl.Builder();
    }

    @Override
    public MethodCall.Builder newMethodCallBuilder(MethodCall methodCall) {
        return new MethodCallImpl.Builder(methodCall);
    }

    @Override
    public MethodPrinter newMethodPrinter(MethodInfo methodInfo) {
        return new MethodPrinterImpl(methodInfo);
    }

    @Override
    public MethodReference.Builder newMethodReferenceBuilder() {
        return new MethodReferenceImpl.Builder();
    }

    @Override
    public ModuleInfo.Builder newModuleInfoBuilder() {
        return new ModuleInfoImpl.BuilderImpl();
    }

    @Override
    public Expression newMultiExpressions(List<Expression> expressions) {
        return new CommaExpressionImpl(List.of(), null, expressions);
    }

    @Override
    public MultiLineComment newMultilineComment(Source source, String comment) {
        return new MultiLineCommentImpl(source, comment);
    }

    @Override
    public NullConstant newNullConstant(List<Comment> comments, Source source) {
        return new NullConstantImpl(comments, source, parameterizedTypeNullConstant());
    }

    @Override
    public Or.Builder newOrBuilder() {
        return new OrImpl.Builder().setBooleanParameterizedType(booleanParameterizedType());
    }

    @Override
    public OutputBuilder newOutputBuilder() {
        return new OutputBuilderImpl();
    }

    @Override
    public ParameterizedType newParameterizedType(TypeInfo typeInfo, List<ParameterizedType> newParameters) {
        return new ParameterizedTypeImpl(typeInfo, null, newParameters, 0, null);
    }

    @Override
    public ParameterizedType newParameterizedType(TypeInfo typeInfo, int arrays) {
        return new ParameterizedTypeImpl(typeInfo, null, List.of(), arrays, null);
    }

    @Override
    public ParameterizedType newParameterizedType(TypeParameter typeParameter, int arrays, Wildcard wildCard) {
        return new ParameterizedTypeImpl(null, typeParameter, List.of(), arrays, wildCard);
    }

    @Override
    public ParameterizedType newParameterizedType(TypeInfo typeInfo,
                                                  int arrays,
                                                  Wildcard wildCard,
                                                  List<ParameterizedType> parameters) {
        return new ParameterizedTypeImpl(typeInfo, null, parameters, arrays, wildCard);
    }

    @Override
    public Source newParserSource(String index, int beginLine, int beginPos, int endLine, int endPos,
                                  DetailedSources detailedSources) {
        return new SourceImpl(index, beginLine, beginPos, endLine, endPos, detailedSources);
    }

    @Override
    public RecordPattern.Builder newRecordPatternBuilder() {
        return new RecordPatternImpl.Builder();
    }

    @Override
    public ReturnStatement.Builder newReturnBuilder() {
        return new ReturnStatementImpl.Builder();
    }

    @Override
    public ReturnStatement newReturnStatement(Expression expression) {
        return new ReturnStatementImpl(expression);
    }

    @Override
    public ShortConstant newShort(short s) {
        return new ShortConstantImpl(this, s);
    }

    @Override
    public ShortConstant newShort(List<Comment> comments, Source source, short s) {
        return new ShortConstantImpl(comments, source, shortParameterizedType(), s);
    }

    @Override
    public SingleLineComment newSingleLineComment(Source source, String comment) {
        return new SingleLineCommentImpl(source, comment);
    }

    @Override
    public SwitchEntry newStatementsSwitchEntry(VariableExpression selector, List<Expression> labels, List<Statement> statements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression newStringConcat(Expression l, Expression r) {
        return new StringConcatImpl(this, l, r);
    }

    @Override
    public StringConstant newStringConstant(String string) {
        return new StringConstantImpl(this, string);
    }

    @Override
    public StringConstant newStringConstant(List<Comment> comments, Source source, String string) {
        return new StringConstantImpl(comments, source, stringParameterizedType(), string);
    }

    @Override
    public SwitchEntry.Builder newSwitchEntryBuilder() {
        return new SwitchEntryImpl.EntryBuilderImpl();
    }

    @Override
    public SwitchExpression.Builder newSwitchExpressionBuilder() {
        return new SwitchExpressionImpl.BuilderImpl();
    }

    @Override
    public SwitchStatementOldStyle.SwitchLabel newSwitchLabelOldStyle(Expression literal, int pos,
                                                                      RecordPattern patternVariable,
                                                                      Expression whenExpression) {
        return new SwitchStatementOldStyleImpl.SwitchLabelImpl(literal, pos, patternVariable, whenExpression);
    }

    @Override
    public SwitchStatementNewStyle.Builder newSwitchStatementNewStyleBuilder() {
        return new SwitchStatementNewStyleImpl.BuilderImpl();
    }

    @Override
    public SwitchStatementOldStyle.Builder newSwitchStatementOldStyleBuilder() {
        return new SwitchStatementOldStyleImpl.Builder();
    }

    @Override
    public SynchronizedStatement.Builder newSynchronizedBuilder() {
        return new SynchronizedStatementImpl.Builder();
    }

    @Override
    public OutputElement newText(String text) {
        return new TextImpl(text);
    }

    @Override
    public This newThis(ParameterizedType parameterizedType, TypeInfo explicitlyWriteType, boolean writeSuper) {
        return new ThisImpl(parameterizedType, explicitlyWriteType, writeSuper);
    }

    @Override
    public ThrowStatement.Builder newThrowBuilder() {
        return new ThrowStatementImpl.Builder();
    }

    @Override
    public TranslationMap.Builder newTranslationMapBuilder() {
        return new TranslationMapImpl.Builder();
    }

    @Override
    public TranslationMap.Builder newTranslationMapBuilder(TranslationMap startingPoint) {
        if (startingPoint instanceof TranslationMapImpl tmi) {
            return new TranslationMapImpl.Builder(tmi);
        } else throw new UnsupportedOperationException("Incompatible. You'll need a wrapper rather than a copy");
    }

    @Override
    public TryStatement.Builder newTryBuilder() {
        return new TryStatementImpl.Builder();
    }

    @Override
    public TypeExpression newTypeExpression(ParameterizedType parameterizedType, Diamond diamond) {
        return new TypeExpressionImpl(parameterizedType, diamond);
    }

    @Override
    public TypeExpression.Builder newTypeExpressionBuilder() {
        return new TypeExpressionImpl.Builder();
    }

    @Override
    public TypeInfo newTypeInfo(TypeInfo typeInfo, String capitalized) {
        return new TypeInfoImpl(typeInfo, capitalized);
    }

    @Override
    public TypeInfo newTypeInfo(MethodInfo methodInfo, String simpleName, int index) {
        return new TypeInfoImpl(methodInfo, simpleName, index);
    }

    @Override
    public TypeInfo newTypeInfo(CompilationUnit cu, String simpleName) {
        return new TypeInfoImpl(cu, simpleName);
    }

    @Override
    public TypeParameter newTypeParameter(int index, String simpleName, Info owner) {
        if (owner instanceof TypeInfo typeInfo) {
            return new TypeParameterImpl(index, simpleName, Either.left(typeInfo));
        } else if (owner instanceof MethodInfo methodInfo) {
            return new TypeParameterImpl(index, simpleName, Either.right(methodInfo));
        } else throw new UnsupportedOperationException();
    }

    @Override
    public TypePrinter newTypePrinter(TypeInfo typeInfo, boolean formatter2) {
        return new TypePrinterImpl(typeInfo, formatter2);
    }

    @Override
    public UnaryOperator newUnaryOperator(List<Comment> comments, Source source, MethodInfo operator, Expression e, Precedence precedence) {
        return new UnaryOperatorImpl(comments, source, operator, e, precedence);
    }

    @Override
    public LocalVariable newUnnamedLocalVariable(ParameterizedType parameterizedType, Expression assignmentExpression) {
        return new LocalVariableImpl(parameterizedType, assignmentExpression);
    }

    @Override
    public VariableExpression newVariableExpression(Variable variable) {
        assert variable != null;
        return new VariableExpressionImpl(variable);
    }

    @Override
    public VariableExpression.Builder newVariableExpressionBuilder() {
        return new VariableExpressionImpl.Builder();
    }

    @Override
    public VariableExpression.Suffix newVariableFieldSuffix(int statementTime, String latestAssignment) {
        return new VariableExpressionImpl.VariableFieldSuffix(statementTime, latestAssignment);
    }

    @Override
    public WhileStatement.Builder newWhileBuilder() {
        return new WhileStatementImpl.Builder();
    }

    @Override
    public YieldStatement.Builder newYieldBuilder() {
        return new YieldStatementImpl.Builder();
    }

    @Override
    public Source noSource() {
        return SourceImpl.NO_SOURCE;
    }

    @Override
    public Expression notNull(Expression expression) {
        return newBinaryOperatorBuilder()
                .setLhs(nullConstant(List.of(), noSource()))
                .setOperator(notEqualsOperatorObject())
                .setRhs(expression)
                .setPrecedence(precedenceEquality())
                .setParameterizedType(booleanParameterizedType())
                .setSource(noSource())
                .build();
    }

    @Override
    public Expression nullConstant() {
        return new NullConstantImpl(List.of(), noSource(), parameterizedTypeNullConstant());
    }

    @Override
    public Expression nullConstant(List<Comment> comments, Source source) {
        return new NullConstantImpl(comments, source, parameterizedTypeNullConstant());
    }

    @Override
    public Expression nullValue(ParameterizedType parameterizedType) {
        return nullValue(parameterizedType, noSource());
    }

    @Override
    public Expression nullValue(ParameterizedType parameterizedType, Source source) {
        if (parameterizedType.arrays() == 0) {
            TypeInfo typeInfo = parameterizedType.bestTypeInfo();
            if (typeInfo != null) {
                if (typeInfo.isBoolean()) return newBoolean(List.of(), source, false);
                if (typeInfo.isInt()) return newInt(List.of(), source, 0);
                if (typeInfo.isLong()) return newLong(List.of(), source, 0L);
                if (typeInfo.isShort()) return newShort(List.of(), source, (short) 0);
                if (typeInfo.isByte()) return newByte(List.of(), source, (byte) 0);
                if (typeInfo.isFloat()) return newFloat(List.of(), source, 0);
                if (typeInfo.isDouble()) return newDouble(List.of(), source, 0);
                if (typeInfo.isChar()) return newChar(List.of(), source, '\0');
            }
        }
        return nullConstant(List.of(), source);
    }

    @Override
    public Collector<OutputBuilder, OutputBuilder, OutputBuilder> outputBuilderJoining(OutputElement outputElement) {
        return OutputBuilderImpl.joining(outputElement);
    }

    @Override
    public ParameterizedType parameterizedTypeNullConstant() {
        return ParameterizedTypeImpl.NULL_CONSTANT;
    }

    @Override
    public ParameterizedType parameterizedTypeReturnTypeOfConstructor() {
        return ParameterizedTypeImpl.RETURN_TYPE_OF_CONSTRUCTOR;
    }

    @Override
    public ParameterizedType parameterizedTypeWildcard() {
        return ParameterizedTypeImpl.WILDCARD_PARAMETERIZED_TYPE;
    }

    private static final Pattern COMPACT2_PATTERN = Pattern.compile("(\\d+)-(\\d+):(\\d+)-(\\d+)");

    @Override
    public Source parseSourceFromCompact2(String compact2) {
        Matcher m = COMPACT2_PATTERN.matcher(compact2);
        if (m.matches()) {
            return new SourceImpl(null, Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
        }
        throw new UnsupportedOperationException("Illegal format: " + compact2);
    }

    @Override
    public Precedence precedenceAdditive() {
        return PrecedenceEnum.ADDITIVE;
    }

    @Override
    public Precedence precedenceArrayAccess() {
        return PrecedenceEnum.ACCESS;
    }

    @Override
    public Precedence precedenceAssignment() {
        return PrecedenceEnum.ASSIGNMENT;
    }

    @Override
    public Precedence precedenceBitwiseAnd() {
        return PrecedenceEnum.AND;
    }

    @Override
    public Precedence precedenceBitwiseOr() {
        return PrecedenceEnum.OR;
    }

    @Override
    public Precedence precedenceBitwiseXor() {
        return PrecedenceEnum.XOR;
    }

    @Override
    public Precedence precedenceBottom() {
        return PrecedenceEnum.BOTTOM;
    }

    @Override
    public Precedence precedenceEquality() {
        return PrecedenceEnum.EQUALITY;
    }

    @Override
    public Precedence precedenceLogicalAnd() {
        return PrecedenceEnum.LOGICAL_AND;
    }

    @Override
    public Precedence precedenceLogicalOr() {
        return PrecedenceEnum.LOGICAL_OR;
    }

    @Override
    public Precedence precedenceMultiplicative() {
        return PrecedenceEnum.MULTIPLICATIVE;
    }

    @Override
    public Precedence precedenceOfBinaryOperator(MethodInfo op) {
        Precedence precedence = precedenceMap.get(op);
        assert precedence != null;
        return precedence;
    }

    @Override
    public Precedence precedenceRelational() {
        return PrecedenceEnum.RELATIONAL;
    }

    @Override
    public Precedence precedenceShift() {
        return PrecedenceEnum.SHIFT;
    }

    @Override
    public Precedence precedenceTop() {
        return PrecedenceEnum.TOP;
    }

    @Override
    public Precedence precedenceUnary() {
        return PrecedenceEnum.UNARY;
    }

    @Override
    public Qualification qualificationFullyQualifiedNames() {
        return QualificationImpl.FULLY_QUALIFIED_NAMES;
    }

    @Override
    public Qualification qualificationQualifyFromPrimaryType() {
        return new QualificationImpl(false, TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE, null);
    }

    @Override
    public Qualification qualificationQualifyFromPrimaryType(Qualification.Decorator decorator) {
        return new QualificationImpl(false, TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE, decorator);
    }

    @Override
    public Qualification qualificationSimpleNames() {
        return QualificationImpl.SIMPLE_NAMES;
    }

    @Override
    public Set<TypeInfo> rewire(Set<TypeInfo> types) {
        InfoMap infoMap = new InfoMapImpl(types);
        Set<TypeInfo> rewired = new HashSet<>();
        for (TypeInfo typeInfo : types) {
            if (infoMap.typeInfoNullIfAbsent(typeInfo) == null) {
                rewired.add(typeInfo.rewirePhase1(infoMap));
            }
        }
        for (TypeInfo typeInfo : types) {
            typeInfo.rewirePhase2(infoMap);
        }
        for (TypeInfo typeInfo : types) {
            typeInfo.rewirePhase3(infoMap);
        }
        return Set.copyOf(rewired);
    }

    @Override
    public void setGetSetField(MethodInfo getSetMethod, FieldInfo fieldInfo, boolean setter, int parameterIndexOfIndex) {
        assert fieldInfo != null;
        assert !getSetMethod.isConstructor();
        if (!getSetMethod.analysis().haveAnalyzedValueFor(PropertyImpl.GET_SET_FIELD)) {
            getSetMethod.analysis().set(PropertyImpl.GET_SET_FIELD,
                    new ValueImpl.GetSetValueImpl(fieldInfo, setter, parameterIndexOfIndex));
        }
    }

    @Override
    public void setModificationComponent(MethodInfo methodInfo, FieldInfo component) {
        if (!methodInfo.analysis().haveAnalyzedValueFor(PropertyImpl.MODIFIED_COMPONENTS_METHOD)) {
            FieldReference fr = newFieldReference(component);
            ValueImpl.VariableBooleanMapImpl value = new ValueImpl.VariableBooleanMapImpl(Map.of(fr, true));
            methodInfo.analysis().set(PropertyImpl.MODIFIED_COMPONENTS_METHOD, value);
        }
    }

    @Override
    public Split splitNever() {
        return SplitEnum.NEVER;
    }

    @Override
    public OutputElement symbolColon() {
        return SymbolEnum.COLON;
    }

    @Override
    public OutputElement symbolComma() {
        return SymbolEnum.COMMA;
    }

    @Override
    public OutputElement symbolLeftParenthesis() {
        return SymbolEnum.LEFT_PARENTHESIS;
    }

    @Override
    public OutputElement symbolRightParenthesis() {
        return SymbolEnum.RIGHT_PARENTHESIS;
    }

    @Override
    public Variable translateVariableRecursively(TranslationMap translationMap, Variable variable) {
        return TranslationMapImpl.translateVariableRecursively(translationMap, variable);
    }

    @Override
    public TypeModifier typeModifierAbstract() {
        return TypeModifierEnum.ABSTRACT;
    }

    @Override
    public TypeModifier typeModifierFinal() {
        return TypeModifierEnum.FINAL;
    }

    @Override
    public TypeModifier typeModifierNonSealed() {
        return TypeModifierEnum.NON_SEALED;
    }

    @Override
    public TypeModifier typeModifierPrivate() {
        return TypeModifierEnum.PRIVATE;
    }

    @Override
    public TypeModifier typeModifierProtected() {
        return TypeModifierEnum.PROTECTED;
    }

    @Override
    public TypeModifier typeModifierPublic() {
        return TypeModifierEnum.PUBLIC;
    }

    @Override
    public TypeModifier typeModifierSealed() {
        return TypeModifierEnum.SEALED;
    }

    @Override
    public TypeModifier typeModifierStatic() {
        return TypeModifierEnum.STATIC;
    }

    @Override
    public TypeNature typeNatureAnnotation() {
        return TypeNatureEnum.ANNOTATION;
    }

    @Override
    public TypeNature typeNatureClass() {
        return TypeNatureEnum.CLASS;
    }

    @Override
    public TypeNature typeNatureEnum() {
        return TypeNatureEnum.ENUM;
    }

    @Override
    public TypeNature typeNatureInterface() {
        return TypeNatureEnum.INTERFACE;
    }

    @Override
    public TypeNature typeNaturePackageInfo() {
        return TypeNatureEnum.PACKAGE_INFO;
    }

    @Override
    public TypeNature typeNatureRecord() {
        return TypeNatureEnum.RECORD;
    }

    @Override
    public TypeNature typeNatureStub() {
        return TypeNatureEnum.STUB;
    }

    @Override
    public Wildcard wildcardExtends() {
        return WildcardEnum.EXTENDS;
    }

    @Override
    public Wildcard wildcardSuper() {
        return WildcardEnum.SUPER;
    }

    @Override
    public TextBlockFormatting.Builder newTextBlockFormattingBuilder() {
        return new TextBlockFormattingImpl.Builder();
    }

    @Override
    public TextBlock newTextBlock(List<Comment> comments,
                                  Source source,
                                  String content,
                                  TextBlockFormatting textBlockFormatting) {
        return new TextBlockImpl(comments, source, stringParameterizedType(), content, textBlockFormatting);
    }
}

