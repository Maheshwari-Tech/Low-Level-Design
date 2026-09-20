# Text Editor

## Interview Prompt

Design an in-memory editor with insert, delete, copy, paste, print, undo, and redo. Preserve three actual upstream approaches instead of treating one data structure as the only solution.

## Model and Requirements

`Document` owns text and a monotonically increasing revision. `TextRange` is a validated half-open interval. `Cursor`, `Selection`, `Clipboard`, `EditCommand`, `History`, and `TextBuffer` separate user intent from storage.

- Insert or delete at a valid range and return the new cursor/revision.
- Copy is read-only; cut and paste are compound edits.
- Undo reverses the last committed edit; redo reapplies only until a new branch is created.
- Keep clipboard state independent from document history.
- Support a simple string/list buffer first; discuss a gap buffer, piece table, or rope for larger files.

## Invariants

- Every command is validated against the revision and text length it targets.
- A committed mutation records enough inverse data to restore the exact previous content and cursor.
- Failed edits do not enter history; undo/redo move one command exactly once.
- A new edit after undo clears the redo branch.
- A retried command ID does not apply the same mutation twice.

## API Sketch

```java
EditResult insert(DocumentId id, long version, int offset, String text, String commandId);
EditResult delete(DocumentId id, long version, TextRange range, String commandId);
String copy(DocumentId id, TextRange range);
EditResult paste(DocumentId id, long version, int offset, String commandId);
EditResult undo(DocumentId id, long version);
EditResult redo(DocumentId id, long version);
DocumentSnapshot snapshot(DocumentId id);
```

## Design Solution

Represent each edit as Command plus inverse data. The history owns two stacks or a revision graph; `execute` pushes a successful command and clears redo, `undo` invokes its inverse and moves it to redo, and `redo` reapplies it. Keep buffer operations behind an interface so interview discussion can move from a small `StringBuilder` to a piece table without changing command semantics. Serialize mutations per document or compare an expected revision; never hold a shared lock while calling UI callbacks.

## Source-Backed Variations

1. `References/kumaransg-LLD/Low_level_Design_Problems/Machine_coding_FLIPKART/TextPadApplication/` — Java/Eclipse command-style insert, delete, copy, paste, and print. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Machine_coding_FLIPKART/TextPadApplication)
2. `References/kumaransg-LLD/Low_level_Design_Problems/Text_editor_DLL/` — single-file doubly-linked-list fragment. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/Text_editor_DLL)
3. `References/kumaransg-LLD/Low_level_Design_Problems/texteditor/` — separate Java model/service/implementation variation. [Pinned source](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/texteditor)

`Low-Level-Design-1/texteditor` is an exact duplicate of variation 3, and `TextPadApplication.zip` duplicates the extracted first tree. They remain indexed as provenance, not additional solutions.

## Follow-Ups and Status

Search/replace, styled spans, file persistence, snapshots, huge-file paging, macros, and collaborative OT/CRDT editing. This page is the maintained interview solution; actual upstream code stays unmodified in the no-root-license clone.
