package parser.parser;

import error.ErrorCollector;
import error.syntax.UnexpectedTokenError;
import lexer.token.family.*;
import lexer.token.family.literal.IdentifierLiteral;
import lexer.token.type.LiteralToken;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.BlockStatement;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.*;
import parser.parser.util.ParseException;
import parser.parser.util.ParserBase;
import lexer.token.ReturnType;

import java.util.ArrayList;
import java.util.Objects;

/// Parses class declarations, including their methods, fields, superclasses, generic parameters, inner classes, access modifiers, and constructors.
///
/// <hr>
///
/// GRAMMAR FOR CLASS DECLARATIONS
///
/// ```
/// classDecl      → "class" IDENTIFIER ["[" IDENTIFIER "]"] ["::" IDENTIFIER ("," IDENTIFIER)*] "{" classMembers "}"
/// classMembers   → (classField | classMethod | constructor | innerClass)*
///
/// classField     → accessModifier? type IDENTIFIER ("=" expression)? ";"
/// classMethod    → accessModifier? type IDENTIFIER "(" parameters? ")" "{" declaration* "}"
/// constructor    → accessModifier? CLASSNAME "(" parameters? ")" "{" declaration* "}"
/// innerClass     → accessModifier? classDecl
///
/// accessModifier → "public" | "private" | "protected"
/// parameters     → type IDENTIFIER ("," type IDENTIFIER)*
/// ```
///
/// Features:
/// - **Generic Parameters**: Classes can have a single generic parameter specified with square brackets `[T]`
/// - **Superclasses**: Multiple inheritance supported using double colon `::` followed by comma-separated class names
/// - **Inner Classes**: Nested class declarations within the class body
/// - **Members**: Mix of fields (with optional initializers), methods, and constructors
/// - **Access Control**: Public, private, and protected modifiers for all class members
///
/// All scope mutations are wrapped in try/finally blocks so the parser state is always restored even
/// when a ParseException is thrown mid-class.
///
/// @see DeclarationParser
/// @see ExpressionParser
public class ClassParser extends ParserBase {

    private DeclarationParser declarationParser;

    /// Constructs a new ClassParser with the given DeclarationParser.
    /// @param declarationParser The DeclarationParser to use for parsing types and other declarations within the class.
    public ClassParser(DeclarationParser declarationParser) {

        super(declarationParser.getState(), declarationParser.getSymbolTable(), declarationParser.getTypeRegistry());
        this.declarationParser = declarationParser;
    }

