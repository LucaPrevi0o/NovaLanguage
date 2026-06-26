# Type model boundary

Nova currently separates type spelling from resolved type meaning.

For the wider compiler pipeline, see [`architecture.md`](architecture.md). For the semantic passes that consume resolved types, see [`semantic-analysis.md`](semantic-analysis.md).

## Rule

Parser-owned code preserves type syntax. Semantic-owned code resolves type meaning.

In practical terms:

- `TypeSyntax` represents what the source file wrote.
- `TypeSymbol` represents what semantic analysis proved that spelling means.
- Parser code should not validate whether a type exists, is visible, is assignable, or has members.
- Semantic passes should resolve parsed type syntax through `semantic.type.TypeResolver`.

## TypeSyntax

`TypeSyntax` nodes live under `parser.ast.nodes.type`.

They preserve source-level type shape before semantic resolution:

- `NamedTypeSyntax` stores a written type name such as `int`, `Box`, `Missing`, or `T`.
- `ArrayTypeSyntax` stores the element type syntax plus parsed dimension expressions.
- `GenericTypeSyntax` stores class generic-parameter syntax such as `T` from `class Box[T]`.

Examples:

```nova
int count;
```

The parser records a primitive named type syntax for `int`.

```nova
Box value;
```

The parser records a non-primitive named type syntax for `Box`. It does not decide whether `Box` exists.

```nova
int[3][] values;
```

The parser records array type syntax whose element spelling is `int` and whose dimensions are `[3]` and `[]`.

```nova
public class Box[T] {
  public T value;
}
```

The parser records the class generic parameter spelling and the field type spelling. Full generic constraints and specialization are still future work.

## TypeSymbol

`TypeSymbol` implementations live under `semantic.type.symbol`.

They represent resolved semantic categories:

- `ValueTypeSymbol` for Nova value/math types, including built-in primitive-like types.
- `ClassTypeSymbol` for object/class types backed by semantic class declarations.
- `ArrayTypeSymbol` for arrays with a resolved element type.
- `GenericParameterSymbol` for a visible generic parameter.
- `UnknownTypeSymbol` for unresolved names that should still allow later diagnostics to continue.

Examples:

```nova
int count;
bool ok;
string text;
```

These names resolve to `ValueTypeSymbol` instances with domains such as integral, boolean, or text.

```nova
public class Box { }
Box value;
```

`Box` resolves to a `ClassTypeSymbol` backed by the semantic class declaration.

```nova
Box[] values;
```

The array resolves to an `ArrayTypeSymbol` whose element type is the resolved `ClassTypeSymbol` for `Box`.

```nova
Missing value;
```

The parser accepts this as named type syntax. Semantic resolution produces an `UnknownTypeSymbol` and an undefined-type diagnostic if no visible declaration defines `Missing`.

## Resolution ownership

Type resolution belongs to semantic analysis.

Current semantic passes use resolved type symbols for:

- declared type validation;
- initializer and assignment compatibility;
- array access;
- function and method argument checks;
- direct and inherited member lookup;
- class subtype assignment;
- basic overload selection.

This keeps parser behavior syntactic and allows the front end to preserve an AST for semantically invalid programs.

## ReturnType during migration

`ReturnType` is a temporary compatibility adapter.

It is allowed to:

- support older manual AST construction in tests and transitional code;
- support printer paths that still expect a `ReturnType`;
- expose source `TypeSyntax` when it is available;
- fall back through `semantic.type.ReturnTypeSyntaxBridge` only for syntaxless compatibility nodes.

It should not be used as the semantic type model. New parser-created declarations should carry `TypeSyntax` directly, and semantic passes should prefer that syntax before consulting any adapter fallback.

## Future work

The current boundary is deliberately conservative. It supports the MVP front end while keeping room for:

- multi-file type resolution;
- source-loaded standard-library declarations;
- full generics and constraints;
- user-defined Nova value/math types;
- operator overload resolution;
- backend-neutral IR that consumes semantic symbols instead of parser token metadata.
