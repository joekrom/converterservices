package de.axxepta.converterservices.security;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class RSAKeyPairGenerator {

    private RSAKeyPairGenerator() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();
            writeToFile("publicKey", publicKey.getEncoded());
            writeToFile("privateKey", privateKey.getEncoded());
        } catch (Exception ignored) {}
    }

    public void writeToFile(String path, byte[] key) {
        try {
            Files.write(Paths.get(path), key);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        new RSAKeyPairGenerator();
    }
}
