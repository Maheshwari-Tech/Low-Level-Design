# Composite Pattern

## Intent

Represent part-whole hierarchies so clients can treat an individual object and a group of objects through the same interface.

## When to use

- The domain naturally forms a tree, such as files, UI components, menus, or organization units.
- Operations should apply recursively without client-side type checks.
- Clients should not need separate workflows for leaves and containers.

## Participants and mechanics

- **Component** defines operations shared by every node.
- **Leaf** performs the operation directly and has no children.
- **Composite** stores child components and usually forwards or aggregates operations recursively.
- The client talks to the component contract, starting at any node in the tree.

## Trade-offs

- Simplifies tree traversal and makes new node types easy to add.
- A very broad component contract may expose child operations that are meaningless for leaves.
- Cycles, ownership, ordering, and error handling need explicit policies.

## Implementation status

**Runnable.** [`CompositeDemo.java`](CompositeDemo.java) treats file leaves and nested directories through one `FileNode` contract and recursively calculates and prints aggregate size.

## Run

```bash
out="$(mktemp -d)"
javac --release 17 -Xlint:all -d "$out" Patterns/structural/composite/CompositeDemo.java
java -cp "$out" code.structural.composite.CompositeDemo
```
