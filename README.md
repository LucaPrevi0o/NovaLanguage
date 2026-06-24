# Nova — Compiler Front-End
A modular, self-contained tokenizer and parser for the **Nova** programming language.

## What is Nova?
Nova is a compiled, object-oriented programming language designed around a single ambition: take everything the modern language ecosystem does well, and commit to it fully — rather than bolting it on as an afterthought.

It draws from the performance and directness of C and C++, the natural OOP model of Java, the modularity of Python, and the package ecosystem philosophy of Ruby — while addressing what each of those leaves incomplete.
The standard library is designed to be truly exhaustive: I/O, math, string utilities, event-driven programming, GPU-dispatched graphics, audio interfaces, and more are all first-class citizens, injectable on demand, rather than delegated to fragile third-party dependencies.

The syntax is intentionally familiar — close to C and Java — so that the language feels natural from day one, without the cognitive overhead of learning an entirely new grammar.
(Yes - Python and Ruby fans, I'm watching you, you'll learn this the hard way... I'm sorry, but to me this is just cleaner)

### Classes vs Types
Nova introduces a deliberate distinction between two fundamental concepts that most languages conflate:

- **Classes** — Java-style objects with identity, behavior, and lifecycle, inheriting from a common root that provides sensible defaults.
- **Types** — algebraic entities defined entirely by their mathematical properties, supporting operator overloading to construct structures like tensors, matrices, and other composable forms directly in the language.

A class is not a type, and a type is not a class: this separation is a first-class design decision, not a convention.

Developers can define their own types with custom operators, and the compiler will optimize them as aggressively as built-in primitives — without the overhead of object identity or method dispatch.
However, types **need** to overload basic operators and provide explicit castings.

On the other hand, classes are designed for modeling real-world entities with mutable state and complex behavior.
Inheritance and polymorphism are supported, as well as generic types and class parameters, but operator overloading is not.
- By design, every single member (fields and methods) **must** provide its own access modifier (`public`, `private`, `protected`).
- Public members are always accessible; private and protected are strictly enforced by the compiler, with no reflection or runtime access allowed.
  - Any public field of the class can be read and written directly.
  - Private fields can only be accessed within the class body, or using specific (public) getter/setter methods defined by the developer.
- Overloading methods can be done, by simply modifying the parameter list (number, types, order).

### Hard-static typing
Nova is statically typed - more specifically, **hard-static**.
This means that type information is always available at compile time, and there is no concept of "dynamic" or "runtime" types.
To enforce this, a strict set of rules govern the type system:
- Reflection is ***not*** a core language feature, and type information is **always** available at compile time: this allows for aggressive compile-time optimizations and zero-cost abstractions, while still supporting a rich type system with generics, inheritance, and user-defined types.
  - As types are always known at compile-time, every construct that is based on **type checking** (e.g. method overload resolution, operator dispatch for user-defined types, type inference) is resolved during compilation, with no runtime overhead.
  - On the other hand, features that would require **non-statically-typed declarations** (e.g. reflection, dynamic method invocation) are intentionally excluded from the language design.
- Type inheritance is supported, but only for classes — not for types.
  - When a class extends another, it inherits both its method signatures and its implementation (fields and method bodies).
  - If a class extends multiple classes, it inherits the method signatures from all of them.
    - For conflicting methods, an explicit override is required to resolve the ambiguity — the compiler will reject any class that inherits two methods with the same signature, without providing its own implementation.
    - Conflicting fields — i.e. two parent classes declaring a field with the same name — are a **compile-time error** with no resolution path.
      - This is a deliberate constraint: field identity is considered part of a class's private contract, and silently merging or shadowing inherited fields would introduce subtle aliasing bugs.
      - If two parent classes share a field name, the inheritance hierarchy must be redesigned.
      - Possible future developments ***could*** include different field resolution strategies (e.g. explicit field renaming in the subclass, or qualified access to parent fields).
  - Abstract methods can be declared: a class with one or more abstract methods is considered abstract, and cannot be instantiated directly.
  - A class extending an abstract class must provide concrete implementations for all inherited abstract methods; if not, it is also considered abstract and cannot be instantiated.
  - Final classes (not extendable) and final methods (not overrideable) are also supported, to allow developers to explicitly control the inheritance hierarchy and prevent unintended overrides.
  - The static root of type inheritance allows for bounded polymorphism:
    - Parameters can be declared with an upper bound (e.g. `T : Comparable`), which allows any type that extends `Comparable` to be used as an argument.
    - Any type that matches the expected type (e.g. `List[int]` when `List[T : Comparable]` is expected) can be used as an argument.
    - Only the signature provided by the upper bound is actually accessible within the function body - no other field and/or method of the concrete type is available.
- Generic classes are supported, but objects of a generic class have a type that is fully determined by their type parameters at compile time.
  - When a generic class is declared, the compiler generates a distinct class for each unique combination of type parameters used in the codebase.
    - This means that `List[int]` and `List[string]` are compiled into separate classes, representing **different types** with (almost) no shared runtime representation.
    - Instances of classes where the generic type parameter can be cast to one another (e.g. `List[int]` and `List[float]`) are still treated as different types.
  - Subclasses allow to share code between different generic instantiations, to avoid code duplication when the logic is the same regardless of the type parameters:
    - Class details about how a generic `List[T]` works (e.g. internal array management, resizing logic) is declared once in the `List[T]` class, and inherited by all specific instantiations.
    - Every time a concrete instantiation of a generic class is used in the codebase, the compiler generates a new class that extends the generic superclass, with all type parameters replaced by their concrete types.
    - In substance, generic classes *are* abstract classes — the compiler just generates concrete subclasses for each instantiation.
  - Type parameters can extend other types themselves:
    - For example, `class List[T : Comparable]` declares a generic class `List` with a type parameter `T` that must extend the `Comparable` type.
    - This allows the compiler to enforce constraints on the type parameters and enable operator overloading for user-defined types that implement the required interfaces.
    - Recursively, the extended type can also be generic, and/or have its own constraints (e.g. `class List[T : Comparable[T]]`).
    - If the type parameter allows bounded polymorphism for different instances of the same generic class (e.g. `List[int]` and `List[float]`), the two types can interact with each other:
      - Any type parameter that matches the expected type (e.g. `List[int]` when `List[T : Comparable]` is expected) can be used as an argument.
      - Only the signature provided by the upper bound is actually accessible within the function body.
      - Objects of different types can interact as they were of the same type (the upper bound itself).
        - If the type is non-abstract, any operation defined for the parameter type (e.g. `Comparable`) is available for all instances of the generic class.
        - Even if the class is abstract, non-abstract operations already defined in the parameter type are still usable.
  - As type erasure is not a thing, constructs like *classes with a variable number of type parameters* (e.g. `Tuple[{A}]`) are possible in principle:
    - The compiler generates a distinct class for each unique combination of type parameters that is used in the codebase, regardless of the number of type parameters.
    - Objects of these classes have a fully concrete type, that is determined by their type parameters.
    - The number of type parameters (and the specific ones) is part of the type identity, so `Tuple[{int, string}]` and `Tuple[{int, string, float}]` are different types.
- Class parameters can be declared, similar to C++ template non-type parameters, but with the same compile-time semantics as type parameters:
  - On top of a generic type, a class can also declare parameters that are not types, but values (e.g. `class Tensor[T, (N : int)]`).
  - These parameters are also part of the type identity, so `Tensor[int, N = 3]` and `Tensor[int, N = 2]` are different types.
  - The class parameter must explicitly declare its type (e.g. `N : int`) — note: a **type**, not a ***class***.
  - Class parameters must be propagated correctly through inheritance chains: if `class SquareMatrix[T, (N : int)] :: Tensor[T, N, N]`, the compiler must substitute parameter values when generating the concrete superclass, which adds non-trivial complexity to the monomorphizer.

This design allows for a powerful and flexible type system, without sacrificing performance or safety.
Common patterns like collections, data structures, and algorithms can be implemented as generic classes, while still benefiting from the full power of static typing and compile-time optimizations.

> [!WARNING]
> The avoidance of type erasure and reflection rejects constructs like runtime type inspection or dynamic method invocation.
> This has a direct consequence for tooling that other languages implement via reflection — most notably, **test frameworks** that discover test methods at runtime.
>
> In Nova, such tooling must be implemented through **compile-time code generation** instead: the compiler scans for methods marked as tests (via a dedicated language construct, to be designed) and generates a static test registry that holds direct references to all test methods, allowing them to be executed without any runtime discovery mechanism.
>
> More broadly, any pattern that would typically require runtime type information must be redesigned as a compile-time construct.
> This is not a limitation to be worked around — it is a deliberate constraint that keeps the runtime lean and the compiler's guarantees meaningful.
> The design of these compile-time metaprogramming facilities is a significant open item for Nova's future development.

> [!WARNING]
> Variadic type parameters — constructs like `Tuple[{A}]` with an unbounded number of type parameters — are the single hardest implementation challenge in Nova's type system.
>
> The difficulty is not at runtime (monomorphization handles that cleanly), but in the **compiler's internal representation and type checker**: the monomorphizer must handle unbounded arity uniformly, and the type checker must reason about constraints across a variable-length parameter list.
> Critically, `Function[{A}, R]` — the base class for all lambdas — itself depends on variadic type parameters, which means **lambda expressions cannot be fully implemented until variadic generics are resolved**.
> These two features must be designed and implemented together.
>
> Languages that support this (C++ variadic templates, recently-stabilized Rust const generics) treat it as a significant language feature in its own right.
> Nova should do the same.

### Lambda expressions and functional programming
Nova supports lambda expressions and functional programming constructs, but they are designed to be fully statically typed and optimized at compile time.
- An abstract generic class `Function[{A}, R]` is provided as the base for all function types, where `{A}` is a list of type parameters representing the argument types, and `R` is the return type.
- Lambda expressions are syntactic sugar for anonymous subclasses of `Function`:
  - A lambda expression like `(x: int, y: int) -> int { return x + y; }` is parsed into an anonymous class that extends `Function[{int, int}, int]` (with `{A} = int, int` and `R = int`), and implements the `call` method with the provided body.
  - The compiler generates a concrete class for each unique lambda expression, with the appropriate type parameters based on the lambda's argument and return types.
  - This allows lambda expressions to be fully statically typed, while still supporting the full expressiveness of functional programming constructs.
- Since lambdas are subclasses of `Function`, calls to them go through virtual dispatch by default. For short, frequently-called lambdas in tight loops (e.g. `list.map(x -> x * 2)`), this would introduce measurable overhead. The compiler must devirtualize lambda calls whenever the concrete lambda type is statically known at the call site — which it almost always is, given Nova's hard-static type system. This is a well-understood optimization in AOT compilers and should be treated as a required feature, not a stretch goal.

### Package management
Nova source files use the **`.nv`** extension.
The package manager is **Orbit**, and the community package registry is **the Nebula**.

> [!NOTE]
> This repository contains the **compiler front-end**: the lexer and recursive-descent parser that produce the AST and Symbol Table from Nova source code.
> The backend (IR generation, optimization, and native code emission) is a future milestone described in the [Further development](#further-development) section.

## Requirements
- **Maven Wrapper** (included; use `./mvnw` from the repository root)
- **Java** (version 24+ - suggested: [**OpenJDK release 26**](https://jdk.java.net/26/) for latest features and improvements)

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
  - Support for class-level defined custom return types, with the help of a parsing-level [`TypeRegistry`](src/lexer/token/TypeRegistry.java) definition
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

## Architecture

The project is organized into four main layers: the **Lexer**, the **Parser** (composed of specialized sub-parsers), the **AST node hierarchy**, and the **Printers**.
Each layer has a well-defined responsibility and communicates with the next through clean interfaces.

### Lexer

The [`Lexer`](src/lexer/Lexer.java) performs lexical analysis, converting raw source text into a flat list of [`Token`](src/lexer/Token.java) objects.
It handles:

- **Whitespace and comment skipping** — both single-line (`//`) and multi-line (`/* */`) comments are discarded transparently.
- **Literal recognition** — numbers (with optional type suffixes `l/L`, `f/F`, `d/D`, `b/B`), strings, character literals, and booleans (`true`/`false`) are each tokenized into a dedicated [`LiteralToken`](src/lexer/token/type/LiteralToken.java) subtype.
- **Keyword and type disambiguation** — identifiers are checked against a static map of known keywords ([`Keyword`](src/lexer/token/family/Keyword.java), [`AccessModifier`](src/lexer/token/family/AccessModifier.java)) and primitive types ([`PrimitiveType`](src/lexer/token/family/PrimitiveType.java)), producing [`KeywordToken`](src/lexer/token/type/KeywordToken.java) or [`TypeToken`](src/lexer/token/type/TypeToken.java) respectively; unrecognized sequences become [`LiteralToken`](src/lexer/token/type/LiteralToken.java) identifiers.
- **Operator and delimiter tokenization** — both one- and two-character forms (e.g. `=` vs `==`, `:` vs `::`) are resolved greedily.

Every token carries its **source position** (line and column), which propagates through the parser into error messages and AST nodes.

### Token families

All token types implement the [`TokenClass`](src/lexer/token/TokenClass.java) marker interface, organized into enums and classes under `token/family/`:

| Type                                                           | Examples                                       |
|----------------------------------------------------------------|------------------------------------------------|
| [`PrimitiveType`](src/lexer/token/family/PrimitiveType.java)   | `int`, `float`, `string`, `void`, …            |
| [`Keyword`](src/lexer/token/family/Keyword.java)               | `if`, `while`, `for`, `class`, `return`, …     |
| [`AccessModifier`](src/lexer/token/family/AccessModifier.java) | `public`, `private`, `protected`               |
| [`Operator`](src/lexer/token/family/Operator.java)             | `+`, `==`, `&&`, `++`, `+=`, …                 |
| [`Delimiter`](src/lexer/token/family/Delimiter.java)           | `(`, `{`, `[`, `;`, `::`, …                    |
| [`Literal`](src/lexer/token/family/Literal.java)               | identifiers, numbers, strings, booleans, chars |
| [`Special`](src/lexer/token/family/Special.java)               | `EOF`, `UNKNOWN`                               |

Non-primitive class names are represented at parse time by [`NonPrimitiveType`](src/lexer/token/family/NonPrimitiveType.java), and generic parameters by [`GenericParameterType`](src/lexer/token/family/GenericParameterType.java).

### Parser

The [`Parser`](src/parser/Parser.java) is the entry point for syntactic analysis.
It owns a per-session [`TypeRegistry`](src/lexer/token/TypeRegistry.java) (pre-populated with all primitive types and extended as classes are declared), a global [`SymbolTable`](src/parser/ast/SymbolTable.java), and a small set of built-in standard-library stubs (`print`, `println`, `parseInt`, etc.).
Parsing is delegated to three specialized sub-parsers:

#### DeclarationParser
[`DeclarationParser`](src/parser/parser/DeclarationParser.java) handles the top-level grammar: access-modified class declarations, function declarations (with pre-registration for recursion support), variable declarations (including comma-separated multi-variable forms), and all statement types — `if`/`else`, `while`, `for`, for-each, `switch`/`case`, `return`, `break`, `continue`, and block statements.
It also owns type parsing (`primitiveType | className arrayDimensions?`) and parameter list parsing, both used by the other sub-parsers.

#### ClassParser
[`ClassParser`](src/parser/parser/ClassParser.java) handles class body parsing.
After consuming the class header (name, optional generic parameter `[T]`, optional superclass list `:: A, B`), it registers the class early in both the `TypeRegistry` and the enclosing `SymbolTable` so that forward references within the same file resolve correctly.
It then iterates over class members, dispatching to dedicated helpers for **fields** (with optional initializers), **methods** (pre-registered before their body for recursive calls), **constructors**, and **inner classes**.

#### ExpressionParser
[`ExpressionParser`](src/parser/parser/ExpressionParser.java) implements a standard recursive-descent expression grammar with explicit precedence levels:

```
assignment → ternary → logicOr → logicAnd → equality → comparison → term → factor → unary → call → primary
```

Compound assignment operators (`+=`, `-=`, etc.) are desugared into their binary equivalents.
Postfix `++`/`--` are distinguished from prefix forms by a dedicated [`PostfixUnaryExpression`](src/parser/ast/nodes/expression/PostfixUnaryExpression.java) node.
The `primary` rule handles object creation (`new ClassName(...)`), parenthesized expressions, and all literal forms.

#### Error recovery
When a [`ParseException`](src/parser/parser/util/ParseException.java) is thrown at the top-level declaration boundary, the [`Parser`](src/parser/Parser.java) records the error and calls `synchronize()` — which skips tokens until a safe resumption point (a semicolon or a statement-opening keyword) — then continues parsing.
At the end of the run, all collected errors are bundled into a [`ParseErrorsException`](src/parser/parser/util/ParseErrorsException.java) and thrown together, giving the caller a complete picture of every problem in a single pass.

### Symbol Table

The [`SymbolTable`](src/parser/ast/SymbolTable.java) is a tree of scopes.
Each scope holds a flat list of [`Symbol`](src/parser/ast/nodes/Symbol.java) entries (variables, functions, classes, parameters) and a reference to its parent.
Scopes are created on entry to every block, function, method, constructor, loop, and class body, and restored on exit — always via `try/finally` blocks to guarantee correct scope restoration even when parsing fails mid-construct.
Symbol lookup walks up the parent chain, enabling full lexical scoping; re-declaration within the same scope is a hard parse error.

### Type Registry

The [`TypeRegistry`](src/lexer/token/TypeRegistry.java) is a per-session catalogue of all known types.
It is pre-populated with the primitive types at construction and extended with each new [`ClassDeclarationStatement`](src/parser/ast/nodes/statement/ClassDeclarationStatement.java) as it is parsed.
The parser consults the registry to distinguish class names from plain identifiers during type parsing and `new` expressions, and to resolve superclass names in inheritance declarations.

### AST node hierarchy

All AST nodes extend [`AstNode`](src/parser/ast/AstNode.java) (carrying source position) and implement [`Printable`](src/parser/ast/Printable.java) (exposing a label and a list of [`PrintEntry`](src/parser/ast/Printable.java) values for tree rendering).
The hierarchy is split into two branches:

- **[`StatementNode`](src/parser/ast/nodes/StatementNode.java)** — declarations (`VariableDeclarationStatement`, `FunctionDeclarationStatement`, `ClassDeclarationStatement`, and their class-member variants), control-flow statements (`IfStatement`, `WhileStatement`, `ForStatement`, `ForEachStatement`, `SwitchStatement`), and structural nodes (`BlockStatement`, `ReturnStatement`, `BreakStatement`, `ContinueStatement`).
- **[`ExpressionNode`](src/parser/ast/nodes/ExpressionNode.java)** — literals (`NumberLiteralExpression` and its typed subclasses, `StringLiteralExpression`, `BoolLiteralExpression`, `CharLiteralExpression`, `NullLiteralExpression`, `IdentifierLiteralExpression`), operations (`BinaryExpression`, `UnaryExpression`, `PostfixUnaryExpression`, `TernaryExpression`, `AssignmentExpression`), and access forms (`CallExpression`, `ArrayAccessExpression`, `MemberAccessExpression`, `ObjectCreationExpression`).

### Printers

Two utility classes render the internal structures to standard output for inspection and debugging:

- **[`AstPrinter`](src/printer/AstPrinter.java)** — recursively walks the AST using each node's `Printable` interface, rendering the tree with `├─` / `└─` / `│` branch characters and managing indentation centrally. Array type annotations (e.g. `int[3][]`) are formatted by the shared `buildTypeStringWithSizes` helper.
- **[`SymbolTablePrinter`](src/printer/SymbolTablePrinter.java)** — walks the `SymbolTable` scope tree, printing each symbol with its kind and declared type, and attaching child scopes (function bodies, class bodies, anonymous blocks) as indented subtrees beneath their owning symbol.

## Further development

- **Error handling**: The parser already implements top-level error recovery with synchronization and multi-error collection across declaration boundaries. Possible extensions include:
  - Finer-grained recovery inside statement and expression parsing, not just at the top-level declaration boundary, to surface more errors per run.
  - Richer semantic checks beyond out-of-scope symbol detection — for example, type mismatch, return-type consistency, duplicate class/method overloads, and unreachable code.
  - Warning detections for non-critical issues (e.g. unused variables, unreachable code after return, deprecated constructs) that do not prevent successful parsing but should be flagged to the developer.
  - Code suggestions in error messages, such as "Did you mean `foo`?" when an unknown identifier is encountered, or "Expected `;` here" when a statement is missing its terminator.
  - Detailed error messages with column-accurate caret indicators to pinpoint issues visually in the source text.
  - Expandable error types with specific subclasses for different categories of errors (e.g. `SyntaxError`, `SemanticError`, `TypeError`) to allow more precise handling and reporting.

- **Multi-file parsing**: The current pipeline handles a single source file. Supporting real projects requires a multi-file orchestration layer:
  - A project-level entry point that discovers, orders, and feeds multiple source files to the lexer and parser in dependency order.
  - A shared, cross-file `TypeRegistry` and global `SymbolTable` that accumulate declarations across files, so that a class defined in one file can be referenced in another.
  - Forward-declaration or two-pass strategies to handle mutual references between files without requiring a strict declaration order.

- **Standard library**: At the moment a handful of stub functions (`print`, `println`, `parseInt`, etc.) are hard-coded directly in the `Parser` constructor. A proper standard library should be:
  - Authored as ordinary source files in Nova's own syntax, living in a designated `stdlib/` directory within the project.
  - Preloaded automatically by the multi-file orchestrator before user code is parsed, so that standard types and functions are available globally without any import.
  - Extensible: new built-in modules (collections, I/O, math, string utilities, …) can be added as source files rather than requiring changes to the parser itself.

- **Compile-time metaprogramming**: As a consequence of Nova's hard-static design and the deliberate exclusion of reflection, any language construct that requires runtime type inspection must be replaced by a compile-time equivalent.
  - This includes specific tooling, such as:
    - Test frameworks;
    - Serialization;
    - Dependency injection.
  - A dedicated metaprogramming facility — allowing the compiler to generate code from structured compile-time directives — is a significant open design item and a prerequisite for Nova's standard tooling ecosystem to be complete.

- **Variadic generics and lambda implementation**: Variadic type parameters (`Tuple[{A}]`, `Function[{A}, R]`) and the full lambda expression system are co-dependent features that must be designed and implemented together.
  - Neither can be considered complete without the other.
  - This represents the single largest open item in Nova's type system, and should be treated as a dedicated implementation milestone rather than an incremental addition.

- **Code generation**: No backend exists yet. The roadmap for adding one would roughly follow these stages:
  - Transform the AST into an intermediate representation (IR) — a lower-level, platform-independent form that is easier to analyze and optimize than the source-level AST.
  - Apply basic optimizations on the IR (constant folding, dead-code elimination, inlining, and lambda devirtualization where the concrete lambda type is statically known at the call site).
  - Emit a target artifact: either JVM bytecode (leveraging existing tooling and the Java runtime), a custom bytecode for a purpose-built virtual machine, or — as a longer-term goal — native machine code to produce self-contained binary executables without any intermediate runtime.
  - Add runtime support for standard-library operations that cannot be expressed purely in the source language (e.g. I/O, memory management).

## Contributing
Contributions are welcome! Please fork the repository and submit a pull request with your changes.
For major changes, please open an issue first to discuss what you would like to change.

## License
This project is licensed under the **Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License** (CC BY-NC-SA 4.0).
See the [LICENSE](LICENSE) file for details.
