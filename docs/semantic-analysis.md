# Semantic analysis

Semantic analysis validates the meaning of syntactically valid Nova programs.

For the general compiler pipeline, see [`architecture.md`](architecture.md). For parser details, see [`parser.md`](parser.md).

## Purpose

The parser answers:

> Is this valid syntax?

Semantic analysis answers:

> Does this program make sense according to Nova's language rules?

This separation keeps the parser simpler and makes semantic behavior easier to test.

## Current semantic pipeline

The current semantic side is being split into explicit passes and supporting models:

```text
AST
  ↓
Semantic declaration collection
  ↓
Semantic scope construction
  ↓
Name resolution
  ↓
Duplicate declaration validation
  ↓
Type-related checks
  ↓
Context checks
  ↓
Diagnostics
```

The exact pass structure may continue to evolve as the compiler front end stabilizes.

## Semantic declarations

Semantic declarations represent named program entities independently from parser helper state.

Examples of declaration kinds include:

- class;
- field;
- method;
- constructor;
- function;
- parameter;
- local variable;
- for-each variable.

Declarations should preserve enough information to support later passes:

- name;
- declaration kind;
- declared type, where applicable;
- source AST node;
- owning scope or owning AST node.

## Semantic scopes

Semantic scopes represent lexical visibility.

Current scope categories include, or are moving toward:

- global scope;
- class scope;
- function scope;
- constructor scope;
- block scope;
- loop scope;
- switch scope;
- branch scope.

The semantic scope tree is the source of truth for name visibility. Parser-owned symbol-table scope construction has been removed, so new visibility rules should be implemented here rather than in grammar parsers.

## Name resolution

Name resolution checks whether identifiers and referenced classes/types can be found from the current semantic scope.

Examples:

```nova
x + 1;
```

If `x` has not been declared in a visible scope, semantic analysis should produce an undefined-name diagnostic.

```nova
new MissingClass();
```

If `MissingClass` is not declared or otherwise known to the compilation context, semantic analysis should produce an undefined-class diagnostic.

## Duplicate declaration validation

Duplicate validation should report repeated declarations in the same semantic scope.

Examples:

```nova
int x;
int x;
```

```nova
class A {}
class A {}
```

The parser should not crash or reject these while parsing. It should build the AST and allow semantic analysis to report the duplicate declaration.

Functions, methods, and constructors are overloadable by parameter signature. Declarations with the same name but different parameter type lists are allowed; declarations with the same name and the same parameter type list are duplicate declarations. Return type alone does not create a distinct overload.

## Type checking

Type checking is currently partial and should be expanded carefully.

Currently implemented checks include:

- initializer compatibility;
- assignment compatibility;
- function-call argument count and argument type checks;
- array access checks;
- direct class field access;
- direct class method calls;
- class subtype assignment and argument compatibility through superclass chains;
- inherited class field and method lookup;
- basic function and method overload selection by argument count and semantic argument types;
- no-match and ambiguous-overload diagnostics;
- return value checks.

Future checks should include:

- inheritance conflict checks;
- override compatibility;
- access modifiers;
- generic constraints;
- generic-aware overload specificity;
- user-defined Nova value types;
- operator overload resolution.

## L-value checking

Assignment target validity belongs in semantic analysis.

For example:

```nova
1 = 2;
```

This is syntactically recognizable as an assignment expression, but it is semantically invalid because a literal is not assignable.

Valid assignment targets will generally include things like:

- variables;
- fields;
- array elements;
- member accesses that resolve to mutable storage.

The exact rule should remain explicit and test-covered.

## Return checking

Return checking validates whether `return` statements appear in valid contexts and whether their value matches the declared function or method return type.

Checks include:

- return outside a function/method/constructor context;
- returning a value from a `void` function;
- missing return values for non-void functions;
- simple missing-return cases.

More advanced control-flow-sensitive return analysis can be added later.

## Loop-control checking

`break` and `continue` are syntactically valid statements, but they are only meaningful inside loop or switch contexts depending on the rule.

Semantic analysis should report invalid placement such as:

```nova
break;
```

at the top level.

## Current limitations

The current semantic architecture is still transitional.

Known limitations include:

- declaration AST nodes and semantic declarations expose parsed `TypeSyntax` directly, while `ReturnType` remains as a compatibility adapter;
- semantic passes resolve parsed type syntax into semantic type symbols, with syntaxless `ReturnType` fallback conversion isolated in `semantic.type.ReturnTypeSyntaxBridge`;
- type checking uses semantic type symbols internally, but some compatibility constructors/getters still expose `ReturnType` for older manual AST and printer paths;
- semantic type symbols now distinguish Nova class/object types from Nova value/math types;
- parser-side type registry metadata has been removed; declaration/class parsing preserves type spelling through `TypeSyntax`;
- basic inheritance checks support subtype assignment and inherited member lookup, but access control, inherited-field conflicts, override compatibility, abstract/final behavior, and generic specificity are not implemented yet;
- the compiler is still single-file oriented;
- builtins and standard-library declarations are not yet modeled semantically or loaded through the same source pipeline as user code;
- advanced features such as full generics, lambdas, and monomorphization are deliberately deferred.

These limitations are tracked in [`../PLAN.md`](../PLAN.md).

## Testing semantic changes

Semantic tests should prefer small snippets with explicit diagnostics.

Useful test patterns:

```java
assertSemanticError("unknownName + 1;", "Undefined name");
assertSemanticError("1 = 2;", "Invalid assignment target");
assertSemanticError("int x; int x;", "Duplicate declaration");
```

A semantic test should generally avoid asserting parser internals unless the behavior is specifically about the parser/semantic boundary.
