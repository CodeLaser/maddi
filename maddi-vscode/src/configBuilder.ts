/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 * LGPL v3 or later; see the module sources for the full header.
 */

import { AnalyzeConfig, ClasspathEntry, SourceRoot } from './analysisModel';

/**
 * Builds maddi's `AnalyzeConfig` from the Java project model, the counterpart of `MaddiConfigBuilder`
 * (IntelliJ) and `MaddiEclipseConfigBuilder` (Eclipse).
 *
 * VS Code has no project model of its own, which was the open question for this front-end: the answer is to
 * ask **jdt.ls**, the language server behind `redhat.java`. It already knows the classpath, and — the part
 * that actually matters — it *compiles*, so the `.class` files the openjdk parser needs exist on disk and
 * stay fresh. That is the same trick the other two front-ends use (IntelliJ's and JDT's compiler output
 * directories); here the compiler simply belongs to another extension.
 *
 * jdt.ls is reached through `java.execute.workspaceCommand`, which delegates to a server-side command. The
 * ones used here, with their real shapes (jdt.ls 1.59):
 *
 * - `java.project.getAll` -> project root URIs (string[])
 * - `java.project.getClasspaths(uri, {scope})` -> `{projectRoot, classpaths[], modulepaths[]}`
 * - `java.project.listSourcePaths` -> `{data: [{path, displayPath, classpathEntry, projectName, projectType}]}`
 */

/**
 * Everything this module needs from VS Code, so the mapping can be tested without a language server — and
 * the reason this file imports no `vscode`: that module only exists inside the extension host, so anything
 * touching it cannot be unit-tested at all.
 */
export type ExecuteCommand = (command: string, ...args: unknown[]) => Promise<unknown>;

/**
 * jdt.ls keeps a synthetic project for files that belong to no real one. `java.project.getAll` reports it
 * like any other, and it has nothing to analyse.
 */
export const DEFAULT_PROJECT_NAME = 'jdt.ls-java-project';

export function isDefaultProject(projectUri: string): boolean {
    return projectUri.endsWith(DEFAULT_PROJECT_NAME);
}

export interface ClasspathResult {
    projectRoot: string;
    classpaths: string[];
    modulepaths: string[];
}

export interface SourcePath {
    path: string;
    displayPath: string;
    classpathEntry?: string;
    projectName: string;
    projectType?: string;
}

/** JDK modules loaded by default; the same list the IntelliJ and Eclipse front-ends use. */
export const DEFAULT_JMODS = [
    'java.base', 'java.logging', 'java.xml', 'java.sql', 'java.naming',
    'java.desktop', 'java.management', 'java.net.http', 'java.compiler',
];

const WORKSPACE_COMMAND = 'java.execute.workspaceCommand';

export interface BuildOptions {
    /** Home of the maddi JDK (25+): the analysis SDK, NOT the JDK the project targets. */
    jdkHome: string;
    warnNearMisses: boolean;
    /** Absolute path of the folder being analysed; becomes the working directory. */
    workingDirectory: string;
}

export async function buildConfig(execute: ExecuteCommand, options: BuildOptions): Promise<AnalyzeConfig> {
    const all = (await execute(WORKSPACE_COMMAND, 'java.project.getAll')) as string[] | undefined ?? [];
    // jdt.ls reports its synthetic catch-all project alongside the real ones; it has nothing to analyse
    const projects = all.filter((uri) => !isDefaultProject(uri));

    // path -> scope, deduplicated and order-preserving, as in the other front-ends
    const classpath = new Map<string, string>();
    for (const project of projects) {
        // Two scopes, and the difference between them is the point: the runtime classpath is what compiles
        // and runs the main code, so anything the test scope adds on top is test-only. Asking once with the
        // wider scope would flatten that distinction and analyse test dependencies as production ones.
        const compile = await classpathsOf(execute, project, 'runtime');
        for (const entry of compile) classpath.set(entry, 'compile');
        const test = await classpathsOf(execute, project, 'test');
        for (const entry of test) if (!classpath.has(entry)) classpath.set(entry, 'test');
    }

    const sourcePaths = await listSourcePaths(execute);
    const sources: SourceRoot[] = sourcePaths.map((sp) => ({
        name: sp.displayPath || sp.path,
        path: sp.path,
        test: looksLikeTestRoot(sp.path),
    }));

    const classpathEntries: ClasspathEntry[] = [...classpath.entries()].map(([path, scope]) => ({ path, scope }));

    return {
        workingDirectory: options.workingDirectory,
        sdkHome: options.jdkHome,
        sourceEncoding: 'UTF-8',
        jmods: DEFAULT_JMODS,
        sources,
        classpath: classpathEntries,
        restrictToPackages: [],
        parallel: true,
        warnNearMisses: options.warnNearMisses,
    };
}

async function classpathsOf(execute: ExecuteCommand, projectUri: string, scope: string): Promise<string[]> {
    const result = (await execute(WORKSPACE_COMMAND, 'java.project.getClasspaths', projectUri, { scope })) as
        ClasspathResult | undefined;
    if (!result) return [];
    // modulepaths matter as much as classpaths for a modular project: on one, the dependencies are on the
    // module path and `classpaths` alone would be nearly empty.
    return [...(result.classpaths ?? []), ...(result.modulepaths ?? [])];
}

async function listSourcePaths(execute: ExecuteCommand): Promise<SourcePath[]> {
    const result = (await execute(WORKSPACE_COMMAND, 'java.project.listSourcePaths')) as
        { data?: SourcePath[] } | undefined;
    return result?.data ?? [];
}

/**
 * Whether a source root holds tests.
 *
 * jdt.ls's `listSourcePaths` does not say, so this goes on layout convention, which covers Maven and Gradle
 * (`src/test/java`, `src/integrationTest/…`) and the common plain layouts. It is the one guess in this
 * mapping; getting it wrong mis-scopes a root rather than losing it, so the analysis still runs.
 */
export function looksLikeTestRoot(sourcePath: string): boolean {
    const normalized = sourcePath.replace(/\\/g, '/');
    return /(^|\/)(test|tests)(\/|$)/i.test(normalized) || /\/src\/[^/]*[Tt]est[^/]*\//.test(normalized);
}
