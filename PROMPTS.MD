# Historial de Prompts - Cyphail G05

## Sprint P1.1
* **Modelo utilizado:** Gemini
* **Propósito:** Configuración de arquitectura inicial, archivo pom.xml y scripts de compilación.

### Registro
1. Configuración de dependencias Maven e integración del plugin Shade para empaquetado Fat JAR en Java 26.
2. Creación del script ejecutable batch (`cyphail.bat`) y estructuración de paquetes segun la arquitectura del curso.

## Sprint P1
* **Modelo utilizado:** Claude
* **Propósito:** Asistente de revisión de código y casos de prueba, no de generación autónoma.

### Registro
1. **Análisis inicial:** lectura completa del repositorio, los dos SPEC (inicial y P1
   preliminar), la gramática `.g4`, el archivo de casos de prueba del profesor, la guía de
   revisión y las notas de los estudiantes 2 y 3, para diagnosticar qué faltaba y priorizar.
2. **Combinadores de parsing** (`Parsers.java`): se pidió ayuda para entender e implementar
   `FlatMap`, `Right`, `Left`, `Between`, `SepBy1`, `SepBy` y `Choice`, siguiendo el modelo
   funcional de parser combinators visto en el curso. Se agregaron pruebas unitarias
   (`ParsersTest`).
3. **Lexer** (`CyphailLexers.java`): se pidió ayuda para agregar los tokens de `CREATE`,
   `DELETE`, `DETACH`, `{`, `}`, `<=`, `>=`, para que se ignoren los comentarios `//` y `/* */`
   de la gramática, y para un `Eof()` que detecta cuándo sobra texto sin parsear. Se corrigió un
   error existente en el orden de los operadores de comparación (`<>` se leía como `<` seguido de
   un `>` suelto). Se agregaron pruebas (`CyphailLexersTest`).
4. **AST** (`ast/*.java`): se pidió ayuda para extender el modelo de árbol (heredado del
   Estudiante 2) de modo que soportara varios patrones de nodo por `MATCH`, varias etiquetas,
   propiedades `{clave: valor}` y las cláusulas `CREATE`/`DELETE`, siguiendo el estilo ya usado
   por el equipo (`sealed interface` + `record`, `Optional` en vez de `null`).
5. **Parser** (`CyphailParser.java`): se pidió ayuda para reescribir el parser (heredado del
   Estudiante 2, que solo soportaba un patrón simple) como composición de los combinadores del
   punto 2, siguiendo la gramática regla por regla, para cubrir los casos de prueba C1-C11 del
   profesor.
6. **`.tree` / `TreePrinter`**: se pidió ayuda para reescribir la impresión del árbol (heredada
   del Estudiante 2, en un formato distinto) al formato exacto de "pretty-print tipo JSON" que
   pide el SPEC-Sprint-P1 (página 2), con las expresiones en preorden.
7. **Validador de variables** (`VariableScopeChecker.java`): se pidió ayuda para corregir el
   validador (heredado del Estudiante 3, que solo soportaba un patrón y se detenía en el primer
   error) para que recorra la consulta en orden, reporte todos los errores encontrados y cubra
   los casos C8 a C11.
8. **Pruebas del parser y del validador** (`ParserTest`, `ValidationTest`, `TreePrinterTest`): se
   pidió ayuda para escribir las pruebas correspondientes a los 11 casos de prueba oficiales del
   profesor, reemplazando las pruebas `@Disabled` que había dejado el Estudiante 3.
9. **REPL de varias líneas** (`MultilineInput.java`, `Repl.java`): se pidió ayuda para que el
   REPL acepte consultas repartidas en varias líneas (como las escribe el profesor en el
   documento de casos de prueba y en el ejemplo del SPEC), sin cambiar el comportamiento para una
   consulta de una sola línea. Se agregaron pruebas (`MultilineInputTest`).
10. **Documentación:** se pidió ayuda para redactar este archivo y el `README.md` del proyecto.

### Integración final del REPL y datos fake

