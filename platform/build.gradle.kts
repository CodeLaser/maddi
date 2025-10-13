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

plugins {
    `java-platform`
}

dependencies {
    constraints {
        api("org.jgrapht:jgrapht-core:1.5.2")
        api("org.jgrapht:jgrapht-io:1.5.2")

        api("org.junit.jupiter:junit-jupiter-api:5.13.0")

        api("org.slf4j:slf4j-api:2.0.17")
        api("ch.qos.logback:logback-classic:1.5.18")

        api("org.jetbrains:annotations:26.0.2")
        api("com.fasterxml.jackson.core:jackson-databind:2.19.2")
        api("commons-cli:commons-cli:1.10.0")

        api("org.ow2.asm:asm:9.8")
    }
}

