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

package io.codelaser.maddi.aapi.archive.libs.mockito;

import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.mockito.stubbing.Stubber;

import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;

/**
 * Mockito's stubbing chain (mockito-core 5.23), the {@code org.mockito.stubbing} half of OrgMockito, which explains
 * the reading: stored values and throwables {@code @NotModified}, an {@code Answer} {@code @IgnoreModifications}.
 */
public class OrgMockitoStubbing {
    public static final String PACKAGE_NAME = "org.mockito.stubbing";

    //public interface OngoingStubbing<T>
    class OngoingStubbing$<T> {
        OngoingStubbing<T> thenReturn(T value, @NotModified T... values) { return null; }
        OngoingStubbing<T> thenThrow(@NotModified Throwable... throwables) { return null; }
        OngoingStubbing<T> thenAnswer(@IgnoreModifications Answer<?> answer) { return null; }
        OngoingStubbing<T> then(@IgnoreModifications Answer<?> answer) { return null; }
    }

    //public interface BaseStubber
    class BaseStubber$ {
        Stubber doThrow(@NotModified Throwable... toBeThrown) { return null; }
        Stubber doAnswer(@IgnoreModifications Answer answer) { return null; }
        Stubber doReturn(Object toBeReturned, @NotModified Object... nextToBeReturned) { return null; }
    }
}
