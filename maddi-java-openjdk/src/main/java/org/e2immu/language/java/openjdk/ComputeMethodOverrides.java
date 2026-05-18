package org.e2immu.language.java.openjdk;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;

import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;

public record ComputeMethodOverrides(Types types, Elements elements) {

    List<Symbol.MethodSymbol> findOverriddenMethods(Symbol.MethodSymbol method) {
        List<Symbol.MethodSymbol> result = new ArrayList<>();
        Symbol.ClassSymbol owner = method.enclClass();

        for (Type supertype : types.closure(owner.type)) {
            if (supertype.tsym == owner) continue;

            try {
                ((Symbol.ClassSymbol) supertype.tsym).complete();
            } catch (Symbol.CompletionFailure e) {
                continue;
            }

            for (Symbol member : supertype.tsym.members().getSymbols()) {
                if (!(member instanceof Symbol.MethodSymbol candidate)) continue;
                if (candidate.isStatic()) continue;
                if (!candidate.name.equals(method.name)) continue;

                // Elements.overrides() handles generic methods correctly
                if (elements.overrides(method, candidate, owner)) {
                    result.add(candidate);
                }
            }
        }

        return result;
    }
}
