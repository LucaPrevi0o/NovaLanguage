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
import lexer.token.ReturnType;
import lexer.token.TypeRegistry;
import lexer.token.family.AccessModifier;
import lexer.token.family.Delimiter;
import lexer.token.family.Keyword;
import lexer.token.family.NonPrimitiveType;
import lexer.token.family.Operator;
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

    /// Constructs a new DeclarationParser with the given parser state and type registry.
    /// @param state        The parser state containing the tokens to parse.
    /// @param typeRegistry The per-session type registry.
    public DeclarationParser(ParserState state, TypeRegistry typeRegistry) {

        super(state, typeRegistry);
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
        while (match(Delimiter.LSQUARE)) {

            if (!check(Delimiter.RSQUARE)) {

                var sizeExpr = expressionParser.parseExpression();
                arraySizes.add(sizeExpr);
            } else arraySizes.add(null);  // No size specified
            consume(Delimiter.RSQUARE, "Expect ']' after array dimension");
        }
        return arraySizes.toArray(new ExpressionNode[0]);
    }

    /// Checks if a token is syntactically a valid type token (either a primitive type or an identifier type name).
    /// @param token The token to checkCurrentTokenType.
    /// @return `true` if the token is a syntactic type token, `false` otherwise.
    public boolean isValidType(Token token) { return (token instanceof TypeToken) || isTypeName(token); }

    /// Parses a type, which can be either a primitive type or a class name, along with any array dimensions.
    ///
    /// Grammar rule:
    /// `type → (primitiveType | CLASSNAME) arrayDimensions?`
    /// @return A ReturnType representing the parsed type, including base type, array dimensions, super types, and generic parameters as applicable.
    protected ReturnType parseType() {

        var token = peek();
        if (token instanceof TypeToken) {

            var typeToken = advance();
            var baseType = typeToken.getType(); // TokenClass (PrimitiveType etc.)
            var arrayDims = parseArrayDimensions();
            return new ReturnType(baseType, arrayDims, null, null);
        } else if (isTypeName(token)) {

            var classToken = advance();
            var className = getLiteralValue(classToken);

            var classType = typeRegistry.getReturnType(className);
            var arrayDims = parseArrayDimensions();
            if (classType == null) return new ReturnType(new NonPrimitiveType(className), arrayDims, null, null);
            if (arrayDims.length == 0) return classType;
            return new ReturnType(classType.getTokenClass(), arrayDims, classType.getSuperTypes(), classType.getGenericParameterType());
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
        if (!check(Delimiter.RPAREN)) do {

            //if (parameters.size() >= 255) throw parseError("Cannot have more than 255 parameters", getCurrentToken());
            var paramType = parseType();
            var paramName = consume(new IdentifierLiteral(), "Expect parameter name");
            parameters.add(new FunctionParameter(paramName.getLine(), paramName.getColumn(), getLiteralValue(paramName), paramType));
        } while (match(Delimiter.COMMA));
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
        if (match(Keyword.CLASS)) {

            if (accessModifier == null) throw parseError("Class declaration requires an access modifier", previous());
            return classParser.parseClassDeclaration(accessModifier);
        }

        if (startsTypedDeclaration()) {

            var returnType = parseType();
            if (check(new IdentifierLiteral())) {

                var nameToken = advance();
                var name = getLiteralValue(nameToken);
                if (check(Delimiter.LPAREN)) return parseFunctionDeclaration(returnType, name, nameToken.getLine(), nameToken.getColumn());
                return parseVariableDeclaration(returnType, name, nameToken.getLine(), nameToken.getColumn());
            }
        }

        return parseStatement();
    }

    /// Checks if the current token sequence starts a typed declaration (function or variable).
    /// This is used to disambiguate between a statement and a declaration that starts with a type.
    /// @return `true` if the current token sequence starts a typed declaration, `false` otherwise.
    private boolean startsTypedDeclaration() {

        if (!isValidType(peek())) return false;
        var savedCurrent = state.getCurrentPosition();
        try {

            parseType();
            return check(new IdentifierLiteral());
        } catch (ParseException e) { return false; }
        finally { state.setCurrentPosition(savedCurrent); }
    }

    /// Parses a function declaration, which consists of a return type, a name, a parameter list, and a body enclosed in braces.
    ///
    /// Grammar rule:
    /// `functionDecl → type IDENTIFIER "(" parameters? ")" "{" declaration* "}"`
    /// @param returnType The return type of the function, already parsed.
    /// @param name The name of the function.
    /// @param line The line number where the function is declared (for error reporting and AST node construction).
    /// @param column The column number where the function is declared (for error reporting and AST node construction).
    /// @return A FunctionDeclarationStatement representing the parsed function declaration.
    private FunctionDeclarationStatement parseFunctionDeclaration(ReturnType returnType, String name, int line, int column) {

        consume(Delimiter.LPAREN, "Expect '(' after function name");

        var parameters = parseParameters();
        consume(Delimiter.RPAREN, "Expect ')' after parameters");

        consume(Delimiter.LBRACE, "Expect '{' before function body");

        var bodyStatements = parseBlockStatements();
        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));

        return new FunctionDeclarationStatement(line, column, returnType, name, parameters, body);
    }

    /// Parses a variable declaration, which consists of a type, a name, an optional initializer, and may include multiple declarations separated by commas.
    ///
    /// Grammar rule:
    /// `varDecl → type arrayDimensions? IDENTIFIER ("=" expression)? ("," IDENTIFIER ("=" expression)?)* ";"`
    /// @param type The return type of the variable, already parsed.
    /// @param name The name of the variable.
    /// @param line The line number where the variable is declared (for error reporting and AST node construction).
    /// @param column The column number where the variable is declared (for error reporting and AST node construction).
    /// @return A StatementNode representing the parsed variable declaration(s).
    /// - If there is only one variable declared, returns a VariableDeclarationStatement.
    /// - If there are multiple variables declared in the same statement, returns a BlockStatement containing multiple VariableDeclarationStatements.
    private StatementNode parseVariableDeclaration(ReturnType type, String name, int line, int column) {

        ExpressionNode initializer = null;
        if (match(Operator.ASSIGN)) initializer = expressionParser.parseExpression();

        var firstDecl = new VariableDeclarationStatement(line, column, type, name, initializer);

        if (match(Delimiter.COMMA)) {

            var declarations = new ArrayList<StatementNode>();
            declarations.add(firstDecl);

            do {

                var nameToken = consume(new IdentifierLiteral(), "Expect variable name");
                var varName = getLiteralValue(nameToken);

                ExpressionNode init = null;
                if (match(Operator.ASSIGN)) init = expressionParser.parseExpression();

                var decl = new VariableDeclarationStatement(nameToken.getLine(), nameToken.getColumn(), type, varName, init);
                declarations.add(decl);
            } while (match(Delimiter.COMMA));

            consume(Delimiter.SEMICOLON, "Expect ';' after variable declaration");
            return new BlockStatement(line, column, declarations.toArray(new StatementNode[0]));
        }

        consume(Delimiter.SEMICOLON, "Expect ';' after variable declaration");
        return firstDecl;
    }

     /// Parses a statement, which can be an if statement, while loop, for loop, for-each loop, switch statement,
     /// return statement, block, break, continue, or expression statement.
     ///
     /// Grammar rule:
     /// `statement → exprStmt | ifStmt | whileStmt | forStmt | forEachStmt | switchStmt | returnStmt | breakStmt | continueStmt | block`
     /// @return A StatementNode representing the parsed statement.
     public StatementNode parseStatement() {

         if (match(Keyword.IF)) return parseIfStatement();
         if (match(Keyword.WHILE)) return parseWhileStatement();
         if (match(Keyword.FOR)) return parseForOrForEachStatement();
         if (match(Keyword.SWITCH)) return parseSwitchStatement();
         if (match(Keyword.BREAK)) {

             var breakToken = previous();
             consume(Delimiter.SEMICOLON, "Expect ';' after break statement");
             return new BreakStatement(breakToken.getLine(), breakToken.getColumn());
         }
         if (match(Keyword.CONTINUE)) {

             var continueToken = previous();
             consume(Delimiter.SEMICOLON, "Expect ';' after continue statement");
             return new ContinueStatement(continueToken.getLine(), continueToken.getColumn());
         }
        if (match(Keyword.RETURN)) return parseReturnStatement();
        if (match(Delimiter.LBRACE)) return parseBlock();
        return parseExpressionStatement();
    }

    /// Parses an if statement.
    ///
    /// Grammar rule:
    /// `ifStmt → "if" "(" expression ")" statement ("else" statement)?`
    /// @return An IfStatement representing the parsed if statement.
    private IfStatement parseIfStatement() {

        var ifToken = previous();

        consume(Delimiter.LPAREN, "Expect '(' after 'if'");
        var condition = expressionParser.parseExpression();
        consume(Delimiter.RPAREN, "Expect ')' after if condition");

        var thenBranch = parseStatement();
        StatementNode elseBranch = null;

        if (match(Keyword.ELSE)) elseBranch = parseStatement();
        return new IfStatement(ifToken.getLine(), ifToken.getColumn(), condition, thenBranch, elseBranch);
    }

    /// Parses a while statement.
    ///
    /// Grammar rule:
    /// `whileStmt → "while" "(" expression ")" statement`
    /// @return A WhileStatement representing the parsed while loop.
    private WhileStatement parseWhileStatement() {

        var whileToken = previous();

        consume(Delimiter.LPAREN, "Expect '(' after 'while'");
        var condition = expressionParser.parseExpression();
        consume(Delimiter.RPAREN, "Expect ')' after while condition");

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

        var forToken = previous();
        consume(Delimiter.LPAREN, "Expect '(' after 'for'");

        // Peek ahead: if we see <type> <identifier> ":" it's a for-each
        if (isValidType(peek())) {

            // Save position to restore if this is not a for-each
            var savedCurrent = state.getCurrentPosition();
            var type = parseType();

            if (check(new IdentifierLiteral())) {

                var nameToken = advance();
                if (match(Delimiter.COLON)) {

                    var elementName = getLiteralValue(nameToken);
                    var iterable = expressionParser.parseExpression();
                    consume(Delimiter.RPAREN, "Expect ')' after for-each expression");
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
        if (match(Delimiter.SEMICOLON)) initializer = null;
        else if (startsTypedDeclaration()) {

            var type = parseType();
            var nameToken = consume(new IdentifierLiteral(), "Expect variable name");
            var name = getLiteralValue(nameToken);

            ExpressionNode init = null;
            if (match(Operator.ASSIGN)) init = expressionParser.parseExpression();
            consume(Delimiter.SEMICOLON, "Expect ';' after variable declaration");

            initializer = new VariableDeclarationStatement(nameToken.getLine(), nameToken.getColumn(), type, name, init);
        } else {

            var expr = expressionParser.parseExpression();
            consume(Delimiter.SEMICOLON, "Expect ';' after expression");
            initializer = new ExpressionStatement(expr.getLine(), expr.getColumn(), expr);
        }

        ExpressionNode condition = null;
        if (!check(Delimiter.SEMICOLON)) condition = expressionParser.parseExpression();
        consume(Delimiter.SEMICOLON, "Expect ';' after loop condition");

        ExpressionNode incrementExpr = null;
        if (!check(Delimiter.RPAREN)) incrementExpr = expressionParser.parseExpression();
        consume(Delimiter.RPAREN, "Expect ')' after for clauses");

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

        var switchToken = previous();
        consume(Delimiter.LPAREN, "Expect '(' after 'switch'");
        var subject = expressionParser.parseExpression();
        consume(Delimiter.RPAREN, "Expect ')' after switch expression");
        consume(Delimiter.LBRACE, "Expect '{' before switch body");

        var cases = new ArrayList<SwitchCase>();
        while (!check(Delimiter.RBRACE) && isNotAtEnd()) if (match(Keyword.CASE)) {

            var caseToken = previous();
            var value = expressionParser.parseExpression();
            consume(Delimiter.COLON, "Expect ':' after case value");
            var body = parseStatement();
            cases.add(new SwitchCase(caseToken.getLine(), caseToken.getColumn(), value, body));
        } else if (match(Keyword.DEFAULT)) {

            var defaultToken = previous();
            consume(Delimiter.COLON, "Expect ':' after 'default'");
            var body = parseStatement();
            cases.add(new SwitchCase(defaultToken.getLine(), defaultToken.getColumn(), null, body));
        } else {

            var badToken = peek();
            throw parseError("Expect 'case' or 'default' in switch body", badToken);
        }

        consume(Delimiter.RBRACE, "Expect '}' after switch body");
        return new SwitchStatement(switchToken.getLine(), switchToken.getColumn(), subject, cases.toArray(new SwitchCase[0]));
    }

    /// Parses a return statement.
    ///
    /// Grammar rule:
    /// `returnStmt → "return" expression? ";"`
    /// @return A ReturnStatement.
    private ReturnStatement parseReturnStatement() {

        var returnToken = previous();

        ExpressionNode value = null;
        if (!check(Delimiter.SEMICOLON)) value = expressionParser.parseExpression();

        consume(Delimiter.SEMICOLON, "Expect ';' after return value");
        return new ReturnStatement(returnToken.getLine(), returnToken.getColumn(), value);
    }

    /// Parses a block statement.
    ///
    /// Grammar rule:
    /// `block → "{" declaration* "}"`
    /// @return A BlockStatement.
    public BlockStatement parseBlock() {

        var lbrace = previous();
        var statements = parseBlockStatements();
        return new BlockStatement(lbrace.getLine(), lbrace.getColumn(), statements.toArray(new StatementNode[0]));
    }

    /// Helper method to parse a list of statements within a block, until the closing right brace is encountered.
    /// @return An ArrayList of StatementNode objects.
    ArrayList<StatementNode> parseBlockStatements() {

        var statements = new ArrayList<StatementNode>();

        while (isNotAtEnd() && !currentTokenIs(Delimiter.RBRACE)) try {
            statements.add(parseDeclaration());
        } catch (ParseException e) {

            state.report(e);
            synchronizeBlockStatement();
        }

        consume(Delimiter.RBRACE, "Expect '}' after block");
        return statements;
    }

    /// Synchronizes after a block-level statement error without escaping the current block.
    private void synchronizeBlockStatement() {

        if (!isNotAtEnd() || currentTokenIs(Delimiter.RBRACE) || isBlockRecoveryBoundary(peek())) return;

        advance();  // skip the offending token
        while (isNotAtEnd() && !currentTokenIs(Delimiter.RBRACE)) {

            if (previous().getType() == Delimiter.SEMICOLON) return;
            if (isBlockRecoveryBoundary(peek())) return;
            advance();
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
        return type == Delimiter.LBRACE ||
               type == Keyword.CLASS ||
               type == Keyword.IF ||
               type == Keyword.WHILE ||
               type == Keyword.FOR ||
               type == Keyword.SWITCH ||
               type == Keyword.RETURN ||
               type == Keyword.BREAK ||
               type == Keyword.CONTINUE ||
               type == AccessModifier.PUBLIC ||
               type == AccessModifier.PRIVATE ||
               type == AccessModifier.PROTECTED;
    }

    /// Parses an expression statement.
    ///
    /// Grammar rule:
    /// `exprStmt → expression ";"`
    /// @return An ExpressionStatement.
    private ExpressionStatement parseExpressionStatement() {

        var expr = expressionParser.parseExpression();
        consume(Delimiter.SEMICOLON, "Expect ';' after expression");
        return new ExpressionStatement(expr.getLine(), expr.getColumn(), expr);
    }
}
