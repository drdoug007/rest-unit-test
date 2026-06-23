package one.dastec.restunittest.js;

import java.security.*;
import java.security.spec.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.graalvm.polyglot.Value;

public class SubtleCryptoJS {

    public static class CryptoKey {
        public final Key key;
        public final Map<String, Object> algorithm;
        public final boolean extractable;
        public final List<String> usages;

        public CryptoKey(Key key, Map<String, Object> algorithm, boolean extractable, List<String> usages) {
            this.key = key;
            this.algorithm = algorithm;
            this.extractable = extractable;
            this.usages = usages;
        }
    }

    public static class KeyPair {
        public final CryptoKey publicKey;
        public final CryptoKey privateKey;

        public KeyPair(CryptoKey publicKey, CryptoKey privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
    }

    public Object generateKey(Map<String, Object> algorithm, boolean extractable, List<String> keyUsages) throws Exception {
        String name = (String) algorithm.get("name");
        if ("RSA-PSS".equals(name) || "RSASSA-PKCS1-v1_5".equals(name) || "RSA-OAEP".equals(name)) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            int modulusLength = ((Number) algorithm.getOrDefault("modulusLength", 2048)).intValue();
            kpg.initialize(modulusLength);
            java.security.KeyPair kp = kpg.generateKeyPair();
            return new KeyPair(
                new CryptoKey(kp.getPublic(), algorithm, extractable, keyUsages),
                new CryptoKey(kp.getPrivate(), algorithm, extractable, keyUsages)
            );
        } else if ("ECDSA".equals(name)) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            String namedCurve = (String) algorithm.get("namedCurve"); // e.g., "P-256"
            String stdName = namedCurve;
            if ("P-256".equals(namedCurve)) stdName = "secp256r1";
            else if ("P-384".equals(namedCurve)) stdName = "secp384r1";
            else if ("P-521".equals(namedCurve)) stdName = "secp521r1";
            
