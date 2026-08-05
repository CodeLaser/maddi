/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, Bart Naudts, https://github.com/CodeLaser/maddi
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

// No `requires`: the annotations import nothing outside java.base. Keep it that way -- this is the
// artifact a user's own code compiles against, and anything added here lands in their classpath.
module io.codelaser.maddi.annotation {
    exports io.codelaser.maddi.annotation;
    exports io.codelaser.maddi.annotation.eventual;
    exports io.codelaser.maddi.annotation.type;
    exports io.codelaser.maddi.annotation.method;
    exports io.codelaser.maddi.annotation.rare;
}
