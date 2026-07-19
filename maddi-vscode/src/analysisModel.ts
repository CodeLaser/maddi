/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 * LGPL v3 or later; see the module sources for the full header.
 */

/**
 * The daemon's wire contract, in TypeScript.
 *
 * This is the ONLY thing VS Code shares with the other front-ends: the JSON, not the code. The Java clients
 * (`maddi-ide-client`) declare the same shapes independently, and they are matched by FIELD NAME over the
 * socket — so a name changed here and not there fails silently rather than loudly. Keep it in step with
 * `AnalysisModel.java` and `DaemonProtocol.java`.
 */

/** One source or test root fed to maddi. */
export interface SourceRoot {
    name: string;
    path: string;
    test: boolean;
}

/** One classpath entry: a jar, or a compiler-output directory holding hot .class files. */
export interface ClasspathEntry {
    path: string;
    scope: string; // compile | test | runtime | test-runtime
}

export interface AnalyzeConfig {
    workingDirectory: string;
    sdkHome: string;
    sourceEncoding: string;
    jmods: string[];
    sources: SourceRoot[];
    classpath: ClasspathEntry[];
    restrictToPackages: string[];
    parallel: boolean;
    warnNearMisses: boolean;
}

/**
 * A located finding. `severity` is only ERROR or WARN; `category` is the analyzer's free-form kebab-case
 * discriminator (`contract-violation`, `near-miss-container`, `parse`, …) and is what separates findings
 * that share a severity — see {@link isNearMiss}.
 */
export interface Finding {
    uri: string;
    beginLine?: number;
    beginCol?: number;
    endLine?: number;
    endCol?: number;
    severity: string;
    category: string;
    message: string;
    causes?: Finding[];
}

/** One rendered annotation with polarity (POSITIVE/NEGATIVE/NEUTRAL) and context-default-ness. */
export interface Annotation {
    text: string;
    polarity: string;
    contextDefault: boolean;
}

export interface ElementAnnotation {
    uri: string;
    beginLine?: number;
    beginCol?: number;
    endLine?: number;
    endCol?: number;
    kind: string; // TYPE | METHOD | FIELD | PARAMETER | OTHER
    fqn: string;
    displayAnnotations: string[];
    annotations: Annotation[];
    properties: Record<string, string>;
}

export interface Result {
    requestId: string;
    findings: Finding[];
    elementAnnotations: ElementAnnotation[];
    initializationProblems: string[];
    parseErrorCount: number;
    hintsLoaded: number;
    elapsedMillis: number;
}

/**
 * Values established after one analysis pass, streamed before the run finishes. Arrives as a non-terminal
 * `partialResult` frame, i.e. through the status consumer.
 */
export interface PartialResult {
    requestId: string;
    iteration: number;
    fullPass: boolean;
    certain: boolean;
    elements: ElementAnnotation[];
}

export const PARTIAL_RESULT = 'partialResult';

/** Category prefix of the analyzer's advisory near-miss warnings. */
export const NEAR_MISS_PREFIX = 'near-miss-';

/**
 * Is this an advisory near-miss ("one member away from @Container") rather than a defect? Near misses arrive
 * as WARN like any other warning, so only the category separates them; they are shown at the editor's
 * weakest level so a suggestion never reads as a problem.
 */
export function isNearMiss(finding: Finding): boolean {
    return !!finding && !!finding.category && finding.category.startsWith(NEAR_MISS_PREFIX);
}

/**
 * Fold a streamed pass into what is on screen.
 *
 * Merges by element, never replaces: a frame carries the elements of ONE pass, so replacing would drop
 * everything earlier passes established. Identity is `kind` + `fqn` — an element's position moves as the
 * user types, its identity does not. Elements a pass did not mention are kept: values are write-once and
 * only strengthen, so silence says nothing against them.
 */
export function merge(current: Result | undefined, partial: PartialResult): Result {
    const byIdentity = new Map<string, ElementAnnotation>();
    for (const e of current?.elementAnnotations ?? []) {
        byIdentity.set(identity(e), e);
    }
    for (const e of partial.elements ?? []) {
        byIdentity.set(identity(e), e);
    }
    const elementAnnotations = [...byIdentity.values()];
    if (!current) {
        return {
            requestId: partial.requestId,
            findings: [],
            elementAnnotations,
            initializationProblems: [],
            parseErrorCount: 0,
            hintsLoaded: 0,
            elapsedMillis: 0,
        };
    }
    return { ...current, elementAnnotations };
}

function identity(e: ElementAnnotation): string {
    return `${e.kind} ${e.fqn}`;
}