            kpg.initialize(new ECGenParameterSpec(stdName));
            java.security.KeyPair kp = kpg.generateKeyPair();
            return new KeyPair(
                new CryptoKey(kp.getPublic(), algorithm, extractable, keyUsages),
                new CryptoKey(kp.getPrivate(), algorithm, extractable, keyUsages)
            );
        }
        throw new UnsupportedOperationException("Algorithm not supported: " + name);
    }

    public CryptoKey importKey(String format, Value keyData, Map<String, Object> algorithm, boolean extractable, List<String> keyUsages) throws Exception {
        byte[] bytes = toBytes(keyData);
        String name = (String) algorithm.get("name");
        KeyFactory kf;
        if (name.startsWith("RSA")) {
            kf = KeyFactory.getInstance("RSA");
        } else if ("ECDSA".equals(name)) {
            kf = KeyFactory.getInstance("EC");
        } else {
            throw new UnsupportedOperationException("Algorithm not supported: " + name);
        }

        Key key;
        if ("pkcs8".equals(format)) {
            key = kf.generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } else if ("spki".equals(format)) {
            key = kf.generatePublic(new X509EncodedKeySpec(bytes));
        } else {
            throw new UnsupportedOperationException("Format not supported: " + format);
        }

        return new CryptoKey(key, algorithm, extractable, keyUsages);
    }

    public byte[] exportKey(String format, CryptoKey key) {
        if (!key.extractable) throw new RuntimeException("Key is not extractable");
        if ("pkcs8".equals(format) || "spki".equals(format)) {
            return key.key.getEncoded();
        }
        throw new UnsupportedOperationException("Format not supported: " + format);
    }

    public byte[] sign(Map<String, Object> algorithm, CryptoKey key, Value data) throws Exception {
        byte[] bytes = toBytes(data);
        String name = (String) algorithm.get("name");
        String hash = (String) key.algorithm.get("hash");
        if (hash == null) hash = (String) algorithm.get("hash");
        if (hash != null) hash = hash.replace("-", "");

        if ("RSA-PSS".equals(name)) {
            Signature sig = Signature.getInstance("RSASSA-PSS");
            int saltLength = ((Number) algorithm.getOrDefault("saltLength", 32)).intValue();
            sig.setParameter(new PSSParameterSpec(hash, "MGF1", new MGF1ParameterSpec(hash), saltLength, 1));
            sig.initSign((PrivateKey) key.key);
            sig.update(bytes);
            return sig.sign();
        } else if ("RSASSA-PKCS1-v1_5".equals(name)) {
            Signature sig = Signature.getInstance(hash + "withRSA");
            sig.initSign((PrivateKey) key.key);
            sig.update(bytes);
            return sig.sign();
        } else if ("ECDSA".equals(name)) {
            Signature sig = Signature.getInstance(hash + "withECDSA");
            sig.initSign((PrivateKey) key.key);
            sig.update(bytes);
            return sig.sign();
        }
        throw new UnsupportedOperationException("Algorithm not supported: " + name);
    }

    public boolean verify(Map<String, Object> algorithm, CryptoKey key, Value signature, Value data) throws Exception {
        byte[] sigBytes = toBytes(signature);
        byte[] dataBytes = toBytes(data);
        String name = (String) algorithm.get("name");
        String hash = (String) key.algorithm.get("hash");
        if (hash == null) hash = (String) algorithm.get("hash");
        if (hash != null) hash = hash.replace("-", "");

        if ("RSA-PSS".equals(name)) {
            Signature sig = Signature.getInstance("RSASSA-PSS");
            int saltLength = ((Number) algorithm.getOrDefault("saltLength", 32)).intValue();
            sig.setParameter(new PSSParameterSpec(hash, "MGF1", new MGF1ParameterSpec(hash), saltLength, 1));
            sig.initVerify((PublicKey) key.key);
            sig.update(dataBytes);
            return sig.verify(sigBytes);
        } else if ("RSASSA-PKCS1-v1_5".equals(name)) {
            Signature sig = Signature.getInstance(hash + "withRSA");
            sig.initVerify((PublicKey) key.key);
            sig.update(dataBytes);
            return sig.verify(sigBytes);
        } else if ("ECDSA".equals(name)) {
            Signature sig = Signature.getInstance(hash + "withECDSA");
            sig.initVerify((PublicKey) key.key);
            sig.update(dataBytes);
            return sig.verify(sigBytes);
        }
        throw new UnsupportedOperationException("Algorithm not supported: " + name);
    }

    public byte[] encrypt(Map<String, Object> algorithm, CryptoKey key, Value data) throws Exception {
        byte[] bytes = toBytes(data);
        String name = (String) algorithm.get("name");
        if ("RSA-OAEP".equals(name)) {
            String hash = (String) key.algorithm.get("hash");
            if (hash == null) hash = "SHA-1";
            hash = hash.replace("-", "");
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWith" + hash + "AndMGF1Padding");
            OAEPParameterSpec oaepSpec = new OAEPParameterSpec(hash, "MGF1", new MGF1ParameterSpec(hash), PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.ENCRYPT_MODE, (Key) key.key, oaepSpec);
            return cipher.doFinal(bytes);
        }
        throw new UnsupportedOperationException("Algorithm not supported: " + name);
    }

    public byte[] decrypt(Map<String, Object> algorithm, CryptoKey key, Value data) throws Exception {
        byte[] bytes = toBytes(data);
        String name = (String) algorithm.get("name");
        if ("RSA-OAEP".equals(name)) {
            String hash = (String) key.algorithm.get("hash");
            if (hash == null) hash = "SHA-1";
            hash = hash.replace("-", "");
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWith" + hash + "AndMGF1Padding");
            OAEPParameterSpec oaepSpec = new OAEPParameterSpec(hash, "MGF1", new MGF1ParameterSpec(hash), PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, (Key) key.key, oaepSpec);
            return cipher.doFinal(bytes);
        }
        throw new UnsupportedOperationException("Algorithm not supported: " + name);
    }

    private byte[] toBytes(Object obj) {
        if (obj == null) return null;
        if (obj instanceof byte[]) return (byte[]) obj;
        if (obj instanceof Value) {
            Value v = (Value) obj;
            if (v.hasArrayElements()) {
                long size = v.getArraySize();
                byte[] arr = new byte[(int) size];
                for (int i = 0; i < size; i++) {
                    Value element = v.getArrayElement(i);
                    if (element.isNumber()) {
                        arr[i] = (byte) element.asInt();
                    }
                }
                return arr;
            }
            if (v.isString()) return v.asString().getBytes();
        }
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            byte[] arr = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof Number) {
                    arr[i] = ((Number) item).byteValue();
                }
            }
            return arr;
        }
        if (obj instanceof String) return ((String) obj).getBytes();
        throw new IllegalArgumentException("Cannot convert to bytes: " + obj.getClass().getName());
    }
}
