# Cyphail - Graph Query Engine Prototype

## Context
Project developed for the course **EIF400 Paradigmas de Programación (II-2026)**.
Escuela de Informática, Universidad Nacional (UNA), Costa Rica.

Cyphail is a small transpiler/engine prototype for a subset of the **Cypher** graph query
language (the one used by Neo4j). In this **Sprint P2** deliverable a Cypher-like statement goes
through the whole pipeline:

```
REPL  ->  Parser (AST)  ->  Analyzer  ->  PrologCodegen  ->  Broker (MQI over TCP)  ->  SWI-Prolog engine
                                                                                          |
                         table in the REPL  <-  RowsTable  <-  "Rows" text  <-------------+
```

The graph lives in a real **SWI-Prolog** process (as `user_G_node/1`, `user_G_label/2`,
`user_G_edge/4` and `user_G_prop/3` facts). Each statement is translated to a Prolog goal and sent
to that process through SWI-Prolog's Machine Query Interface (MQI). The fake JSON engine of Sprint
P1 no longer exists.

## Authors
* Luis Felipe Jiménez Fernández - ID: 119130110 - Grupo: G05
* Jose David Chavarria Villalobos - ID: 402710170 - Grupo: G05
* Jostin Jimenez Alfaro - ID: 119620942 - Grupo: G05
* Angel Rojas Ruano - ID: 118780534 - Grupo: G05

## Prerequisites
* **Java Development Kit (JDK)**: Version 26 or higher.
* **Apache Maven**: Version 3.8+
* **SWI-Prolog 10** (developed and tested with 10.0.2). The REPL looks for `swipl` in this order:
  1. the `CYPHAIL_SWIPL` environment variable (full path to `swipl` / `swipl.exe`),
  2. the `PATH`,
  3. `C:\Program Files\swipl\bin\swipl.exe`.

  The engine is only started when the first query needs it, so `.help`, `.about`, `.tree` and
  syntax errors work even without SWI-Prolog installed. If `swipl` cannot be found, running a query
  prints an `ERROR:` line that says so.

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
The Java suite covers the parser combinators, the lexer, the parser (including relationship
patterns, `CREATE`/`SET`/`REMOVE`/`DELETE` and the SPEC statements), the `.tree` output, the
Analyzer, the Prolog code generator (exact text of every generated goal), the broker (MQI framing,
response parsing, `swipl` lookup, `Rows` parsing) and the REPL (multi-line input, `;`, `.load`,
`.use`, error messages).

The tests under `EngineBrokerTest` start a real SWI-Prolog process; they are **skipped
automatically** if `swipl` is not installed. The Prolog engine has its own unit tests (plunit):
```cmd
swipl -g run_tests -t halt src/test/resources/prolog/cyphail_engine_tests.pl
```

## Running the Project
Once built, start the Cyphail REPL from the command line (from the project root):
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
.use [graph]      List the engine graphs (the one in use is marked) or select the graph in use
.tree <query>     Show the AST of a query (SPEC tree format)
.load "file"      Run every statement of a .cyphail script, in order
.exit             Exit the REPL
```
Anything else typed at the prompt is treated as a Cyphail statement. The REPL starts with the
sample graph `amigos` in use.

### Statements end with `;`
A statement may span several lines (the prompt changes to `... `). It runs when:
* the last line ends with `;` (several statements can share a line: `CREATE (a); CREATE (b);`), or
* it ends with a `RETURN` clause and already parses (so one-line queries do not need the `;`), or
* an empty line is entered.

A `;` inside a string (`'a;b'`) or inside a comment (`// ...`, `/* ... */`) does not end the
statement.

### `.load`
```
>>> .load "examples/demo_p2.cyphail"
[1/23] CREATE (a:Autor {nombre: 'Bill Gates'})
OK. Query executed in 554 ms.
...
OK. 23 statement(s) executed.
```
The file is split into statements (same rules as above) and they are executed in order, each one
preceded by `[n/total]` and its first line. Loading stops at the first statement that fails and
reports its number (`ERROR: .load stopped at statement 3 of 8.`). Relative paths are resolved from
the directory where the REPL was started.

### Results
A query with `RETURN` prints a table and `OK. N row(s) in X ms.`; a statement without `RETURN`
(a bare `CREATE`, `SET`, `DELETE`...) prints `OK. Query executed in X ms.`. Returning a node or
relationship variable (`RETURN a, r`) shows its **id**; `RETURN a.name` shows the property.
Errors are always one `ERROR: ...` line, in English: syntax errors, semantic errors, features not
supported yet, Prolog errors (e.g. deleting a node that still has relationships), query time-outs
(10 s) and a missing SWI-Prolog.

## Demo
`examples/demo_p2.cyphail` creates its own data and then runs the statements of the SPEC with
visible results: `CREATE` with and without `MATCH`, multi-path `MATCH`, relationships with
properties, `SET`, `REMOVE`, `DELETE` of a relationship, `DETACH DELETE`, and `AND`/`OR`/`NOT`
over the sample graph. Load it **once per REPL session** (the graph lives in memory while the REPL
is open):
```
>>> .load "examples/demo_p2.cyphail"
```
`examples/spec_p2.cyphail` holds the eight statements exactly as they appear in the SPEC; they all
parse and run, but several of them refer to data that does not exist in `amigos`, so they succeed
without matching anything.

