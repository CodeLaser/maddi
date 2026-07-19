/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 * LGPL v3 or later; see the module sources for the full header.
 */

import * as assert from 'assert';
import { test } from 'node:test';
import { buildConfig, looksLikeTestRoot } from '../configBuilder';

/**
 * The jdt.ls project model to maddi's AnalyzeConfig. Driven with canned responses in the exact shapes
 * jdt.ls 1.59 returns, so the mapping is pinned without a language server: getting it wrong produces an
 * analysis of nothing rather than an error, which is the failure mode worth guarding.
 */

const PROJECT = 'file:///w/app';

function jdtls(responses: Record<string, unknown>) {
    const calls: string[] = [];
    const execute = async (command: string, ...args: unknown[]): Promise<unknown> => {
        const delegate = String(args[0]);
        calls.push(delegate);
        if (command !== 'java.execute.workspaceCommand') throw new Error(`unexpected bridge ${command}`);
        if (delegate === 'java.project.getClasspaths') {
            const scope = (args[2] as { scope: string }).scope;
            return responses[`getClasspaths:${scope}`];
        }
        return responses[delegate];
    };
    return { execute, calls };
}

const OPTIONS = { jdkHome: '/opt/jdk-25', warnNearMisses: false, workingDirectory: '/w' };

test('compiler output directories become classpath entries', async () => {
    const { execute } = jdtls({
        'java.project.getAll': [PROJECT],
        'getClasspaths:runtime': {
            projectRoot: PROJECT,
            classpaths: ['/w/app/target/classes', '/home/me/.m2/guava.jar'],
            modulepaths: [],
        },
        'getClasspaths:test': { projectRoot: PROJECT, classpaths: [], modulepaths: [] },
        'java.project.listSourcePaths': { data: [] },
    });
    const config = await buildConfig(execute, OPTIONS);

    // this is the whole point of using jdt.ls: it compiles, so the .class files the openjdk parser needs
    // are on disk, and they reach maddi as ordinary classpath entries
    const output = config.classpath.find((c) => c.path === '/w/app/target/classes');
    assert.ok(output, 'the compiler output dir must be on the classpath');
    assert.strictEqual(output.scope, 'compile');
    assert.ok(config.classpath.some((c) => c.path.endsWith('guava.jar')), 'libraries too');
});

test('what the test scope adds beyond runtime is scoped as test', async () => {
    const { execute } = jdtls({
        'java.project.getAll': [PROJECT],
        'getClasspaths:runtime': { projectRoot: PROJECT, classpaths: ['/w/app/target/classes'], modulepaths: [] },
        'getClasspaths:test': {
            projectRoot: PROJECT,
            classpaths: ['/w/app/target/classes', '/w/app/target/test-classes', '/home/me/.m2/junit.jar'],
            modulepaths: [],
        },
        'java.project.listSourcePaths': { data: [] },
    });
    const config = await buildConfig(execute, OPTIONS);

    const scopeOf = (path: string) => config.classpath.find((c) => c.path === path)?.scope;
    assert.strictEqual(scopeOf('/w/app/target/classes'), 'compile', 'shared entries stay compile-scoped');
    assert.strictEqual(scopeOf('/w/app/target/test-classes'), 'test');
    assert.strictEqual(scopeOf('/home/me/.m2/junit.jar'), 'test');
});

test('module paths are included: on a modular project the classpath is nearly empty', async () => {
    const { execute } = jdtls({
        'java.project.getAll': [PROJECT],
        'getClasspaths:runtime': {
            projectRoot: PROJECT,
            classpaths: [],
            modulepaths: ['/w/app/target/classes', '/home/me/.m2/some.jar'],
        },
        'getClasspaths:test': { projectRoot: PROJECT, classpaths: [], modulepaths: [] },
        'java.project.listSourcePaths': { data: [] },
    });
    const config = await buildConfig(execute, OPTIONS);
    assert.strictEqual(config.classpath.length, 2, 'modulepath entries must not be dropped');
});

test("jdt.ls's synthetic catch-all project is skipped", async () => {
    const { execute, calls } = jdtls({
        'java.project.getAll': ['file:///w/jdt.ls-java-project', PROJECT],
        'getClasspaths:runtime': { projectRoot: PROJECT, classpaths: ['/w/app/bin'], modulepaths: [] },
        'getClasspaths:test': { projectRoot: PROJECT, classpaths: [], modulepaths: [] },
        'java.project.listSourcePaths': { data: [] },
    });
    await buildConfig(execute, OPTIONS);
    // one real project, asked twice (runtime + test); the synthetic one is never queried
    assert.strictEqual(calls.filter((c) => c === 'java.project.getClasspaths').length, 2);
});

test('source roots come through, with test roots marked', async () => {
    const { execute } = jdtls({
        'java.project.getAll': [PROJECT],
        'getClasspaths:runtime': { projectRoot: PROJECT, classpaths: [], modulepaths: [] },
        'getClasspaths:test': { projectRoot: PROJECT, classpaths: [], modulepaths: [] },
        'java.project.listSourcePaths': {
            data: [
                { path: '/w/app/src/main/java', displayPath: 'app/src/main/java', projectName: 'app' },
                { path: '/w/app/src/test/java', displayPath: 'app/src/test/java', projectName: 'app' },
            ],
        },
    });
    const config = await buildConfig(execute, OPTIONS);
    assert.strictEqual(config.sources.length, 2);
    assert.strictEqual(config.sources[0].test, false);
    assert.strictEqual(config.sources[1].test, true);
});

test('an empty workspace yields a config rather than throwing', async () => {
    const { execute } = jdtls({ 'java.project.getAll': undefined, 'java.project.listSourcePaths': undefined });
    const config = await buildConfig(execute, OPTIONS);
    assert.deepStrictEqual(config.sources, []);
    assert.deepStrictEqual(config.classpath, []);
    // the analysis SDK is maddi's own JDK, never the project's
    assert.strictEqual(config.sdkHome, '/opt/jdk-25');
});

test('test-root detection covers the common layouts', () => {
    assert.ok(looksLikeTestRoot('/w/app/src/test/java'));
    assert.ok(looksLikeTestRoot('/w/app/test'));
    assert.ok(looksLikeTestRoot('/w/app/src/integrationTest/java'));
    assert.ok(!looksLikeTestRoot('/w/app/src/main/java'));
    // a project that merely lives under a directory called "latest" is not a test root
    assert.ok(!looksLikeTestRoot('/w/latest/src/main/java'));
});
