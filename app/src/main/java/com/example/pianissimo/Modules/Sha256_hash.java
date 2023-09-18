package com.example.pianissimo.Modules;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// 비밀번호 sha256 으로 hash
public class Sha256_hash {
    public static String hexString(String stringToHex) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(
                    stringToHex.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (int i = 0; i < encodedHash.length; i++) {
                String hex = Integer.toHexString(0xff & encodedHash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (Exception e){
            System.out.println(e);
            return null;
        }
    }
}
