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
export async function waitForStandardServer(): Promise<void> {
    const extension = vscode.extensions.getExtension(JAVA_EXTENSION_ID);
    if (!extension) {
        throw new Error(
            'maddi needs the Language Support for Java extension (redhat.java); it is not installed');
    }
    const api = extension.isActive ? extension.exports : await extension.activate();
    if (!api) return; // very old versions expose no API; the commands work once the server is up

    if (api.serverMode !== undefined && api.serverMode !== 'Standard') {
        await new Promise<void>((resolve) => {
            const subscription = api.onDidServerModeChange((mode: string) => {
                if (mode === 'Standard') {
                    subscription.dispose();
                    resolve();
                }
            });
        });
    }
    if (typeof api.serverReady === 'function') {
        await api.serverReady();
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

