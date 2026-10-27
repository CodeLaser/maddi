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

package io.codelaser.maddi.inspection.impl.parser;

/**
 * Thrown by type resolution when a referenced type is not found on the (partial) classpath. Distinguished from
 * other resolution failures so the resolver surfaces it as a tolerable <em>warning</em> rather than a fatal error
 * — maddi analyzes with a deliberately partial classpath, so unresolved library types are expected, matching the
 * openjdk front-end (which reports the same as a Summary warning). Extends {@link UnsupportedOperationException}
 * so existing catch sites are unaffected.
 */
public class UnresolvedTypeException extends UnsupportedOperationException {
    public UnresolvedTypeException(String typeNameOrFqn) {
        super("Cannot find type " + typeNameOrFqn);
    }
}
