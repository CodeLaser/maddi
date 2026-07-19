/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 * LGPL v3 or later; see the module sources for the full header.
 */

import { ChildProcess, spawn } from 'child_process';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Starts the analysis daemon and finds out which port it chose.
 *
 * maddi needs a JDK 25+, VS Code does not ship one, and the analyser must not run inside the extension host
 * anyway — so it runs as a separate process, exactly as it does for IntelliJ and Eclipse, and is spoken to
 * over a socket. The daemon prints `DAEMON_PORT=<n>` on stdout once it is listening; asking for port 0 and
 * reading it back avoids picking a port that turns out to be taken.
 */
export interface DaemonHandle {
    port: number;
    process: ChildProcess;
}

export interface LaunchOptions {
    /** The daemon distribution: the directory containing `bin/` and `lib/`. */
    installDir: string;
    /** Home of a JDK 25+; becomes JAVA_HOME for the daemon process. */
    jdkHome: string;
    maxHeapMb: number;
    timeoutMs?: number;
    /** Where the daemon's own output goes, so a failure to start is diagnosable. */
    log?: (line: string) => void;
}

export function launcherPath(installDir: string): string {
    const name = process.platform === 'win32' ? 'maddi-ide-daemon.bat' : 'maddi-ide-daemon';
    return path.join(installDir, 'bin', name);
}

export async function launchDaemon(options: LaunchOptions): Promise<DaemonHandle> {
    const launcher = launcherPath(options.installDir);
    if (!fs.existsSync(launcher)) {
        throw new Error(`no daemon launcher at ${launcher}; set maddi.daemonInstall`);
    }
    // Packaging strips the executable bit (a .vsix is a zip, exactly as p2 and Maven copies do), so restore
    // it rather than failing with a bare EACCES the user cannot act on.
    if (process.platform !== 'win32') {
        try {
            fs.chmodSync(launcher, 0o755);
        } catch {
            // if this fails the spawn below reports the real problem
        }
    }

    const child = spawn(launcher, ['--port', '0'], {
        env: {
            ...process.env,
            JAVA_HOME: options.jdkHome,
            JAVA_OPTS: `-Xmx${options.maxHeapMb}m -XX:+UseG1GC`,
        },
        cwd: options.installDir,
    });

    const timeoutMs = options.timeoutMs ?? 60_000;
    const port = await new Promise<number>((resolve, reject) => {
        let settled = false;
        let stdout = '';
        const timer = setTimeout(() => {
            if (settled) return;
            settled = true;
            child.kill();
            reject(new Error(`daemon did not report a port within ${timeoutMs} ms. Output so far:\n${stdout}`));
        }, timeoutMs);

        const finish = (fn: () => void) => {
            if (settled) return;
            settled = true;
            clearTimeout(timer);
            fn();
        };

        child.stdout?.setEncoding('utf8');
        child.stdout?.on('data', (chunk: string) => {
            stdout += chunk;
            options.log?.(chunk.trimEnd());
            const match = /DAEMON_PORT=(\d+)/.exec(stdout);
            if (match) finish(() => resolve(Number(match[1])));
        });
        child.stderr?.setEncoding('utf8');
        child.stderr?.on('data', (chunk: string) => options.log?.(chunk.trimEnd()));
        child.on('error', (e) => finish(() => reject(e)));
        child.on('exit', (code) =>
            finish(() => reject(new Error(`daemon exited with code ${code} before reporting a port:\n${stdout}`))));
    });

    return { port, process: child };
}
