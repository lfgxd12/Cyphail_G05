@echo off
REM Cyphail - Graph Query Engine Prototype
REM EIF400-II-2026 - Escuela de Informatica, UNA
REM Grupo: G05
REM Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
REM Jostin Jimenez Alfaro, Angel Rojas Ruano

if not exist "target\cyphail.jar" (
    echo [ERROR] target\cyphail.jar not found. Please build the project first by running: mvn clean package
    exit /b 1
)

java -jar target\cyphail.jar %*