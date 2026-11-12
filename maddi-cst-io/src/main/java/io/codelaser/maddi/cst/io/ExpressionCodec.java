/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.io;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.runtime.PredefinedWithoutParameterizedType;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.type.Diamond;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.Variable;

public class ExpressionCodec {
    private final Codec codec;
    private final Codec.Context context;
    private final Runtime runtime;
    private final Map<String, ECodec> map = new HashMap<>();

    public ExpressionCodec(Runtime runtime, Codec codec, Codec.Context context) {
        this.runtime = runtime;
        this.codec = codec;
        this.context = context;
        map.put(VariableExpression.NAME, new VariableExpressionCodec());
        map.put(EnclosedExpression.NAME, new EnclosedExpressionCodec());
        map.put(ConstructorCall.NAME, new ConstructorCallCodec());
        map.put(NullConstant.NAME, new NullConstantCodec());
        map.put(StringConstant.NAME, new StringConstantCodec());
        map.put(Cast.NAME, new CastCodec());
        map.put(BooleanConstant.NAME, new BooleanConstantCodec());
        map.put(IntConstant.NAME, new IntConstantCodec());
        map.put(TypeExpression.NAME, new TypeExpressionCodec());
        // the expressions a link names that the ones above do not cover: the scope of a field reference
        // (a.b().c), the index of an array element, the assigned value of a marker variable; without them the whole
        // value was left out of a stored analysis (fernflower: 59 of 777 values not encoded, 2026-11-12)
        map.put(MethodCall.NAME, new MethodCallCodec());
        map.put(CharConstant.NAME, new CharConstantCodec());
        map.put(LongConstant.NAME, new NumberConstantCodec());
        map.put(DoubleConstant.NAME, new NumberConstantCodec());
        map.put(FloatConstant.NAME, new NumberConstantCodec());
        map.put(BinaryOperator.NAME, new BinaryOperatorCodec());
        map.put(UnaryOperator.NAME, new UnaryOperatorCodec());
        map.put(InlineConditional.NAME, new InlineConditionalCodec());
        // and those guava's links add (2026-11-12)
        map.put(Assignment.NAME, new AssignmentCodec());
        map.put(ClassExpression.NAME, new ClassExpressionCodec());
        map.put(ArrayLength.NAME, new ArrayLengthCodec());
    }

    /*
     The operators are the runtime's own MethodInfo objects (int.+, boolean.!, ...), one of each, so they are written
     by the name of the getter that hands them out, and read back through it.
     */
    private Map<MethodInfo, String> operatorNames;
    private Map<String, MethodInfo> operatorsByName;

