/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 * LGPL v3 or later; see the module sources for the full header.
 */

import * as assert from 'assert';
import { test } from 'node:test';
import { ElementAnnotation } from '../analysisModel';
import { textFor } from '../hintFilter';

/**
 * The hint filter, which has to mean the same thing here as in the Eclipse and IntelliJ front-ends —
 * three independent implementations of one user-facing setting, so the semantics are worth pinning.
 */

const CONTAINER = { text: '@Container', polarity: 'POSITIVE', contextDefault: false };
const NOT_MODIFIED = { text: '@NotModified', polarity: 'POSITIVE', contextDefault: true };
const MODIFIED = { text: '@Modified', polarity: 'NEGATIVE', contextDefault: false };

function element(...annotations: { text: string; polarity: string; contextDefault: boolean }[]): ElementAnnotation {
    return {
        uri: 'file:///X.java', beginLine: 1, beginCol: 1, endLine: 1, endCol: 2,
        kind: 'TYPE', fqn: 'x.Box',
        displayAnnotations: annotations.map((a) => a.text),
        annotations,
        properties: {},
    };
}

test('the default hides what the enclosing declaration already implies', () => {
    const e = element(CONTAINER, NOT_MODIFIED, MODIFIED);
    assert.strictEqual(textFor('hide-implied-defaults', e), '@Container @Modified');
});

test('"all" shows everything, including implied defaults', () => {
    assert.strictEqual(textFor('all', element(CONTAINER, NOT_MODIFIED, MODIFIED)),
        '@Container @NotModified @Modified');
});

test('polarity filters keep their own side only', () => {
    const e = element(CONTAINER, NOT_MODIFIED, MODIFIED);
    assert.strictEqual(textFor('positive-only', e), '@Container @NotModified');
    assert.strictEqual(textFor('negative-only', e), '@Modified');
});

test('"none" shows nothing at all', () => {
    assert.strictEqual(textFor('none', element(CONTAINER, MODIFIED)), '');
});

test('untagged annotations fall back to the display set rather than vanishing', () => {
    // the daemon sent no polarity metadata; showing the computed annotations still beats showing nothing
    const untagged: ElementAnnotation = {
        uri: 'file:///X.java', kind: 'TYPE', fqn: 'x.Box',
        displayAnnotations: ['@Container'], annotations: [], properties: {},
    };
    assert.strictEqual(textFor('hide-implied-defaults', untagged), '@Container');
    assert.strictEqual(textFor('none', untagged), '', 'except when asked for nothing');
});

test('an element with nothing computed produces no hint', () => {
    const empty: ElementAnnotation = {
        uri: 'file:///X.java', kind: 'TYPE', fqn: 'x.Box',
        displayAnnotations: [], annotations: [], properties: {},
    };
    assert.strictEqual(textFor('all', empty), '');
});
