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

package io.codelaser.maddi.cst.impl.expression.eval;

import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.type.IsAssignableFrom;
import io.codelaser.maddi.util.IntUtil;

public class EvalCast {
    private final Runtime runtime;

    public EvalCast(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression eval(Expression e, Cast cast) {
        ParameterizedType castType = cast.parameterizedType();
        IsAssignableFrom isAssignableFrom = new IsAssignableFrom(runtime, castType, e.parameterizedType());
        int score = isAssignableFrom.execute(false, true,
                IsAssignableFrom.Mode.COVARIANT);
        if (score >= 0) return e;
        if (!(e instanceof ConstantExpression<?>)) return cast;
        Expression ee = redundantIntegerCast(e, castType);
        if (ee != null) return ee;
        Expression eee = redundantDoubleCast(e, castType);
        if (eee != null) return eee;
        return cast;
    }

    private Expression redundantDoubleCast(Expression e, ParameterizedType castType) {
        double d;
        if (e instanceof FloatConstant fc) d = fc.doubleValue();
        else if (e instanceof DoubleConstant dc) d = dc.doubleValue();
        else return null;
        if (castType.isFloat()) return runtime.newFloat((float) d);
        if (castType.isDouble()) return runtime.newDouble(d);
        return null;
    }

    private Expression redundantIntegerCast(Expression e, ParameterizedType castType) {
        long l;
        if (e instanceof ByteConstant bc) l = bc.constant();
        else if (e instanceof ShortConstant sc) l = sc.constant();
        else if (e instanceof IntConstant ic) l = ic.constant();
        else if (e instanceof LongConstant lc) l = lc.constant();
        else if (e instanceof DoubleConstant dc && IntUtil.isMathematicalInteger(dc.constant()))
            l = (long) dc.doubleValue();
        else if (e instanceof FloatConstant fc && IntUtil.isMathematicalInteger(fc.doubleValue()))
            l = (long) fc.doubleValue();
        else return null;
        if (castType.isByte()) return runtime.newByte((byte) l);
        if (castType.isShort()) return runtime.newShort((short) l);
        if (castType.isInt()) return runtime.newInt((int) l);
        if (castType.isLong()) return runtime.newLong(l);
        return null;
    }
}
