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
 * isEmpty() on a property map is a read of one slot: a recording that asked it stays reusable while the answer
 * holds, and reports the emptiness slot once it changes. Reading the whole map still makes a recording unusable.
 */
public class TestAnalysisAccessRecorderEmptiness {
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

    @Test
    public void emptinessIsOneSlot() {
        PropertyValueMapImpl map = new PropertyValueMapImpl();
        AnalysisAccessRecorder.Recording recording = record(() -> assertTrue(map.isEmpty()));
        assertNull(recording.unusable());
        assertNull(recording.firstChange(null));
        map.set(P, ValueImpl.BoolImpl.TRUE);
        assertEquals("<isEmpty>", recording.firstChange(null));
    }

    @Test
    public void nonEmptyStaysNonEmpty() {
        PropertyValueMapImpl map = new PropertyValueMapImpl();
        map.set(P, ValueImpl.BoolImpl.TRUE);
        AnalysisAccessRecorder.Recording recording = record(() -> assertFalse(map.isEmpty()));
        assertNull(recording.firstChange(null));
        map.set(new PropertyImpl("q"), ValueImpl.BoolImpl.FALSE);
        assertNull(recording.firstChange(null), "another property, still not empty");
    }

    @Test
    public void wholeMapReadIsUnusable() {
        PropertyValueMapImpl map = new PropertyValueMapImpl();
        AnalysisAccessRecorder.Recording recording = record(() -> map.propertyValueStream().count());
        assertEquals("readAll", recording.unusable());
    }
}
