/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 * LGPL v3 or later; see the module sources for the full header.
 */

import * as vscode from 'vscode';
import { Finding, isNearMiss } from './analysisModel';

/**
 * Findings as editor diagnostics, with the why-chain that explains them.
 *
 * Severity mirrors the other front-ends: contract violations are errors, and advisory near misses drop to
 * Information rather than sharing Warning with real problems — a suggestion that looks like a defect is
 * why people switch a feature off. maddi's own severity is only ERROR/WARN, so the category is what
 * separates them.
 */
export function toDiagnostics(findings: Finding[]): Map<string, vscode.Diagnostic[]> {
    const byUri = new Map<string, vscode.Diagnostic[]>();
    for (const finding of findings) {
        if (!finding.uri) continue;
        const range = rangeOf(finding);
        const diagnostic = new vscode.Diagnostic(range, message(finding), severityOf(finding));
        diagnostic.source = 'maddi';
        diagnostic.code = finding.category;
        const related = relatedInformation(finding);
        if (related.length > 0) diagnostic.relatedInformation = related;
        const list = byUri.get(finding.uri);
        if (list) list.push(diagnostic);
        else byUri.set(finding.uri, [diagnostic]);
    }
    return byUri;
}

function severityOf(finding: Finding): vscode.DiagnosticSeverity {
    if (isNearMiss(finding)) return vscode.DiagnosticSeverity.Information;
    return finding.severity === 'ERROR'
        ? vscode.DiagnosticSeverity.Error
        : vscode.DiagnosticSeverity.Warning;
}

/**
 * maddi counts lines and columns from 1, VS Code from 0. A finding without a position is pinned to the top
 * of the file rather than dropped: it still says something true about the file.
 */
function rangeOf(finding: Finding): vscode.Range {
    const beginLine = Math.max(0, (finding.beginLine ?? 1) - 1);
    const beginCol = Math.max(0, (finding.beginCol ?? 1) - 1);
    const endLine = Math.max(beginLine, (finding.endLine ?? finding.beginLine ?? 1) - 1);
    const endCol = Math.max(0, (finding.endCol ?? finding.beginCol ?? 1) - 1);
    return new vscode.Range(beginLine, beginCol, endLine, endCol);
}

function message(finding: Finding): string {
    return finding.message ?? '';
}

/**
 * The why-chain, flattened into related information so each step is clickable. This is the part that makes a
 * violation actionable — the finding says what broke, the chain says which call made it break.
 */
function relatedInformation(finding: Finding, depth = 0): vscode.DiagnosticRelatedInformation[] {
    if (!finding.causes || finding.causes.length === 0 || depth > 8) return [];
    const out: vscode.DiagnosticRelatedInformation[] = [];
    for (const cause of finding.causes) {
        if (cause.uri) {
            out.push(new vscode.DiagnosticRelatedInformation(
                new vscode.Location(vscode.Uri.parse(cause.uri), rangeOf(cause)),
                '  '.repeat(depth) + (cause.message ?? '')));
        }
        out.push(...relatedInformation(cause, depth + 1));
    }
    return out;
}
