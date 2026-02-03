package com.example;

import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    
    private static final String DEFAULT_PASSPHRASE = "jmlekk";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            usage();
            return;
        }

        String pass = System.getenv().getOrDefault("VAULT_PASSPHRASE", DEFAULT_PASSPHRASE);
        char[] passphrase = pass.toCharArray();

        SecureVault vault = new SecureVault(Paths.get("vault"));

        String cmd = args[0];
        if ("save".equalsIgnoreCase(cmd)) {
            if (args.length < 3) {
                System.err.println("Usage: save clientId \"content\"");
                return;
            }
            String clientId = args[1];
           
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) sb.append(' ');
                sb.append(args[i]);
            }
            String content = sb.toString();

            vault.save(clientId, content, passphrase);
            System.out.println("[OK] Saved secure report for client: " + clientId);
        } else if ("read".equalsIgnoreCase(cmd)) {
            if (args.length != 2) {
                System.err.println("Usage: read clientId");
                return;
            }
            String clientId = args[1];
            try {
                String result = vault.read(clientId, passphrase);
                System.out.println("--- Report (client=" + clientId + ") ---");
                System.out.println(result);
                System.out.println("--- End ---");
            } catch (Exception e) {
                System.err.println("[ERROR] Could not read report: " + e.getMessage());
            }
        } else {
            usage();
        }
    }

    private static void usage() {
        System.out.println("Usage:\n  save clientId \"content\"  -> store encrypted+signed report\n  read clientId -> read report (fallback to backup if needed)");
        System.out.println("Passphrase source: env VAULT_PASSPHRASE or hardcoded default in code");
    }
}
