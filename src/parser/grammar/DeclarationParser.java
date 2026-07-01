package parser.grammar;

import lexer.*;
import lexer.token.family.literal.IdentifierLiteral;
import lexer.token.type.LiteralToken;
import lexer.token.type.TypeToken;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.*;
import parser.ast.nodes.statement.conditional.*;
import parser.ast.nodes.statement.declaration.*;
import error.diagnostic.ParseException;
import parser.support.ParserBase;
import parser.support.ParserState;
import lexer.token.family.AccessModifier;
import lexer.token.family.Delimiter;
import lexer.token.family.Keyword;
import lexer.token.family.Operator;
import parser.ast.nodes.type.ArrayTypeSyntax;
import parser.ast.nodes.type.NamedTypeSyntax;
import parser.ast.nodes.type.TypeSyntax;
import java.util.ArrayList;

/// Unified parser for declarations and statements.
/// Handles:
/// - Declarations (variables, functions, classes)
/// - Statements (control flow, blocks, expressions)
/// - Type parsing (primitives and custom classes)
///
/// <hr>
///
/// GRAMMAR FOR DECLARATIONS AND STATEMENTS
///
/// Declarations:
/// ```
/// declaration     → accessModifier classDecl | functionDecl | varDecl | statement
///
/// functionDecl    → type IDENTIFIER "(" parameters? ")" "{" declaration* "}"
/// parameters      → type IDENTIFIER ("," type IDENTIFIER)*
///
/// varDecl         → type arrayDimensions? IDENTIFIER ("=" expression)? ("," IDENTIFIER ("=" expression)?)* ";"
/// ```
///
/// Statements:
/// ```
/// statement       → exprStmt | ifStmt | whileStmt | forStmt | forEachStmt | switchStmt | returnStmt | block
///
/// ifStmt          → "if" "(" expression ")" statement ("else" statement)?
/// whileStmt       → "while" "(" expression ")" statement
/// forStmt         → "for" "(" (varDecl | exprStmt | ";") expression? ";" expression? ")" statement
/// forEachStmt     → "for" "(" type IDENTIFIER ":" expression ")" statement
/// switchStmt      → "switch" "(" expression ")" "{" switchCase* "}"
/// switchCase      → "case" expression "->" statement | "default" "->" statement
/// returnStmt      → "return" expression? ";"
///
/// block           → "{" declaration* "}"
/// exprStmt        → expression ";"
/// ```
///
/// Type System:
/// ```
/// type            → (primitiveType | CLASSNAME) arrayDimensions?
/// arrayDimensions → "[" expression? "]" (arrayDimensions)?
///
/// primitiveType   → "int" | "float" | "double" | "long" | "boolean" | "byte" | "string" | ...
/// accessModifier  → "public" | "private" | "protected"
/// ```
///
/// @see ClassParser
/// @see ExpressionParser
public class DeclarationParser extends ParserBase {

    private final ExpressionParser expressionParser;
    private final ClassParser classParser;

    /// Constructs a new DeclarationParser with the given parser state.
    /// @param state The parser state containing the tokens to parse.
    public DeclarationParser(ParserState state) {

        super(state);
        this.expressionParser = new ExpressionParser(state);
        this.classParser = new ClassParser(this);
    }

    /// Parses array dimensions for a type, returning an array of ExpressionNode representing the sizes of each dimension.
    ///
    /// Grammar rule:
    /// `arrayDimensions → "[" expression? "]" (arrayDimensions)?`
    /// @return An array of ExpressionNode representing the sizes of each array dimension, or `null` for unspecified dimensions.
    private ExpressionNode[] parseArrayDimensions() {

        var arraySizes = new ArrayList<ExpressionNode>();
        while (state.match(Delimiter.LSQUARE)) {

            if (!state.check(Delimiter.RSQUARE)) {

                var sizeExpr = expressionParser.parseExpression();
                arraySizes.add(sizeExpr);
            } else arraySizes.add(null);  // No size specified
            state.consume(Delimiter.RSQUARE, "Expect ']' after array dimension");
        }
        return arraySizes.toArray(new ExpressionNode[0]);
    }