* **Modelo utilizado:** ChatGPT (GPT-5.6)
* **Propósito:** Asistencia para revisar la implementación existente, integrar los componentes del REPL y desarrollar pruebas de integración.

### Registro
1. **`JsonFakeBroker`**: se solicitó ayuda para implementar la lectura de `data/test_cases.json` mediante Gson, normalizar las consultas y generar respuestas de tipo tabla o confirmación.
2. **Integración del REPL** (`Repl.java`): se solicitó ayuda para conectar el flujo `CyphailParser -> VariableScopeChecker -> JsonFakeBroker`, evitando que consultas con errores sintácticos o semánticos lleguen al broker.
3. **Casos C3-C9** (`data/test_cases.json`): se solicitó ayuda para agregar los datos fake correspondientes a los casos oficiales, incluyendo respuestas tabulares y confirmaciones para operaciones de actualización.
4. **Comando `.use`** (`UseHandler.java`): se solicitó ayuda para eliminar la lista de grafos hardcodeada y cargar la información desde `data/graphs.json` mediante Gson.
5. **Pruebas** (`JsonFakeBrokerTest`, `ReplIntegrationTest`): se solicitó ayuda para crear pruebas del broker y de la integración del REPL, incluyendo consultas con `RETURN`, confirmaciones de `CREATE`, errores sintácticos y variables no declaradas.
6. **Revisión y depuración**: se utilizó el asistente para revisar errores de compilación, configuración de Java 26 y verificar la integración con la estructura existente del repositorio.






## Sprint P2

### Luis Felipe Jiménez Fernández
* **Modelo utilizado:** Claude (Anthropic) Sonnet 5.5
* **Propósito:** Asistente de diseño, revisión, depuración y apoyo en la escritura de código. Todas las decisiones de diseño, la integración, las pruebas y la supervisión del resultado fueron mías.

