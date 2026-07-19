/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 * LGPL v3 or later; see the module sources for the full header.
 */

import { Annotation, ElementAnnotation } from './analysisModel';

/**
 * Which computed annotations are worth showing, mirroring `HintFilter` (Eclipse) and `InlineHintsMode`
 * (IntelliJ) so one filter means the same thing in all three front-ends.
 *
 * maddi's decorator collapses "baseline" and "negative polarity" onto the same boundary, so the choices are
 * two axes rather than three: context-default-ness (is this already implied by the enclosing declaration?)
 * and polarity (proven-positive vs baseline-negative).
 */
export type HintFilter =
    | 'hide-implied-defaults'
    | 'all'
    | 'positive-only'
    | 'negative-only'
    | 'none';

export const DEFAULT_HINT_FILTER: HintFilter = 'hide-implied-defaults';

export function shows(filter: HintFilter, annotation: Annotation): boolean {
    switch (filter) {
        case 'all': return true;
        case 'none': return false;
        case 'hide-implied-defaults': return !annotation.contextDefault;
        case 'positive-only': return annotation.polarity !== 'NEGATIVE';
        case 'negative-only': return annotation.polarity !== 'POSITIVE';
        default: return true;
    }
}

/**
 * What an element should read as under this filter: the shown annotations joined, or empty for "show
 * nothing here".
 *
 * Falls back to the untagged display set when the daemon sent no tagged annotations, since having something
 * to show beats showing nothing because the polarity metadata was missing.
 */
export function textFor(filter: HintFilter, element: ElementAnnotation): string {
    const annotations = element.annotations;
    if (!annotations || annotations.length === 0) {
        if (filter === 'none' || !element.displayAnnotations) return '';
        return element.displayAnnotations.join(' ');
    }
    return annotations
        .filter((a) => shows(filter, a))
        .map((a) => a.text)
        .join(' ');
}
