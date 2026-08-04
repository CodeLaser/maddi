#!/usr/bin/env perl
#
# Scan for internal / customer-identifying names that must never enter this
# public repository. Patterns live in internal-names.txt next to this script.
#
#   scan-internal-names.pl --staged     what a commit would record (pre-commit)
#   scan-internal-names.pl --tracked    every tracked file at its working-tree state
#   scan-internal-names.pl FILE...      the named files (commit-msg passes the message)
#
# Exits 0 when clean, 1 on a hit, 2 on a usage or setup error.

use strict;
use warnings;

# ------------------------------------------------------- locate the list ---
# The term list is NOT in this repository, and must never be: a list of the
# names to redact is a decoder ring for the redaction. It lives outside, and
# this repository ships only internal-names.txt.example (format, no terms).
#
# Resolution order: $MADDI_INTERNAL_NAMES, then `git config maddi.internalNames`,
# then ~/.config/maddi/internal-names.txt.
my $configured = 0;
my $patterns_file = $ENV{MADDI_INTERNAL_NAMES};
$configured = 1 if $patterns_file;
unless ($patterns_file) {
    chomp(my $c = `git config --get maddi.internalNames 2>/dev/null` // '');
    if ($c) { $patterns_file = $c; $configured = 1; }
}
unless ($patterns_file) {
    my $home = $ENV{HOME} // '';
    $patterns_file = "$home/.config/maddi/internal-names.txt";
}

unless (-r $patterns_file) {
    # Explicitly configured but unreadable is a broken setup — fail loudly.
    die "internal-names guard: configured list '$patterns_file' is missing or unreadable.\n"
      . "Fix it, or unset MADDI_INTERNAL_NAMES / maddi.internalNames.\n" if $configured;
    # Not configured at all: an outside contributor, who cannot leak names they
    # do not have. Say so on every commit rather than passing silently.
    print STDERR "internal-names guard: no term list at $patterns_file — scan skipped.\n"
               . "  Maintainers: see CONTRIBUTING.md \"Names that must not appear\".\n";
    exit 0;
}

# ---------------------------------------------------------------- patterns ---
open(my $pf, '<', $patterns_file) or die "cannot read $patterns_file: $!\n";
my @patterns;
while (my $line = <$pf>) {
    chomp $line;
    next if $line =~ /^\s*(#|$)/;
    my $re = eval { qr/$line/i };
    die "bad pattern in $patterns_file line $.: $line\n" unless $re;
    push @patterns, { source => $line, re => $re };
}
close $pf;
die "no patterns in $patterns_file\n" unless @patterns;

# ------------------------------------------------------------------- input ---
my $mode = 'files';
my @args = @ARGV;
if (@args && $args[0] =~ /^--(staged|tracked)$/) {
    $mode = $1;
    shift @args;
}

my @hits;

if ($mode eq 'files') {
    die "usage: $0 [--staged|--tracked] [FILE...]\n" unless @args;
    for my $path (@args) {
        open(my $fh, '<', $path) or die "cannot read $path: $!\n";
        my $content = do { local $/; <$fh> };
        close $fh;
        scan($path, $content);
    }
} elsif ($mode eq 'tracked') {
    for my $path (git_lines('ls-files', '-z')) {
        next unless -f $path;
        open(my $fh, '<:raw', $path) or next;
        my $content = do { local $/; <$fh> };
        close $fh;
        scan($path, $content);
    }
} else {    # --staged: read the blob that would be committed, not the file on disk
    for my $path (git_lines('diff', '--cached', '--name-only', '--diff-filter=ACMR', '-z')) {
        my $content = `git show :"$path" 2>/dev/null`;
        next unless defined $content && length $content;
        scan($path, $content);
    }
}

# ------------------------------------------------------------------ report ---
if (@hits) {
    print STDERR "\nInternal names found — this repository is public.\n\n";
    for my $h (@hits) {
        print STDERR "  $h->{path}:$h->{line}: matched /$h->{pattern}/\n";
        print STDERR "      " . trim_for_display($h->{text}) . "\n";
    }
    print STDERR <<"END";

Rename before committing. The established stand-ins are `closed-core` for the
customer corpus and `com.example.*` for its packages; see CONTRIBUTING.md
"Names that must not appear".

To change the list itself, edit .githooks/internal-names.txt.
END
    exit 1;
}
exit 0;

# ------------------------------------------------------------------- utils ---
sub scan {
    my ($path, $content) = @_;
    return if $content =~ /\0/;    # binary
    my $lineno = 0;
    for my $text (split /\n/, $content, -1) {
        $lineno++;
        for my $p (@patterns) {
            push @hits, { path => $path, line => $lineno, text => $text, pattern => $p->{source} }
                if $text =~ $p->{re};
        }
    }
}

sub git_lines {
    my @cmd = @_;
    my $out = do {
        open(my $gh, '-|', 'git', @cmd) or die "cannot run git @cmd: $!\n";
        local $/;
        <$gh>;
    };
    return () unless defined $out;
    return grep { length } split /\0/, $out;
}

sub trim_for_display {
    my ($text) = @_;
    $text =~ s/^\s+//;
    return length($text) > 110 ? substr($text, 0, 107) . '...' : $text;
}
