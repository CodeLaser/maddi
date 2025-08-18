/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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
    id("java-library-conventions")
    id("org.jreleaser") version "1.19.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

version = "0.8.2"

jreleaser {
    gitRootSearch = true

    project {
        name.set("maddi-support")
        description = "Support library for Maddi, a modification analyser for duplication detection and immutability."

        // Maven Central requires SPDX identifier (not arbitrary text)
        license.set("LGPL-3.0-or-later")

        authors.set(listOf("Bart Naudts"))

        copyright.set("2020-20205 Bart Naudts")

        links {
            homepage.set("https://github.com/CodeLaser/maddi")
            documentation.set("hhttps://github.com/CodeLaser/maddi/road-to-immutability")
        }
    }

    distributions {
        create("maddi-support") {
            artifact {
                setPath("build/libs/maddi-support-${version}.jar")
            }
        }
    }

    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored = true
        mode = org.jreleaser.model.Signing.Mode.FILE

    }

    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("target/staging-deploy")
                }
            }
        }
    }
}