    /// Checks if a token is syntactically a valid type token (either a primitive type or an identifier type name).
    /// @param token The token to checkCurrentTokenType.
    /// @return `true` if the token is a syntactic type token, `false` otherwise.
    public boolean isValidType(Token token) { return (token instanceof TypeToken) || isTypeName(token); }

    /// Parses source-level type syntax without resolving its meaning.
    ///
    /// Grammar rule:
    /// `type → (primitiveType | typeName) arrayDimensions?`
    /// @return A TypeSyntax node representing the parsed type spelling.
    protected TypeSyntax parseTypeSyntax() {

        var token = state.peek();
        if (token instanceof TypeToken) {

            var typeToken = state.advance();
            var baseType = new NamedTypeSyntax(
                typeToken.getLine(),
                typeToken.getColumn(),
                typeToken.getType().token(),
                true
            );
            var arrayDims = parseArrayDimensions();
            if (arrayDims.length == 0) return baseType;
            return new ArrayTypeSyntax(baseType.getLine(), baseType.getColumn(), baseType, arrayDims);
        } else if (isTypeName(token)) {

            var classToken = state.advance();
            var className = state.getLiteralValue(classToken);

            var baseType = new NamedTypeSyntax(classToken.getLine(), classToken.getColumn(), className, false);
            var arrayDims = parseArrayDimensions();
            if (arrayDims.length == 0) return baseType;
            return new ArrayTypeSyntax(baseType.getLine(), baseType.getColumn(), baseType, arrayDims);
        }

        throw parseError("Expect type (primitive or class name)", token);
    }

    /// Checks if a token is syntactically a type name.
    /// @param token The token to checkCurrentTokenType.
    /// @return `true` if the token is an identifier literal, `false` otherwise.
    private boolean isTypeName(Token token) {

        if (!(token instanceof LiteralToken)) return false;
        if (!(token.getType() instanceof IdentifierLiteral)) return false;

        var name = token.getType().token();
        return name != null;
    }

    /// Parses an expression using the ExpressionParser.
    /// This is exposed for use in parsing variable initializers and other contexts where an expression is expected.
    /// @return An ExpressionNode representing the parsed expression.
    public ExpressionNode parseExpression() { return expressionParser.parseExpression(); }

    /// Parses a list of function parameters, which consist of a type and an identifier for each parameter, separated by commas.
    ///
    /// Grammar rule:
    /// `parameters → type IDENTIFIER ("," type IDENTIFIER)*`
    /// @return An array of FunctionParameter objects representing the parsed function parameters.
    public FunctionParameter[] parseParameters() {

        var parameters = new ArrayList<FunctionParameter>();
        if (!state.check(Delimiter.RPAREN)) do {

            var paramType = parseTypeSyntax();
            var paramName = state.consume(new IdentifierLiteral(), "Expect parameter name");
            parameters.add(new FunctionParameter(paramName.getLine(), paramName.getColumn(), state.getLiteralValue(paramName), paramType));
        } while (state.match(Delimiter.COMMA));
        return parameters.toArray(new FunctionParameter[0]);
    }

