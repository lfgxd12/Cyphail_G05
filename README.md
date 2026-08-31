# Cyphail - Graph Query Engine Prototype

## Context
Project developed for the course **EIF400 Paradigmas de Programación (II-2026)**.  
Escuela de Informática, Universidad Nacional (UNA), Costa Rica.

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

## Sources and Credits
* [ascii-table](https://github.com/freva/ascii-table) library (com.github.freva) — used for tabular console output.
* Project specification and REPL examples provided by Prof. Carlos Loría-Sáenz, EIF400-II-2026, UNA.

## AI Usage Declaration
AI assistance (Gemini) was used during this sprint to help configure the
initial Maven setup (pom.xml) and to draft the cyphail.bat build script.
The full history of prompts used and the model involved is documented in
[PROMPTS.MD](./PROMPTS.MD), as required by the course policy on AI usage.
