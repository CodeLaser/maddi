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

package org.e2immu.language.cst.impl.analysis;

import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.Info;

import java.util.List;

public record MessageImpl(Source source, Info info, Level level, String message, String category,
                          List<Message> causes) implements Message {
    public static final String GENERAL = "general";

    public MessageImpl {
        causes = causes == null ? List.of() : List.copyOf(causes);
        category = category == null ? GENERAL : category;
    }

    public MessageImpl(Source source, Info info, Level level, String message) {
        this(source, info, level, message, GENERAL, List.of());
    }

    public static Message warn(Info info, String message) {
        return new MessageImpl(sourceOf(info), info, Severity.WARN, message);
    }

    public static Message warn(Info info, String category, String message, Message... causes) {
        return new MessageImpl(sourceOf(info), info, Severity.WARN, message, category, List.of(causes));
    }

    public static Message error(Info info, String category, String message, Message... causes) {
        return new MessageImpl(sourceOf(info), info, Severity.ERROR, message, category, List.of(causes));
    }

    /** A cause carries evidence for a parent message; its severity is informational, we default to WARN. */
    public static Message cause(Info info, String message, Message... causes) {
        return new MessageImpl(sourceOf(info), info, Severity.WARN, message, GENERAL, List.of(causes));
    }

    // every element in the CST carries a Source (line/col); recover it so that messages are locatable
    private static Source sourceOf(Info info) {
        if (info == null) return null;
        Source source = info.source();
        return source == null || source.isNoSource() ? null : source;
    }
}
