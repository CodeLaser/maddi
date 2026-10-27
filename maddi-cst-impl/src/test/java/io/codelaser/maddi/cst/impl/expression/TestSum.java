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

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.expression.Expression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSum extends CommonTest {


    @Test
    public void test1() {
        Expression s = r.sum(r.intOne(), i);
        assertEquals("1+i", s.toString());
        Expression s2 = r.sum(r.newInt(2), s);
        assertEquals("3+i", s2.toString());
        Expression s3 = r.sum(i, r.intOne());
        assertEquals(s, s3);
        assertEquals("1+i", s3.toString());
    }

    @Test
    public void test4() {
        Expression s1 = r.sum(i, j);
        Expression s2 = r.sum(j, i);
        assertEquals(s1, s2);
        assertEquals("i+j", s1.toString());
        assertEquals("i+j", s2.toString());
    }

    @Test
    public void test2() {
        Expression s = r.sum(r.intOne(), r.sum(i, r.newInt(3)));
        assertEquals("4+i", s.toString());
        Expression s2 = r.sum(r.sum(r.newInt(3), r.newInt(2)), r.sum(s, r.newInt(-9)));
        assertEquals("i", s2.toString());
        Expression s3 = r.sum(r.sum(s2, r.newInt(3)), r.negate(s2));
        assertEquals("3", s3.toString());
    }

    @Test
    public void test3() {
        Expression s = r.sum(r.sum(r.intMinusOne(), r.product(j, r.newInt(-3))), r.product(r.newInt(2), i));
        assertEquals("-1+2*i+-3*j", s.toString());
    }
}
