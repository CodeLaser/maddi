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

package org.e2immu.analyzer.modification.link.staticvalues;

import org.e2immu.annotation.method.GetSet;

import java.util.Iterator;
import java.util.function.Function;

public class Loop {

    public static LoopData run(LoopData initial) {
        LoopData ld = initial;
        while (ld.hasNext()) {
            if (ld.loop() != null) {
                Object loopValue = ld.loop().next();
                LoopData nextLd = ld.withLoopValue(loopValue);
                ld = ld.body().apply(nextLd);
            } else {
                ld =ld.body().apply(ld);
            }
        }
        return ld;
    }

    public interface LoopData {

        Throwable exception();

        @GetSet("variables")
        Object get(int i);

        Object getReturnValue();

        boolean hasNext();

        boolean isReturn();

        Object loopValue();

        int level();

        LoopData with(int pos, Object value);

        LoopData withBreak(boolean doBreak);

        LoopData withBreakLevel(boolean doBreak, int level);

        LoopData withContinueLevel(boolean doContinue, int level);

        LoopData withException(Throwable e);

        LoopData withLoopValue(Object loopValue);

        LoopData withReturn(boolean doReturn);

        LoopData withReturnValue(boolean doReturn, Object returnValue);

        @GetSet
        Iterator<?> loop();

        @GetSet
        Function<LoopData, LoopData> body();
    }

    public interface Exit {
    }

    public static class Break implements Exit {
        private final int level;

        public Break(int level) {
            this.level = level;
        }
    }

    public static class Continue implements Exit {
        private final int level;

        public Continue(int level) {
            this.level = level;
        }
    }

    public static class Return implements Exit {
        private final Object value;

        public Return(Object value) {
            this.value = value;
        }

        public Object value() {
            return value;
        }
    }

    public static class ExceptionThrown implements Exit {
        private final Throwable exception;

        public ExceptionThrown(Throwable exception) {
            this.exception = exception;
        }

        public Throwable exception() {
            return exception;
        }
    }

    public static class LoopDataImpl implements LoopData {
        private final Iterator<?> loop;
        private final Exit exit;
        private final Object[] variables;
        private final Function<LoopData, LoopData> body;
        private final Object loopValue;

        public LoopDataImpl(Iterator<?> loop,
                            Function<LoopData, LoopData> body,
                            Exit exit,
                            Object[] variables,
                            Object loopValue) {
            this.exit = exit;
            this.variables = variables;
            this.loop = loop;
            this.body = body;
            this.loopValue = loopValue;
        }

        @Override
        public Iterator<?> loop() {
            return loop;
        }

        @Override
        public Function<LoopData, LoopData> body() {
            return body;
        }

        @Override
        public int level() {
            if (exit instanceof Break) {
                Break b = (Break) exit;
                return b.level;
            } else if (exit instanceof Continue) {
                Continue c = (Continue) exit;
                return c.level;
            } else {
                return -1;
            }
        }

        @Override
        public Throwable exception() {
            if (exit instanceof ExceptionThrown) return ((ExceptionThrown) exit).exception();
            return null;
        }

        @Override
        public Object get(int i) {
            return variables[i];
        }

        @Override
        public Object getReturnValue() {
            return ((Return) exit).value;
        }

        @Override
        public boolean hasNext() {
            if (exit != null) return false;
            return loop == null || loop.hasNext();
        }

        @Override
        public boolean isReturn() {
            return exit instanceof Return;
        }

        @Override
        public Object loopValue() {
            return loopValue;
        }

        @Override
        public LoopData with(int pos, Object value) {
            Object[] newVariables = variables.clone();
            newVariables[pos] = value;
            return new LoopDataImpl(loop, body, exit, newVariables, loopValue);
        }

        @Override
        public LoopData withException(Throwable e) {
            return new LoopDataImpl(loop, body, new ExceptionThrown(e), variables, loopValue);
        }

        @Override
        public LoopData withBreak(boolean doBreak) {
            return withBreakLevel(doBreak, -1);
        }

        @Override
        public LoopData withBreakLevel(boolean doBreak, int level) {
            if (exit instanceof ExceptionThrown || exit instanceof Return) {
                // the break has lower priority
                return this;
            }
            if (exit instanceof Continue || exit instanceof Break) {
                if (!doBreak) {
                    return this;
                }
                throw new UnsupportedOperationException("Runtime: we cannot already have a break or continue");
            }
            Exit exit = doBreak ? new Break(level) : null;
            return new LoopDataImpl(loop, body, exit, variables, loopValue);
        }

        @Override
        public LoopData withContinueLevel(boolean doContinue, int level) {
            if (exit instanceof ExceptionThrown || exit instanceof Return) {
                // the 'continue' has lower priority
                return this;
            }
            if (exit instanceof Continue || exit instanceof Break) {
                if (!doContinue) {
                    return this;
                }
                throw new UnsupportedOperationException("Runtime: we cannot already have a break or continue");
            }
            Exit exit = doContinue ? new Continue(level) : null;
            return new LoopDataImpl(loop, body, exit, variables, loopValue);
        }

        @Override
        public LoopData withReturnValue(boolean doReturn, Object returnValue) {
            if (exit instanceof ExceptionThrown || !doReturn) {
                // the return value has lower priority
                return this;
            }
            return new LoopDataImpl(loop, body, new Return(returnValue), variables, loopValue);
        }

        @Override
        public LoopData withReturn(boolean doReturn) {
            return withReturnValue(doReturn, null);
        }

        @Override
        public LoopData withLoopValue(Object loopValue) {
            return new LoopDataImpl(loop, body, exit, variables, loopValue);
        }

        private static final int NUM_VARIABLES = 20;

        public static class Builder {
            private final Object[] variables = new Object[NUM_VARIABLES];
            private Iterator<?> loop;
            private Function<LoopData, LoopData> body;

            public Builder body(Function<LoopData, LoopData> body) {
                this.body = body;
                return this;
            }

            public LoopDataImpl build() {
                return new LoopDataImpl(loop, body, null, variables, null);
            }

            public Builder iterator(Iterator<?> iterator) {
                this.loop = iterator;
                return this;
            }

            public Builder set(int pos, Object value) {
                variables[pos] = value;
                return this;
            }
        }
    }
}