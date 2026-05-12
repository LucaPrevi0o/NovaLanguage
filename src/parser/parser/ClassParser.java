package parser.parser;

import error.ErrorCollector;
import error.syntax.MissingTokenError;
import error.syntax.UnexpectedTokenError;
import lexer.token.family.*;
import lexer.token.family.literal.IdentifierLiteral;
import lexer.token.type.LiteralToken;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.BlockStatement;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.*;
import parser.parser.util.ParserBase;
import lexer.token.ReturnType;

import java.util.ArrayList;

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

        var classToken = previous();

        advance();
        var nameToken = peek();
        if (!match(new IdentifierLiteral())) {

            ErrorCollector.add(new UnexpectedTokenError(nameToken.getType(), nameToken.getLine(), nameToken.getColumn()));
            this.state.synchronize();
            return null;
        }
        var className = getLiteralValue((LiteralToken) nameToken);

        var superClasses = new ArrayList<ReturnType>();
        ReturnType genericClassParameter = null;

        advance();  // consume class name
        if (match(Delimiter.LSQUARE)) {

            advance();  // consume '['
            var genToken = peek();
            if (!match(new IdentifierLiteral())) {

                ErrorCollector.add(new UnexpectedTokenError(genToken.getType(), genToken.getLine(), genToken.getColumn()));
                this.state.synchronize();
                return null;
            }
            var genericParameterName = getLiteralValue((LiteralToken) genToken);

            advance();
            if (!match(Delimiter.RSQUARE)) {

                var badToken = peek();
                ErrorCollector.add(new UnexpectedTokenError(badToken.getType(), badToken.getLine(), badToken.getColumn()));
                this.state.synchronize();
                return null;
            }

            genericClassParameter = new ReturnType(new GenericParameterType(genericParameterName));
            typeRegistry.registerType(genericClassParameter); // Register generic parameter as a type to allow it to be used in member declarations
        }

        advance();
        if (match(Delimiter.DOUBLE_COLON)) do {

            advance();
            var superClassToken = peek();
            if (!match(new IdentifierLiteral())) {

                ErrorCollector.add(new UnexpectedTokenError(superClassToken.getType(), superClassToken.getLine(), superClassToken.getColumn()));
                this.state.synchronize();
                return null;
            }
            var superClassName = getLiteralValue((LiteralToken) superClassToken);
            superClasses.add(typeRegistry.getReturnType(superClassName));
        } while (match(Delimiter.COMMA));

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

        advance();
        if (!match(Delimiter.LBRACE)) {

            ErrorCollector.add(new UnexpectedTokenError(classToken.getType(), classToken.getLine(), classToken.getColumn()));
            this.state.synchronize();
            return null;
        }

        var classScope = enterScope(classDecl);
        var savedSymbolTable = this.symbolTable;

        this.symbolTable = classScope;
        this.declarationParser = new DeclarationParser(state, this.symbolTable, this.typeRegistry);

        var fields = new ArrayList<ClassFieldDeclaration>();
        var constructors = new ArrayList<ClassConstructorDeclaration>();
        var methods = new ArrayList<ClassMethodDeclaration>();
        var innerClasses = new ArrayList<ClassDeclarationStatement>();

        try {

            advance();
            while (!check(Delimiter.RBRACE) && isNotAtEnd()) {

                advance();
                var memberAccessModifier = parseAccessModifier();
                if (memberAccessModifier == null) {

                    ErrorCollector.add(new MissingTokenError());
                    this.state.synchronize();
                    return null;
                }

                advance();
                if (match(Keyword.CLASS)) {

                    advance();
                    innerClasses.add(parseClassDeclaration(memberAccessModifier));
                    continue;
                } else if (match(new IdentifierLiteral(className))) {

                    advance();  // consume class name
                    constructors.add(parseConstructor(classToken.getLine(), classToken.getColumn(), memberAccessModifier));
                    continue;
                } else if (!declarationParser.isValidType(peek())) {

                    var badToken = peek();
                    ErrorCollector.add(new UnexpectedTokenError(badToken.getType(), badToken.getLine(), badToken.getColumn()));
                    this.state.synchronize();  // Attempt to recover by synchronizing to the next statement/declaration
                    continue; // Skip the invalid member and continue parsing the rest of the class
                }

                advance();
                var type = declarationParser.parseType();
                var memberNameToken = peek();
                if (!match(new IdentifierLiteral())) {

                    ErrorCollector.add(new UnexpectedTokenError(memberNameToken.getType(), memberNameToken.getLine(), memberNameToken.getColumn()));
                    this.state.synchronize();
                    continue; // Skip the invalid member and continue parsing the rest of the class
                }

                var memberName = getLiteralValue((LiteralToken) memberNameToken);
                if (check(Delimiter.LPAREN)) methods.add(parseClassMethod(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
                else fields.add(parseClassField(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
            }
        } finally {

            advance();
            if (!match(Delimiter.RBRACE)) {

                var badToken = peek();
                ErrorCollector.add(new UnexpectedTokenError(badToken.getType(), badToken.getLine(), badToken.getColumn()));
                this.state.synchronize();
            }
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

        if (match(Operator.ASSIGN)) initializer = declarationParser.parseExpression();
        consume(Delimiter.SEMICOLON);
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

        advance();
        if (!match(Delimiter.LPAREN)) {

            var badToken = peek();
            ErrorCollector.add(new UnexpectedTokenError(badToken.getType(), badToken.getLine(), badToken.getColumn()));
            this.state.synchronize();
            return null;
        }

        var parameters = declarationParser.parseParameters();
        advance();
        if (!match(Delimiter.RPAREN)) {

            var badToken = peek();
            ErrorCollector.add(new UnexpectedTokenError(badToken.getType(), badToken.getLine(), badToken.getColumn()));
            this.state.synchronize();
            return null;
        }

        // Pre-register method in class scope so recursive calls resolve
        var placeholder = new ClassMethodDeclaration(line, column, returnType, name, parameters, null, accessModifier);
        this.symbolTable.register(placeholder);

        var methodScope = enterScope(placeholder);
        var savedSymbolTable = this.symbolTable;

        this.symbolTable = methodScope;
        this.declarationParser = new DeclarationParser(state, this.symbolTable, this.typeRegistry);
        for (var param : parameters) this.symbolTable.register(param);

        try {

            var body = parseClassMemberBody(line, column);
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

        advance();
        if (!match(Delimiter.LPAREN)) {

            var badToken = peek();
            ErrorCollector.add(new UnexpectedTokenError(badToken.getType(), badToken.getLine(), badToken.getColumn()));
            this.state.synchronize();
            return null;
        }

        var parameters = declarationParser.parseParameters();
        advance();
        if (!match(Delimiter.RPAREN)) {

            var badToken = peek();
            ErrorCollector.add(new UnexpectedTokenError(badToken.getType(), badToken.getLine(), badToken.getColumn()));
            this.state.synchronize();
            return null;
        }

        var constructorScope = enterScope();
        var savedSymbolTable = this.symbolTable;

        this.symbolTable = constructorScope;
        this.declarationParser = new DeclarationParser(state, this.symbolTable, this.typeRegistry);
        for (var param : parameters) this.symbolTable.register(param);

        try {

            var body = parseClassMemberBody(line, column);
            return new ClassConstructorDeclaration(line, column, parameters, body, accessModifier);
        } finally {

            this.symbolTable = exitScope(constructorScope);
            this.declarationParser = new DeclarationParser(state, savedSymbolTable, this.typeRegistry);
        }
    }

    /// Parses the body of a class member (method or constructor), which consists of a block of statements.
    /// @param line The line number where the member body starts.
    /// @param column The column number where the member body starts.
    /// @return A BlockStatement representing the parsed body of the class member.
    private BlockStatement parseClassMemberBody(int line, int column) {

        advance();
        if (!match(Delimiter.LBRACE)) {

            var badToken = peek();
            ErrorCollector.add(new UnexpectedTokenError(badToken.getType(), badToken.getLine(), badToken.getColumn()));
            this.state.synchronize();
            return null;
        }
        var bodyStatements = new ArrayList<StatementNode>();
        while (!check(Delimiter.RBRACE) && isNotAtEnd()) bodyStatements.add(declarationParser.parseDeclaration());

        advance();
        if (!match(Delimiter.RBRACE)) {

            var badToken = peek();
            ErrorCollector.add(new UnexpectedTokenError(badToken.getType(), badToken.getLine(), badToken.getColumn()));
            this.state.synchronize();
            return null;
        }

        return new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
    }
}
