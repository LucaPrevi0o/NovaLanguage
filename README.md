# Programming Language Parser
A modular, self-contained tokenizer and parser for custom programming language syntax.

## Requirements
- **Maven** (see [pom.xml](https://github.com/LucaPrevi0o/CustomLanguage/blob/main/pom.xml))
- **Java** (version 21+)

## Overview
A modular Java-based code analyzer that produces the *AST* (Abstract Syntax Tree) and *Symbol Table* of the source code.
Full grammar explaination in class documentation (see [ClassParser.java](https://github.com/LucaPrevi0o/CustomLanguage/blob/main/src/parser/parser/ClassParser.java), [DeclarationParser.java](https://github.com/LucaPrevi0o/CustomLanguage/blob/main/src/parser/parser/DeclarationParser.java) and [ExpressionParser.java](https://github.com/LucaPrevi0o/CustomLanguage/blob/main/src/parser/parser/ExpressionParser.java) for details).

### Main features
- Lexical analysis
  - Source code tokenization with line escapes, space comprehension and comment avoidance
  - Token list available for parsing or direct analysis
- Source parsing
  - AST production with specific context-aware nodes
  - Support for class-level defined custom return types, with the help of a parsing-level [`TypeRegistry`](https://github.com/LucaPrevi0o/CustomLanguage/blob/main/src/token/TypeRegistry.java) definition
  - Support for multi-dimensional array type with optional static size definition
  - Support for class inheritance and generic type parameters
  - Implementation of symbol scoping for variables, classes and functions
    - Full-depth scope tree production from global scope
    - Semantic analysis for out-of-scope symbols
  - Recursive printer helper classes for the AST and the scoped symbol table

### Example source code
```
public class TestClass {

  private int field1;
  private float field2 = 3.14;
  private int[5][] arrayField;

  public void method() {

    // List of statements...
  }
}

public class NewClass [T] :: TestClass {

  private T genericTypeField;

  public int method(int param1, int param2) { return param1 + param2; }

  public NewClass(T[10] params) {

    for (int a = 0; a < 10; a++) {
      if (a == 5) { genericTypeField = params[i]; }
    }

    /* Other statements... */
  }
}
```
