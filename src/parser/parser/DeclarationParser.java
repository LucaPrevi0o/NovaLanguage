package parser.parser;

import lexer.*;
import lexer.token.LiteralToken;
import parser.ast.SymbolTable;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.*;
import parser.ast.nodes.statement.conditional.*;
import parser.ast.nodes.statement.declaration.*;
import parser.parser.util.ParseException;
import parser.parser.util.ParserBase;
import parser.parser.util.ParserState;
import token.ReturnType;
import token.TypeRegistry;
import token.family.Delimiter;
import token.family.Keyword;
import token.family.Literal;
import token.family.Operator;
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
/// declaration     → accessModifier? classDecl | functionDecl | varDecl | statement
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

    private ExpressionParser expressionParser;
    private final ClassParser classParser;

    /// Constructs a new DeclarationParser with the given parser state, symbol table, and type registry.
    /// @param state        The parser state containing the tokens to parse.
    /// @param symbolTable  The symbol table for tracking declared symbols (variables, functions, classes).
    /// @param typeRegistry The per-session type registry.
    public DeclarationParser(ParserState state, SymbolTable symbolTable, TypeRegistry typeRegistry) {

        super(state, symbolTable, typeRegistry);
        this.expressionParser = new ExpressionParser(state, symbolTable, typeRegistry);
        this.classParser = new ClassParser(this);
    }

    /// Parses array dimensions for a type, returning an array of ExpressionNode representing the sizes of each dimension.
    ///
    /// Grammar rule:
    /// `arrayDimensions → "[" expression? "]" (arrayDimensions)?`
    /// @return An array of ExpressionNode representing the sizes of each array dimension, or null for unspecified dimensions.
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

    /// Checks if a token is a valid type token (either a primitive type or a class name).
    /// @param token The token to check.
    /// @return True if the token is a valid type token (primitive or class name), false otherwise.
    public boolean isValidType(Token token) { return isTypeToken(token) || isClassName(token); }

    /// Parses a type, which can be either a primitive type or a class name, along with any array dimensions.
    ///
    /// Grammar rule:
    /// `type → (primitiveType | CLASSNAME) arrayDimensions?`
    /// @return A ReturnType representing the parsed type, including base type, array dimensions, super types, and generic parameters as applicable.
    protected ReturnType parseType() {

        var token = peek();
        if (isTypeToken(token)) {

            var typeToken = advance();
            var baseType = typeToken.getType(); // TokenFamily (PrimitiveType etc.)
            var arrayDims = parseArrayDimensions();
            return new ReturnType(baseType, arrayDims, null, null);
        } else if (isClassName(token)) {

            var classToken = (LiteralToken) advance();
            var className = classToken.getValue();

            var classType = typeRegistry.getReturnType(className);
            if (classType == null) throw new ParseException("Class '" + className + "' not found", classToken);

            var superTypes = classType.getSuperTypes();
            var genericParamType = classType.getGenericParameterType();

            var arrayDims = parseArrayDimensions();
            if (arrayDims.length == 0) return classType;
            return new ReturnType(classType.getBaseType(), arrayDims, superTypes, genericParamType);
        }

        throw new ParseException("Expect type (primitive or class name)", token);
    }

    /// Checks if a token is a valid class name, which is determined by being an identifier literal that corresponds to
    /// a registered custom class in the TypeRegistry.
    /// @param token The token to check.
    /// @return True if the token is a valid class name, false otherwise.
    private boolean isClassName(Token token) {

        if (!(token instanceof LiteralToken)) return false;
        if (!(token.getType() instanceof Literal.IdentifierLiteral)) return false;

        var name = ((LiteralToken) token).getValue();
        return typeRegistry.isCustomClass(name);
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

            if (parameters.size() >= 255) throw new ParseException("Cannot have more than 255 parameters", peek());

            var paramType = parseType();
            var paramName = consume(new Literal.IdentifierLiteral(), "Expect parameter name");
            parameters.add(new FunctionParameter(paramName.getLine(), paramName.getColumn(), getLiteralValue(paramName), paramType));
        } while (match(Delimiter.COMMA));
        return parameters.toArray(new FunctionParameter[0]);
    }

    /// Parses a declaration, which can be an access modifier followed by a class declaration, a function declaration,
    /// a variable declaration, or a statement.
    ///
    /// Grammar rule:
    /// `declaration → accessModifier? classDecl | functionDecl | varDecl | statement`
    /// @return A StatementNode representing the parsed declaration or statement.
    public StatementNode parseDeclaration() {

        var accessModifier = parseAccessModifier();
        if (match(Keyword.CLASS)) {

            if (accessModifier == null) throw new ParseException("Class declaration requires an access modifier", previous());
            return classParser.parseClassDeclaration(accessModifier);
        }

        if (isValidType(peek())) {

            var returnType = parseType();
            if (check(new Literal.IdentifierLiteral())) {

                var nameToken = advance();
                var name = getLiteralValue(nameToken);

                if (check(Delimiter.LPAREN)) return parseFunctionDeclaration(returnType, name, nameToken.getLine(), nameToken.getColumn());
                return parseVariableDeclaration(returnType, name, nameToken.getLine(), nameToken.getColumn());
            }
        }
        return parseStatement();
    }

    /// Parses a function declaration, which consists of a return type, a name, a parameter list, and a body enclosed in braces.
    ///
    /// Grammar rule:
    /// `functionDecl → type IDENTIFIER "(" parameters? ")" "{" declaration* "}"`
    ///
    /// The function is pre-registered in the enclosing scope before its body is parsed so that
    /// recursive calls inside the body can resolve the function name.
    ///
    /// @param returnType The return type of the function, already parsed.
    /// @param name The name of the function.
    /// @param line The line number where the function is declared (for error reporting and AST node construction).
    /// @param column The column number where the function is declared (for error reporting and AST node construction).
    /// @return A FunctionDeclarationStatement representing the parsed function declaration.
    private FunctionDeclarationStatement parseFunctionDeclaration(ReturnType returnType, String name, int line, int column) {

        consume(Delimiter.LPAREN, "Expect '(' after function name");

        var parameters = parseParameters();
        consume(Delimiter.RPAREN, "Expect ')' after parameters");

        // Pre-register with null body in the enclosing scope so recursive calls resolve.
        var decl = new FunctionDeclarationStatement(line, column, returnType, name, parameters, null);
        this.symbolTable.register(decl);

        var functionScope = enterScope();
        this.symbolTable = functionScope;

        for (FunctionParameter param : parameters) this.symbolTable.register(param);
        this.expressionParser = new ExpressionParser(state, this.symbolTable, this.typeRegistry);

        consume(Delimiter.LBRACE, "Expect '{' before function body");

        var bodyStatements = getStatementList(functionScope);
        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));

        decl.setBody(body);  // Attach parsed body to the pre-registered declaration
        return decl;
    }

    /// Parses a variable declaration, which consists of a type, a name, an optional initializer, and may include multiple declarations separated by commas.
    ///
    /// Grammar rule:
    /// `varDecl → type arrayDimensions? IDENTIFIER ("=" expression)? ("," IDENTIFIER ("=" expression)?)* ";"`
    ///
    /// The first variable is registered in the symbol table immediately so that it is visible
    /// to subsequent initialisers in the same comma-list.
    ///
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
        this.symbolTable.register(firstDecl);  // Register first variable immediately

        if (match(Delimiter.COMMA)) {

            var declarations = new ArrayList<StatementNode>();
            declarations.add(firstDecl);

            do {

                var nameToken = consume(new Literal.IdentifierLiteral(), "Expect variable name");
                var varName = getLiteralValue(nameToken);

                ExpressionNode init = null;
                if (match(Operator.ASSIGN)) init = expressionParser.parseExpression();

                var decl = new VariableDeclarationStatement(nameToken.getLine(), nameToken.getColumn(), type, varName, init);
                this.symbolTable.register(decl);  // Register each subsequent variable
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

            if (check(new Literal.IdentifierLiteral())) {

                var nameToken = advance();
                if (match(Delimiter.COLON)) {

                    // for-each: for (Type name : iterable) body
                    var elementName = getLiteralValue(nameToken);
                    var forEachScope = enterScope();
                    this.symbolTable = forEachScope;
                    this.expressionParser = new ExpressionParser(state, this.symbolTable, this.typeRegistry);

                    try {
                        var elementDecl = new VariableDeclarationStatement(nameToken.getLine(), nameToken.getColumn(), type, elementName, null);
                        this.symbolTable.register(elementDecl);

                        var iterable = expressionParser.parseExpression();
                        consume(Delimiter.RPAREN, "Expect ')' after for-each expression");
                        var body = parseStatement();
                        return new ForEachStatement(forToken.getLine(), forToken.getColumn(), type, elementName, iterable, body);
                    } finally {
                        this.symbolTable = exitScope(forEachScope);
                        this.expressionParser = new ExpressionParser(state, this.symbolTable, this.typeRegistry);
                    }
                }

                // Not a for-each — restore and fall through to regular for parsing
                state.setCurrentPosition(savedCurrent);
            } else {
                state.setCurrentPosition(savedCurrent);
            }
        }

        return parseForStatement(forToken);
    }

    /// Parses a regular for loop (opening parenthesis already consumed).
    ///
    /// Grammar rule:
    /// `forStmt → "for" "(" (varDecl | exprStmt | ";") expression? ";" expression? ")" statement`
    /// @param forToken The "for" keyword token, used for line/column in the resulting node.
    /// @return A ForStatement.
    private ForStatement parseForStatement(Token forToken) {

        var forScope = enterScope();
        this.symbolTable = forScope;
        this.expressionParser = new ExpressionParser(state, this.symbolTable, this.typeRegistry);

        try {
            StatementNode initializer;
            if (match(Delimiter.SEMICOLON)) initializer = null;
            else if (isValidType(peek())) {

                var type = parseType();
                var nameToken = consume(new Literal.IdentifierLiteral(), "Expect variable name");
                var name = getLiteralValue(nameToken);

                ExpressionNode init = null;
                if (match(Operator.ASSIGN)) init = expressionParser.parseExpression();
                consume(Delimiter.SEMICOLON, "Expect ';' after variable declaration");

                initializer = new VariableDeclarationStatement(
                    nameToken.getLine(), nameToken.getColumn(), type, name, init);
                this.symbolTable.register((VariableDeclarationStatement) initializer);
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
        } finally {
            this.symbolTable = exitScope(forScope);
            this.expressionParser = new ExpressionParser(state, this.symbolTable, this.typeRegistry);
        }
    }

    /// Parses a switch statement.
    ///
    /// Grammar rule:
    /// ```
    /// switchStmt → "switch" "(" expression ")" "{" switchCase* "}"
    /// switchCase → "case" expression "->" statement | "default" "->" statement
    /// ```
    private SwitchStatement parseSwitchStatement() {

        var switchToken = previous();
        consume(Delimiter.LPAREN, "Expect '(' after 'switch'");
        var subject = expressionParser.parseExpression();
        consume(Delimiter.RPAREN, "Expect ')' after switch expression");
        consume(Delimiter.LBRACE, "Expect '{' before switch body");

        var cases = new ArrayList<SwitchCase>();
        while (!check(Delimiter.RBRACE) && isNotAtEnd()) {

            if (match(Keyword.CASE)) {

                var caseToken = previous();
                var value = expressionParser.parseExpression();
                consume(Operator.ARROW, "Expect '->' after case value");
                var body = parseStatement();
                cases.add(new SwitchCase(caseToken.getLine(), caseToken.getColumn(), value, body));
            } else if (match(Keyword.DEFAULT)) {

                var defaultToken = previous();
                consume(Operator.ARROW, "Expect '->' after 'default'");
                var body = parseStatement();
                cases.add(new SwitchCase(defaultToken.getLine(), defaultToken.getColumn(), null, body));
            } else throw new ParseException("Expect 'case' or 'default' in switch body", peek());
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

        var blockScope = enterScope();
        this.symbolTable = blockScope;
        this.expressionParser = new ExpressionParser(state, this.symbolTable, this.typeRegistry);

        var statements = getStatementList(blockScope);
        return new BlockStatement(lbrace.getLine(), lbrace.getColumn(), statements.toArray(new StatementNode[0]));
    }

    /// Helper method to parse a list of statements within a block, until the closing right brace is encountered.
    /// @param blockScope The symbol table representing the scope of the block.
    /// @return An ArrayList of StatementNode objects.
    ArrayList<StatementNode> getStatementList(SymbolTable blockScope) {

        var statements = new ArrayList<StatementNode>();

        while (!check(Delimiter.RBRACE) && isNotAtEnd()) statements.add(parseDeclaration());
        consume(Delimiter.RBRACE, "Expect '}' after block");

        this.symbolTable = exitScope(blockScope);
        this.expressionParser = new ExpressionParser(state, this.symbolTable, this.typeRegistry);
        return statements;
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
