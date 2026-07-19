/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 * LGPL v3 or later; see the module sources for the full header.
 */

import * as assert from 'assert';
import { test } from 'node:test';
import { ElementAnnotation, PartialResult, Result, isNearMiss, merge } from '../analysisModel';
import { FrameReader } from '../daemonClient';

/** NDJSON framing, and the merge that turns streamed passes into what the editor shows. */

test('a frame split across chunks is reassembled', () => {
    const reader = new FrameReader();
    // exactly what a large result does: TCP hands it over in pieces with no regard for line boundaries
    assert.deepStrictEqual(reader.push('{"type":"sta'), []);
    assert.deepStrictEqual(reader.push('tus","phase":"parse"}\n'), [{ type: 'status', phase: 'parse' }]);
});

test('several frames in one chunk all come out, in order', () => {
    const reader = new FrameReader();
    const frames = reader.push('{"type":"status"}\n{"type":"result"}\n');
    assert.deepStrictEqual(frames, [{ type: 'status' }, { type: 'result' }]);
});

test('a partial line is held until its newline arrives', () => {
    const reader = new FrameReader();
    assert.deepStrictEqual(reader.push('{"type":"result","findings":[]}'), []);
    assert.deepStrictEqual(reader.push('\n'), [{ type: 'result', findings: [] }]);
});

test('near misses are told apart from defects by category', () => {
    const finding = (category: string) => ({ uri: 'file:///X.java', severity: 'WARN', category, message: 'm' });
    assert.ok(isNearMiss(finding('near-miss-container')));
    assert.ok(isNearMiss(finding('near-miss-immutable')));
    assert.ok(!isNearMiss(finding('contract-violation')));
    assert.ok(!isNearMiss(finding('parse')));
});

function element(kind: string, fqn: string, ...shown: string[]): ElementAnnotation {
    return {
        uri: 'file:///X.java', beginLine: 1, beginCol: 1, endLine: 1, endCol: 2,
        kind, fqn, displayAnnotations: shown, annotations: [], properties: {},
    };
}

function pass(iteration: number, ...elements: ElementAnnotation[]): PartialResult {
    return { requestId: 'req', iteration, fullPass: iteration === 1, certain: false, elements };
}

test('the first streamed pass becomes what is displayed', () => {
    const result = merge(undefined, pass(1, element('TYPE', 'x.Box', '@Container')));
    assert.strictEqual(result.elementAnnotations.length, 1);
    assert.deepStrictEqual(result.findings, [], 'a partial frame never carries findings');
});

test('a later pass updates what it mentions and leaves the rest standing', () => {
    const first = merge(undefined, pass(1,
        element('TYPE', 'x.Box', '@Container'),
        element('METHOD', 'x.Box.get', '@NotModified')));
    // pass 2 is a worklist subset: it says nothing about Box.get, which must survive
    const second = merge(first, pass(2, element('TYPE', 'x.Box', '@ImmutableContainer')));

    assert.strictEqual(second.elementAnnotations.length, 2);
    const byFqn = (fqn: string) => second.elementAnnotations.find((e) => e.fqn === fqn)!;
    assert.deepStrictEqual(byFqn('x.Box').displayAnnotations, ['@ImmutableContainer']);
    assert.deepStrictEqual(byFqn('x.Box.get').displayAnnotations, ['@NotModified']);
});

test('identity is kind and name, so an element that moved is not duplicated', () => {
    const first = merge(undefined, pass(1, element('TYPE', 'x.Box', '@Container')));
    const moved = { ...element('TYPE', 'x.Box', '@Container'), beginLine: 40, endLine: 40 };
    const second = merge(first, pass(2, moved));
    assert.strictEqual(second.elementAnnotations.length, 1);
    assert.strictEqual(second.elementAnnotations[0].beginLine, 40);
});

test('a type and a method of the same name stay distinct', () => {
    const result = merge(undefined, pass(1,
        element('TYPE', 'x.Box', '@Container'),
        element('METHOD', 'x.Box', '@NotModified')));
    assert.strictEqual(result.elementAnnotations.length, 2);
});

test('merging keeps the findings already on screen', () => {
    const current: Result = {
        requestId: 'req',
        findings: [{ uri: 'file:///X.java', severity: 'ERROR', category: 'contract-violation', message: 'm' }],
        elementAnnotations: [], initializationProblems: [], parseErrorCount: 0, hintsLoaded: 7, elapsedMillis: 5,
    };
    const merged = merge(current, pass(2, element('TYPE', 'x.Box', '@Container')));
    assert.strictEqual(merged.findings.length, 1, 'a partial frame must not drop findings');
    assert.strictEqual(merged.hintsLoaded, 7);
});
