# Nova language design

This document describes the long-term Nova language vision.

It is intentionally separate from the implementation status. Many ideas in this document are **planned language goals**, not features that are fully implemented in the compiler today.

For current compiler status, see [`../README.md`](../README.md), [`../PLAN.md`](../PLAN.md), and [`compiler-roadmap.md`](compiler-roadmap.md).

> [!IMPORTANT]
> This document describes the intended Nova language model, not the exact feature set implemented by the current Java front end.
> Current implementation status lives in [`../README.md`](../README.md), [`../PLAN.md`](../PLAN.md), and [`compiler-roadmap.md`](compiler-roadmap.md).

## Design goal

Nova is intended to be a compiled, object-oriented language with a strong static type system and a syntax that feels familiar to C-like and Java-like programmers.

The long-term goal is to combine:

- native compilation;
- predictable performance;
- object-oriented modeling;
- strong compile-time guarantees;
- rich standard tooling;
- a standard library that covers common needs without forcing every project to depend on fragile third-party packages.

## Syntax philosophy

Nova syntax is intended to remain familiar.

The goal is not to invent a new visual language for its own sake, but to make the language readable to programmers who already know C, C++, Java, or similar languages.

The language should favor explicitness where it improves compiler guarantees.

## Classes and types

A central design idea is the distinction between **classes** and **types**.

### Classes

Classes are object-oriented entities with identity, state, behavior, and lifecycle.

They are intended for modeling entities that behave like objects:

- user-defined domain objects;
- services;
- components;
- data structures with behavior;
- objects that may use inheritance and polymorphism.

Class features planned for Nova include:

- fields;
- methods;
- constructors;
- access modifiers;
- inheritance;
- method overriding;
- method overloading;
- generic class parameters;
- strict compile-time visibility checks.

Class rules that define the intended model:

- every class member must declare its own access modifier: `public`, `private`, or `protected`;
- public members are always accessible where visibility permits;
- private and protected access are enforced by the compiler, with no reflection loophole;
- public fields can be read and written directly;
- private fields can only be accessed from inside the class body, or through explicit public API such as getters and setters;
- method overloading is based on the parameter list: number, order, and types.

Operator overloading does not belong to ordinary classes. Classes are for object identity, state, lifecycle, and dispatch.

### Types

Types are intended to represent mathematical or value-like entities whose behavior is defined by their algebraic properties rather than object identity.

Examples of eventual user-defined types may include:

- vectors;
- matrices;
- tensors;
- numeric domains;
- values with operator overloads.

Long-term design idea:

- classes model object identity and behavior;
- types model value semantics and operator-defined algebra;
- operator overloading belongs to types, not ordinary classes.

User-defined types are expected to define their behavior explicitly through supported operators and explicit casts. The compiler should be able to optimize user-defined types as aggressively as built-in primitives because type information is available at compile time.

This distinction is not fully implemented yet. It is part of the future type-system work described in [`compiler-roadmap.md`](compiler-roadmap.md).

## Hard-static typing

Nova is intended to be statically typed in a stricter sense: **hard-static**.

Hard-static means type information is always available at compile time. Nova should not need dynamic declarations, runtime type discovery, or reflection-driven dispatch to understand what a program means.

Rules and consequences:

- reflection is not a core language feature;
- runtime type inspection should not be required for ordinary language semantics;
- dynamic method invocation is intentionally excluded from the core model;
- overload resolution happens during compilation;
- operator dispatch for user-defined types happens during compilation;
- type inference, where supported, must resolve to concrete compile-time types;
- generic instantiations are known at compile time;
- declarations that cannot be statically typed should not be accepted as ordinary Nova declarations;
- compiler guarantees should enable aggressive optimization and zero-cost abstractions.

This design intentionally pushes complexity into the compiler.

> [!WARNING]
> Hard-static typing is not just "static typing with fewer dynamic features."
> It is a language constraint: any construct that depends on runtime type inspection must be redesigned as a compile-time construct.
> This keeps the runtime lean, but it means Nova needs strong compiler-side facilities for code generation, registries, and tooling.

## Inheritance and bounded polymorphism

Inheritance is planned for classes, not mathematical/value types.

When a class extends another class, it inherits method signatures, method implementations, and fields according to the class model.
Multiple inheritance is allowed in the long-term design, but it must remain compile-time safe.

Conflict rules:

- if multiple parents provide the same method signature, the subclass must explicitly override it;
- if multiple parents declare a field with the same name, that is a compile-time error;
- inherited field conflicts have no automatic merge or shadowing rule;
- the hierarchy must be redesigned if inherited field identity becomes ambiguous.

Abstract and final class behavior is also part of the intended design:

- abstract methods may be declared;
- a class with unimplemented abstract methods is abstract and cannot be instantiated;
- a class extending an abstract class must implement all inherited abstract methods or remain abstract;
- final classes cannot be extended;
- final methods cannot be overridden.

Bounded polymorphism should remain compile-time visible. For example, a parameter may be constrained to a known upper bound such as `T : Comparable`. Inside the generic body, only the signature guaranteed by that bound is available, regardless of the concrete type later used.

## Reflection and tooling