    /// Parses a class declaration according to the grammar defined in the class documentation.
    ///
    /// Grammar rule:
    /// ```
    /// classDecl → "class" IDENTIFIER ["[" IDENTIFIER "]"] ["::" IDENTIFIER ("," IDENTIFIER)*]
    ///             "{" classMembers "}"
    /// ```
    /// @param classAccessModifier The access modifier of the class (e.g., public, private).
    /// @return A ClassDeclarationStatement representing the parsed class declaration.
    public ClassDeclarationStatement parseClassDeclaration(AccessModifier classAccessModifier) {

        var classToken = getPreviousToken(); // 'class' token was consumed by caller to determine access modifier

        // Parse class name
        var nextToken = getNextToken(new IdentifierLiteral(), new UnexpectedTokenError(
            new IdentifierLiteral(),
            getCurrentToken().getLine(),
            getCurrentToken().getColumn()
        ));

        var className = getLiteralValue((LiteralToken) nextToken);

        var superClasses = new ArrayList<ReturnType>();
        ReturnType genericClassParameter = null;

        nextToken = getNextToken(); // Advance past class name for next parsing steps

        // Parse optional generic parameter
        if (nextToken.getType().equals(Delimiter.LSQUARE)) {

            // Only one generic parameter allowed for simplicity; can be extended to multiple if needed
            var genToken = getNextToken(new IdentifierLiteral(), new UnexpectedTokenError(
                new IdentifierLiteral(),
                getCurrentToken().getLine(),
                getCurrentToken().getColumn())
            );
            var genericParameterName = getLiteralValue((LiteralToken) genToken);

            // Expect closing square bracket after generic parameter
            getNextToken(Delimiter.RSQUARE, new UnexpectedTokenError(
                Delimiter.RSQUARE,
                getCurrentToken().getLine(),
                getCurrentToken().getColumn()
            ));

            genericClassParameter = new ReturnType(new GenericParameterType(genericParameterName));
            typeRegistry.registerType(genericClassParameter); // Register generic parameter as a type to allow it to be used in member declarations
        } else if (nextToken.getType().equals(Delimiter.DOUBLE_COLON)) do {

            var superClassToken = getNextToken(new IdentifierLiteral(), new UnexpectedTokenError(
                new IdentifierLiteral(),
                getCurrentToken().getLine(),
                getCurrentToken().getColumn()
            ));

            // Look up superclass type in registry to ensure it exists and to get its ReturnType for the ClassDeclarationStatement
            var superClassName = getLiteralValue((LiteralToken) superClassToken);
            superClasses.add(typeRegistry.getReturnType(superClassName));

            getNextToken();  // Advance past superclass name for next iteration (either comma or opening brace)
        } while (this.checkCurrentTokenType(Delimiter.COMMA));

        // Create a placeholder ClassDeclarationStatement and register it in the type registry and symbol table before
        // parsing members to allow for recursive references (e.g., a method that returns the class type or a field of the class type)
        var classDecl = new ClassDeclarationStatement(
            classToken.getLine(),
            classToken.getColumn(),
            className,
            new ClassMethodDeclaration[0],
            new ClassFieldDeclaration[0],
            superClasses.toArray(new ReturnType[0]),
            genericClassParameter,
            new ClassDeclarationStatement[0],
            classAccessModifier,
            new ClassConstructorDeclaration[0]
        );

        typeRegistry.registerType(classDecl.getReturnType()); // Register class as a type before parsing members to allow for recursive references
        this.symbolTable.register(classDecl); // Register class declaration as a symbol in the current scope

        // Expect opening brace to start class body
        getNextToken(Delimiter.LBRACE, new UnexpectedTokenError(
            Delimiter.LBRACE,
            getCurrentToken().getLine(),
            getCurrentToken().getColumn()
        ));

        var classScope = enterScope(classDecl);
        var savedSymbolTable = this.symbolTable;

        this.symbolTable = classScope;
        this.declarationParser = new DeclarationParser(state, this.symbolTable, this.typeRegistry);

        var fields = new ArrayList<ClassFieldDeclaration>();
        var constructors = new ArrayList<ClassConstructorDeclaration>();
        var methods = new ArrayList<ClassMethodDeclaration>();
        var innerClasses = new ArrayList<ClassDeclarationStatement>();

        try {

            while (!checkCurrentTokenType(Delimiter.RBRACE) && isNotAtEnd()) {

                var memberAccessModifier = getNextToken();
                if (!checkCurrentTokenType(AccessModifier.values())) {

                    ErrorCollector.add(new UnexpectedTokenError(
                        AccessModifier.PRIVATE,
                        memberAccessModifier.getLine(),
                        memberAccessModifier.getColumn()
                    ));
                    return null; // return null as error
                }

                var nextToken = getNextToken(); // Advance past access modifier for member declaration parsing
                if (nextToken.getType() == Keyword.CLASS) innerClasses.add(parseClassDeclaration((AccessModifier) memberAccessModifier.getType()));
                else if (nextToken.getType().equals(new IdentifierLiteral(className))) {

                    getNextToken();  // getNextToken class name
                    constructors.add(parseConstructor(classToken.getLine(), classToken.getColumn(), (AccessModifier) memberAccessModifier.getType()));
                } else if (declarationParser.isValidType(nextToken)) {

                    var type = nextToken.getType();
                    var memberNameToken = getNextToken(new IdentifierLiteral(), new UnexpectedTokenError(
                        new IdentifierLiteral(),
                        getCurrentToken().getLine(),
                        getCurrentToken().getColumn()
                    ));
                    var memberName = getLiteralValue((LiteralToken) memberNameToken);

                    nextToken = getCurrentToken();
                    if (checkCurrentTokenType(Delimiter.LPAREN)) methods.add(parseClassMethod(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
                    else fields.add(parseClassField(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));

                    ErrorCollector.add(new UnexpectedTokenError(nextToken.getType(), nextToken.getLine(), nextToken.getColumn()));
                    return null; // return null as error
                }
            }
        } finally {

            getNextToken(Delimiter.RBRACE, "Expect '}' after class body");
            this.symbolTable = exitScope(classScope);
            this.declarationParser = new DeclarationParser(state, savedSymbolTable, this.typeRegistry);
        }

        // Populate the already-registered placeholder with the fully parsed members.
        // Using the same object keeps the symbol table and type registry consistent.
        classDecl.setMethods(methods.toArray(new ClassMethodDeclaration[0]));
        classDecl.setFields(fields.toArray(new ClassFieldDeclaration[0]));
        classDecl.setConstructors(constructors.toArray(new ClassConstructorDeclaration[0]));
        classDecl.setInnerClasses(innerClasses.toArray(new ClassDeclarationStatement[0]));
        return classDecl;
    }

     /// Parses a class field declaration, which may optionally include an initializer.
    ///
    /// Grammar rule:
    /// ```
    /// classField → accessModifier? type IDENTIFIER ("=" expression)? ";"
    /// ```
    /// @param type The return type of the field.
    /// @param name The name of the field.
    /// @param line The line number where the field declaration appears.
    /// @param column The column number where the field declaration starts.
    /// @param accessModifier The access modifier of the field (e.g., public, private).
    /// @return A ClassFieldDeclaration representing the parsed class field declaration.
    private ClassFieldDeclaration parseClassField(ReturnType type, String name, int line, int column, AccessModifier accessModifier) {

        ExpressionNode initializer = null;

        if (this.checkCurrentTokenType(Operator.ASSIGN)) initializer = declarationParser.parseExpression();
        getNextToken(Delimiter.SEMICOLON, "Expect ';' after field declaration");
        var decl = new ClassFieldDeclaration(line, column, type, name, initializer, accessModifier);
        this.symbolTable.register(decl); // Register field as symbol
        return decl;
    }

    /// Parses a class method declaration, including its parameters and body.
    ///
    /// Grammar rule:
    /// ```
    /// classMethod → accessModifier? type IDENTIFIER "(" parameters? ")" "{" declaration* "}"
    /// ```
    /// @param returnType The return type of the method.
    /// @param name The name of the method.
    /// @param line The line number where the method declaration appears.
    /// @param column The column number where the method declaration starts.
    /// @param accessModifier The access modifier of the method (e.g., public, private).
    /// @return A ClassMethodDeclaration representing the parsed class method declaration.
    private ClassMethodDeclaration parseClassMethod(ReturnType returnType, String name, int line, int column, AccessModifier accessModifier) {

        getNextToken(Delimiter.LPAREN, "Expect '(' after method name");

        var parameters = declarationParser.parseParameters();
        getNextToken(Delimiter.RPAREN, "Expect ')' after parameters");

        // Pre-register method in class scope so recursive calls resolve
        var placeholder = new ClassMethodDeclaration(line, column, returnType, name, parameters, null, accessModifier);
        this.symbolTable.register(placeholder);

        var methodScope = enterScope(placeholder);
        var savedSymbolTable = this.symbolTable;

        this.symbolTable = methodScope;
        this.declarationParser = new DeclarationParser(state, this.symbolTable, this.typeRegistry);
        for (var param : parameters) this.symbolTable.register(param);

        try {

            getNextToken(Delimiter.LBRACE, "Expect '{' before method body");

            var bodyStatements = new ArrayList<StatementNode>();
            while (!checkCurrentTokenType(Delimiter.RBRACE) && isNotAtEnd()) bodyStatements.add(declarationParser.parseDeclaration());
            getNextToken(Delimiter.RBRACE, "Expect '}' after method body");

            var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
            placeholder.setBody(body);
            return placeholder;
        } finally {

            this.symbolTable = exitScope(methodScope);
            this.declarationParser = new DeclarationParser(state, savedSymbolTable, this.typeRegistry);
        }
    }

    /// Parses a class constructor declaration, including its parameters and body.
    ///
    /// Grammar rule:
    /// ```
    /// constructor → accessModifier? CLASSNAME "(" parameters? ")" "{" declaration* "}"
    /// ```
    /// @param line The line number where the constructor declaration appears.
    /// @param column The column number where the constructor declaration starts.
    /// @param accessModifier The access modifier of the constructor (e.g., public, private).
    /// @return A ClassConstructorDeclaration representing the parsed class constructor declaration.
    private ClassConstructorDeclaration parseConstructor(int line, int column, AccessModifier accessModifier) {

        getNextToken(Delimiter.LPAREN, "Expect '(' after constructor name");

        var parameters = declarationParser.parseParameters();
        getNextToken(Delimiter.RPAREN, "Expect ')' after parameters");

        var constructorScope = enterScope();
        var savedSymbolTable = this.symbolTable;

        this.symbolTable = constructorScope;
        this.declarationParser = new DeclarationParser(state, this.symbolTable, this.typeRegistry);
        for (var param : parameters) this.symbolTable.register(param);

        try {

            getNextToken(Delimiter.LBRACE, "Expect '{' before constructor body");

            var bodyStatements = new ArrayList<StatementNode>();
            while (!checkCurrentTokenType(Delimiter.RBRACE) && isNotAtEnd()) bodyStatements.add(declarationParser.parseDeclaration());
            getNextToken(Delimiter.RBRACE, "Expect '}' after constructor body");

            var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
            return new ClassConstructorDeclaration(line, column, parameters, body, accessModifier);
        } finally {

            this.symbolTable = exitScope(constructorScope);
            this.declarationParser = new DeclarationParser(state, savedSymbolTable, this.typeRegistry);
        }
    }
}
