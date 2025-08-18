package org.e2immu.language.cst.api.runtime;

import org.e2immu.language.cst.api.element.*;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.OutputElement;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.output.element.ElementarySpace;
import org.e2immu.language.cst.api.output.element.Split;
import org.e2immu.language.cst.api.output.element.TextBlockFormatting;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.*;
import org.e2immu.language.cst.api.variable.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;

public interface Factory {

    Access accessPackage();

    Access accessPrivate();

    Access accessProtected();

    Access accessPublic();

    ParameterizedType commonType(ParameterizedType pt1, ParameterizedType pt2);

    BooleanConstant constantFalse();

    BooleanConstant constantTrue();

    DescendMode descendModeNo();

    DescendMode descendModeYes();

    Diamond diamondNo();

    Diamond diamondShowAll();

    Diamond diamondYes();

    ElementarySpace elementarySpaceNice();

    ElementarySpace elementarySpaceRelaxedNone();

    Block emptyBlock();

    FieldModifier fieldModifierFinal();

    FieldModifier fieldModifierPrivate();

    FieldModifier fieldModifierProtected();

    FieldModifier fieldModifierPublic();

    FieldModifier fieldModifierStatic();

    FieldModifier fieldModifierTransient();

    FieldModifier fieldModifierVolatile();

    IntConstant intMinusOne();

    default IntConstant intOne() {
        return intOne(noSource());
    }

    IntConstant intOne(Source source);

    Numeric intOrDouble(double v);

    IntConstant intZero();

    int isAssignableFromCovariantErasure(ParameterizedType typeOfParameter, ParameterizedType actualType);

    int isNotAssignable();

    Lambda.OutputVariant lambdaOutputVariantEmpty();

    Lambda.OutputVariant lambdaOutputVariantTyped();

    Lambda.OutputVariant lambdaOutputVariantVar();

    LocalVariableCreation.Modifier localVariableModifierFinal();

    LocalVariableCreation.Modifier localVariableModifierVar();

    MethodInfo.MissingData methodMissingMethodBody();

    MethodModifier methodModifierAbstract();

    MethodModifier methodModifierDefault();

    MethodModifier methodModifierFinal();

    MethodModifier methodModifierPrivate();

    MethodModifier methodModifierProtected();

    MethodModifier methodModifierPublic();

    MethodModifier methodModifierStatic();

    MethodModifier methodModifierSynchronized();

    MethodInfo.MethodType methodTypeAbstractMethod();

    MethodInfo.MethodType methodTypeCompactConstructor();

    MethodInfo.MethodType methodTypeConstructor();

    MethodInfo.MethodType methodTypeDefaultMethod();

    MethodInfo.MethodType methodTypeMethod();

    MethodInfo.MethodType methodTypeStaticBlock();

    MethodInfo.MethodType methodTypeStaticMethod();

    MethodInfo.MethodType methodTypeSyntheticArrayConstructor();

    MethodInfo.MethodType methodTypeSyntheticConstructor();

    And.Builder newAndBuilder();

    AnnotationExpression.Builder newAnnotationExpressionBuilder();

    AnnotationExpression.KV newAnnotationExpressionKeyValuePair(String key, Expression value);

    TypeInfo newAnonymousType(TypeInfo enclosingType, int index);

    MethodInfo newArrayCreationConstructor(ParameterizedType returnType);

    ArrayInitializer.Builder newArrayInitializerBuilder();

    ArrayLength.Builder newArrayLengthBuilder();

    AssertStatement.Builder newAssertBuilder();

    Assignment newAssignment(VariableExpression target, Expression value);

    Assignment.Builder newAssignmentBuilder();

    BinaryOperator.Builder newBinaryOperatorBuilder();

    BitwiseNegation newBitwiseNegation(List<Comment> comments, Source source, Expression value);

    Block.Builder newBlockBuilder();

    BooleanConstant newBoolean(boolean value);

    BooleanConstant newBoolean(List<Comment> comments, Source source, boolean value);

    BreakStatement.Builder newBreakBuilder();

