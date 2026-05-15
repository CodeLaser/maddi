package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.DirectiveTree;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.Source;

import java.util.List;

public record ScanModule(ScanData sd) {

    void visitDirective(DirectiveTree dt, ModuleInfo.Builder builder) {
        Source source = sd.scanSource(dt);
        SourceCodeScan.Result scanResult = sd.scanResult();
        List<Comment> comments = sd.commentsForNode(source);
        DetailedSources.Builder dsb = sd.runtime().newDetailedSourcesBuilder();
        switch (dt) {
            case JCTree.JCRequires rd -> {
                String moduleName = rd.moduleName.toString();
                dsb.put(moduleName, scanResult.find(moduleName, sd.scanSource(rd.getModuleName())));
                builder.addRequires(source.withDetailedSources(dsb.build()), comments,
                        moduleName, rd.isStatic(), rd.isTransitive());
            }

            case JCTree.JCExports ed -> {
                String packageName = ed.getPackageName().toString();
                dsb.put(packageName, scanResult.find(packageName, sd.scanSource(ed.getPackageName())));
                String moduleName = ed.moduleNames == null ? null : ed.moduleNames.getFirst().toString();
                if (moduleName != null) {
                    dsb.put(moduleName, scanResult.find(moduleName, sd.scanSource(ed.getModuleNames().getFirst())));
                }
                builder.addExports(source, comments, packageName, moduleName);
            }
            case JCTree.JCOpens od -> {
                String packageName = od.getPackageName().toString();
                dsb.put(packageName, scanResult.find(packageName, sd.scanSource(od.getPackageName())));
                String moduleName = od.moduleNames == null ? null : od.moduleNames.getFirst().toString();
                if (moduleName != null) {
                    dsb.put(moduleName, scanResult.find(moduleName, sd.scanSource(od.getModuleNames().getFirst())));
                }
                builder.addOpens(source.withDetailedSources(dsb.build()), comments, packageName, moduleName);
            }
            case JCTree.JCProvides p -> {
                String serviceName = p.getServiceName().toString();
                dsb.put(serviceName, scanResult.find(serviceName, sd.scanSource(p.getServiceName())));
                String implName = p.implNames == null ? null : p.implNames.getFirst().toString();
                if (implName != null) {
                    dsb.put(implName, scanResult.find(implName, sd.scanSource(p.getImplementationNames().getFirst())));
                }
                builder.addProvides(source, comments, serviceName, implName);
            }
            case JCTree.JCUses u -> {
                String serviceName = u.getServiceName().toString();
                dsb.put(serviceName, scanResult.find(serviceName, sd.scanSource(u.getServiceName())));
                builder.addUses(source, comments, serviceName);
            }
            case null, default -> throw new UnsupportedOperationException();
        }
    }

}
