/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 * LGPL v3 or later; see the module sources for the full header.
 */

import * as path from 'path';
import * as vscode from 'vscode';
import { PARTIAL_RESULT, PartialResult, Result, merge } from './analysisModel';
import { buildConfig } from './configBuilder';
import { DaemonClient, Frame } from './daemonClient';
import { DaemonHandle, launchDaemon } from './daemonLauncher';
import { toDiagnostics } from './diagnostics';
import { MaddiInlayHintsProvider } from './hints';
import { CompileStatus, compileWorkspace, waitForStandardServer } from './javaLanguageServer';
import { ResultStore } from './results';

/**
 * The maddi VS Code front-end.
 *
 * Same architecture as the IntelliJ and Eclipse plugins: the analyser is far too heavy — and needs a much
 * newer JDK — to live inside the editor, so it runs as a daemon and is spoken to over a socket in plain
 * JSON. What is different here is where the project model comes from: VS Code has none, so it is asked of
 * jdt.ls (see {@link buildConfig}), which also supplies the compiled `.class` files the parser needs.
 */

let daemon: DaemonHandle | undefined;
let diagnostics: vscode.DiagnosticCollection;
let output: vscode.OutputChannel;
let store: ResultStore;
let running = false;

export function activate(context: vscode.ExtensionContext): void {
    diagnostics = vscode.languages.createDiagnosticCollection('maddi');
    output = vscode.window.createOutputChannel('maddi');
    store = new ResultStore();
    const hints = new MaddiInlayHintsProvider(store);

    context.subscriptions.push(
        diagnostics,
        output,
        store,
        hints,
        vscode.languages.registerInlayHintsProvider({ language: 'java' }, hints),
        vscode.commands.registerCommand('maddi.analyze', () => analyzeCommand(context)),
        // the daemon is a child process: it does not go away with the window unless it is killed
        { dispose: () => stopDaemon() });
}

export function deactivate(): void {
    stopDaemon();
}

function stopDaemon(): void {
    daemon?.process.kill();
    daemon = undefined;
}

async function analyzeCommand(context: vscode.ExtensionContext): Promise<void> {
    if (running) {
        vscode.window.showInformationMessage('maddi is already analyzing.');
        return;
    }
    const folder = vscode.workspace.workspaceFolders?.[0];
    if (!folder) {
        vscode.window.showWarningMessage('maddi: open a folder containing a Java project first.');
        return;
    }
    const settings = vscode.workspace.getConfiguration('maddi');
    const jdkHome = settings.get<string>('jdkHome')?.trim();
    if (!jdkHome) {
        const choice = await vscode.window.showWarningMessage(
            'maddi needs a JDK 25 or later to run the analysis on.', 'Open settings');
        if (choice) await vscode.commands.executeCommand('workbench.action.openSettings', 'maddi.jdkHome');
        return;
    }

    running = true;
    try {
        await vscode.window.withProgress(
            { location: vscode.ProgressLocation.Notification, title: 'maddi', cancellable: false },
            (progress) => analyze(context, folder, jdkHome, settings, progress));
    } catch (e) {
        // Drop what is on screen: it came from a run that did not finish, and stale findings presented as
        // current are worse than none — they look like an answer.
        diagnostics.clear();
        store.clear();
        const message = e instanceof Error ? e.message : String(e);
        output.appendLine(`analysis failed: ${message}`);
        vscode.window.showErrorMessage(`maddi: ${message}`, 'Show log')
            .then((choice) => { if (choice) output.show(); });
    } finally {
        running = false;
    }
}

