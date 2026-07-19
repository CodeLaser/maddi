/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 * LGPL v3 or later; see the module sources for the full header.
 */

import * as vscode from 'vscode';
import { ElementAnnotation, Finding, Result } from './analysisModel';

/**
 * The latest analysis result, indexed for the display surfaces — the counterpart of `MaddiResults`
 * (Eclipse) and the cache inside `MaddiAnalysisService` (IntelliJ).
 *
 * Indexed by filesystem path rather than by URI string: maddi reports `file:///abs/X.java` and VS Code hands
 * out its own spelling of the same file, and the two are not reliably equal as strings.
 */
export class ResultStore {
    private latest: Result | undefined;
    private annotations = new Map<string, ElementAnnotation[]>();

    private readonly changed = new vscode.EventEmitter<void>();
    /** Fires when the surfaces should re-read; also drives inlay-hint refresh. */
    readonly onDidChange = this.changed.event;

    set(result: Result): void {
        this.latest = result;
        this.annotations = index(result.elementAnnotations ?? []);
        this.changed.fire();
    }

    clear(): void {
        this.latest = undefined;
        this.annotations = new Map();
        this.changed.fire();
    }

    get current(): Result | undefined {
        return this.latest;
    }

    findings(): Finding[] {
        return this.latest?.findings ?? [];
    }

    annotationsFor(uri: vscode.Uri): ElementAnnotation[] {
        return this.annotations.get(uri.fsPath) ?? [];
    }

    dispose(): void {
        this.changed.dispose();
    }
}

function index(elements: ElementAnnotation[]): Map<string, ElementAnnotation[]> {
    const byPath = new Map<string, ElementAnnotation[]>();
    for (const element of elements) {
        if (!element.uri) continue;
        let path: string;
        try {
            path = vscode.Uri.parse(element.uri).fsPath;
        } catch {
            continue; // an element we cannot place is one we cannot draw
        }
        const list = byPath.get(path);
        if (list) list.push(element);
        else byPath.set(path, [element]);
    }
    return byPath;
}
