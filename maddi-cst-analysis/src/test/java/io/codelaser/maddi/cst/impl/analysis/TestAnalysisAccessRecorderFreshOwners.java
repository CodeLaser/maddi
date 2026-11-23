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
package io.codelaser.maddi.cst.impl.analysis;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.analysis.Property;

/**
 * A property map created during a computation holds that computation's output: checking a slot before writing it
 * there is no input. Recorded, every computation that creates its own maps came out "reproduced", unusable for the
 * link cache. A map that existed before the computation still counts as input.
 */
public class TestAnalysisAccessRecorderFreshOwners {
    private static final Property P = new PropertyImpl("p");

    private static AnalysisAccessRecorder.Recording record(Runnable computation) {
        AnalysisAccessRecorder.arm();
        AnalysisAccessRecorder.Recording recording = AnalysisAccessRecorder.start();
        try {
            computation.run();
        } finally {
            AnalysisAccessRecorder.stop(recording);
        }
        return recording;
    }

    // the pattern of an overwrite check: look, then write
    private static void checkThenWrite(PropertyValueMapImpl map) {
        if (map.getOrNull(P, ValueImpl.BoolImpl.class) == null) map.set(P, ValueImpl.BoolImpl.TRUE);
    }

    @Test
    public void mapCreatedInsideIsOutput() {
        AnalysisAccessRecorder.Recording recording = record(() -> {
            PropertyValueMapImpl own = new PropertyValueMapImpl();
            checkThenWrite(own);
            assertFalse(own.isEmpty());
            own.propertyValueStream().count();
        });
        assertNull(recording.unusable());
        assertEquals(0, recording.size());
        assertNull(recording.firstChange(null));
    }

    @Test
    public void mapFromBeforeIsInput() {
        PropertyValueMapImpl existing = new PropertyValueMapImpl();
        AnalysisAccessRecorder.Recording recording = record(() -> checkThenWrite(existing));
        assertEquals("reproduced:p", recording.unusable());
    }

    @Test
    public void createdInNestedIsOutputOfEnclosingToo() {
        PropertyValueMapImpl[] made = new PropertyValueMapImpl[1];
        AnalysisAccessRecorder.Recording outer = record(() -> {
            AnalysisAccessRecorder.Recording inner = AnalysisAccessRecorder.start();
            made[0] = new PropertyValueMapImpl();
            checkThenWrite(made[0]);
            AnalysisAccessRecorder.stop(inner);
            assertNull(inner.unusable());
            checkThenWrite(made[0]);
        });
        assertNull(outer.unusable());
        assertEquals(0, outer.size());
    }
}