## Supported Statements (Sprint P2)
```
MATCH pattern [, pattern]* [WHERE condition] [CREATE|SET|REMOVE|DELETE|DETACH DELETE ...]* [RETURN items]
CREATE pattern [CREATE|SET|REMOVE|DELETE ...]* [RETURN items]        -- no MATCH needed
```
* **Patterns:** nodes `(a:Label1:Label2 {key: value})` and relationships
  `-[r:TYPE {key: value}]->`, `<-[...]-`, `-[...]-` (direction OUT / IN / NONE), chained into paths
  `(a)-[r]->(b)-[s]->(c)`. The same node variable can be repeated across paths of a `MATCH`.
* **WHERE:** comparisons `=  <>  >  <  >=  <=` combined with `AND`, `OR`, `NOT` and parentheses.
* **Updates:** `CREATE` (new nodes/relationships, reusing nodes already bound by `MATCH`),
  `SET a.prop = value`, `REMOVE a.prop`, `DELETE x`, `DETACH DELETE x`.
* **RETURN:** variables, `a.prop`, with `AS` aliases.
* **Comments:** `//` and `/* */`.

### Semantic rules (checked by the Analyzer before anything reaches the engine)
* A variable must be declared in a pattern before it is used.
* A relationship variable cannot be repeated; the same name cannot be a node and a relationship.
* Variable names starting with `__` are reserved for the code generator.
* `DELETE` only accepts variables.

### Known limitations
* A property that does not exist makes the row disappear (there is no `null`).
* Alternative relationship types (`[:A|B]`) parse, but the code generator answers
  "Not supported yet"; `CREATE` of a relationship needs exactly one type and a direction.
* `DELETE` of a node that still has relationships is an error; use `DETACH DELETE`.
* The graph is in memory: it is lost when the REPL exits. Only the sample graph `amigos` exists.

## Project Structure
```
src/main/java/cr/ac/una/eif400/cyphail/
├── ast/            AST nodes: sealed interfaces + records (QueryNode, PathPattern, RelPattern, ...)
├── parser/         CyphailParser (combinator-based) and:
│   └── core/       the combinator library itself (Parser, Parsers, lexers, Ok/Fail)
├── validation/     Analyzer + SymbolTable (scope and naming rules) and SemanticException
├── codegen/        PrologCodegen: AST -> Prolog goal (PrologVariables, PrologTerms, PredicateNames)
├── broker/         EngineBroker (starts swipl), MqiClient (TCP/MQI protocol), MqiResponseParser,
│                   SwiplLocator, RowsTable
├── output/         TreePrinter (the .tree pretty-printer) and TablePrinter
├── frontend/       Repl, ReplStep (pure input logic), Statements (splits on ';'), Session
│   └── handlers/   the `.xxx` command handlers
└── model/          small value types (Author)
src/main/resources/prolog/cyphail_engine.pl    the engine: comparators, Rows formatting,
                                               query lifecycle and write operations
src/test/resources/prolog/cyphail_engine_tests.pl   plunit tests of the engine
examples/                                      demo_p2.cyphail and spec_p2.cyphail
```

### How a statement is executed
1. `CyphailParser` builds the AST (`.tree` prints it).
2. `Analyzer` checks scope and naming rules.
3. `PrologCodegen` produces one goal: it stores the query clause, runs it with `findall` and
   formats the result as a `Rows` string. `MATCH`/`WHERE` become `user_G_*` goals, `WHERE`
   comparisons become `cy_eq`/`cy_gt`..., and writes become `cy_create_node`, `cy_create_edge`,
   `cy_set_prop`, `cy_remove_prop`, `cy_delete`, `cy_detach_delete`.
4. `EngineBroker` sends the goal to the `swipl mqi` process over a local TCP connection.
5. `RowsTable` parses the returned text (header + quoted cells) and `TablePrinter` draws the table.

## Sources and Credits
* [SWI-Prolog](https://www.swi-prolog.org/) and its Machine Query Interface (`mqi` library) — the engine.
* [ascii-table](https://github.com/freva/ascii-table) library (com.github.freva) — used for tabular console output.
* [Gson](https://github.com/google/gson) library (com.google.code.gson) — used to read the MQI JSON responses.
* Project specification, grammar and REPL examples provided by Prof. Carlos Loría-Sáenz, EIF400-II-2026, UNA.
* The sample graph `amigos` and the eight statements in `examples/spec_p2.cyphail` come from the course SPEC.

## AI Usage Declaration
AI assistance was used during this project, always for understanding/studying and for reviewing
code that a team member wrote, never to generate the project unattended. The full history of
prompts and models used, by sprint and by student, is documented in
[PROMPTS.MD](./PROMPTS.MD), as required by the course policy on AI usage.
