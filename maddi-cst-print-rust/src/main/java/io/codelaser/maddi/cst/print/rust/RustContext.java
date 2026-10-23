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

package io.codelaser.maddi.cst.print.rust;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.print.translate.PrintMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The state of printing one Java compilation unit as one Rust module: the facts, the messages, the {@code use}
 * declarations the printed text needs, and where the printer is. Passed along explicitly; one per file.
 */
final class RustContext {
    final RustFacts facts;
    /** The module being printed: a type in it needs no {@code use}. */
    final String module;
    private final List<PrintMessage<RustPrintCode>> messages = new ArrayList<>();
    private final Set<String> uses = new TreeSet<>();

    /** The type whose members are being printed. */
    TypeInfo type;
    /** The method whose body is being printed. */
    MethodInfo method;
    /**
     * How the body names the object under construction: {@code self} in a method, {@code this} in a constructor
     * body, which builds a {@code Self} into a local of that name before returning it.
     */
    String receiver = "self";

    RustContext(RustFacts facts, String module) {
        this.facts = facts;
        this.module = module;
    }

    void use(String path) {
        uses.add(path);
    }

    Set<String> uses() {
        return uses;
    }

    List<PrintMessage<RustPrintCode>> messages() {
        return messages;
    }

    void message(RustPrintCode code, Element element, String detail) {
        Source source = element == null ? null : element.source();
        String typeName = type == null ? "" : type.fullyQualifiedName();
        messages.add(new PrintMessage<>(code, typeName, source == null ? -1 : source.beginLine(),
                source == null ? -1 : source.beginPos(), detail));
    }
}
