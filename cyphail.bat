@echo off

if not exist "target\cyphail.jar" (
    echo [ERROR] target\cyphail.jar not found. Please build the project first by running: mvn clean package
    exit /b 1
)

java -jar target\cyphail.jar %*