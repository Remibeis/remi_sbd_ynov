#!/bin/sh
# Simple helper to compile and run the demo (Linux / macOS)
rm -rf out
javac -d out src/main/java/com/example/*.java

echo "To save:   java -cp out com.example.Main save client1 \"Mon secret\""
echo "To read:   java -cp out com.example.Main read client1"

echo
printf "Set VAULT_PASSPHRASE for this shell (example):\n"
printf "  export VAULT_PASSPHRASE=monpass\n"
printf "Then run:\n  java -cp out com.example.Main read client1\n"
