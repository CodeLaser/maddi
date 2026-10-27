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

package io.codelaser.maddi.graph.util;

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

    public void debug(String string, Object... objects) {
        if (logger.isDebugEnabled() && allow()) {
            logger.debug(string, objects);
        }
    }

    private boolean allow() {
        long now = Instant.now().toEpochMilli();
        boolean ok = now - latest >= delay;
        if (ok) latest = now;
        return ok;
    }
}
