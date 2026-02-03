package com.example;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Arrays;

public class SecureVault {
    private static final byte[] MAGIC = new byte[]{'S','V','L','T'};
    private static final byte VERSION = 0x01;
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12; // GCM standard
    private static final int PBKDF2_ITER = 65536;

    private static final SecureRandom RNG = new SecureRandom();

    private final Path vaultDir;

    public SecureVault(Path vaultDir) throws IOException {
        this.vaultDir = vaultDir;
        if (!Files.exists(vaultDir)) Files.createDirectories(vaultDir);
    }

    /**
     * Enregistre le rapport de façon simple : on dérive la clé, on chiffre avec AES/GCM,
     * puis on écrit un fichier binaire : MAGIC | VERSION | SALT | IV | CT_LEN | CT
     * On maintient une copie de secours en .dat.bak (simple rename/copy).
     */
    public void save(String clientId, String content, char[] passphrase) throws Exception {
        byte[] salt = randomBytes(SALT_LEN);
        byte[] iv = randomBytes(IV_LEN);

        // On dérive une clé AES de 32 octets (256 bits)
        byte[] aesKey = deriveKey(passphrase, salt, PBKDF2_ITER, 32);

        byte[] ciphertext = encrypt(aesKey, iv, content.getBytes("UTF-8"));

        // Compose le contenu du fichier
        ByteBuffer bb = ByteBuffer.allocate(4 + 1 + SALT_LEN + IV_LEN + 4 + ciphertext.length);
        bb.put(MAGIC);
        bb.put(VERSION);
        bb.put(salt);
        bb.put(iv);
        bb.putInt(ciphertext.length);
        bb.put(ciphertext);
        byte[] fileBytes = bb.array();

        Path mainFile = vaultDir.resolve(clientId + ".dat");
        Path tmpFile = vaultDir.resolve(clientId + ".dat.tmp");
        Path bakFile = vaultDir.resolve(clientId + ".dat.bak");

        // Ecriture simple : tmp -> main, et copie vers bak
        Files.write(tmpFile, fileBytes);
        if (Files.exists(mainFile)) {
            // copie simple de l'ancien vers le backup (remplace si existe)
            Files.copy(mainFile, bakFile, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmpFile, mainFile, StandardCopyOption.REPLACE_EXISTING);

        // Nettoyage sensible : efface la clé en mémoire (bonnes pratiques)
        zero(aesKey);
    }

    /**
     * Lit le rapport : on essaie le fichier principal, sinon la sauvegarde.
     */
    public String read(String clientId, char[] passphrase) throws Exception {
        Path mainFile = vaultDir.resolve(clientId + ".dat");
        Path bakFile = vaultDir.resolve(clientId + ".dat.bak");

        IOException lastEx = null;
        if (Files.exists(mainFile)) {
            try { return readFromFile(mainFile, passphrase); }
            catch (Exception e) { lastEx = new IOException("Main file invalid: " + e.getMessage(), e); }
        }
        if (Files.exists(bakFile)) {
            try { return readFromFile(bakFile, passphrase); }
            catch (Exception e) { lastEx = new IOException("Backup file invalid: " + e.getMessage(), e); }
        }
        if (lastEx != null) throw lastEx;
        throw new IOException("No vault file found for clientId: " + clientId);
    }

    private String readFromFile(Path file, char[] passphrase) throws Exception {
        byte[] all = Files.readAllBytes(file);
        ByteBuffer bb = ByteBuffer.wrap(all);

        byte[] magic = new byte[4]; bb.get(magic);
        if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1] || magic[2] != MAGIC[2] || magic[3] != MAGIC[3])
            throw new IOException("Bad magic");
        byte version = bb.get();
        if (version != VERSION) throw new IOException("Unsupported version: " + version);

        byte[] salt = new byte[SALT_LEN]; bb.get(salt);
        byte[] iv = new byte[IV_LEN]; bb.get(iv);
        int ctLen = bb.getInt();
        if (ctLen < 0 || ctLen > bb.remaining()) throw new IOException("Bad ciphertext length");
        byte[] ct = new byte[ctLen]; bb.get(ct);

        // Dérive la clé et déchiffre (AES/GCM vérifie l'intégrité automatiquement)
        byte[] aesKey = deriveKey(passphrase, salt, PBKDF2_ITER, 32);
        try {
            byte[] plain = decrypt(aesKey, iv, ct);
            return new String(plain, "UTF-8");
        } finally {
            zero(aesKey);
        }
    }

    /* ---- Utilitaires simples ---- */
    private static byte[] encrypt(byte[] key, byte[] iv, byte[] plain) throws Exception {
        SecretKeySpec k = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, k, spec);
        return cipher.doFinal(plain);
    }

    private static byte[] decrypt(byte[] key, byte[] iv, byte[] ct) throws Exception {
        SecretKeySpec k = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, k, spec);
        return cipher.doFinal(ct);
    }

    private static byte[] deriveKey(char[] pass, byte[] salt, int iter, int lenBytes) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pass, salt, iter, lenBytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private static byte[] randomBytes(int len) {
        byte[] b = new byte[len];
        RNG.nextBytes(b);
        return b;
    }

    private static void zero(byte[] b) { if (b != null) for (int i = 0; i < b.length; i++) b[i] = 0; }
}
