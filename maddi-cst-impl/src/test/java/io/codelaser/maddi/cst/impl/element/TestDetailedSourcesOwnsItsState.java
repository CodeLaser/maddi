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

package io.codelaser.maddi.cst.impl.element;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A built {@link DetailedSources} owns its state (#51): before, {@code build()} handed over the builder's maps and
 * {@code details()} returned the builder's live lists, so a later {@code put} on the builder changed an object
 * already built, and a caller could append to what {@code details()} returned. The analyzer read that, correctly,
 * as {@code @Dependent}, and through {@code Source.detailedSources()} it capped the {@code Element} family.
 */
public class TestDetailedSourcesOwnsItsState {
    private final Source s1 = new SourceImpl("1", 1, 1, 1, 5);
    private final Source s2 = new SourceImpl("2", 1, 7, 1, 9);
    private final Source s3 = new SourceImpl("3", 2, 1, 2, 3);
    private final Object key = new Object();

    @DisplayName("a put on the builder after build() does not reach the built object")
    @Test
    public void builderMutationAfterBuild() {
        DetailedSources.Builder builder = new DetailedSourcesImpl.BuilderImpl();
        builder.put(key, s1).put(key, s2);
        DetailedSources built = builder.build();
        builder.put(key, s3);
        assertEquals(List.of(s1, s2), built.details(key));
    }

    @DisplayName("details() hands out an immutable list")
    @Test
    public void detailsIsImmutable() {
        DetailedSources.Builder builder = new DetailedSourcesImpl.BuilderImpl();
        builder.put(key, s1).put(key, s2);
        DetailedSources built = builder.build();
        assertThrows(UnsupportedOperationException.class, () -> built.details(key).add(s3));
    }

    @DisplayName("putList and withSources do not keep the caller's list")
    @Test
    public void callerListsAreCopied() {
        List<Source> mine = new ArrayList<>(List.of(s1));
        DetailedSources built = new DetailedSourcesImpl.BuilderImpl().putList(key, mine).build();
        DetailedSources with = built.withSources(key, mine);
        mine.add(s2);
        assertEquals(List.of(s1), built.details(key));
        assertEquals(List.of(s1), with.details(key));
    }
}
