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

package org.e2immu.analyzer.aapi.archive.jdk;

import org.e2immu.annotation.*;

import java.lang.constant.ConstantDesc;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

class JavaLangConstant {
    final static String PACKAGE_NAME = "java.lang.constant";

    @ImmutableContainer
    @Independent
    interface ConstantDesc$ {
        @NotNull
        Object resolveConstantDesc(MethodHandles.Lookup lookup);
    }

    @ImmutableContainer
    @Independent
    interface Constable$ {

        @NotNull
        Optional<? extends ConstantDesc> describeConstable();
    }
}

