# Nova language design

This document describes the long-term Nova language vision.

It is intentionally separate from the implementation status. Many ideas in this document are **planned language goals**, not features that are fully implemented in the compiler today.

For current compiler status, see [`../README.md`](../README.md), [`../PLAN.md`](../PLAN.md), and [`compiler-roadmap.md`](compiler-roadmap.md).

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

This distinction is not fully implemented yet. It is part of the future type-system work described in [`compiler-roadmap.md`](compiler-roadmap.md).

## Hard-static typing

Nova is intended to be hard-static: type information should be available at compile time, and the compiler should avoid depending on runtime type discovery.

The intended consequences are:

- no ordinary reflection-based runtime model as a core feature;
- overload resolution at compile time;
- operator dispatch at compile time;
- generic instantiation known at compile time;
- aggressive optimization opportunities;
- fewer runtime type checks.

This design intentionally pushes complexity into the compiler.

## Reflection and tooling

Because runtime reflection is not intended to be a core language feature, tooling that other languages often implement with reflection should be implemented through compile-time mechanisms.

For example, a future Nova test framework should not discover tests by scanning runtime methods. Instead, the compiler or build tooling could generate a static test registry from source-level annotations or dedicated test constructs.

This is a design constraint, not an implementation detail.

## Generics

Nova's long-term generic design is intended to avoid type erasure.

The design goal is that generic instantiations remain concrete and known at compile time.

For example:

```nova
List[int]
List[string]
```

would represent different concrete instantiations.

Potential future features include:

- generic classes;
- bounded generic parameters;
- generic parameter propagation through inheritance;
- class/value parameters similar in spirit to non-type template parameters;
- monomorphization or another compile-time specialization strategy.

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

## Lambdas

Nova is intended to support lambdas eventually, but lambda design depends on the generic type model.

One possible model is to treat lambdas as compiler-generated classes implementing a function-like abstraction.

For example:

```nova
(x: int, y: int) -> int { return x + y; }
```

could lower to an anonymous generated class with a statically typed call method.

However, this depends on a robust representation of function types and possibly variadic generic parameter lists. Lambdas should therefore wait until the type system and IR design are ready.

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
