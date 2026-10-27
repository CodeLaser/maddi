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

tasks.asciidoctor {
    setOutputDir(file("build/docs/html"))
}

tasks.asciidoctorPdf {
    setOutputDir(file("build/docs/pdf"))
}
