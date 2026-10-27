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

package io.codelaser.maddi.inspection.integration.java.importhelper.access;

public class AccessExample_1 {

    private static class I1 {

        public static final int I = 42; // effective visibility = private
        private static final int J = 420; // effective visibility = private

        public static class I2 {
            public static final int IK = 43; // effective visibility = private
            private static final int JK = 430; // effective visibility = private
        }
    }


    public static class I3 {
        static int L = 32; // effective visibility = package-private
        protected static int M = 33; // effective visibility = protected (members of same package only, because static)

        protected int N = 3; // effective visibility = protected (subclasses and members of same package)

        @Override
        public String toString() { // must be public, overrides public method
            return "i=" + I1.I + ", j=" + I1.J + ", ik=" + I1.I2.IK
                    + ", jk=" + AccessExample_1.I1.I2.JK
                    + ", jk=" + io.codelaser.maddi.inspection.integration.java.importhelper.access.AccessExample_1.I1.I2.JK; // all in the same primary type
        }
    }
}
