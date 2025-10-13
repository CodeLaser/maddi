/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.runtime.Runtime;

public class Parsers {

    private final ParseType parseType;
    private final ParseExpression parseExpression;
    private final ParseTypeDeclaration parseTypeDeclaration;
    private final ParseStatement parseStatement;
    private final ParseMethodDeclaration parseMethodDeclaration;
    private final ParseAnnotationMethodDeclaration parseAnnotationMethodDeclaration;
    private final ParseFieldDeclaration parseFieldDeclaration;
    private final ParseAnnotationExpression parseAnnotationExpression;
    private final ParseMethodCall parseMethodCall;
    private final ParseMethodReference parseMethodReference;
    private final ParseConstructorCall parseConstructorCall;
    private final ParseLambdaExpression parseLambdaExpression;
    private final ParseBlock parseBlock;
    private final ParseRecordPattern parseRecordPattern;

    public Parsers(Runtime runtime) {
        parseType = new ParseType(runtime);
        parseBlock = new ParseBlock(runtime, this);
        parseExpression = new ParseExpression(runtime, this);
        parseFieldDeclaration = new ParseFieldDeclaration(runtime, this);
        parseMethodDeclaration = new ParseMethodDeclaration(runtime, this);
        parseTypeDeclaration = new ParseTypeDeclaration(runtime, this);
        parseStatement = new ParseStatement(runtime, this);
        parseAnnotationExpression = new ParseAnnotationExpression(runtime, this);
        parseAnnotationMethodDeclaration = new ParseAnnotationMethodDeclaration(runtime, this);
        parseMethodCall = new ParseMethodCall(runtime, this);
        parseMethodReference = new ParseMethodReference(runtime, this);
        parseConstructorCall = new ParseConstructorCall(runtime, this);
        parseLambdaExpression = new ParseLambdaExpression(runtime, this);
        parseRecordPattern = new ParseRecordPattern(runtime, this);
    }

    public ParseBlock parseBlock() {
        return parseBlock;
    }

    public ParseLambdaExpression parseLambdaExpression() {
        return parseLambdaExpression;
    }

    public ParseMethodReference parseMethodReference() {
        return parseMethodReference;
    }

    public ParseConstructorCall parseConstructorCall() {
        return parseConstructorCall;
    }

    public ParseMethodCall parseMethodCall() {
        return parseMethodCall;
    }

    public ParseAnnotationExpression parseAnnotationExpression() {
        return parseAnnotationExpression;
    }

    public ParseExpression parseExpression() {
        return parseExpression;
    }

    public ParseFieldDeclaration parseFieldDeclaration() {
        return parseFieldDeclaration;
    }

    public ParseAnnotationMethodDeclaration parseAnnotationMethodDeclaration() {
        return parseAnnotationMethodDeclaration;
    }

    public ParseMethodDeclaration parseMethodDeclaration() {
        return parseMethodDeclaration;
    }

    public ParseStatement parseStatement() {
        return parseStatement;
    }

    public ParseType parseType() {
        return parseType;
    }

    public ParseTypeDeclaration parseTypeDeclaration() {
        return parseTypeDeclaration;
    }

    public ParseRecordPattern parseRecordPattern() {
        return parseRecordPattern;
    }
}