    private void operators() {
        if (operatorNames != null) return;
        operatorNames = new IdentityHashMap<>();
        operatorsByName = new HashMap<>();
        for (Method m : PredefinedWithoutParameterizedType.class.getMethods()) {
            if (m.getParameterCount() != 0 || m.getReturnType() != MethodInfo.class) continue;
            try {
                MethodInfo operator = (MethodInfo) m.invoke(runtime);
                if (operator != null && !operatorNames.containsKey(operator)) {
                    operatorNames.put(operator, m.getName());
                    operatorsByName.put(m.getName(), operator);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Cannot read the operator " + m.getName(), e);
            }
        }
    }

    private Codec.EncodedValue encodeOperator(MethodInfo operator) {
        operators();
        String name = operatorNames.get(operator);
        if (name == null) throw new UnsupportedOperationException("Not a predefined operator: " + operator);
        return codec.encodeString(context, name);
    }

    private Codec.EncodedValue encodeOperatorOrNull(MethodInfo operator) {
        return operator == null ? codec.encodeString(context, "") : encodeOperator(operator);
    }

    private MethodInfo decodeOperatorOrNull(Codec.EncodedValue ev) {
        return codec.decodeString(context, ev).isEmpty() ? null : decodeOperator(ev);
    }

    private MethodInfo decodeOperator(Codec.EncodedValue ev) {
        operators();
        String name = codec.decodeString(context, ev);
        MethodInfo operator = operatorsByName.get(name);
        if (operator == null) throw new UnsupportedOperationException("Not a predefined operator: " + name);
        return operator;
    }

    private Source source(Codec.EncodedValue ev) {
        return runtime.parseSourceFromCompact2(codec.decodeString(context, ev));
    }

    private interface ECodec {
        List<Codec.EncodedValue> encode(Expression e);

        Expression decode(List<Codec.EncodedValue> list);
    }

    public Codec.EncodedValue encodeExpression(Expression e) {
        if (e == null) {
            return codec.encodeList(context, List.of());
        }
        ECodec eCodec = map.get(e.name());
        assert eCodec != null : "No codec yet for " + e.name();
        List<Codec.EncodedValue> list = eCodec.encode(e);
        Codec.EncodedValue e0 = codec.encodeString(context, e.name());
        Source source = e.source() != null ? e.source() : runtime.noSource();
        Codec.EncodedValue e1 = codec.encodeString(context, source.compact2());
        return codec.encodeList(context, Stream.concat(Stream.of(e0, e1), list.stream()).toList());
    }

    public Expression decodeExpression(Codec.EncodedValue encodedValue) {
        List<Codec.EncodedValue> list = codec.decodeList(context, encodedValue);
        if (list.isEmpty()) return null;
        String name = codec.decodeString(context, list.getFirst());
        ECodec eCodec = map.get(name);
        return eCodec.decode(list);
    }

    class VariableExpressionCodec implements ECodec {

        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            return List.of(codec.encodeVariable(context, ((VariableExpression) e).variable()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            String compact2 = codec.decodeString(context, list.get(1));
            Variable variable = codec.decodeVariable(context, list.get(2));
            return runtime.newVariableExpressionBuilder()
                    .setVariable(variable)
                    .setSource(runtime.parseSourceFromCompact2(compact2))
                    .build();
        }
    }

    class EnclosedExpressionCodec implements ECodec {

        @Override
        public List<Codec.EncodedValue> encode(Expression enclosedExpression) {
            return List.of(encodeExpression(((EnclosedExpression) enclosedExpression).inner()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            String compact2 = codec.decodeString(context, list.get(1));
            Expression inner = decodeExpression(list.get(2));
            return runtime.newEnclosedExpressionBuilder().setSource(runtime.parseSourceFromCompact2(compact2))
                    .setExpression(inner).build();
        }
    }


    class StringConstantCodec implements ECodec {

        @Override
        public List<Codec.EncodedValue> encode(Expression stringConstant) {
            return List.of(codec.encodeString(context, ((StringConstant) stringConstant).constant()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            String compact2 = codec.decodeString(context, list.get(1));
            String constant = codec.decodeString(context, list.get(2));
            return runtime.newStringConstant(List.of(), runtime.parseSourceFromCompact2(compact2), constant);
        }
    }

    class ConstructorCallCodec implements ECodec {

        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            ConstructorCall cc = (ConstructorCall) e;
            return List.of(codec.encodeInfoOutOfContext(context, cc.constructor()),
                    encodeDiamond(cc.diamond()),
                    codec.encodeType(context, cc.parameterizedType()),
                    codec.encodeExpression(context, cc.object()),
                    codec.encodeList(context, cc.parameterExpressions().stream()
                            .map(ExpressionCodec.this::encodeExpression).toList()),
                    codec.encodeExpression(context, cc.arrayInitializer()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            String source = codec.decodeString(context, list.get(1));
            MethodInfo constructor = (MethodInfo) codec.decodeInfoOutOfContext(context, list.get(2));
            Diamond diamond = decodeDiamond(list.get(3));
            ParameterizedType concreteReturnType = codec.decodeType(context, list.get(4));
            Expression object = codec.decodeExpression(context, list.get(5));
            List<Expression> arguments = codec.decodeList(context, list.get(6))
                    .stream().map(e -> codec.decodeExpression(context, e)).toList();
            ArrayInitializer arrayInitializer = (ArrayInitializer) codec.decodeExpression(context, list.get(7));
            return runtime.newConstructorCallBuilder()
                    .setSource(runtime.parseSourceFromCompact2(source))
                    .setConstructor(constructor)
                    .setDiamond(diamond)
                    .setConcreteReturnType(concreteReturnType)
                    .setObject(object)
                    .setParameterExpressions(arguments)
                    .setArrayInitializer(arrayInitializer)
                    .build();
        }

        private Codec.EncodedValue encodeDiamond(Diamond diamond) {
            String s;
            if (diamond.isNo()) s = "N";
            else if (diamond.isYes()) s = "Y";
            else if (diamond.isShowAll()) s = "A";
            else throw new UnsupportedOperationException();
            return codec.encodeString(context, s);
        }

        private Diamond decodeDiamond(Codec.EncodedValue encodedValue) {
            String s = codec.decodeString(context, encodedValue);
            return switch (s) {
                case "Y" -> runtime.diamondYes();
                case "N" -> runtime.diamondNo();
                case "A" -> runtime.diamondShowAll();
                default -> throw new UnsupportedOperationException();
            };
        }
    }

    class NullConstantCodec implements ECodec {

        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            return List.of();
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            String source = codec.decodeString(context, list.get(1));
            return runtime.newNullConstant(List.of(), runtime.parseSourceFromCompact2(source));
        }
    }

    // a type used as an expression (e.g. in a marker variable's assignment expression); surfaced by
    // the fernflower results-write after the 2026-07-19 merge
    class TypeExpressionCodec implements ECodec {

        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            TypeExpression te =
                    (TypeExpression) e;
            return List.of(codec.encodeType(context, te.parameterizedType()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            String source = codec.decodeString(context, list.get(1));
            ParameterizedType type = codec.decodeType(context, list.get(2));
            return runtime.newTypeExpressionBuilder()
                    .setSource(runtime.parseSourceFromCompact2(source))
                    .setParameterizedType(type)
                    .setDiamond(runtime.diamondNo())
                    .build();
        }
    }

    class CastCodec implements ECodec {

        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            Cast cast = (Cast) e;
            return List.of(codec.encodeType(context, cast.parameterizedType()),
                    codec.encodeExpression(context, cast.expression()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            String source = codec.decodeString(context, list.get(1));
            ParameterizedType type = codec.decodeType(context, list.get(2));
            Expression expression = codec.decodeExpression(context, list.get(3));
            return runtime.newCastBuilder()
                    .setSource(runtime.parseSourceFromCompact2(source))
                    .setParameterizedType(type).setExpression(expression)
                    .build();
        }
    }

    class BooleanConstantCodec implements ECodec {

        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            return List.of(codec.encodeBoolean(context, e.isBoolValueTrue()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            String source = codec.decodeString(context, list.get(1));
            return runtime.newBoolean(List.of(), runtime.parseSourceFromCompact2(source),
                    codec.decodeBoolean(context, list.get(2)));
        }
    }

    class IntConstantCodec implements ECodec {
        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            return List.of(codec.encodeInt(context, ((IntConstant) e).constant()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            String source = codec.decodeString(context, list.get(1));
            int value = codec.decodeInt(context, list.get(2));
            return runtime.newInt(List.of(), runtime.parseSourceFromCompact2(source), value);
        }
    }

    class MethodCallCodec implements ECodec {
        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            MethodCall mc = (MethodCall) e;
            return List.of(codec.encodeInfoOutOfContext(context, mc.methodInfo()),
                    encodeExpression(mc.object()),
                    codec.encodeList(context, mc.parameterExpressions().stream()
                            .map(ExpressionCodec.this::encodeExpression).toList()),
                    codec.encodeType(context, mc.concreteReturnType()),
                    codec.encodeBoolean(context, mc.objectIsImplicit()),
                    codec.encodeList(context, mc.typeArguments().stream()
                            .map(t -> codec.encodeType(context, t)).toList()),
                    codec.encodeString(context, mc.modificationTimes() == null ? "" : mc.modificationTimes()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            List<ParameterizedType> typeArguments = codec.decodeList(context, list.get(7)).stream()
                    .map(ev -> codec.decodeType(context, ev)).toList();
            return runtime.newMethodCallBuilder()
                    .setSource(source(list.get(1)))
                    .setMethodInfo((MethodInfo) codec.decodeInfoOutOfContext(context, list.get(2)))
                    .setObject(decodeExpression(list.get(3)))
                    .setParameterExpressions(codec.decodeList(context, list.get(4)).stream()
                            .map(ExpressionCodec.this::decodeExpression).toList())
                    .setConcreteReturnType(codec.decodeType(context, list.get(5)))
                    .setObjectIsImplicit(codec.decodeBoolean(context, list.get(6)))
                    .setTypeArguments(typeArguments)
                    .setModificationTimes(codec.decodeString(context, list.get(8)))
                    .build();
        }
    }

    class CharConstantCodec implements ECodec {
        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            return List.of(codec.encodeInt(context, ((CharConstant) e).constant()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            return runtime.newChar(List.of(), source(list.get(1)), (char) codec.decodeInt(context, list.get(2)));
        }
    }

    // long, double and float, as text: a JSON number need not keep a long's or a double's every bit
    class NumberConstantCodec implements ECodec {
        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            Object constant = ((ConstantExpression<?>) e).constant();
            return List.of(codec.encodeString(context, String.valueOf(constant)));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            String name = codec.decodeString(context, list.getFirst());
            Source source = source(list.get(1));
            String value = codec.decodeString(context, list.get(2));
            return switch (name) {
                case LongConstant.NAME -> runtime.newLong(List.of(), source, Long.parseLong(value));
                case DoubleConstant.NAME -> runtime.newDouble(List.of(), source, Double.parseDouble(value));
                case FloatConstant.NAME -> runtime.newFloat(List.of(), source, Float.parseFloat(value));
                default -> throw new UnsupportedOperationException(name);
            };
        }
    }

    class BinaryOperatorCodec implements ECodec {
        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            BinaryOperator bo = (BinaryOperator) e;
            return List.of(encodeOperator(bo.operator()), encodeExpression(bo.lhs()), encodeExpression(bo.rhs()),
                    codec.encodeType(context, bo.parameterizedType()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            MethodInfo operator = decodeOperator(list.get(2));
            return runtime.newBinaryOperatorBuilder()
                    .setSource(source(list.get(1)))
                    .setOperator(operator)
                    .setLhs(decodeExpression(list.get(3)))
                    .setRhs(decodeExpression(list.get(4)))
                    .setParameterizedType(codec.decodeType(context, list.get(5)))
                    .setPrecedence(runtime.precedenceOfBinaryOperator(operator))
                    .build();
        }
    }

    class UnaryOperatorCodec implements ECodec {
        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            UnaryOperator uo = (UnaryOperator) e;
            return List.of(encodeOperator(uo.operator()), encodeExpression(uo.expression()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            return runtime.newUnaryOperator(List.of(), source(list.get(1)), decodeOperator(list.get(2)),
                    decodeExpression(list.get(3)), runtime.precedenceUnary());
        }
    }

    class InlineConditionalCodec implements ECodec {
        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            InlineConditional ic = (InlineConditional) e;
            return List.of(encodeExpression(ic.condition()), encodeExpression(ic.ifTrue()),
                    encodeExpression(ic.ifFalse()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            return runtime.newInlineConditionalBuilder()
                    .setSource(source(list.get(1)))
                    .setCondition(decodeExpression(list.get(2)))
                    .setIfTrue(decodeExpression(list.get(3)))
                    .setIfFalse(decodeExpression(list.get(4)))
                    .build(runtime);
        }
    }

    class AssignmentCodec implements ECodec {
        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            Assignment a = (Assignment) e;
            Boolean prefix = a.prefixPrimitiveOperator();
            return List.of(encodeExpression(a.target()), encodeExpression(a.value()),
                    encodeOperatorOrNull(a.assignmentOperator()),
                    codec.encodeString(context, prefix == null ? "" : prefix.toString()),
                    encodeOperatorOrNull(a.binaryOperator()),
                    codec.encodeBoolean(context, a.assignmentOperatorIsPlus()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            String prefix = codec.decodeString(context, list.get(5));
            return runtime.newAssignmentBuilder()
                    .setSource(source(list.get(1)))
                    .setTarget((VariableExpression) decodeExpression(list.get(2)))
                    .setValue(decodeExpression(list.get(3)))
                    .setAssignmentOperator(decodeOperatorOrNull(list.get(4)))
                    .setPrefixPrimitiveOperator(prefix.isEmpty() ? null : Boolean.valueOf(prefix))
                    .setBinaryOperator(decodeOperatorOrNull(list.get(6)))
                    .setAssignmentOperatorIsPlus(codec.decodeBoolean(context, list.get(7)))
                    .build();
        }
    }

    class ClassExpressionCodec implements ECodec {
        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            return List.of(codec.encodeType(context, ((ClassExpression) e).type()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            return runtime.newClassExpressionBuilder(codec.decodeType(context, list.get(2)))
                    .setSource(source(list.get(1))).build();
        }
    }

    class ArrayLengthCodec implements ECodec {
        @Override
        public List<Codec.EncodedValue> encode(Expression e) {
            return List.of(encodeExpression(((ArrayLength) e).scope()));
        }

        @Override
        public Expression decode(List<Codec.EncodedValue> list) {
            return runtime.newArrayLengthBuilder().setSource(source(list.get(1)))
                    .setExpression(decodeExpression(list.get(2))).build();
        }
    }
}
