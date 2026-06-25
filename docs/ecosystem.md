# Nova ecosystem

This document defines the naming vocabulary planned for the Nova ecosystem.

It describes intended names and responsibilities. Most of these tools and packages are **not implemented yet**. For current compiler implementation status, see [`../README.md`](../README.md), [`../PLAN.md`](../PLAN.md), and [`compiler-roadmap.md`](compiler-roadmap.md).

## Naming theme

Nova uses an astronomy and particle-physics inspired naming scheme.

The intent is to make the ecosystem memorable while keeping names attached to clear technical roles.

## Core names

| Name       | Intended role                            | Status                                               |
|------------|------------------------------------------|------------------------------------------------------|
| **Nova**   | The programming language                 | Design and compiler front-end in progress            |
| **Pulsar** | The Nova compiler                        | Current Java front-end is the beginning of this tool |
| **Orbit**  | Package manager and dependency tool      | Planned                                              |
| **Nebula** | Community package registry used by Orbit | Planned                                              |
| **Quark**  | A standalone Nova artifact/package       | Planned vocabulary                                   |
| **Core**   | Standard base package                    | Planned                                              |

A typical future dependency flow should read naturally as:

> Orbit downloads and upgrades Quark dependencies from Nebula.

## Quarks

A **Quark** is any standalone Nova artifact that can be versioned, published, downloaded, and depended on.

A Quark may eventually represent:

- a library package;
- an application package;
- a standard-library component;
- a compiler plugin or tool integration, if the ecosystem later supports them;
- a package containing generated Nova code or metadata.

The exact package format is not designed yet. The name is reserved now so documentation, CLI language, and future metadata files can stay consistent.

Possible future examples:

```text
core
solar
pulse
spectrum
atlas
my-company/http-server
my-user/math-experiments
```

## Orbit

**Orbit** is the planned package manager for Nova.

Intended responsibilities:

- initialize Nova projects;
- read project metadata;
- resolve Quark dependencies;
- download Quarks from Nebula or another configured source;
- update dependency versions;
- cache downloaded Quarks;
- invoke Pulsar with the correct project and dependency graph;
- eventually support lockfiles for reproducible builds.

Possible future commands:

```bash
orbit init
orbit add solar
orbit add atlas@1.2.0
orbit update
orbit build
orbit test
```

These commands are examples, not committed CLI syntax.

## Nebula

**Nebula** is the planned community-driven package registry.

Intended responsibilities:

- host published Quarks;
- expose version metadata;
- serve dependency metadata to Orbit;
- support package discovery;
- eventually support trust, ownership, signatures, or other supply-chain features.

Nebula should be treated as the community registry, not necessarily the only possible registry. Long term, Orbit may support private or alternate registries.

## Pulsar

**Pulsar** is the name reserved for the Nova compiler.

The current Java project is the early front-end implementation of Pulsar. As the compiler grows, Pulsar is expected to include:

- lexing;
- parsing;
- diagnostics;
- semantic analysis;
- type checking;
- project-level compilation;
- standard-library loading;
- IR generation;
- optimization;
- backend code generation.

Potential future command shape:

```bash
pulsar check main.nv
pulsar build
pulsar run
```

Actual CLI design is not finalized.

## Standard package family

Nova's standard library is expected to be split into a small base package plus optional on-demand packages.

| Name         | Intended area                                               |
|--------------|-------------------------------------------------------------|
| **Core**     | Base language/runtime package and fundamental declarations  |
| **Solar**    | Math and numeric utilities                                  |
| **Pulse**    | Event-driven programming, I/O, and asynchronous interaction |
| **Spectrum** | Graphics, rendering, and GPU-oriented utilities             |
| **Echo**     | Audio utilities                                             |
| **Atlas**    | Data structures and collections                             |

The standard library should not be treated as one monolithic dependency forever. The intended model is:

- `Core` is the minimal base package;
- additional standard packages are loaded on demand;
- standard packages are still Quarks conceptually;
- standard packages should eventually pass through the same dependency and compilation model as user packages.

## Core

**Core** is the standard base package.

It may eventually provide:

- primitive declarations and aliases required by the language model;
- base object/class declarations;
- fundamental errors or diagnostics visible to Nova code;
- basic string and character support;
- minimal standard functions needed by ordinary programs.

Core should stay small. If a feature can live in a focused optional package, it probably should.

## Solar

**Solar** is the planned math and numeric utilities package.

Possible areas:

- numeric functions;
- constants;
- vector and matrix helpers;
- tensor-oriented utilities, if aligned with the future type model;
- statistics or numerical methods;
- numeric traits/interfaces once the type system supports them.

## Pulse

**Pulse** is the planned event-driven and I/O package.

Possible areas:

- file I/O;
- stream abstractions;
- event loops;
- timers;
- asynchronous task coordination;
- network I/O, if included in the standard ecosystem.

## Spectrum

**Spectrum** is the planned graphics and GPU-oriented package.

Possible areas:

- rendering abstractions;
- GPU dispatch helpers;
- shaders or shader metadata, if supported later;
- image buffers;
- color utilities;
- graphics pipelines.

Spectrum is a long-term package and should wait until the compiler and runtime model can support its requirements.

## Echo

**Echo** is the planned audio package.

Possible areas:

- audio buffers;
- sampling utilities;
- audio streams;
- device interfaces;
- synthesis or effects helpers;
- audio file formats.

## Atlas

**Atlas** is the planned data-structures and collections package.

Possible areas:

- lists;
- maps;
- sets;
- queues;
- graphs;
- trees;
- iterators or traversal utilities;
- collection algorithms.

Some collection fundamentals may live in Core at first, but Atlas should be the long-term home for richer data structures.

## Possible future names

The following areas still need names. These are placeholders for future design discussions, not reserved decisions.

| Area                                 | Possible role                                           |
|--------------------------------------|---------------------------------------------------------|
| Build/workspace metadata             | Project manifests, lockfiles, workspaces                |
| Test framework                       | Compile-time test discovery and generated test registry |
| Documentation generator              | Nova API documentation generation                       |
| Formatter                            | Source formatting tool                                  |
| Linter/static analyzer               | Style and correctness checks beyond compilation         |
| Language server                      | Editor integration and IDE features                     |
| Formatter/package publishing profile | Release automation and package publishing               |
| Native runtime support               | Low-level runtime helpers, if needed                    |

When adding new names, prefer names that:

- fit the astronomy/particle theme;
- are easy to pronounce;
- are short enough for CLI usage;
- do not collide with established Nova ecosystem names;
- describe one clear responsibility.

## Documentation rule

Use these names consistently, but avoid implying implementation status too early.

Preferred wording:

> Orbit is the planned package manager.

Avoid wording such as:

> Orbit downloads packages today.

until the tool actually exists.
