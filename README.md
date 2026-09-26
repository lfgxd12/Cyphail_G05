# Cyphail - Graph Query Engine Prototype

## Context
Project developed for the course **EIF400 Paradigmas de Programación (II-2026)**.
Escuela de Informática, Universidad Nacional (UNA), Costa Rica.

Cyphail is a small transpiler/engine prototype for a subset of the **Cypher** graph query
language (the one used by Neo4j). This is the **Sprint P1** deliverable: a hand-written
combinator parser builds an AST from a Cypher-like query, the `.tree` REPL command pretty-prints
that AST, and a semantic checker flags undefined variables. The engine itself is still a *fake*
broker that reads its answers from JSON files on disk (no hardcoded data), as required by the
Sprint P1 spec.

## Authors
* Luis Felipe Jiménez Fernández - ID: 119130110 - Grupo: G05
* Jose David Chavarria Villalobos - ID: 402710170 - Grupo: G05
* Jostin Jimenez Alfaro - ID: 119620942 - Grupo: G05
* Angel Rojas Ruano - ID: 118780534 - Grupo: G05

## Prerequisites
* **Java Development Kit (JDK)**: Version 26 or higher.
* **Apache Maven**: Version 3.8+

## Building the Project
To compile and package the application from the command line (CMD), run:
```cmd
mvn clean package
```
This generates an executable JAR at `target/cyphail.jar`.

## Running the Tests
```cmd
mvn test
```
The suite covers the parser combinators, the lexer, the parser itself (the professor's C1-C11
test cases), the `.tree` output format, variable-scope validation and the REPL's multi-line
input rules.

## Running the Project
Once built, start the Cyphail REPL from the command line:
```cmd
cyphail.bat repl
```
Or directly with Java:
```cmd
java -jar target\cyphail.jar repl
```
You should see a welcome banner followed by the `>>>` prompt. Type `.help` to see
the available REPL commands, or type `.exit` to quit.

## REPL Commands
```
.help             Show this help message
.about            Show project authors and course details
.use [graph]      List available graphs or select a graph
.tree <query>     Show the AST of a query (SPEC tree format)
.exit             Exit the REPL
```
Anything else typed at the prompt is treated as a Cyphail query and resolved against the fake
JSON-backed engine (see below).

### Multi-line queries
A query does not have to fit on one line. The REPL keeps reading lines (the prompt changes to
`... `) until the input is complete. An input is considered complete when:
* it already parses successfully **and** it has a `RETURN` clause (the last clause in the
  grammar), or
* the last line ends with `;` (useful for statements without `RETURN`, e.g. a bare `DELETE`), or
* an empty line is entered.

A single-line query with `RETURN` still runs immediately after pressing Enter, exactly as before.

### `.tree` example
```
>>> .tree
... MATCH (m:Movie)
... WHERE m.year > 1990
... RETURN m.title AS title,
...        m.year AS year
Query{
  Match: {
    Patterns: [
      PatternNode: {
        var: m
        labels: [ Movie ]
        properties: []
      }
    ]
  }
  Where: {
    Expr: (> (. m year) 1990)
  }
  Updates: []
  Return: {
    Projection: {
      Items: [
        {as (. m title) title}
        {as (. m year) year}
      ]
      Modifiers: []
    }
  }
}
```
If the query has a syntax error, `.tree` prints `ERROR: Syntax error. ...` instead. If it parses
but uses a variable that was never declared in a pattern, it still prints the tree and then one
`ERROR: Undefined variable '...' ...` line per offending use (in English, as required by the spec).

## Fake Data (`data/`)
The engine's answers are **not** hardcoded in Java. `FakeQueryHandler` reads `data/test_cases.json`
at query time, matches the query text against the `query` field of each entry, and prints the
associated `headers`/`data` as a table. New test queries and their fake results can be added to
that file, or edited, **without recompiling** the project — just save the JSON and re-run the
already-built query in the REPL.

## Project Structure
```
src/main/java/cr/ac/una/eif400/cyphail/
├── ast/            AST nodes: sealed interfaces + records (Statement, Clause, Expression, ...)
├── parser/         CyphailParser (combinator-based) and:
│   └── core/       the combinator library itself (Parser, Parsers, lexers, Ok/Fail)
├── validation/     VariableScopeChecker + SemanticException
├── output/         TreePrinter (the .tree pretty-printer) and TablePrinter
├── engine/         FakeQueryHandler (JSON-backed fake broker) and JsonFakeBroker
├── frontend/       Repl, MultilineInput and the `.xxx` command handlers
└── model/          small value types (Author, GraphInfo)
```

## Supported Queries (Sprint P1)
The parser supports the grammar subset needed for the professor's reference test cases (C1-C11):
`MATCH` with one or more comma-separated node patterns, each with zero or more labels and an
optional `{key: value, ...}` property map (values may themselves be expressions, e.g.
`{personId: p.id}`); an optional single-comparison `WHERE`; any combination of `CREATE`,
`(DETACH) DELETE` and `REMOVE`; and an optional `RETURN` with aliases. Comments (`//` and
`/* */`) are ignored, as required by the grammar.

**Known limitations** (out of scope for this sprint, matching the reference test cases): a single
`MATCH` per query, a single comparison per `WHERE` (no `AND`/`OR`/`NOT`), no relationship patterns
(`-[:REL]->`), and no `SET`.

## Sources and Credits
* [ascii-table](https://github.com/freva/ascii-table) library (com.github.freva) — used for tabular console output.
* [Gson](https://github.com/google/gson) library (com.google.code.gson) — used to read the fake data JSON files.
* Project specification, grammar and REPL examples provided by Prof. Carlos Loría-Sáenz, EIF400-II-2026, UNA.

## AI Usage Declaration
AI assistance was used during this project, always for understanding/studying and for reviewing
code that a team member wrote, never to generate the project unattended. The full history of
prompts and models used, by sprint and by student, is documented in
[PROMPTS.MD](./PROMPTS.MD), as required by the course policy on AI usage.
