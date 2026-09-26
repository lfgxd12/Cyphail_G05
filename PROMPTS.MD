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