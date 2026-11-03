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

package io.codelaser.maddi.project.config;

/** Hears what a capture or a regeneration is doing, and holds the build it runs, so that a stop can end it. */
public interface CaptureListener {

    /** "n compilation(s) logged", "regenerating the configuration: ..." */
    void progress(String message);

    /** The build process while it runs; null once it ended. */
    void process(Process process);

    CaptureListener NONE = new CaptureListener() {
        @Override
        public void progress(String message) {
        }

        @Override
        public void process(Process process) {
        }
    };
}
