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
    id("org.asciidoctor.jvm.convert") version "4.0.4"
    id("org.asciidoctor.jvm.pdf") version "4.0.4"
    id("org.asciidoctor.jvm.gems") version "4.0.4"
}

// Configure AsciiDoctor dependencies
dependencies {
    // AsciiDoctor gems for PDF and syntax highlighting
    asciidoctorGems("rubygems:asciidoctor-pdf:2.3.9")
    asciidoctorGems("rubygems:rouge:4.1.3")
}

// Common attributes for all formats
val commonAttributes = mapOf(
    "source-highlighter" to "rouge",
    "rouge-style" to "github",
    "icons" to "font",
    "sectlinks" to "",
    "sectanchors" to "",
    "idprefix" to "",
    "idseparator" to "-",
    "includedir" to "sections",
    "imagesdir" to "images",
    "toc" to "left",
    "toclevels" to "3",
    "numbered" to "",
    "experimental" to "",
    "attribute-missing" to "warn"
)

tasks.asciidoctor {
    baseDirFollowsSourceDir()

    sources {
        include("index.adoc")
    }

    attributes(commonAttributes)

    resources {
        from("src/docs/asciidoc/images") {
            into("images")
        }
        from("src/docs/asciidoc/attachments") {
            into("attachments")
        }
    }
}

tasks.asciidoctorPdf {
    baseDirFollowsSourceDir()

    sources {
        include("index.adoc")
    }

    attributes(commonAttributes + mapOf(
        "pdf-theme" to "default",
        "pdf-themesdir" to "themes",
        "allow-uri-read" to "",
        "toc" to "macro"  // Different TOC placement for PDF
    ))

    resources {
        from("src/docs/asciidoc/images") {
            into("images")
        }
        from("src/docs/asciidoc/themes") {
            into("themes")
        }
    }
}

// Create a combined task to build both HTML and PDF
tasks.register("buildDocs") {
    dependsOn("asciidoctor", "asciidoctorPdf")
    group = "documentation"
    description = "Build both HTML and PDF documentation"
}

// Configure output directories (optional)
tasks.asciidoctor {
    setOutputDir(file("build/docs/html"))
}

tasks.asciidoctorPdf {
    setOutputDir(file("build/docs/pdf"))
}