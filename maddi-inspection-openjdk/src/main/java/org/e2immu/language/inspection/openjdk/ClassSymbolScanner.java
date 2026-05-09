package org.e2immu.language.inspection.openjdk;

import com.sun.tools.javac.code.Symbol;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ClassSymbolScanner(Runtime runtime,
                                 FlagHelper flagHelper,
                                 Elements elements,
                                 TypeData typeData) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassSymbolScanner.class);

    TypeInfo typeInfo(Symbol.ClassSymbol cs) {
        String packageName = cs.owner.toString();
        URI uri = cs.classfile.toUri();
        SourceSet sourceSet = ensureSourceSet(uri);
        CompilationUnit cu = runtime.newCompilationUnitBuilder()
                .setPackageName(packageName)
                .setSourceSet(sourceSet)
                .setURI(uri)
                .build();
        String simpleName = cs.name.toString();
        TypeInfo newTypeInfo = runtime.newTypeInfo(cu, simpleName);
        flagHelper.type(cs.flags(), newTypeInfo.builder());
        //The following completely loads 'cs'
        List<? extends Element> members = elements.getAllMembers(cs);
        for (var member : members) {
            scanByteCode(newTypeInfo, member);
        }
        return newTypeInfo;
    }

    private static final Pattern JAR_FILE = Pattern.compile("(jar:file:.+)/([^/!]+)!/.*");
    private static final Pattern JAVA_RUNTIME = Pattern.compile("jrt:/([^/]+)/.*");
    private SourceSet ensureSourceSet(URI uri) {
        Matcher m = JAR_FILE.matcher(uri.toString());
        if (m.matches()) {
            String jarName = m.group(2);
            URI jarUri = URI.create(m.group(1) + "/" + m.group(2));
            SourceSet known = typeData.getSourceSet(jarName);
            if (known == null) {
                SourceSet sourceSet = new SourceSetImpl(jarName, jarUri, false, false, true,
                        true, false);
                typeData.put(sourceSet);
                return sourceSet;
            }
            return known;
        }
        Matcher rt = JAVA_RUNTIME.matcher(uri.toString());
        if (rt.matches()) {
            String module = rt.group(1);
            URI jarUri = URI.create("jmod:"+module);
            SourceSet known = typeData.getSourceSet(module);
            if (known == null) {
                SourceSet sourceSet = new SourceSetImpl(module, jarUri, false, false, true,
                        true, true);
                typeData.put(sourceSet);
                return sourceSet;
            }
            return known;
        }
        throw new UnsupportedOperationException("NYI");
    }

    private void scanByteCode(TypeInfo typeInfo, Element member) {
        if (member instanceof Symbol.MethodSymbol ms) {
            String name = ms.getSimpleName().toString();
            LOGGER.info("Adding member {} to {}", name, typeInfo);
            MethodInfo method = runtime.newMethod(typeInfo, name, runtime.methodTypeMethod());
            typeInfo.builder().addMethod(method);
        }
    }

}
