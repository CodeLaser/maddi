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

package io.codelaser.maddi.analysis.api;

import java.util.List;
import java.util.Set;

import io.codelaser.maddi.cst.api.analysis.Message;
import io.codelaser.maddi.cst.api.info.Info;

/**
 * @param fingerprintedSourceSets the number of source sets whose fingerprint was stored; 0 unless asked for
 * @param scope                   null after a cold analysis. After a seeded one ({@link ModificationRequest#seed()}),
 *                                the elements it analysed: the seed, and whatever its worklist pulled in. The
 *                                {@code messages} are about these elements only; an element outside the scope kept
 *                                its values, and the messages an earlier analysis gave about it still hold
 */
public record ModificationOutcome(List<Message> messages, int fingerprintedSourceSets, Set<Info> scope) {

    /** A cold analysis' outcome: no scope. */
    public ModificationOutcome(List<Message> messages, int fingerprintedSourceSets) {
        this(messages, fingerprintedSourceSets, null);
    }
}