### Registro
1. **Diseño de la tubería:** se conversó el flujo REPL -> Parser -> Analyzer -> Codegen -> Broker (MQI) -> engine SWI-Prolog -> tabla, el esquema del grafo en Prolog según el SPEC (user_G_node/label/edge/prop), el formato del texto "Rows" y los contratos entre los cuatro miembros.
2. **AST nuevo** (`ast/*.java`): relaciones, caminos, dirección, SET, REMOVE, DELETE y MATCH opcional.
3. **Codegen** (`codegen/*`: PrologCodegen, PrologVariables, PrologTerms, PredicateNames, CodegenResult): traducción del AST a un goal Prolog, tanto de lectura (MATCH/WHERE/RETURN) como de escritura (CREATE con y sin MATCH, SET, REMOVE, DELETE, DETACH DELETE). Se comprobó ejecutando los goals generados contra SWI-Prolog real.
4. **Analyzer y SymbolTable** (`validation/*`): alcance de variables y las reglas nuevas del SPEC (variable de relación repetida, mismo nombre como nodo y relación, prefijo `__` reservado), más el rechazo de DELETE sobre algo que no es una variable.
5. **Broker** (`broker/MqiClient`, `EngineBroker`, `SwiplLocator`, `RowsTable`, `GoalRunner`, `EngineException`): cliente del protocolo MQI sobre TCP (marcos de bytes en UTF-8, autenticación, timeout), arranque y cierre del proceso swipl, búsqueda del ejecutable y lectura del texto Rows. Estas clases estaban asignadas a Jostin; por acuerdo con él se desarrollaron en esta sesión para agilizar la integración, y se le informó de los cambios.
6. **REPL conectado** (`frontend/Repl`, `Session`, `ReplStep`, `Statements`, `handlers/UseHandler`): ejecución real contra el engine, `;` como terminador, comando `.load`, `.use` contra el engine y núcleo funcional (`ReplStep.step`, sin entrada/salida). Se eliminó el fake de P1 (JsonFakeBroker, FakeQueryHandler, data/*.json).
7. **Operaciones de escritura del engine** (`cyphail_engine.pl`: cy_create_node, cy_create_edge, cy_set_prop, cy_remove_prop, cy_delete, cy_detach_delete) y sus pruebas plunit. Esta parte pertenece al área de Angel; se integró en esta sesión y se le informará de los cambios.
8. **Errores encontrados en pruebas de integración:** (a) una palabra clave dentro de un comentario se leía como token en el lexer (`CyphailLexers`, archivo de Jose); (b) OR devolvía filas repetidas (se corrigió con once/1 en el Codegen); (c) condición de carrera al conectar con el engine recién arrancado (reintentos en `MqiClient`); (d) el MQI aplana los errores que no vienen envueltos en error/2, por lo que los errores del engine se lanzan como error(cy_error(...), _).
9. **Pruebas:** ayuda para escribir las pruebas JUnit de Codegen, Analyzer, broker, REPL y lexer, y las pruebas plunit del engine.
10. **Documentación y demo:** README de P2 y el script `examples/demo_p2.cyphail`.
11. **Revisión de código y apoyo en la integración** del trabajo de Jose, Jostin y Angel.

### Jose David Chavarria Villalobos
* **Modelo utilizado:** Claude (Anthropic) Sonnet 5.5
* **Propósito:** Asistente para escribir el código y las pruebas del parser de P2 (CREATE sin MATCH, SET, AND/OR/NOT, literales true/false) y para explicarme el código. Yo verifiqué que todo funcionara: corrí las pruebas y revisé el resultado.

### Registro
1. **Lexer** (`CyphailLexers.java`): tokens nuevos para P2 (relaciones, `SET`, `AND`, `OR`, `NOT` y decimales). Se desarrolló a la par con la IA. Pruebas en `CyphailLexersTest`.
2. **Parser de relaciones** (`CyphailParser.java`): patrones `(a)-[r:TIPO {p: v}]->(b)` con las tres direcciones (OUT, IN y NONE) y propiedades inline. Se desarrolló a la par con la IA. Pruebas en `RelationshipParserTest`.
3. **MATCH opcional** (`CyphailParser.java`, `query()`): una consulta puede empezar con `CREATE`, `SET`, `DELETE` o `REMOVE` sin `MATCH`. Se decide por la primera palabra, y sin `MATCH` se exige al menos una cláusula de actualización, de modo que `RETURN 1` y la entrada vacía siguen siendo error. Pruebas de las 8 sentencias del SPEC en `SpecStatementsParserTest`.
4. **SET** (`setItem`, `setClause`): `SET p.precio = 19.99, p.nombre = 'x'` dentro de las cláusulas de actualización, en el orden en que se escriben.
5. **Condiciones** (`expression`, `leftAssociative`, `lazyExpression`): `AND`, `OR`, `NOT` y paréntesis con precedencia NOT > AND > OR, asociativos a la izquierda (se arman con `reduce`). `AND` y `OR` se guardan como `"AND"` y `"OR"` en mayúscula, que es el contrato con el Codegen. Pruebas en `ConditionParserTest`.
6. **Palabras reservadas** (`RESERVED`, `variableName`): una palabra reservada ya no se lee como variable.
7. **Literales `true` y `false`** (`booleanLiteral`): se guardan como `LiteralExpr("true")` y `LiteralExpr("false")`, sin importar mayúsculas. Se comprobaron los 13 casos unitarios del profesor y se agregó `ProfessorP2CasesParserTest`.
8. **Errores encontrados al probar:** (a) `WHERE NOT` y `... AND RETURN` se aceptaban, porque `Id()` reconoce cualquier palabra como identificador; (b) `true` se leía como una variable y el Analyzer marcaba `Undefined variable 'true'` en los casos 8 y 10 del profesor.
9. **Verificación:** corrí `mvn test` con todas las pruebas del proyecto en verde antes de subir los cambios.
10. **Explicación y revisión:** la IA me explicó el AST, los combinadores y `CyphailParser`, y se usó para revisar la rama de Luis y para configurar Java 26, Maven y git.
11. **Nota:** Luis corrigió en `CyphailLexers` un problema con palabras clave dentro de comentarios (ver Luis, punto 8a). Jose fue informado.

### Jostin Jiménez Alfaro
* **Modelo utilizado:** [COMPLETAR: herramienta de IA, o "no se usó IA"]
* **Propósito:** [COMPLETAR]

### Registro
1. **Resultados del broker:** `PrologResult`, `PrologCompound` y `MqiResponseParser` (incluido el caso en que un goal que falla llega como el string "false") y sus pruebas.
2. [COMPLETAR: en qué puntos se usó IA y cómo]
3. Nota: las clases restantes del broker (MqiClient, EngineBroker, SwiplLocator, RowsTable) se desarrollaron junto con Luis (ver Luis, punto 5). Jostin conoce los cambios.

### Angel Rojas Ruano
* **Modelo utilizado:** Claude (Anthropic) Sonnet 5
* **Propósito:** Apoyo para redactar el código y las pruebas de las piezas del engine en SWI-Prolog que me tocaron (Pasos 1 a 3 de la tarjeta de la Persona 4). El código fue revisado, ejecutado con `swipl` y subido por mí.

### Registro
1. **Comparadores y formato de Rows** (commit 99a87c0): la IA me ayudó a redactar `cy_eq`, `cy_neq`, `cy_gt`, `cy_lt`, `cy_gte` y `cy_lte`, que fallan en silencio cuando los valores no son números, y `cy_cell`, `cy_escapar` y `cy_format_rows`, que arman el texto Rows del contrato C2 (celdas entre comillas, con `\`, `"` y salto de línea escapados).
2. **Ciclo de vida de la consulta** (commit d59d6f7): me apoyó para redactar `cy_create_query`, `cy_exec_query` y `cy_erase_query`, con id único por `gensym`, la cláusula guardada en el hecho dinámico `cy_stored/2` y las filas recolectadas con `findall` (sin soluciones devuelve solo el encabezado, y sin columnas devuelve el texto vacío).
3. **Pruebas plunit** (commit 94b7a5d, `cyphail_engine_tests.pl`): me ayudó a redactar las pruebas de los comparadores, del formato de Rows y de las cuatro consultas esperadas de la tarjeta contra el grafo `amigos` (incluidos el caso de 0 filas y el borrado de la cláusula), más una prueba que comprueba que el engine solo imprime el puerto y la contraseña al arrancar con `mqi_start`.
4. **Problemas encontrados al probar:** (a) la tarjeta sugiere arrancar el engine con el argumento `mqi`, pero no funcionó en el SWI-Prolog 9.0.4 donde se probó primero; la prueba usa `-g mqi_start -t halt -- --write_connection_values=true` y también pasó en la 10.0.2; (b) la ruta al engine dentro de la prueba dependía del directorio desde el que se corriera `swipl`, y me ayudó a cambiarla para resolverla respecto al archivo de pruebas.
5. **Verificación:** corrí las pruebas con SWI-Prolog 10.0.2 (`swipl -g run_tests -t halt`, 25 de 25 aprobadas) antes de subir el último commit.
6.**Nota:** las operaciones de escritura del engine (`cy_create_node`, `cy_create_edge`, `cy_set_prop`, `cy_remove_prop`, `cy_delete` y `cy_detach_delete`, commit f29cf21) no las escribí yo; las desarrolló Luis sobre este mismo archivo.
### Registro
1. **Engine Prolog (`cyphail_engine.pl`):** comparadores cy_eq, cy_neq, cy_gt, cy_lt, cy_gte, cy_lte; formato del texto Rows (cy_cell, cy_format_rows); ciclo de vida de la consulta (cy_create_query, cy_exec_query, cy_erase_query).
2. **Pruebas plunit** del engine.
3. [COMPLETAR: en qué puntos se usó IA y cómo]
4. Nota: las operaciones de escritura del engine se integraron junto con Luis (ver Luis, punto 7); Angel fue informado de los cambios.