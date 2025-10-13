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

package org.e2immu.analyzer.modification.linkedvariables.graph.impl;

import org.e2immu.analyzer.modification.linkedvariables.graph.Cache;
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class GraphCacheImpl implements Cache {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphCacheImpl.class);

    private final LinkedHashMap<Hash, CacheElement> cache;
    private final MessageDigest md;
    private final int maxSize;
    private int hits;
    private int misses;
    private int removals;
    private int sumSavings;

    private final TimedLogger timedLogger = new TimedLogger(LOGGER, 1000L);

    public GraphCacheImpl(int maxSize) {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        this.maxSize = maxSize;
        // LRU-cache
        cache = new LinkedHashMap<>(maxSize / 2, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Hash, CacheElement> eldest) {
                boolean remove = size() > maxSize;
                if (remove) {
                    removals++;
                    sumSavings += eldest.getValue().savings();
                }
                return remove;
            }
        };
    }

    public Hash createHash(String string) {
        synchronized (md) {
            md.reset();
            md.update(string.getBytes());
            byte[] digest = md.digest();
            return new Hash(digest);
        }
    }

    @Override
    public CacheElement computeIfAbsent(Hash hash, Function<Hash, CacheElement> elementSupplier) {
        synchronized (cache) {
            timedLogger.info("Graph cache {} hits, {} misses, {} removals, {} savings from removed",
                    hits, misses, removals, sumSavings);
            CacheElement inCache = cache.get(hash);
            if (inCache != null) {
                hits++;
                return inCache;
            }
            misses++;
            CacheElement newElement = elementSupplier.apply(hash);
            cache.put(hash, newElement);
            return newElement;
        }
    }

    public int getHits() {
        return hits;
    }

    public int getMisses() {
        return misses;
    }

    public int getRemovals() {
        return removals;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