    ByteConstant newByte(byte b);

    ByteConstant newByte(List<Comment> comments, Source source, byte b);

    Cast newCast(Expression e, ParameterizedType parameterizedType);

    Cast.Builder newCastBuilder();

    TryStatement.CatchClause.Builder newCatchClauseBuilder();

    CharConstant newChar(char c);

    CharConstant newChar(List<Comment> comments, Source source, char c);

    ClassExpression.Builder newClassExpressionBuilder(ParameterizedType parameterizedType);

    CommaExpression.Builder newCommaBuilder();

    CompilationUnit.Builder newCompilationUnitBuilder();

    CompilationUnit newCompilationUnitStub(String candidatePackageName);

    Source newCompiledClassSource(CompilationUnit compilationUnit);

    MethodInfo newConstructor(TypeInfo owner);

    MethodInfo newConstructor(TypeInfo owner, MethodInfo.MethodType methodType);

    ConstructorCall.Builder newConstructorCallBuilder();

    ContinueStatement.Builder newContinueBuilder();

    DependentVariable newDependentVariable(Expression arrayExpression, Expression indexExpression);

    // Direct access, useful for synthetic constructs. Preferably use the other method, where
    // parameterizedType == arrayExpression.parameterizedType().copyWithOneFewerArrays().
    DependentVariable newDependentVariable(Expression arrayExpression,
                                           Expression indexExpression,
                                           ParameterizedType parameterizedType);

    DetailedSources.Builder newDetailedSourcesBuilder();

    DoStatement.Builder newDoBuilder();

    DoubleConstant newDouble(List<Comment> comments, Source source, double v);

    DoubleConstant newDouble(double d);

    EmptyExpression newEmptyExpression();

    EmptyExpression newEmptyExpression(String msg);

    EmptyStatement.Builder newEmptyStatementBuilder();

    EnclosedExpression.Builder newEnclosedExpressionBuilder();

    Equals newEquals(Expression lhs, Expression rhs);

    ExplicitConstructorInvocation.Builder newExplicitConstructorInvocationBuilder();

    ExpressionAsStatement newExpressionAsStatement(Expression standardized);

    ExpressionAsStatement.Builder newExpressionAsStatementBuilder();

    FieldInfo newFieldInfo(String name, boolean isStatic, ParameterizedType parameterizedType, TypeInfo owner);

    FieldReference newFieldReference(FieldInfo fieldInfo);

    FieldReference newFieldReference(FieldInfo fieldInfo, Expression scope, ParameterizedType concreteReturnType);

    FloatConstant newFloat(List<Comment> comments, Source source, float v);

    FloatConstant newFloat(float f);

    ForStatement.Builder newForBuilder();

    ForEachStatement.Builder newForEachBuilder();

    GreaterThanZero newGreaterThanZero(Expression e, boolean allowEquals);

    IfElseStatement.Builder newIfElseBuilder();

    default ImportComputer newImportComputer(int minForAsterisk) {
        return newImportComputer(minForAsterisk, null);
    }

    ImportComputer newImportComputer(int minForAsterisk, Function<String, Collection<TypeInfo>> typesPerPackage);

    ImportStatement.Builder newImportStatementBuilder();

    InfoMap newInfoMap(Set<TypeInfo> primaryTypes);

    InlineConditional newInlineConditional(Expression condition, Expression ifTrue, Expression ifFalse);

    InlineConditional.Builder newInlineConditionalBuilder();

    InstanceOf.Builder newInstanceOfBuilder();

    IntConstant newInt(List<Comment> comments, Source source, int i);

    IntConstant newInt(int i);

    JavaDoc newJavaDoc(Source source, String comment, List<JavaDoc.Tag> tags);

    JavaDoc.Tag newJavaDocTag(JavaDoc.TagIdentifier tagIdentifier, String content,
                              Info resolvedReference, Source source, Source sourceOfReference, boolean blockTag);

    Lambda.Builder newLambdaBuilder();

