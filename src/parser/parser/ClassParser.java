package src.parser.parser;

import src.parser.ast.SymbolTable;
import src.parser.ast.nodes.ExpressionNode;
import src.parser.ast.nodes.StatementNode;
import src.parser.ast.nodes.statement.BlockStatement;
import src.parser.ast.nodes.statement.ClassDeclarationStatement;
import src.parser.ast.nodes.statement.declaration.object.*;
import src.parser.parser.util.ParseException;
import src.parser.parser.util.ParserBase;
import src.token.ReturnType;
import src.token.TypeRegistry;
import src.token.family.AccessModifier;
import src.token.family.Delimiter;
import src.token.family.GenericParameterType;
import src.token.family.Keyword;
import src.token.family.Literal;
import src.token.family.Operator;

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
/// @see DeclarationParser
/// @see ExpressionParser
public class ClassParser extends ParserBase {
    
    private DeclarationParser declarationParser;

    /// Constructs a new ClassParser with the given DeclarationParser.
    /// @param declarationParser The DeclarationParser to use for parsing types and other declarations within the class.
    public ClassParser(DeclarationParser declarationParser) {
        
        super(declarationParser.getState(), declarationParser.getSymbolTable());
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
        var nameToken = consume(new Literal.IdentifierLiteral(), "Expect class name after 'class'");
        var className = getLiteralValue(nameToken);
        
        var superClasses = new ArrayList<ReturnType>();
        ReturnType genericClassParameter = null;
        
        if (match(Delimiter.LSQUARE)) {

            var genToken = consume(new Literal.IdentifierLiteral(), "Expect generic parameter name inside '[' ']'");
            var genericParameterName = getLiteralValue(genToken);
            var genericType = new GenericParameterType(genericParameterName);
            genericClassParameter = new ReturnType(genericType);
            consume(Delimiter.RSQUARE, "Expect ']' after generic parameter");
        } 
        
        if (match(Delimiter.DOUBLE_COLON)) {

            var superClassToken = consume(new Literal.IdentifierLiteral(), "Expect superclass name after '::'");
            var superClassName = getLiteralValue(superClassToken);
            superClasses.add(TypeRegistry.getReturnType(superClassName));
            if (superClasses.getFirst() == null) throw new ParseException("Superclass '" + superClassName + "' not found.", superClassToken);

            while (match(Delimiter.COMMA)) {

                superClassToken = consume(new Literal.IdentifierLiteral(), "Expect superclass name after '::'");
                superClassName = getLiteralValue(superClassToken);
                superClasses.add(TypeRegistry.getReturnType(superClassName));
                if (superClasses.getLast() == null) throw new ParseException("Superclass '" + superClassName + "' not found.", superClassToken);
            }
        } 

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
        
        TypeRegistry.registerClass(classDecl);
        this.symbolTable.register(classDecl);

        consume(Delimiter.LBRACE, "Expect '{' before class body");

        var classScope = enterScope();
        this.symbolTable = classScope;
        
        // Update declarationParser to use the new scope
        this.declarationParser = new DeclarationParser(state, this.symbolTable);

        var fields = new ArrayList<ClassFieldDeclaration>();
        var constructors = new ArrayList<ClassConstructorDeclaration>();
        var methods = new ArrayList<ClassMethodDeclaration>();
        var innerClasses = new ArrayList<ClassDeclarationStatement>();
        
        while (!check(Delimiter.RBRACE) && !isAtEnd()) {

            var memberAccessModifier = parseAccessModifier();
            if (match(Keyword.CLASS)) {

                if (memberAccessModifier == null) throw new ParseException("Inner class declaration requires an access modifier", previous());
                innerClasses.add(parseClassDeclaration(memberAccessModifier));
                continue;
            }
            
            if (check(new Literal.IdentifierLiteral()) && getLiteralValue(peek()).equals(className)) {

                advance();  // consume class name
                constructors.add(parseConstructor(classToken.getLine(), classToken.getColumn(), memberAccessModifier));
                continue;
            }
            
            if (!declarationParser.isValidType(peek()))
                throw new ParseException("Expect type, constructor, inner class, or closing '}' in body", peek());
            
            var type = declarationParser.parseType();
            var memberNameToken = consume(new Literal.IdentifierLiteral(), "Expect member name");
            var memberName = getLiteralValue(memberNameToken);
            
            if (check(Delimiter.LPAREN))
                methods.add(parseClassMethod(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
            else fields.add(parseClassField(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
        }

        consume(Delimiter.RBRACE, "Expect '}' after class body");

        this.symbolTable = exitScope(classScope);
        this.declarationParser = new DeclarationParser(state, this.symbolTable);

        return new ClassDeclarationStatement(
            classToken.getLine(),
            classToken.getColumn(),
            className,
            methods.toArray(new ClassMethodDeclaration[0]),
            fields.toArray(new ClassFieldDeclaration[0]),
            superClasses.toArray(new ReturnType[0]),
            genericClassParameter,
            innerClasses.toArray(new ClassDeclarationStatement[0]),
            classAccessModifier,
            constructors.toArray(new ClassConstructorDeclaration[0])
        );
    }

    /// Parses an access modifier if present. If no access modifier is found, returns null.
    ///
    /// Grammar rule:
    /// ```
    /// accessModifier → "public" | "private" | "protected"
    /// ```
    /// @return The AccessModifier if an access modifier keyword is present; otherwise, null.
    private AccessModifier parseAccessModifier() {

        if (match(AccessModifier.PUBLIC)) return AccessModifier.PUBLIC;
        if (match(AccessModifier.PRIVATE)) return AccessModifier.PRIVATE;
        if (match(AccessModifier.PROTECTED)) return AccessModifier.PROTECTED;
        return null;
    }

    /// Parses a class field declaration, which may optionally include an initializer.
    ///
    /// Grammar rule:
    /// ```
    /// classField → accessModifier? type IDENTIFIER ("=" expression)? ";"
    /// ```
    /// @param type The type of the field, which should have been parsed before calling this method.
    /// @param name The name of the field, which should have been parsed before calling this method.
    /// @param line The line number where the field is declared, used for error reporting and AST node construction.
    /// @param column The column number where the field is declared, used for error reporting and AST node construction.
    /// @param accessModifier The access modifier of the field (e.g., public, private), which should have been parsed before calling this method.
    /// @return A ClassFieldDeclaration representing the parsed field declaration.
    private ClassFieldDeclaration parseClassField(ReturnType type, String name, int line, int column, AccessModifier accessModifier) {

        ExpressionNode initializer = null;

        if (match(Operator.ASSIGN)) initializer = declarationParser.parseExpression();
        consume(Delimiter.SEMICOLON, "Expect ';' after field declaration");
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
    /// @param returnType The return type of the method, which should have been parsed before calling this method.
    /// @param name The name of the method, which should have been parsed before calling this method.
    /// @param line The line number where the method is declared, used for error reporting and AST node construction.
    /// @param column The column number where the method is declared, used for error reporting and AST node construction.
    /// @param accessModifier The access modifier of the method (e.g., public, private), which should have been parsed before calling this method.
    /// @return A ClassMethodDeclaration representing the parsed method declaration.
    private ClassMethodDeclaration parseClassMethod(ReturnType returnType, String name, int line, int column, AccessModifier accessModifier) {

        consume(Delimiter.LPAREN, "Expect '(' after method name");

        var parameters = declarationParser.parseParameters();

        consume(Delimiter.RPAREN, "Expect ')' after parameters");
        var decl = new ClassMethodDeclaration(line, column, returnType, name, parameters, null, accessModifier);
        this.symbolTable.register(decl); // Register method as symbol
        
        var methodScope = enterScope();
        this.symbolTable = methodScope;
        
        this.declarationParser = new DeclarationParser(state, this.symbolTable);
        for (var param : parameters) this.symbolTable.register(param); // Register parameters as symbols

        consume(Delimiter.LBRACE, "Expect '{' before method body");

        var bodyStatements = new ArrayList<StatementNode>();
        while (!check(Delimiter.RBRACE) && !isAtEnd()) bodyStatements.add(declarationParser.parseDeclaration());
        
        consume(Delimiter.RBRACE, "Expect '}' after method body");

        this.symbolTable = exitScope(methodScope);
        this.declarationParser = new DeclarationParser(state, this.symbolTable);

        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        return new ClassMethodDeclaration(line, column, returnType, name, parameters, body, accessModifier);
    }

    /// Parses a class constructor declaration, including its parameters and body.
    ///
    /// Grammar rule:
    /// ```
    /// constructor → accessModifier? CLASSNAME "(" parameters? ")" "{" declaration* "}"
    /// ```
    /// @param line The line number where the constructor is declared, used for error reporting and AST node construction.
    /// @param column The column number where the constructor is declared, used for error reporting and AST node construction.
    /// @param accessModifier The access modifier of the constructor (e.g., public, private), which should have been parsed before calling this method.
    /// @return A ClassConstructorDeclaration representing the parsed constructor declaration.
    private ClassConstructorDeclaration parseConstructor(int line, int column, AccessModifier accessModifier) {

        consume(Delimiter.LPAREN, "Expect '(' after constructor name");

        var parameters = declarationParser.parseParameters();
        consume(Delimiter.RPAREN, "Expect ')' after parameters");

        SymbolTable constructorScope = enterScope();
        this.symbolTable = constructorScope;
        
        this.declarationParser = new DeclarationParser(state, this.symbolTable);
        
        for (var param : parameters) this.symbolTable.register(param);

        consume(Delimiter.LBRACE, "Expect '{' before constructor body");

        var bodyStatements = new ArrayList<StatementNode>();
        while (!check(Delimiter.RBRACE) && !isAtEnd()) bodyStatements.add(declarationParser.parseDeclaration());
        
        consume(Delimiter.RBRACE, "Expect '}' after constructor body");

        this.symbolTable = exitScope(constructorScope);
        this.declarationParser = new DeclarationParser(state, this.symbolTable);

        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        return new ClassConstructorDeclaration(line, column, parameters, body, accessModifier);
    }
}
