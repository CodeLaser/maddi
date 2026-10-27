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

package io.codelaser.maddi.cst.api.element;

import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.translate.TranslationMap;

/**
 * Base interface for source comments that are attached to CST elements and preserved during
 * parsing and printing.
 * <p>
 * Concrete sub-types are {@link SingleLineComment} ({@code // …}) and
 * {@link MultiLineComment} ({@code /* … *}{@code /}), with {@link JavaDoc} ({@code /** … *}{@code /})
 * extending {@code MultiLineComment}.
 * <p>
 * Non-JavaDoc comments carry no type or variable references, so {@link #rewire} and {@link #translate}
 * always return {@code this}.
 */
public interface Comment extends Element {

    /**
     * The comment's CONTENT, not its source text. The implementations strip the delimiters on construction —
     * {@code // a} yields {@code " a"}, {@code /* a *}{@code /} yields {@code " a "} — and a {@link JavaDoc}
     * is normalised further still: its per-line {@code *} prefixes are gone and its tags are held separately,
     * so {@code comment()} returns the prose, not the paragraph the author typed.
     * <p>
     * ⛔ <b>THIS WAS DOCUMENTED AS "the raw text of the comment as it appears in source", WHICH IT IS NOT, AND
     * A CALLER BELIEVED IT.</b> A refactoring lever writing a moved method into another file rebuilt its
     * javadoc from {@code comment()} and emitted the prose and the {@code @param} tags in class-body position:
     * nine javac errors and a broken build, from reading the contract and trusting it.
     * <p>
     * ▶ <b>NOTHING RECONSTRUCTED FROM THE CST IS THE AUTHOR'S TEXT</b>, and adding a method that looked as
     * though it were would only move the trap: the {@code *} prefixes a javadoc lost at parse time cannot be
     * put back. A caller that must write a comment into a source file has to take the SOURCE LINES —
     * {@link #source()} spans the content only, so widen it to whole lines. Everything else is for reading.
     */
    String comment();

    default Comment rewire(InfoMapView infoMap) {
        return this;
    }

    default Comment translate(TranslationMap translationMap) {
        return this;
    }
}
