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

package io.codelaser.maddi.inspection.integration.java.example;

import io.codelaser.maddi.inspection.integration.java.importhelper.a.Resource;
import io.codelaser.maddi.inspection.integration.java.importhelper.a.Resources;

import static io.codelaser.maddi.inspection.integration.java.example.Annotations_2.XX;

// this file is here only to show that we can use a static import for XX!
// see TestAnnotations, INPUT3
@Resources({
        @Resource(name = XX, lookup = "yy", type = java.util.TreeMap.class),
        @Resource(name = Annotations_2.ZZ, type = Integer.class)
})
public class Annotations_2 {
    static final String XX = "xx";
    static final String ZZ = "zz";
}