    LocalTypeDeclaration.Builder newLocalTypeDeclarationBuilder();

    LocalVariable newLocalVariable(ParameterizedType parameterizedType);

    LocalVariable newLocalVariable(String name, ParameterizedType parameterizedType);

    LocalVariable newLocalVariable(String name, ParameterizedType parameterizedType, Expression assignmentExpression);

    LocalVariableCreation newLocalVariableCreation(LocalVariable lvc);

    LocalVariableCreation.Builder newLocalVariableCreationBuilder();

    LongConstant newLong(List<Comment> comments, Source source, long l);

    LongConstant newLong(long l);

    MethodInfo newMethod(TypeInfo owner, String name, MethodInfo.MethodType methodType);

    MethodCall.Builder newMethodCallBuilder();

    MethodCall.Builder newMethodCallBuilder(MethodCall methodCall);

    MethodPrinter newMethodPrinter(MethodInfo methodInfo);

    MethodReference.Builder newMethodReferenceBuilder();

    ModuleInfo.Builder newModuleInfoBuilder();

    Expression newMultiExpressions(List<Expression> newExpressions);

    MultiLineComment newMultilineComment(Source source, String comment);

    NullConstant newNullConstant(List<Comment> comments, Source source);

    Or.Builder newOrBuilder();

    OutputBuilder newOutputBuilder();

    ParameterizedType newParameterizedType(TypeInfo typeInfo, List<ParameterizedType> newParameters);

    ParameterizedType newParameterizedType(TypeInfo typeInfo, int arrays);

    ParameterizedType newParameterizedType(TypeParameter typeParameter, int arrays, Wildcard wildCard);

    ParameterizedType newParameterizedType(TypeInfo typeInfo, int arrays, Wildcard wildCard,
                                           List<ParameterizedType> parameters);

    default Source newParserSource(String index, int beginLine, int beginPos, int endLine, int endPos) {
        return newParserSource(index, beginLine, beginPos, endLine, endPos, null);
    }

    Source newParserSource(String index, int beginLine, int beginPos, int endLine, int endPos,
                           DetailedSources detailedSources);

    RecordPattern.Builder newRecordPatternBuilder();

    ReturnStatement.Builder newReturnBuilder();

    ReturnStatement newReturnStatement(Expression expression);

    ShortConstant newShort(short s);

    ShortConstant newShort(List<Comment> comments, Source source, short s);

    SingleLineComment newSingleLineComment(Source source, String comment);

    SwitchEntry newStatementsSwitchEntry(VariableExpression selector,
                                         List<Expression> labels, List<Statement> statements);

    Expression newStringConcat(Expression l, Expression r);

    StringConstant newStringConstant(List<Comment> comments, Source source, String string);

    StringConstant newStringConstant(String string);

    SwitchEntry.Builder newSwitchEntryBuilder();

    SwitchExpression.Builder newSwitchExpressionBuilder();

    SwitchStatementOldStyle.SwitchLabel newSwitchLabelOldStyle(Expression literal, int pos,
                                                               RecordPattern patternVariable,
                                                               Expression whenExpression);

    SwitchStatementNewStyle.Builder newSwitchStatementNewStyleBuilder();

    SwitchStatementOldStyle.Builder newSwitchStatementOldStyleBuilder();

    SynchronizedStatement.Builder newSynchronizedBuilder();

    OutputElement newText(String text);

    TextBlock newTextBlock(List<Comment> comments, Source source, String content, TextBlockFormatting textBlockFormatting);

    TextBlockFormatting.Builder newTextBlockFormattingBuilder();

    default This newThis(ParameterizedType parameterizedType) {
        return newThis(parameterizedType, null, false);
    }

    This newThis(ParameterizedType parameterizedType, TypeInfo explicitlyWriteType, boolean writeSuper);

    ThrowStatement.Builder newThrowBuilder();

    TranslationMap.Builder newTranslationMapBuilder();

    TranslationMap.Builder newTranslationMapBuilder(TranslationMap startingPoint);

    TryStatement.Builder newTryBuilder();

