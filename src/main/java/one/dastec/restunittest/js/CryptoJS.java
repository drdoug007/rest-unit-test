package one.dastec.restunittest.js;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CryptoJS {

    public Hash sha256() {
        return new Hash("SHA-256");
    }

    public Hash sha512() {
        return new Hash("SHA-512");
    }

    public Hmac hmac = new Hmac();

    public static class Hash {
        private MessageDigest digest;

        public Hash(String algorithm) {
            try {
                this.digest = MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        public Hash updateWithText(String textInput) {
            return updateWithText(textInput, "UTF-8");
        }

        public Hash updateWithText(String textInput, String encoding) {
            digest.update(textInput.getBytes(StandardCharsets.UTF_8));
            return this;
        }

        public Hash updateWithHex(String hexInput) {
            digest.update(hexToBytes(hexInput));
            return this;
        }

        public Hash updateWithBase64(String base64Input) {
            return updateWithBase64(base64Input, false);
        }

        public Hash updateWithBase64(String base64Input, boolean urlSafe) {
            if (urlSafe) {
                digest.update(Base64.getUrlDecoder().decode(base64Input));
            } else {
                digest.update(Base64.getDecoder().decode(base64Input));
            }
            return this;
        }

        public Digest digest() {
            return new Digest(digest.digest());
        }
    }

    public static class Digest {
        private byte[] result;

        public Digest(byte[] result) {
            this.result = result;
        }

        public String toHex() {
            StringBuilder hexString = new StringBuilder();
            for (byte b : result) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }

        public String toBase64() {
            return toBase64(false);
        }

        public String toBase64(boolean urlSafe) {
            if (urlSafe) {
                return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
            } else {
                return Base64.getEncoder().encodeToString(result);
            }
        }
    }

    public static class Hmac {
        public HmacInstance sha256() {
            return new HmacInstance("HmacSHA256");
        }

        public HmacInstance sha512() {
            return new HmacInstance("HmacSHA512");
        }

        public HmacInstance sha3(String bits) {
            return new HmacInstance("HmacSHA3-" + bits);
        }
    }

    public static class HmacInstance {
        private String algorithm;
        private byte[] secret;
        private byte[] data = new byte[0];

        public HmacInstance(String algorithm) {
            this.algorithm = algorithm;
        }

        public HmacInstance withTextSecret(String textSecret) {
            return withTextSecret(textSecret, "UTF-8");
        }

        public HmacInstance withTextSecret(String textSecret, String encoding) {
            this.secret = textSecret.getBytes(StandardCharsets.UTF_8);
            return this;
        }

        public HmacInstance withHexSecret(String hexSecret) {
            this.secret = hexToBytes(hexSecret);
            return this;
        }

        public HmacInstance withBase64Secret(String base64Input) {
            return withBase64Secret(base64Input, false);
        }

        public HmacInstance withBase64Secret(String base64Input, boolean urlSafe) {
            if (urlSafe) {
                this.secret = Base64.getUrlDecoder().decode(base64Input);
            } else {
                this.secret = Base64.getDecoder().decode(base64Input);
            }
            return this;
        }

        public HmacInstance updateWithText(String textInput) {
            byte[] newData = textInput.getBytes(StandardCharsets.UTF_8);
            appendData(newData);
            return this;
        }

        public HmacInstance updateWithHex(String hexInput) {
            byte[] newData = hexToBytes(hexInput);
            appendData(newData);
            return this;
        }

        public HmacInstance updateWithBase64(String base64Input) {
            return updateWithBase64(base64Input, false);
        }

        public HmacInstance updateWithBase64(String base64Input, boolean urlSafe) {
            byte[] newData;
            if (urlSafe) {
                newData = Base64.getUrlDecoder().decode(base64Input);
            } else {
                newData = Base64.getDecoder().decode(base64Input);
            }
            appendData(newData);
            return this;
        }

        private void appendData(byte[] newData) {
            byte[] combined = new byte[data.length + newData.length];
            System.arraycopy(data, 0, combined, 0, data.length);
            System.arraycopy(newData, 0, combined, data.length, newData.length);
            this.data = combined;
        }

        public Digest digest() {
            try {
                Mac mac = Mac.getInstance(algorithm);
                SecretKeySpec secretKey = new SecretKeySpec(secret, algorithm);
                mac.init(secretKey);
                return new Digest(mac.doFinal(data));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
