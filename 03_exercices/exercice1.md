## Exercice – “Secure Vault : coffre-fort de rapports clients”

### Mise en situation

Développer un mini “coffre-fort” local pour stocker des **rapports clients** (texte) sur disque. Ces rapports 
contiennent des informations sensibles.

### Objectif

Implémenter les 3 axes de la triade CIA :

* **Confidentialité** : le rapport ne doit pas être lisible sur disque.
* **Intégrité** : toute modification du fichier doit être détectée.
* **Disponibilité** : la lecture doit rester possible même si le fichier principal est indisponible (supprimé/corrompu).

### Travail demandé (énoncé)

Écrire une application console Java qui :

1. **Enregistre** un rapport (`clientId`, `content`) dans un fichier sur disque.
2. **Chiffre** le contenu avant écriture (confidentialité).
3. **Signe** (ou authentifie) les données stockées pour détecter toute altération (intégrité).
4. Maintient **une copie de secours** et, lors de la lecture, tente :

    * lecture depuis le fichier principal,
    * sinon bascule automatiquement sur la copie de secours (disponibilité).
5. Fournit deux commandes (ou deux exécutions dans le `main`) :

    * `save clientId "..."` : sauvegarde sécurisée
    * `read clientId` : lecture sécurisée (avec bascule si besoin)

Contraintes :

* Pas de dépendances externes (JDK uniquement).
* Choix du format (binaire ou texte) tant qu’il est reproductible.
* La clé doit provenir d’un **secret fourni** (ex : passphrase en dur dans le code pour l’exercice, ou argument CLI).

---

## Solution fournie (implémentation)

J'ai ajouté une petite application Java dans `02_demos/demo_cia` :

- `com.example.SecureVault` : logique de stockage chiffré + HMAC + sauvegarde de secours
- `com.example.Main` : CLI minimal `save clientId "content"` et `read clientId`

Choix techniques (résumé) :
- Confidentialité : AES-256-GCM (clé dérivée via PBKDF2WithHmacSHA256)
- Intégrité/Authenticité : HMAC-SHA256 sur (salt || iv || ciphertext)
- Disponibilité : fichier principal `vault/<clientId>.dat` + copie `vault/<clientId>.dat.bak` (bascule automatique à la lecture)
- Format : binaire simple. Structure du fichier : MAGIC|VERSION|SALT|IV|CT_LEN|CT|HMAC

Comment tester/run (sans Maven) :
1. Compiler :

   - Ouvrir un terminal dans le dossier `02_demos/demo_cia` et exécuter :

     javac -d out src/main/java/com/example/*.java

2. Exécuter :

   - Sauvegarder :
     java -cp out com.example.Main save client1 "Contenu secret du client"

   - Lire :
     java -cp out com.example.Main read client1

3. Passphrase :
   - Par défaut l'application utilise la passphrase codée `ChangeMeForExercise`.
   - Pour utiliser une passphrase différente : définissez la variable d'environnement `VAULT_PASSPHRASE` avant d'exécuter les commandes.

Remarques :
- Maven n'est pas disponible dans l'environnement d'exécution ici, donc j'ai fourni la commande `javac` pour compiler directement avec le JDK.
- Le code est volontairement simple et commenté ; il respecte la contrainte "JDK uniquement".

---

Si tu veux, je peux :
1) ajouter des tests unitaires, 2) fournir un petit script `run.bat`/`run.sh` pour automatiser la compilation/exécution, ou 3) ajouter la gestion d'un mot de passe saisi de façon interactive (masqué) au lieu d'une variable d'environnement.