    /// Parses a declaration, which can be an access modifier followed by a class declaration, a function declaration,
    /// a variable declaration, or a statement.
    ///
    /// Grammar rule:
    /// `declaration → accessModifier classDecl | functionDecl | varDecl | statement`
    /// @return A StatementNode representing the parsed declaration or statement.
    public StatementNode parseDeclaration() {

        var accessModifier = parseAccessModifier();
        if (state.match(Keyword.CLASS)) {

            if (accessModifier == null) throw parseError("Class declaration requires an access modifier", state.previous());
            return classParser.parseClassDeclaration(accessModifier);
        }

        if (startsTypedDeclaration()) {

            var returnType = parseTypeSyntax();
            if (state.check(new IdentifierLiteral())) {

                var nameToken = state.advance();
                var name = state.getLiteralValue(nameToken);
                if (state.check(Delimiter.LPAREN)) return parseFunctionDeclaration(returnType, name, nameToken.getLine(), nameToken.getColumn());
                return parseVariableDeclaration(returnType, name, nameToken.getLine(), nameToken.getColumn());
            }
        }

        return parseStatement();
    }

    /// Checks if the current token sequence starts a typed declaration (function or variable).
    /// This is used to disambiguate between a statement and a declaration that starts with a type.
    /// @return `true` if the current token sequence starts a typed declaration, `false` otherwise.
    private boolean startsTypedDeclaration() {

        if (!isValidType(state.peek())) return false;
        var savedCurrent = state.getCurrentPosition();
        try {

            parseTypeSyntax();
            return state.check(new IdentifierLiteral());
        } catch (ParseException e) { return false; }
        finally { state.setCurrentPosition(savedCurrent); }
    }

