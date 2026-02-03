# demo_cia — Secure Vault (simplified)

But : petit coffre-fort local pour stocker des rapports clients (chiffrement + intégrité + backup).

Raccourci d'utilisation (Windows PowerShell) :

1. Compiler :
   javac -d out src/main/java/com/example/*.java

2. Sauvegarder :
   java -cp out com.example.Main save client1 "Mon secret"

3. Lire :
   java -cp out com.example.Main read client1

Passphrase :
- Par défaut : `ChangeMeForExercise` (dans `Main.java`).
- Pour utiliser la vôtre (session PowerShell) :
  $env:VAULT_PASSPHRASE = 'monpass'
- Si `VAULT_PASSPHRASE` n'est pas défini, le programme demande la passphrase en saisie interactive (masquée si possible).

Fichiers :
- `SecureVault.java` : logique de chiffrement (AES/GCM) et backup `.dat.bak`.
- `Main.java` : interface CLI simple (save/read).

Si besoin, utilise `run.bat` (Windows) ou `run.sh` (Linux/macOS) pour automatiser compile/exécution.