Because runtime reflection is not intended to be a core language feature, tooling that other languages often implement with reflection should be implemented through compile-time mechanisms.

For example, a future Nova test framework should not discover tests by scanning runtime methods. Instead, the compiler or build tooling could generate a static test registry from source-level annotations or dedicated test constructs.

This is a design constraint, not an implementation detail.

> [!NOTE]
> Reflection-heavy patterns such as test discovery, serialization metadata, dependency injection, plugin discovery, and schema generation should be treated as compile-time metaprogramming problems in Nova.
> The compiler should generate direct registries or metadata artifacts instead of relying on runtime scanning.

## Generics

Nova's long-term generic design is intended to avoid type erasure.

The design goal is that generic instantiations remain concrete and known at compile time.

For example:

```nova
List[int]
List[string]
```

would represent different concrete instantiations.

Intended rules:

- `List[int]` and `List[string]` are different concrete types;
- generic instances with cast-compatible parameters, such as `List[int]` and `List[float]`, are still different types;
- generic class implementation details can be shared through an abstract generic superclass;
- concrete instantiations are generated with type parameters substituted by concrete types;
- bounded generic parameters can constrain what operations are available inside generic bodies;
- recursively bounded generics, such as `Comparable[T]`, are possible design targets;
- monomorphization or another compile-time specialization strategy is expected.

Type parameters may themselves have upper bounds. For example:

```nova
class List[T : Comparable] { }
```

Only the operations guaranteed by `Comparable` are available through `T` inside the class body. The concrete type may provide more operations, but those are not visible unless the generic bound exposes them.

### Class parameters

Nova also reserves space for class parameters: compile-time values that are part of a class instantiation.

Example:

```nova
class Tensor[T, (N : int)] { }
```

Rules intended for class parameters:

- class parameters are values, not classes;
- each class parameter must declare its type explicitly;
- class parameter values are part of type identity;
- `Tensor[int, N = 3]` and `Tensor[int, N = 2]` are different types;
- class parameters must be propagated through inheritance chains.

For example:

```nova
class SquareMatrix[T, (N : int)] :: Tensor[T, N, N] { }
```

The compiler must substitute `T` and `N` when generating or resolving the concrete superclass.
That makes class parameters a type-system and monomorphization feature, not parser sugar.

These features are deliberately deferred until the compiler has a real semantic type model.

## Variadic generics

Variadic generics are a possible future feature, but they are also one of the hardest parts of the design.

A syntax such as:

```nova
Tuple[{A}]
```

would require the type checker and monomorphizer to reason about an unbounded number of type parameters.

This is not just a parser feature. It affects:

- type representation;
- constraint solving;
- function signatures;
- overload resolution;
- monomorphization;
- standard library design.

For this reason, variadic generics should remain a late-stage feature.

> [!WARNING]
> Variadic type parameters and lambdas are coupled.
> `Function[{A}, R]`, the likely base representation for function-like values, depends on a variable-length parameter list.
> That means lambdas cannot be fully implemented until the compiler can represent and reason about variadic generic parameters.
>
> Nova should treat this as a major language milestone, not as a small parser extension.

## Lambdas

Nova is intended to support lambda expressions and functional programming constructs eventually, but lambda design depends on the generic type model.

One possible model is to treat lambdas as compiler-generated classes implementing a function-like abstraction:

```nova
Function[{A}, R]
```

Here `{A}` represents the list of argument types and `R` represents the return type.

For example:

```nova
(x: int, y: int) -> int { return x + y; }
```

could lower to an anonymous generated class extending `Function[{int, int}, int]` and implementing a statically typed `call` method.

The compiler should generate a concrete class for each unique lambda expression and preserve static knowledge of its argument and return types.

Because lambdas are function-like class instances, calls may use virtual dispatch by default. The compiler should devirtualize lambda calls when the concrete lambda type is statically known, especially in tight loops or higher-order standard-library operations.

This is a required optimization target for the long-term design, not a stretch goal.

However, this depends on a robust representation of function types, variadic generic parameter lists, and IR lowering. Lambdas should therefore wait until the type system and IR design are ready.

## Package management

Nova source files are expected to use the `.nv` extension.

The language design currently reserves these names:

- **Orbit**: package manager;
- **Nebula**: community package registry.

These are design placeholders, not implemented tools.

## Standard library vision

The long-term standard library should be broad enough that basic development does not require immediately reaching for third-party packages.

Potential areas include:

- I/O;
- strings;
- math;
- collections;
- testing support;
- package tooling;
- event-driven programming;
- graphics/audio interfaces, eventually.

Current compiler work should not try to implement all of this yet. The first step is to make the standard library load through the same source pipeline as user code.

## Native compilation

The long-term backend goal is native compilation.

The current repository does not implement:

- IR generation;
- optimization;
- assembly generation;
- machine-code emission;
- object-file generation;
- executable linking.

The planned route is:

```text
AST + semantic model
  ↓
lowered IR
  ↓
optimization
  ↓
backend code generation
```

Skipping directly from AST to machine code would make the compiler harder to evolve.

## Implementation warning

This document is allowed to be ambitious.

The README should not be.

When a language feature is only planned, keep it in this document or in the roadmap. The README should describe only what a reader can actually build, run, or inspect today.
