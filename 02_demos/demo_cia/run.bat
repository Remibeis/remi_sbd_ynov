@echo off
REM Simple helper to compile and run the demo (Windows PowerShell/Command Prompt)
if exist out rmdir /s /q out
javac -d out src/main/java/com/example/*.java

echo.
echo To save:   java -cp out com.example.Main save client1 "Mon secret"
echo To read:   java -cp out com.example.Main read client1

echo.
echo Example (set passphrase for this session):
set VAULT_PASSPHRASE=monpass
java -cp out com.example.Main read client1
set VAULT_PASSPHRASE=
echo Done
pause