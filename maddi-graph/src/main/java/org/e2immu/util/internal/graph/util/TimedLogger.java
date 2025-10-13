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

package org.e2immu.util.internal.graph.util;

import org.slf4j.Logger;

import java.time.Instant;

public class TimedLogger {

    private final Logger logger;
    private final long delay;
    private long latest;

    public TimedLogger(Logger logger, long delay) {
        this.logger = logger;
        this.delay = delay;
    }

    public void info(String string, Object... objects) {
        if (allow()) {
            logger.info(string, objects);
        }
    }

    private boolean allow() {
        long now = Instant.now().toEpochMilli();
        boolean ok = now - latest >= delay;
        if (ok) latest = now;
        return ok;
    }
}