    TypeExpression newTypeExpression(ParameterizedType parameterizedType, Diamond diamond);

    TypeExpression.Builder newTypeExpressionBuilder();

    TypeInfo newTypeInfo(TypeInfo typeInfo, String simpleName);

    TypeInfo newTypeInfo(MethodInfo methodInfo, String simpleName, int index);

    TypeInfo newTypeInfo(CompilationUnit cu, String simpleName);

    TypeParameter newTypeParameter(int index, String simpleName, Info owner);

    TypePrinter newTypePrinter(TypeInfo typeInfo, boolean formatter2);

    UnaryOperator newUnaryOperator(List<Comment> comments, Source source, MethodInfo operator, Expression e,
                                   Precedence precedence);

    LocalVariable newUnnamedLocalVariable(ParameterizedType parameterizedType, Expression assignmentExpression);

    VariableExpression newVariableExpression(Variable variable);

    VariableExpression.Builder newVariableExpressionBuilder();

    VariableExpression.Suffix newVariableFieldSuffix(int statementTime, String latestAssignment);

    WhileStatement.Builder newWhileBuilder();

    YieldStatement.Builder newYieldBuilder();

    Source noSource();

    Expression notNull(Expression expression);

    Expression nullConstant();

    Expression nullConstant(List<Comment> comments, Source source);

    Expression nullValue(ParameterizedType parameterizedType);

    Expression nullValue(ParameterizedType parameterizedType, Source source);

    Collector<OutputBuilder, OutputBuilder, OutputBuilder> outputBuilderJoining(OutputElement outputElement);

    ParameterizedType parameterizedTypeNullConstant();

    ParameterizedType parameterizedTypeReturnTypeOfConstructor();

    ParameterizedType parameterizedTypeWildcard();

    Source parseSourceFromCompact2(String compact2);

    Precedence precedenceAdditive();

    Precedence precedenceArrayAccess();

    Precedence precedenceAssignment();

    Precedence precedenceBitwiseAnd();

    Precedence precedenceBitwiseOr();

    Precedence precedenceBitwiseXor();

    Precedence precedenceBottom();

    Precedence precedenceEquality();

    Precedence precedenceLogicalAnd();

    Precedence precedenceLogicalOr();

    Precedence precedenceMultiplicative();

    Precedence precedenceOfBinaryOperator(MethodInfo op);

    Precedence precedenceRelational();

    Precedence precedenceShift();

    Precedence precedenceTop();

    Precedence precedenceUnary();

    Qualification qualificationFullyQualifiedNames();

    Qualification qualificationQualifyFromPrimaryType();

    Qualification qualificationQualifyFromPrimaryType(Qualification.Decorator decorator);

    Qualification qualificationSimpleNames();

    Set<TypeInfo> rewire(Set<TypeInfo> types);

    void setGetSetField(MethodInfo getSetMethod, FieldInfo fieldInfo, boolean setter, int parameterIndexOfIndex);

    void setModificationComponent(MethodInfo methodInfo, FieldInfo component);

    Split splitNever();

    OutputElement symbolColon();

    OutputElement symbolComma();

    OutputElement symbolLeftParenthesis();

    OutputElement symbolRightParenthesis();

    Variable translateVariableRecursively(TranslationMap translationMap, Variable variable);

    TypeModifier typeModifierAbstract();

    TypeModifier typeModifierFinal();

    TypeModifier typeModifierNonSealed();

    TypeModifier typeModifierPrivate();

    TypeModifier typeModifierProtected();

    TypeModifier typeModifierPublic();

    TypeModifier typeModifierSealed();

    TypeModifier typeModifierStatic();

    TypeNature typeNatureAnnotation();

    TypeNature typeNatureClass();

    TypeNature typeNatureEnum();

    TypeNature typeNatureInterface();

    TypeNature typeNaturePackageInfo();

    TypeNature typeNatureRecord();

    TypeNature typeNatureStub();

    Wildcard wildcardExtends();

    Wildcard wildcardSuper();
}
