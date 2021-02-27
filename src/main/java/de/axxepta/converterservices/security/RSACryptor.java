package de.axxepta.converterservices.security;

import de.axxepta.converterservices.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSACryptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RSACryptor.class);

    private static final String PUBLIC_KEY_FILE = "de/axxepta/converterservices/security/publicKey";
    private static final String PRIVATE_KEY_FILE = "de/axxepta/converterservices/security/privateKey";

    private static PublicKey publicKey;
    private static PrivateKey privateKey;

    private static boolean publicRead = false;
    private static boolean privateRead = false;

    private static PublicKey getPublicKey() {
        if (publicRead) {
            return publicKey;
        }
        try {
            byte[] publicKey = IOUtils.getResourceAsBytes(PUBLIC_KEY_FILE, RSACryptor.class);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSACryptor.publicKey = keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            RSACryptor.publicKey = null;
        }
        publicRead = true;
        return RSACryptor.publicKey;
    }

    private static PrivateKey getPrivateKey() {
        if (privateRead) {
            return privateKey;
        }
        try {
            byte[] privateKey = IOUtils.getResourceAsBytes(PRIVATE_KEY_FILE, RSACryptor.class);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSACryptor.privateKey = keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            RSACryptor.privateKey = null;
        }
        privateRead = true;
        return RSACryptor.privateKey;
    }

    public static String encrypt(String data)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKey());
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
    }

    private static String decrypt(byte[] data)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, getPrivateKey());
        return new String(cipher.doFinal(data));
    }

    public static String decrypt(String data)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        return decrypt(Base64.getDecoder().decode(data.getBytes()));
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            try {
                String encryptedString = encrypt(args[0]);
                System.out.println(encryptedString);
                System.out.println(decrypt(encryptedString));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
