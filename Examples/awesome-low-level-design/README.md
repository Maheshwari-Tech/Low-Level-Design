# Awesome Low-Level Design — attributed example archive

This directory preserves the educational source code, problem statements, and diagrams imported from [`ashishps1/awesome-low-level-design`](https://github.com/ashishps1/awesome-low-level-design). It is an example archive, not a second canonical problem index: use the repository [problem bank](../../Problems/) for the deduplicated questions and the local Java-first solutions.

## Snapshot

| Field | Value |
| --- | --- |
| Upstream repository | `https://github.com/ashishps1/awesome-low-level-design.git` |
| Upstream commit | `fc26e4033cad6d24f32caa8521044febbf065beb` |
| Commit date | 2026-02-26 |
| Imported on | 2026-08-09 |
| License | [GNU General Public License v3.0](LICENSE) |

The exact upstream README at that revision is retained as [UPSTREAM_README.md](UPSTREAM_README.md). See [NOTICE.md](NOTICE.md) for provenance and import boundaries.

## Contents

- [OOP examples](oop/) in C++, C#, Go, Java, Python, and Rust.
- [Design-pattern examples](design-patterns/) in C++, C#, Go, Java, JavaScript, and Python.
- [Problem statements](problems/) for the upstream interview catalog.
- [Problem solutions](solutions/) in C++, C#, Go, Java, Python, and TypeScript.
- [Class diagrams](class-diagrams/) and [supporting images](images/).

Generated build output and dependency caches were deliberately excluded: `.git`, `bin`, `obj`, `target`, `node_modules`, `__pycache__`, compiled binaries, class files, and logs. Educational source files were otherwise copied without rewriting them.

## How this merges with LLD Atlas

The local repository owns one canonical README per design problem. Alternate names—such as ride sharing and cab booking, or coffee vending machine and vending machine—map to that single page. This archive supplies additional multi-language implementations and diagrams without duplicating canonical navigation or presenting upstream code as locally authored work.

Start with:

1. [Problem bank](../../Problems/) for the deduplicated question and solution map.
2. [Foundations](../../Foundations/) and [Patterns](../../Patterns/) for the Java-first learning path.
3. This archive when you want another language, an alternate implementation, or the original diagram.
