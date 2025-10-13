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

package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.type.IsAssignableFrom;
import org.e2immu.util.internal.util.IntUtil;

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