async function analyze(
    context: vscode.ExtensionContext,
    folder: vscode.WorkspaceFolder,
    jdkHome: string,
    settings: vscode.WorkspaceConfiguration,
    progress: vscode.Progress<{ message?: string }>): Promise<void> {

    progress.report({ message: 'waiting for the Java language server' });
    await waitForStandardServer();

    // The parser reads .class files, so compile before asking for anything. jdt.ls autobuilds, but a file
    // saved a moment ago may still be in flight; this resolves only once the server's build has returned.
    progress.report({ message: 'compiling' });
    const status = await compileWorkspace(false);
    if (status === CompileStatus.failed || status === CompileStatus.cancelled) {
        throw new Error(`the Java build did not complete (${CompileStatus[status]}); fix the build first`);
    }
    if (status === CompileStatus.withError) {
        // partial output still analyses: maddi tolerates a project that does not fully compile
        output.appendLine('the Java build reported errors; analyzing what compiled');
    }

    progress.report({ message: 'starting the analysis daemon' });
    const handle = await ensureDaemon(context, jdkHome, settings);

    progress.report({ message: 'building the project model' });
    const config = await buildConfig(
        (command, ...args) => vscode.commands.executeCommand(command, ...args) as Promise<unknown>,
        {
            jdkHome,
            warnNearMisses: settings.get<boolean>('warnNearMisses') ?? false,
            workingDirectory: folder.uri.fsPath,
        });
    output.appendLine(
        `analyzing ${config.sources.length} source root(s), ${config.classpath.length} classpath entr(ies)`);

    const client = new DaemonClient(handle.port);
    try {
        await client.connect();
        await client.handshake();

        // Values established by each analysis pass arrive before the run ends, so findings and hints appear
        // progressively instead of the editor staying blank until the last pass.
        let displayed: Result | undefined;
        const frame = await client.analyze(`req-${Date.now()}`, config, (streamed: Frame) => {
            if (streamed.type === PARTIAL_RESULT) {
                const partial = streamed as unknown as PartialResult;
                displayed = merge(displayed, partial);
                // show them as they arrive: on a large project this is the difference between an annotated
                // file in seconds and a blank one until the last pass
                store.setPartial(displayed);
                // what has been decided, not a percentage: the run is a fixpoint iteration whose length is
                // not known in advance, so a fraction would be invented
                progress.report({
                    message: `pass ${partial.iteration}, ${displayed.elementAnnotations.length} element(s) so far`,
                });
            } else {
                // the heartbeat carries a message during the long analysis phase; it is what shows the run
                // is alive rather than hung
                const phase = String(streamed.phase ?? 'analyzing');
                const message = String(streamed.message ?? '');
                progress.report({ message: message.length > 0 ? `${phase}: ${message}` : phase });
            }
        });

        if (frame.type === 'error') {
            throw new Error(String(frame.message ?? 'the daemon reported an error'));
        }
        const result = frame as unknown as Result;
        applyResult(result);
        output.appendLine(
            `done in ${result.elapsedMillis} ms: ${result.findings.length} finding(s), ` +
            `${result.elementAnnotations.length} annotated element(s), ${result.hintsLoaded} hints loaded`);
    } finally {
        client.dispose();
    }
}

function applyResult(result: Result): void {
    store.set(result);
    diagnostics.clear();
    for (const [uri, list] of toDiagnostics(result.findings)) {
        diagnostics.set(vscode.Uri.parse(uri), list);
    }
}

async function ensureDaemon(
    context: vscode.ExtensionContext,
    jdkHome: string,
    settings: vscode.WorkspaceConfiguration): Promise<DaemonHandle> {
    if (daemon && daemon.process.exitCode === null) return daemon;

    const configured = settings.get<string>('daemonInstall')?.trim();
    const installDir = configured && configured.length > 0
        ? configured
        : path.join(context.extensionPath, 'daemon');
    output.appendLine(`starting the daemon from ${installDir} on ${jdkHome}`);
    daemon = await launchDaemon({
        installDir,
        jdkHome,
        maxHeapMb: settings.get<number>('daemonXmxMb') ?? 4096,
        log: (line) => output.appendLine(line),
    });
    output.appendLine(`daemon listening on port ${daemon.port}`);
    return daemon;
}
