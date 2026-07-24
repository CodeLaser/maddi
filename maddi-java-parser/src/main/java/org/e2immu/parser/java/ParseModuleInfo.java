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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.Summary;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;

import java.util.ArrayList;
import java.util.List;

public class ParseModuleInfo extends CommonParse {
    public ParseModuleInfo(Runtime runtime, Parsers parsers) {
        super(runtime, parsers);
    }

    public ModuleInfo parse(ModularCompilationUnit mcu, CompilationUnit compilationUnit, Context context) {
        ModuleInfo.Builder builder = runtime.newModuleInfoBuilder();
        DetailedSources.Builder detailedSourcesBuilder = context.newDetailedSourcesBuilder();
        int i = 0;
        boolean openModule;
        if (mcu.get(i) instanceof Token kwOpen && kwOpen.getType() == Token.TokenType.OPEN) {
            ++i;
            openModule = true;
        } else {
            openModule = false;
        }
        if (mcu.get(i) instanceof Token kw && kw.getType() == Token.TokenType.MODULE) {
            ++i;
        } else throw new Summary.ParseException(context, "Expect keyword 'module'");
        if (mcu.get(i) instanceof Name name) {
            String n = name.getSource();
            builder.setName(n);
            if (detailedSourcesBuilder != null) detailedSourcesBuilder.put(n, source(name));
            i += 2;
        } else throw new Summary.ParseException(context, "Expect name after keyword 'module'");
        for (; i < mcu.size(); ++i) {
            Node nodeI = mcu.get(i);
            switch (nodeI) {
                case RequiresDirective rd -> processRequired(context, rd, builder);
                case ExportsDirective ed -> processExportsDirective(context, ed, builder);
                case OpensDirective od -> processOpensDirective(context, od, builder);
                case UsesDirective ud -> processUsesDirective(context, ud, builder);
                case ProvidesDirective pd -> processProvidesDirective(context, pd, builder);
                default -> {
                }
            }
        }
        Source source = source(mcu);
        return builder
                .setOpen(openModule)
                .setCompilationUnit(compilationUnit)
                .setSource(detailedSourcesBuilder == null ? source : source.withDetailedSources(detailedSourcesBuilder.build()))
                .addComments(comments(mcu)).build();
    }

    private void processProvidesDirective(Context context, ProvidesDirective pd, ModuleInfo.Builder builder) {
        DetailedSources.Builder dsb = context.newDetailedSourcesBuilder();
        Node apiNode = pd.get(1);
        String api = apiNode.getSource();
        if (dsb != null) dsb.put(api, source(apiNode));
        String implementation;
        if (pd.get(2) instanceof Token kwTo && Token.TokenType.WITH == kwTo.getType()) {
            Node iNode = pd.get(3);
            implementation = iNode.getSource();
            if (dsb != null) dsb.put(implementation, source(iNode));
        } else {
            implementation = null;
        }
        Source source = source(pd);
        builder.addProvides(dsb == null ? source : source.withDetailedSources(dsb.build()),
                comments(pd), api, implementation);
    }

    private void processUsesDirective(Context context, UsesDirective ud, ModuleInfo.Builder builder) {
        DetailedSources.Builder dsb = context.newDetailedSourcesBuilder();
        Node apiNode = ud.get(1);
        String api = apiNode.getSource();
        if (dsb != null) dsb.put(api, source(apiNode));
        Source source = source(ud);
        builder.addUses(dsb == null ? source : source.withDetailedSources(dsb.build()), comments(ud), api);
    }

    private void processOpensDirective(Context context, OpensDirective ed, ModuleInfo.Builder builder) {
        DetailedSources.Builder dsb = context.newDetailedSourcesBuilder();
        Node packageNameNode = ed.get(1);
        String packageName = packageNameNode.getSource();
        if (dsb != null) dsb.put(packageName, source(packageNameNode));
        List<String> toModules = moduleTargets(ed, dsb);
        Source source = source(ed);
        builder.addOpens(dsb == null ? source : source.withDetailedSources(dsb.build()),
                comments(ed), packageName, toModules);
    }


    private void processExportsDirective(Context context, ExportsDirective ed, ModuleInfo.Builder builder) {
        DetailedSources.Builder dsb = context.newDetailedSourcesBuilder();
        Node packageNameNode = ed.get(1);
        String packageName = packageNameNode.getSource();
        if (dsb != null) dsb.put(packageName, source(packageNameNode));
        List<String> toModules = moduleTargets(ed, dsb);
        Source source = source(ed);
        builder.addExports(dsb == null ? source : source.withDetailedSources(dsb.build()),
                comments(ed), packageName, toModules);
    }

    // Collect all target modules of a qualified 'exports/opens p to a, b, c' directive. The target names follow the
    // 'to' keyword (index 2); the remaining children alternate name, comma, name, ... and end with a semicolon token,
    // so skip the Token nodes. Each target's source is recorded so it can later be located individually.
    private List<String> moduleTargets(Node directive, DetailedSources.Builder dsb) {
        List<String> toModules = new ArrayList<>();
        if (directive.get(2) instanceof Token kwTo && Token.TokenType.TO == kwTo.getType()) {
            for (int i = 3; i < directive.size(); i++) {
                Node n = directive.get(i);
                if (n instanceof Token) continue;
                String name = n.getSource();
                toModules.add(name);
                if (dsb != null) dsb.put(name, source(n));
            }
        }
        return toModules;
    }

    private void processRequired(Context context, RequiresDirective rd, ModuleInfo.Builder builder) {
        int j = 1;
        boolean isStatic = false;
        boolean isTransitive = false;
        while (rd.get(j) instanceof Token modifier) {
            if (Token.TokenType.STATIC == modifier.getType()) {
                isStatic = true;
            } else if (Token.TokenType.TRANSITIVE == modifier.getType()) {
                isTransitive = true;
            } else {
                throw new Summary.ParseException(context, "Unexpected modifier " + modifier.getSource()
                                                          + " in 'requires'");
            }
            ++j;
        }
        Name reqName = (Name) rd.get(j);
        DetailedSources.Builder dsb = context.newDetailedSourcesBuilder();
        String n = reqName.getSource();
        Source source = source(rd);
        if (dsb != null) dsb.put(n, source(reqName));
        builder.addRequires(dsb == null ? source : source.withDetailedSources(dsb.build()),
                comments(rd), n, isStatic, isTransitive);
    }
}
