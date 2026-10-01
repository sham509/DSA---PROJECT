@echo off
cd /d "%~dp0"
if not exist "out" mkdir out
javac -d out src/com/plagiarism/*.java
java -cp out com.plagiarism.Main
