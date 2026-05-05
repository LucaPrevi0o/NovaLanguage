# Programming Language Parser
A modular, self-contained tokenizer and parser for custom programming language syntax.

## Requirements
- **Maven** (see [pom.xml](pom.xml))
- **Java** (version 21+ - suggested: [**OpenJDK release 26**](https://jdk.java.net/26/) for latest features and improvements)

> [!NOTE]
> The use of newer Java versions is recommended to take advantage of the latest language features and performance improvements.
> Switching to a newer version may require updating the `source` and `target` properties in the `pom.xml` file to match the Java version you are using.
> 
> Refer to the [Maven Compiler Plugin documentation](https://maven.apache.org/plugins/maven-compiler-plugin/) for details on how to configure the Java version in your project.
> Use `java -version` in your terminal to check your current Java version and ensure compatibility with the project requirements.

## Overview
A modular Java-based code analyzer that produces the *AST* (Abstract Syntax Tree) and *Symbol Table* of the source code.
Full grammar explanation in class documentation (see [ClassParser.java](src/parser/parser/ClassParser.java), [DeclarationParser.java](src/parser/parser/DeclarationParser.java) and [ExpressionParser.java](src/parser/parser/ExpressionParser.java) for details).

### Main features
- Lexical analysis
  - Source code tokenization with line escapes, space comprehension and comment avoidance
  - Token list available for parsing or direct analysis
- Source parsing
  - AST production with specific context-aware nodes
  - Support for class-level defined custom return types, with the help of a parsing-level [`TypeRegistry`](src/token/TypeRegistry.java) definition
  - Support for multidimensional array type with optional static size definition
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

## Further development
- **Error handling**: Implementing comprehensive error reporting for syntax and semantic errors during parsing.
  - Detailed error messages with line and column information to help identify issues in the source code.
  - Recovery strategies to allow the parser to continue processing after encountering errors, providing more feedback in a single run.
- **Code generation**: Adding functionality to generate intermediate code or bytecode from the AST for execution or further compilation.
  - Transforming the AST into a target representation, such as Java bytecode or an intermediate language for a virtual machine.
  - Final goal: *direct binary executable generation* from the source code, bypassing the need for an intermediate representation and improving performance.
  - Support for standard library functions and built-in types to facilitate code generation and execution.

## Contributing
Contributions are welcome! Please fork the repository and submit a pull request with your changes.
For major changes, please open an issue first to discuss what you would like to change.

## License
This project is licensed under the **Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License** (CC BY-NC-SA 4.0).
See the [LICENSE](LICENSE) file for details.