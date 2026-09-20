# CSV Object Mapper

## Prompt and scope

Design a reusable component that reads a header-based CSV resource and maps each row to an instance of a requested bean type using field annotations and type conversion. The source wraps OpenCSV's `HeaderColumnNameMappingStrategy` and demonstrates mapping an `input.csv` resource to `Person` objects.

CSV writing, spreadsheet formulas, remote ingestion, and arbitrary object-graph mapping are outside the initial scope.

## Core model

- `ParserForFlipkart<T>`: target class plus input resource name and `parse()` boundary.
- `MappingStrategy<T>`: maps header names to annotated bean fields.
- `CsvToBean<T>`: tokenization, conversion, and object construction delegated to OpenCSV.
- `Person`: example bean with `CsvBindByName` annotations.
- Input resource: header row followed by typed records.

## Invariants

- The resource exists, is readable in the declared charset, and has one valid header row.
- Header names map deterministically to target fields; required columns and duplicate headers are rejected.
- The target type can be instantiated and every value has a defined converter/null policy.
- Quoting, escaped delimiters, embedded newlines, and blank rows follow one documented CSV dialect.
- Readers are closed on success and failure; errors identify row, column, raw value, and cause.

## API

The source usage is `new ParserForFlipkart<Person>(Person.class, "input.csv").parse()`. A more extensible boundary is:

```text
parse(InputSource, Class<T>, CsvOptions, ErrorPolicy) -> ParseResult<T>
stream(InputSource, Class<T>, CsvOptions) -> CloseableIterator<T>
registerConverter(Type, ValueConverter)
```

`ParseResult` should expose successful rows and structured errors when partial acceptance is allowed.

## Main flow

1. Open the input through a resource abstraction and create a closeable reader.
2. Parse and normalize headers, then build a cached mapping plan for the target type.
3. Tokenize each record according to the configured CSV dialect.
4. Convert fields and instantiate the bean.
5. Collect or stream results according to memory and error policy; close resources deterministically.

## Concurrency and failure handling

- Keep parser configuration immutable so instances can be shared; readers and row state remain per invocation.
- Cache reflection/mapping metadata by target type and options with bounded, thread-safe storage.
- Distinguish missing resource, malformed CSV, schema mismatch, conversion error, inaccessible constructor, and I/O failure.
- Avoid `ClassLoader.getResource(...).getFile()` for packaged JAR resources; consume the resource stream directly.

## Design solution

Use a facade over a CSV engine, an `InputSource` abstraction for classpath/file/stream inputs, a compiled schema mapping, and pluggable converters. Separate fail-fast versus accumulate-errors policies. Offer streaming for large inputs and materialized lists for machine-coding fixtures.

The source is an adapter around OpenCSV, not an independent CSV lexer or reflection engine; preserve that distinction in an interview.

## Source variations and provenance

| Classification | Exact local clone path | Pinned upstream | Notes |
| --- | --- | --- | --- |
| OpenCSV-backed implementation | [`References/kumaransg-LLD/Low_level_Design_Problems/flipkart-interview-parser`](../../References/kumaransg-LLD/Low_level_Design_Problems/flipkart-interview-parser/) | [tree at `1698cc6`](https://github.com/kumaransg/LLD/tree/1698cc6f993a5014d4370b5e0db9f64d322e2400/Low_level_Design_Problems/flipkart-interview-parser) | Maven project, usage README, generic wrapper, annotated bean, and resource fixture. |

No second implementation or exact duplicate is present in the clone for this question.

## Follow-ups

- Add nested objects, optional/required annotations, defaults, and custom converters.
- Compare reflection setters, constructors, records, generated mappers, and method handles.
- Support streaming backpressure, parallel conversion with ordered results, and error quarantine.
- Add delimiter/dialect detection, schema evolution, and safe limits for untrusted files.

## Implementation status

**Small dependency-backed reference; no code copied here.** The Maven source demonstrates the advertised `Person` mapping with OpenCSV and Lombok, but has no tests, uses raw `Class`, throws broad exceptions, does not close its `FileReader`, assumes a classpath resource can be converted to a filesystem path, and materializes every row.

The code remains unchanged in the `References/kumaransg-LLD` clone. That clone has no repository-level license, so this page summarizes the design and links to the pinned source rather than redistributing it.
