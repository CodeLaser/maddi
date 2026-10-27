/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.runtime.Runtime;

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
