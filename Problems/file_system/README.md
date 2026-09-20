# In-Memory File System

## Problem

Design a hierarchical in-memory file system with files, directories, paths, metadata, and safe mutation operations.

## Requirements

- Create files and directories using absolute normalized paths.
- List directory entries and read or replace/append file content.
- Move, copy, rename, and delete entries.
- Track name, type, size, creation time, modification time, and optional permissions.
- Search by name or predicate and traverse without exposing mutable child collections.
- Reject invalid names, duplicate siblings, missing parents, non-empty directory deletion, and directory cycles.

## Core model

Use a common `Entry` abstraction with `File` and `Directory` variants, a `Path` value object, and a `FileSystem` facade. Composite fits traversal; command objects can add undo/audit behavior. An injected `Clock` makes metadata tests deterministic.

## Invariants and concurrency

- Each entry except root has exactly one parent and a unique name among siblings.
- A directory cannot become its own descendant.
- Path resolution and mutation must agree on `.`/`..`, repeated separators, root, and case sensitivity.
- A move or rename is atomic to readers; concurrent writers cannot lose sibling updates.

## Scenarios

Demonstrate nested creation, path normalization, append/read, recursive listing, move, copy, duplicate-name rejection, cycle rejection, and concurrent creation in one directory.

## Local status

The current Java `Main` is a skeleton; this README is the complete design exercise. A solution should add the model, deterministic tests, and a documented run command.
