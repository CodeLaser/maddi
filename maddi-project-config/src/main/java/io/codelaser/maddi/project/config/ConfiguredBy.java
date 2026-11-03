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

/** The way a project's configuration was made; {@link #key()} is how a store writes it down. */
public enum ConfiguredBy {
    /** An {@code inputConfiguration.json} a build tool wrote (the Gradle plugin's task, the Maven plugin's goal). */
    FILE("file"),
    /** Source directories and a class path named by hand: a change to the build is noticed, not redone. */
    DIRECTORIES("directories"),
    /** The javac invocations of a Gradle build, taken from its debug log ({@link CaptureSpec#gradle}). */
    GRADLE_CAPTURE("gradle-capture"),
    /** The javac invocations of a Maven build, taken from its debug log ({@link CaptureSpec#maven}). */
    MAVEN_CAPTURE("maven-capture"),
    /** The javac invocations of any other build, from a log or a command that writes one ({@link CaptureSpec#compileLog}). */
    COMPILE_LOG("compile-log"),
    /** A caller's own discovery: a record it writes, with a command to make the configuration again. */
    DISCOVER("discover");

    private final String key;

    ConfiguredBy(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public boolean isCapture() {
        return this == GRADLE_CAPTURE || this == MAVEN_CAPTURE || this == COMPILE_LOG;
    }

    public static ConfiguredBy ofKey(String key) {
        for (ConfiguredBy value : values()) if (value.key.equals(key)) return value;
        throw new IllegalArgumentException("unknown way of configuring '" + key + "'; known: file, directories,"
                                           + " gradle-capture, maven-capture, compile-log, discover");
    }
}
