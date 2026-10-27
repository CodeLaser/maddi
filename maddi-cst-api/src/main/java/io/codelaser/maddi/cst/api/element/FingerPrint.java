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

package io.codelaser.maddi.cst.api.element;

/**
 * An opaque hash computed from a source file or an entire {@link SourceSet}, used to detect
 * whether the source has changed between analysis runs. A typical implementation uses an MD5
 * digest of the file contents.
 * <p>
 * Only {@link Object#equals} and {@link Object#toString} are meaningful for comparing
 * or logging fingerprints.
 */
public interface FingerPrint {

    /** Returns {@code true} for the sentinel value that represents the absence of a fingerprint. */
    boolean isNoFingerPrint();

}