    /// Parses a function declaration, which consists of a return type, a name, a parameter list, and a body enclosed in braces.
    ///
    /// Grammar rule:
    /// `functionDecl → type IDENTIFIER "(" parameters? ")" "{" declaration* "}"`
    /// @param returnType The parsed return type syntax of the function.
    /// @param name The name of the function.
    /// @param line The line number where the function is declared (for error reporting and AST node construction).
    /// @param column The column number where the function is declared (for error reporting and AST node construction).
    /// @return A FunctionDeclarationStatement representing the parsed function declaration.
    private FunctionDeclarationStatement parseFunctionDeclaration(TypeSyntax returnType, String name, int line, int column) {

        state.consume(Delimiter.LPAREN, "Expect '(' after function name");

        var parameters = parseParameters();
        state.consume(Delimiter.RPAREN, "Expect ')' after parameters");

        state.consume(Delimiter.LBRACE, "Expect '{' before function body");

        var bodyStatements = parseBlockStatements();
        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));

        return new FunctionDeclarationStatement(line, column, returnType, name, parameters, body);
    }

    /// Parses a variable declaration, which consists of a type, a name, an optional initializer, and may include multiple declarations separated by commas.
    ///
    /// Grammar rule:
    /// `varDecl → type arrayDimensions? IDENTIFIER ("=" expression)? ("," IDENTIFIER ("=" expression)?)* ";"`
    /// @param type The parsed type syntax of the variable.
    /// @param name The name of the variable.
    /// @param line The line number where the variable is declared (for error reporting and AST node construction).
    /// @param column The column number where the variable is declared (for error reporting and AST node construction).
    /// @return A StatementNode representing the parsed variable declaration(s).
    /// - If there is only one variable declared, returns a VariableDeclarationStatement.
    /// - If there are multiple variables declared in the same statement, returns a BlockStatement containing multiple VariableDeclarationStatements.
    private StatementNode parseVariableDeclaration(TypeSyntax type, String name, int line, int column) {

        ExpressionNode initializer = null;
        if (state.match(Operator.ASSIGN)) initializer = expressionParser.parseExpression();

        var firstDecl = new VariableDeclarationStatement(line, column, type, name, initializer);

        if (state.match(Delimiter.COMMA)) {

            var declarations = new ArrayList<StatementNode>();
            declarations.add(firstDecl);

            do {

                var nameToken = state.consume(new IdentifierLiteral(), "Expect variable name");
                var varName = state.getLiteralValue(nameToken);

                ExpressionNode init = null;
                if (state.match(Operator.ASSIGN)) init = expressionParser.parseExpression();

                var decl = new VariableDeclarationStatement(nameToken.getLine(), nameToken.getColumn(), type, varName, init);
                declarations.add(decl);
            } while (state.match(Delimiter.COMMA));

            state.consume(Delimiter.SEMICOLON, "Expect ';' after variable declaration");
            return new BlockStatement(line, column, declarations.toArray(new StatementNode[0]));
        }

        state.consume(Delimiter.SEMICOLON, "Expect ';' after variable declaration");
        return firstDecl;
    }

     /// Parses a statement, which can be an if statement, while loop, for loop, for-each loop, switch statement,
     /// return statement, block, break, continue, or expression statement.
     ///
     /// Grammar rule:
     /// `statement → exprStmt | ifStmt | whileStmt | forStmt | forEachStmt | switchStmt | returnStmt | breakStmt | continueStmt | block`
     /// @return A StatementNode representing the parsed statement.
     public StatementNode parseStatement() {

         if (state.match(Keyword.IF)) return parseIfStatement();
         if (state.match(Keyword.WHILE)) return parseWhileStatement();
         if (state.match(Keyword.FOR)) return parseForOrForEachStatement();
         if (state.match(Keyword.SWITCH)) return parseSwitchStatement();
         if (state.match(Keyword.BREAK)) {

             var breakToken = state.previous();
             state.consume(Delimiter.SEMICOLON, "Expect ';' after break statement");
             return new BreakStatement(breakToken.getLine(), breakToken.getColumn());
         }
         if (state.match(Keyword.CONTINUE)) {

             var continueToken = state.previous();
             state.consume(Delimiter.SEMICOLON, "Expect ';' after continue statement");
             return new ContinueStatement(continueToken.getLine(), continueToken.getColumn());
         }
        if (state.match(Keyword.RETURN)) return parseReturnStatement();
        if (state.match(Delimiter.LBRACE)) return parseBlock();
        return parseExpressionStatement();
    }

    /// Parses an if statement.
    ///
    /// Grammar rule:
    /// `ifStmt → "if" "(" expression ")" statement ("else" statement)?`
    /// @return An IfStatement representing the parsed if statement.
    private IfStatement parseIfStatement() {

        var ifToken = state.previous();

        state.consume(Delimiter.LPAREN, "Expect '(' after 'if'");
        var condition = expressionParser.parseExpression();
        state.consume(Delimiter.RPAREN, "Expect ')' after if condition");

        var thenBranch = parseStatement();
        StatementNode elseBranch = null;

        if (state.match(Keyword.ELSE)) elseBranch = parseStatement();
        return new IfStatement(ifToken.getLine(), ifToken.getColumn(), condition, thenBranch, elseBranch);
    }

    /// Parses a while statement.
    ///
    /// Grammar rule:
    /// `whileStmt → "while" "(" expression ")" statement`
    /// @return A WhileStatement representing the parsed while loop.
    private WhileStatement parseWhileStatement() {

        var whileToken = state.previous();

        state.consume(Delimiter.LPAREN, "Expect '(' after 'while'");
        var condition = expressionParser.parseExpression();
        state.consume(Delimiter.RPAREN, "Expect ')' after while condition");

        var body = parseStatement();
        return new WhileStatement(whileToken.getLine(), whileToken.getColumn(), condition, body);
    }

    /// Dispatches to either a regular for loop or a for-each loop.
    ///
    /// After consuming the opening {@code (} and a type and identifier, if the next token is {@code :}
    /// it is treated as a for-each; otherwise parsing falls back to a regular for loop.
    ///
    /// Grammar:
    /// ```
    /// forStmt     → "for" "(" (varDecl | exprStmt | ";") expression? ";" expression? ")" statement
    /// forEachStmt → "for" "(" type IDENTIFIER ":" expression ")" statement
    /// ```
    private StatementNode parseForOrForEachStatement() {

        var forToken = state.previous();
        state.consume(Delimiter.LPAREN, "Expect '(' after 'for'");

        // Peek ahead: if we see <type> <identifier> ":" it's a for-each
        if (isValidType(state.peek())) {

            // Save position to restore if this is not a for-each
            var savedCurrent = state.getCurrentPosition();
            var type = parseTypeSyntax();

            if (state.check(new IdentifierLiteral())) {

                var nameToken = state.advance();
                if (state.match(Delimiter.COLON)) {

                    var elementName = state.getLiteralValue(nameToken);
                    var iterable = expressionParser.parseExpression();
                    state.consume(Delimiter.RPAREN, "Expect ')' after for-each expression");
                    var body = parseStatement();
                    return new ForEachStatement(forToken.getLine(), forToken.getColumn(), type, elementName, iterable, body);
                }

                // Not a for-each — restore and fall through to regular for parsing
                state.setCurrentPosition(savedCurrent);
            } else state.setCurrentPosition(savedCurrent);
        }

        return parseForStatement(forToken);
    }

    /// Parses a regular for loop (opening parenthesis already consumed).
    ///
    /// Grammar rule:
    /// `forStmt → "for" "(" (varDecl | exprStmt | ";") expression? ";" expression? ")" statement`
    /// @param forToken The `for` keyword token, used for line/column in the resulting node.
    /// @return A ForStatement.
    private ForStatement parseForStatement(Token forToken) {

        StatementNode initializer;
        if (state.match(Delimiter.SEMICOLON)) initializer = null;
        else if (startsTypedDeclaration()) {

            var type = parseTypeSyntax();
            var nameToken = state.consume(new IdentifierLiteral(), "Expect variable name");
            var name = state.getLiteralValue(nameToken);

            ExpressionNode init = null;
            if (state.match(Operator.ASSIGN)) init = expressionParser.parseExpression();
            state.consume(Delimiter.SEMICOLON, "Expect ';' after variable declaration");

            initializer = new VariableDeclarationStatement(nameToken.getLine(), nameToken.getColumn(), type, name, init);
        } else {

            var expr = expressionParser.parseExpression();
            state.consume(Delimiter.SEMICOLON, "Expect ';' after expression");
            initializer = new ExpressionStatement(expr.getLine(), expr.getColumn(), expr);
        }

        ExpressionNode condition = null;
        if (!state.check(Delimiter.SEMICOLON)) condition = expressionParser.parseExpression();
        state.consume(Delimiter.SEMICOLON, "Expect ';' after loop condition");

        ExpressionNode incrementExpr = null;
        if (!state.check(Delimiter.RPAREN)) incrementExpr = expressionParser.parseExpression();
        state.consume(Delimiter.RPAREN, "Expect ')' after for clauses");

        StatementNode increment = null;
        if (incrementExpr != null) increment = new ExpressionStatement(incrementExpr.getLine(), incrementExpr.getColumn(), incrementExpr);

        var body = parseStatement();
        return new ForStatement(forToken.getLine(), forToken.getColumn(), initializer, condition, increment, body);
    }

    /// Parses a switch statement.
    ///
    /// Grammar rule:
    /// ```
    /// switchStmt → "switch" "(" expression ")" "{" switchCase* "}"
    /// switchCase → "case" expression ":" statement | "default" ":" statement
    /// ```
    private SwitchStatement parseSwitchStatement() {

        var switchToken = state.previous();
        state.consume(Delimiter.LPAREN, "Expect '(' after 'switch'");
        var subject = expressionParser.parseExpression();
        state.consume(Delimiter.RPAREN, "Expect ')' after switch expression");
        state.consume(Delimiter.LBRACE, "Expect '{' before switch body");

        var cases = new ArrayList<SwitchCase>();
        while (!state.check(Delimiter.RBRACE) && !state.isAtEnd()) if (state.match(Keyword.CASE)) {

            var caseToken = state.previous();
            var value = expressionParser.parseExpression();
            state.consume(Delimiter.COLON, "Expect ':' after case value");
            var body = parseStatement();
            cases.add(new SwitchCase(caseToken.getLine(), caseToken.getColumn(), value, body));
        } else if (state.match(Keyword.DEFAULT)) {

            var defaultToken = state.previous();
            state.consume(Delimiter.COLON, "Expect ':' after 'default'");
            var body = parseStatement();
            cases.add(new SwitchCase(defaultToken.getLine(), defaultToken.getColumn(), null, body));
        } else {

            var badToken = state.peek();
            throw parseError("Expect 'case' or 'default' in switch body", badToken);
        }

        state.consume(Delimiter.RBRACE, "Expect '}' after switch body");
        return new SwitchStatement(switchToken.getLine(), switchToken.getColumn(), subject, cases.toArray(new SwitchCase[0]));
    }

    /// Parses a return statement.
    ///
    /// Grammar rule:
    /// `returnStmt → "return" expression? ";"`
    /// @return A ReturnStatement.
    private ReturnStatement parseReturnStatement() {

        var returnToken = state.previous();

        ExpressionNode value = null;
        if (!state.check(Delimiter.SEMICOLON)) value = expressionParser.parseExpression();

        state.consume(Delimiter.SEMICOLON, "Expect ';' after return value");
        return new ReturnStatement(returnToken.getLine(), returnToken.getColumn(), value);
    }

    /// Parses a block statement.
    ///
    /// Grammar rule:
    /// `block → "{" declaration* "}"`
    /// @return A BlockStatement.
    public BlockStatement parseBlock() {

        var lbrace = state.previous();
        var statements = parseBlockStatements();
        return new BlockStatement(lbrace.getLine(), lbrace.getColumn(), statements.toArray(new StatementNode[0]));
    }

    /// Helper method to parse a list of statements within a block, until the closing right brace is encountered.
    /// @return An ArrayList of StatementNode objects.
    ArrayList<StatementNode> parseBlockStatements() {

        var statements = new ArrayList<StatementNode>();

        while (!state.isAtEnd() && !currentTokenIs(Delimiter.RBRACE)) try {
            statements.add(parseDeclaration());
        } catch (ParseException e) {

            state.report(e);
            synchronizeBlockStatement();
        }

        state.consume(Delimiter.RBRACE, "Expect '}' after block");
        return statements;
    }

    /// Synchronizes after a block-level statement error without escaping the current block.
    private void synchronizeBlockStatement() {

        if (state.isAtEnd() || currentTokenIs(Delimiter.RBRACE) || isBlockRecoveryBoundary(state.peek())) return;

        state.advance();  // skip the offending token
        while (!state.isAtEnd() && !currentTokenIs(Delimiter.RBRACE)) {

            if (state.previous().getType() == Delimiter.SEMICOLON) return;
            if (isBlockRecoveryBoundary(state.peek())) return;
            state.advance();
        }
    }

    /// Checks if a token is a boundary for block-level statement recovery.
    /// This includes tokens that can start a new statement or declaration, such as keywords and delimiters.
    /// @param token The token to check.
    /// @return `true` if the token is a block recovery boundary, `false` otherwise.
    private boolean isBlockRecoveryBoundary(Token token) {

        if (token == null) return false;
        if (token instanceof TypeToken || isTypeName(token)) return true;

        var type = token.getType();
        return type == Delimiter.LBRACE || type == Keyword.RETURN || isRecoveryBoundary(type) || isAccessModifier(type);
    }

    /// Parses an expression statement.
    ///
    /// Grammar rule:
    /// `exprStmt → expression ";"`
    /// @return An ExpressionStatement.
    private ExpressionStatement parseExpressionStatement() {

        var expr = expressionParser.parseExpression();
        state.consume(Delimiter.SEMICOLON, "Expect ';' after expression");
        return new ExpressionStatement(expr.getLine(), expr.getColumn(), expr);
    }
}
