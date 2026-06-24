package parser.grammar;

import error.diagnostic.ParseException;
import lexer.Token;
import lexer.token.family.*;
import lexer.token.family.literal.IdentifierLiteral;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.BlockStatement;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.*;
import parser.support.ParserBase;
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
/// classField     → accessModifier type IDENTIFIER ("=" expression)? ";"
/// classMethod    → accessModifier type IDENTIFIER "(" parameters? ")" "{" declaration* "}"
/// constructor    → accessModifier CLASSNAME "(" parameters? ")" "{" declaration* "}"
/// innerClass     → accessModifier classDecl
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
/// - **Access Control**: Public, private, and protected modifiers are required for all class members
///
/// @see DeclarationParser
/// @see ExpressionParser
public class ClassParser extends ParserBase {

    private final DeclarationParser declarationParser;

    /// Constructs a new ClassParser with the given DeclarationParser.
    /// @param declarationParser The DeclarationParser to use for parsing types and other declarations within the class.
    public ClassParser(DeclarationParser declarationParser) {

        super(declarationParser.getState(), declarationParser.getTypeRegistry());
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

        var classToken = previous(); // 'class' token was consumed by caller.
        var nameToken = consume(new IdentifierLiteral(), "Expect class name after 'class'");
        var className = getLiteralValue(nameToken);

        var superClasses = new ArrayList<ReturnType>();
        ReturnType genericClassParameter = null;

        // Parse optional generic parameter
        if (match(Delimiter.LSQUARE)) {

            // Only one generic parameter allowed for simplicity; can be extended to multiple if needed
            var genToken = consume(new IdentifierLiteral(), "Expect generic parameter name inside '[' ']'");
            var genericParameterName = getLiteralValue(genToken);

            // Expect closing square bracket after generic parameter
            consume(Delimiter.RSQUARE, "Expect ']' after generic parameter");

            genericClassParameter = new ReturnType(new GenericParameterType(genericParameterName));
            typeRegistry.registerType(genericClassParameter); // Register generic parameter as a type to allow it to be used in member declarations
        }

        if (match(Delimiter.DOUBLE_COLON)) {

            var superClassToken = consume(new IdentifierLiteral(), "Expect superclass name after '::'");
            var superClassName = getLiteralValue(superClassToken);
            superClasses.add(new ReturnType(new NonPrimitiveType(superClassName)));

            while (match(Delimiter.COMMA)) {

                superClassToken = consume(new IdentifierLiteral(), "Expect superclass name after ','");
                superClassName = getLiteralValue(superClassToken);
                superClasses.add(new ReturnType(new NonPrimitiveType(superClassName)));
            }
        }

        // Register the class type before parsing members to allow recursive type references.
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

        consume(Delimiter.LBRACE, "Expect '{' before class body");

        var fields = new ArrayList<ClassFieldDeclaration>();
        var constructors = new ArrayList<ClassConstructorDeclaration>();
        var methods = new ArrayList<ClassMethodDeclaration>();
        var innerClasses = new ArrayList<ClassDeclarationStatement>();

        while (isNotAtEnd() && !currentTokenIs(Delimiter.RBRACE)) try {

            parseClassMember(
                className,
                classToken.getLine(),
                classToken.getColumn(),
                fields,
                constructors,
                methods,
                innerClasses
            );
        } catch (ParseException e) {

            state.report(e);
            synchronizeClassMember();
        }

        consume(Delimiter.RBRACE, "Expect '}' after class body");

        // Populate the already-registered type placeholder with the fully parsed members.
        classDecl.setMethods(methods.toArray(new ClassMethodDeclaration[0]));
        classDecl.setFields(fields.toArray(new ClassFieldDeclaration[0]));
        classDecl.setConstructors(constructors.toArray(new ClassConstructorDeclaration[0]));
        classDecl.setInnerClasses(innerClasses.toArray(new ClassDeclarationStatement[0]));
        return classDecl;
    }

