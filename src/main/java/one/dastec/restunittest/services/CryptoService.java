package one.dastec.restunittest.services;

import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class CryptoService {

    private static final String ALGORITHM = "AES";
    // In a real production app, this should be loaded from a secure environment variable or vault
    private static final String DEFAULT_SECRET_KEY = "RestUnitTestSec!"; 

    public String encrypt(String value) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(DEFAULT_SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting value", e);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(DEFAULT_SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedValue));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting value", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith("{enc}");
    }

    public String decryptIfNeeded(String value) {
        if (isEncrypted(value)) {
            return decrypt(value.substring(5));
        }
        return value;
    }
}
