/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 * LGPL v3 or later; see the module sources for the full header.
 */

import * as vscode from 'vscode';

/**
 * Dealing with jdt.ls, the language server behind `redhat.java`.
 *
 * maddi depends on it for two things: the project model, and the `.class` files. Both have a readiness
 * problem, and both fail quietly rather than loudly, which is why this is its own module.
 */

export const JAVA_EXTENSION_ID = 'redhat.java';

/** `java.workspace.compile` resolves to these; same ordinals server-side. */
export enum CompileStatus {
    failed = 0,
    succeed = 1,
    withError = 2,
    cancelled = 3,
}

/**
 * Wait until the standard language server is up.
 *
 * This is load-bearing, not defensive. `java.server.launchMode` defaults to **Hybrid**, so the server starts
 * in LightWeight mode — and in LightWeight, `java.execute.workspaceCommand` does not fail: it logs a warning
 * and returns `undefined`. Querying too early therefore yields an empty project model and an analysis of
 * nothing, with no error anywhere to explain it.
 */
export async function waitForStandardServer(timeoutMs = 120_000): Promise<void> {
    const extension = vscode.extensions.getExtension(JAVA_EXTENSION_ID);
    if (!extension) {
        throw new Error(
            'maddi needs the Language Support for Java extension (redhat.java); it is not installed');
    }
    // An untrusted workspace never gets a standard server: in Restricted Mode the Java extension runs the
    // syntax server only, so the project model is not merely late, it is never coming. Say so now rather
    // than waiting for a timeout to report something vaguer.
    if (!vscode.workspace.isTrusted) {
        throw new Error(
            'this workspace is not trusted, so the Java language server runs in restricted mode and cannot ' +
            'provide a project model. Trust the folder (Workspaces: Manage Workspace Trust) and try again');
    }

    const api = extension.isActive ? extension.exports : await extension.activate();
    if (!api) return; // very old versions expose no API; the commands work once the server is up

    if (api.serverMode !== undefined && api.serverMode !== 'Standard') {
        await withTimeout(
            new Promise<void>((resolve) => {
                // Subscribe BEFORE re-reading the mode: if it flipped between the check above and here, the
                // event has already fired and waiting on it alone would hang forever.
                const subscription = api.onDidServerModeChange((mode: string) => {
                    if (mode === 'Standard') {
                        subscription.dispose();
                        resolve();
                    }
                });
                if (api.serverMode === 'Standard') {
                    subscription.dispose();
                    resolve();
                }
            }),
            timeoutMs,
            'the Java language server did not reach standard mode. It may still be importing the project, ' +
            'or java.server.launchMode may be set to LightWeight');
    }
    if (typeof api.serverReady === 'function') {
        await withTimeout(api.serverReady(), timeoutMs,
            'the Java language server did not become ready. Check its output channel (Java: Show Server Log)');
    }
}

/**
 * Bound a wait on the language server. Without this a stuck server leaves the analysis spinning with no
 * indication of what it is waiting for — which is worse than a failure, because it looks like progress.
 */
async function withTimeout<T>(promise: Promise<T>, timeoutMs: number, message: string): Promise<T> {
    let timer: NodeJS.Timeout | undefined;
    try {
        return await Promise.race([
            promise,
            new Promise<never>((_, reject) => {
                timer = setTimeout(() => reject(new Error(`${message} (waited ${timeoutMs / 1000}s)`)), timeoutMs);
            }),
        ]);
    } finally {
        if (timer) clearTimeout(timer);
    }
}

/**
 * Compile the workspace, and wait for it.
 *
 * The openjdk parser reads `.class` files, so they must exist and be current before the analysis starts.
 * jdt.ls autobuilds by default (`java.autobuild.enabled`), but "by default" and "has finished" are different
 * claims — a file saved a moment ago may still be compiling. This command resolves only after the server's
 * own workspace build returns, so it is a genuine barrier.
 *
 * The boolean must be passed explicitly: given a non-boolean, vscode-java opens a quick-pick asking the user
 * whether to do a full or incremental build, which is not something to spring on someone mid-analysis.
 */
export async function compileWorkspace(fullBuild = false): Promise<CompileStatus> {
    const status = await vscode.commands.executeCommand<number>('java.workspace.compile', fullBuild);
    return (status ?? CompileStatus.failed) as CompileStatus;
}

