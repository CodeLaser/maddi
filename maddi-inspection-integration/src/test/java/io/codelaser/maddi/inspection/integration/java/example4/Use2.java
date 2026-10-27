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

package io.codelaser.maddi.inspection.integration.java.example4;


import static io.codelaser.maddi.inspection.integration.java.example4.impl.StaticValuesImpl.SV;
import static io.codelaser.maddi.inspection.integration.java.example4.impl.StaticValuesImpl.SubValue;

public class Use2 {
    SubValue sv;
    //not accessible: StaticValues sv;
    //not accessible: Bool bool;
    //not accessible: Immutable immutable;
    //not accessible: ImmutableImpl i;
    //not accessible: ValueImpl v;
    //not accessible: Value v;

    Object method() {
        return SV;
    }
}
