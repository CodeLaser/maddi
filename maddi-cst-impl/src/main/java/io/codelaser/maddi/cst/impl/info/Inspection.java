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

package io.codelaser.maddi.cst.impl.info;

import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.Access;

import java.util.List;

public interface Inspection {

    Access access();

    default boolean accessNotYetComputed() {
        return access() == null;
    }

    List<Comment> comments();

    Source source();

    boolean isSynthetic();

    default boolean isPublic() {
        return access().isPublic();
    }

    default boolean isPrivate() {
        return access().isPrivate();
    }

    default boolean isProtected() {
        return access().isProtected();
    }

    default boolean isPackagePrivate() {
        return access().isPackage();
    }

    @NotNull(content = true)
    List<AnnotationExpression> annotations();

    JavaDoc javaDoc();
}