    private void parseClassMember(
        String className,
        int classLine,
        int classColumn,
        ArrayList<ClassFieldDeclaration> fields,
        ArrayList<ClassConstructorDeclaration> constructors,
        ArrayList<ClassMethodDeclaration> methods,
        ArrayList<ClassDeclarationStatement> innerClasses
    ) {

        var memberAccessModifier = parseAccessModifier();
        if (memberAccessModifier == null) throw parseError("Class member declaration requires an access modifier", peek());

        if (match(Keyword.CLASS)) {

            innerClasses.add(parseClassDeclaration(memberAccessModifier));
            return;
        }

        if (check(new IdentifierLiteral()) && className.equals(getLiteralValue(peek()))) {

            advance();  // consume constructor name
            constructors.add(parseConstructor(classLine, classColumn, memberAccessModifier));
            return;
        }

        if (!declarationParser.isValidType(peek()))
            throw parseError("Expect type, constructor, inner class, or closing '}' in class body", peek());

        var type = declarationParser.parseType();
        var memberNameToken = consume(new IdentifierLiteral(), "Expect member name");
        var memberName = getLiteralValue(memberNameToken);

        if (check(Delimiter.LPAREN)) methods.add(parseClassMethod(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
        else fields.add(parseClassField(type, memberName, memberNameToken.getLine(), memberNameToken.getColumn(), memberAccessModifier));
    }

    /// Synchronizes after a class-body member error without escaping the current class body.
    private void synchronizeClassMember() {

        if (!isNotAtEnd() || currentTokenIs(Delimiter.RBRACE)) return;
        if (currentTokenIs(Delimiter.SEMICOLON)) {

            advance();
            return;
        }
        if (isClassMemberRecoveryBoundary(peek())) return;

        var braceDepth = 0;
        while (isNotAtEnd()) {

            if (braceDepth == 0 && currentTokenIs(Delimiter.RBRACE)) return;
            if (braceDepth == 0 && isClassMemberRecoveryBoundary(peek())) return;

            var consumedType = advance().getType();
            if (consumedType == Delimiter.LBRACE) braceDepth++;
            else if (consumedType == Delimiter.RBRACE && braceDepth > 0) braceDepth--;
            else if (consumedType == Delimiter.SEMICOLON && braceDepth == 0) return;
        }
    }

    private boolean isClassMemberRecoveryBoundary(Token token) {

        if (token == null) return false;

        var type = token.getType();
        return type == AccessModifier.PUBLIC ||
               type == AccessModifier.PRIVATE ||
               type == AccessModifier.PROTECTED;
    }

    /// Parses a class field declaration, which may optionally include an initializer.
    ///
    /// Grammar rule:
    /// ```
    /// classField → accessModifier type IDENTIFIER ("=" expression)? ";"
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
        consume(Delimiter.SEMICOLON, "Expect ';' after field declaration");
        return new ClassFieldDeclaration(line, column, type, name, initializer, accessModifier);
    }

    /// Parses a class method declaration, including its parameters and body.
    ///
    /// Grammar rule:
    /// ```
    /// classMethod → accessModifier type IDENTIFIER "(" parameters? ")" "{" declaration* "}"
    /// ```
    /// @param returnType The return type of the method.
    /// @param name The name of the method.
    /// @param line The line number where the method declaration appears.
    /// @param column The column number where the method declaration starts.
    /// @param accessModifier The access modifier of the method (e.g., public, private).
    /// @return A ClassMethodDeclaration representing the parsed class method declaration.
    private ClassMethodDeclaration parseClassMethod(ReturnType returnType, String name, int line, int column, AccessModifier accessModifier) {

        consume(Delimiter.LPAREN, "Expect '(' after method name");

        var parameters = declarationParser.parseParameters();
        consume(Delimiter.RPAREN, "Expect ')' after parameters");

        consume(Delimiter.LBRACE, "Expect '{' before method body");

        var bodyStatements = declarationParser.parseBlockStatements();
        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        return new ClassMethodDeclaration(line, column, returnType, name, parameters, body, accessModifier);
    }

    /// Parses a class constructor declaration, including its parameters and body.
    ///
    /// Grammar rule:
    /// ```
    /// constructor → accessModifier CLASSNAME "(" parameters? ")" "{" declaration* "}"
    /// ```
    /// @param line The line number where the constructor declaration appears.
    /// @param column The column number where the constructor declaration starts.
    /// @param accessModifier The access modifier of the constructor (e.g., public, private).
    /// @return A ClassConstructorDeclaration representing the parsed class constructor declaration.
    private ClassConstructorDeclaration parseConstructor(int line, int column, AccessModifier accessModifier) {

        consume(Delimiter.LPAREN, "Expect '(' after constructor name");

        var parameters = declarationParser.parseParameters();
        consume(Delimiter.RPAREN, "Expect ')' after parameters");

        consume(Delimiter.LBRACE, "Expect '{' before constructor body");

        var bodyStatements = declarationParser.parseBlockStatements();
        var body = new BlockStatement(line, column, bodyStatements.toArray(new StatementNode[0]));
        return new ClassConstructorDeclaration(line, column, parameters, body, accessModifier);
    }
}
