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
