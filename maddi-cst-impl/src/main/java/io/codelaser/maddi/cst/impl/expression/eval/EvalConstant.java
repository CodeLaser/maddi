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
import io.codelaser.maddi.util.IntUtil;

import java.util.Objects;

public class EvalConstant {
    private final Runtime runtime;

    public EvalConstant(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression equalsExpression(ConstantExpression<?> l, ConstantExpression<?> r) {
        if (l instanceof NullConstant || r instanceof NullConstant)
            throw new UnsupportedOperationException("Not for me");

        if (l instanceof StringConstant ls && r instanceof StringConstant rs) {
            return runtime.newBoolean(ls.constant().equals(rs.constant()));
        }
        if (l instanceof BooleanConstant lb && r instanceof BooleanConstant lr) {
            return runtime.newBoolean(Objects.equals(lb.constant(), lr.constant()));
        }
        if (l instanceof CharConstant lc && r instanceof CharConstant rc) {
            return runtime.newBoolean(Objects.equals(lc.constant(), rc.constant()));
        }
        if (l instanceof CharConstant lc && r instanceof Numeric rc
            && IntUtil.isMathematicalInteger(rc.doubleValue())) {
            return runtime.newBoolean(lc.constant() == (int) rc.doubleValue());
        }
        if (l instanceof Numeric lc && IntUtil.isMathematicalInteger(lc.doubleValue()) && r instanceof CharConstant rc) {
            return runtime.newBoolean((int) lc.doubleValue() == rc.constant());
        }
        if (l instanceof Numeric ln && r instanceof Numeric rn) {
            return runtime.newBoolean(ln.number().equals(rn.number()));
        }
        throw new UnsupportedOperationException("l = " + l + ", r = " + r);
    }
}
