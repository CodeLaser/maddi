/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.aapi.archive.libs.mockito;

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
        org.mockito.stubbing.OngoingStubbing<T> thenReturn(T value, @NotModified T... values) { return null; }
        org.mockito.stubbing.OngoingStubbing<T> thenThrow(@NotModified java.lang.Throwable... throwables) { return null; }
        org.mockito.stubbing.OngoingStubbing<T> thenAnswer(@IgnoreModifications org.mockito.stubbing.Answer<?> answer) { return null; }
        org.mockito.stubbing.OngoingStubbing<T> then(@IgnoreModifications org.mockito.stubbing.Answer<?> answer) { return null; }
    }

    //public interface BaseStubber
    class BaseStubber$ {
        org.mockito.stubbing.Stubber doThrow(@NotModified java.lang.Throwable... toBeThrown) { return null; }
        org.mockito.stubbing.Stubber doAnswer(@IgnoreModifications org.mockito.stubbing.Answer answer) { return null; }
        org.mockito.stubbing.Stubber doReturn(java.lang.Object toBeReturned, @NotModified java.lang.Object... nextToBeReturned) { return null; }
    }
}
