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

package io.codelaser.maddi.cst.api.analysis;

import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.Info;

import java.util.List;

public interface Message {

    interface Level {
        boolean isWarning();

        boolean isError();
    }

    /** Concrete severities, shared by the parse path (Summary.ParseException) and the analysis path (Message). */
    enum Severity implements Level {
        WARN, ERROR;

        @Override
        public boolean isWarning() {
            return this == WARN;
        }

        @Override
        public boolean isError() {
            return this == ERROR;
        }
    }

    Source source();

    Info info();

    String message();

    Level level();

    /**
     * Machine-readable classification of the finding (e.g. "contract-violation", "hierarchy-inconsistency"),
     * for filtering and test assertions. Free-form, kebab-case by convention; emitters define constants.
     */
    default String category() {
        return "general";
    }

    /**
     * The evidence trail: nested messages explaining WHY this finding holds, e.g. the implementation method
     * that breaks a contracted property, which in turn may carry the statement that modifies a parameter.
     * Each cause is itself a located Message; levels of causes are informational only.
     */
    default List<Message> causes() {
        return List.of();
    }
}
