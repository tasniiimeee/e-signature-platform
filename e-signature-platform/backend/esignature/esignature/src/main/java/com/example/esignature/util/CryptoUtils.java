package com.example.esignature.util;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Base64;

@Slf4j
public class CryptoUtils {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256;
    private static final int ITERATION_COUNT = 65536;

    public static KeyPair generateRSAKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.generateKeyPair();
    }

    public static String encryptPrivateKey(PrivateKey privateKey, String passphrase) throws Exception {
        byte[] salt = generateSalt();
        SecretKey secretKey = deriveKey(passphrase, salt);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] iv = generateIV();
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
        
        byte[] encryptedKey = cipher.doFinal(privateKey.getEncoded());
        
        // Combine salt + iv + encrypted data
        byte[] combined = new byte[salt.length + iv.length + encryptedKey.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(encryptedKey, 0, combined, salt.length + iv.length, encryptedKey.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }

    public static byte[] decryptPrivateKey(String encryptedKeyStr, String passphrase) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedKeyStr);
        
        // Extract salt, iv, and encrypted data
        byte[] salt = new byte[16];
        byte[] iv = new byte[16];
        byte[] encryptedKey = new byte[combined.length - 32];
        
        System.arraycopy(combined, 0, salt, 0, 16);
        System.arraycopy(combined, 16, iv, 0, 16);
        System.arraycopy(combined, 32, encryptedKey, 0, encryptedKey.length);
        
        SecretKey secretKey = deriveKey(passphrase, salt);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        
        return cipher.doFinal(encryptedKey);
    }

    private static SecretKey deriveKey(String passphrase, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_SIZE);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static byte[] generateIV() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
}