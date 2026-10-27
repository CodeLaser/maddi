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

package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.TypeInfo;

import java.util.function.Consumer;

/** Every body of a type, its nested, anonymous and local types included, for the tests (KotlinReadOnly's walk). */
public final class KotlinReadOnlyTestAccess {
    private KotlinReadOnlyTestAccess() {
    }

    public static void forEachBody(TypeInfo type, Consumer<Element> consumer) {
        KotlinReadOnly.forEachBody(type, (method, body) -> consumer.accept(body));
    }
}
