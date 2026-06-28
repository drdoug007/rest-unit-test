package one.dastec.restunittest.js;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;
import java.util.List;

public class JwtJS {

    public String sign(Map<String, Object> payload, Object key, Map<String, Object> options) {
        String algName = (String) options.getOrDefault("algorithm", "HS256");
        Algorithm algorithm = getAlgorithm(algName, key);

        com.auth0.jwt.JWTCreator.Builder builder = JWT.create();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) builder.withClaim(entry.getKey(), (String) value);
            else if (value instanceof Integer) builder.withClaim(entry.getKey(), (Integer) value);
            else if (value instanceof Long) builder.withClaim(entry.getKey(), (Long) value);
            else if (value instanceof Double) builder.withClaim(entry.getKey(), (Double) value);
            else if (value instanceof Boolean) builder.withClaim(entry.getKey(), (Boolean) value);
            else if (value instanceof List) builder.withClaim(entry.getKey(), (List<?>) value);
            else if (value instanceof Map) builder.withClaim(entry.getKey(), (Map<String, ?>) value);
        }

        if (options.containsKey("issuer")) builder.withIssuer((String) options.get("issuer"));
        if (options.containsKey("audience")) {
            Object aud = options.get("audience");
            if (aud instanceof String) builder.withAudience((String) aud);
            else if (aud instanceof List) builder.withAudience(((List<String>) aud).toArray(new String[0]));
        }
        if (options.containsKey("expiresIn")) {
             // expiresIn is usually in seconds or a string like '1h' in node-jsonwebtoken
             // for simplicity, assume it's number of seconds
             long seconds = ((Number) options.get("expiresIn")).longValue();
             builder.withExpiresAt(new Date(System.currentTimeMillis() + seconds * 1000));
        }

        return builder.sign(algorithm);
    }

    public boolean verify(String token, Object key, Map<String, Object> options) {
        String algName = (String) options.getOrDefault("algorithm", "HS256");
        Algorithm algorithm = getAlgorithm(algName, key);

        com.auth0.jwt.interfaces.Verification verification = JWT.require(algorithm);
        if (options.containsKey("issuer")) verification.withIssuer((String) options.get("issuer"));
        if (options.containsKey("audience")) {
            Object aud = options.get("audience");
            if (aud instanceof String) verification.withAudience((String) aud);
            // java-jwt withAudience for verification takes a single string or multiple.
        }

        try {
            JWTVerifier verifier = verification.build();
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> decode(String token) {
        DecodedJWT jwt = JWT.decode(token);
        return Map.of(
            "header", jwt.getHeader(),
            "payload", jwt.getClaims()
        );
    }

    private Algorithm getAlgorithm(String name, Object key) {
        return switch (name) {
            case "HS256" -> Algorithm.HMAC256(getKeyBytes(key));
            case "HS384" -> Algorithm.HMAC384(getKeyBytes(key));
            case "HS512" -> Algorithm.HMAC512(getKeyBytes(key));
            case "RS256" -> Algorithm.RSA256((RSAPublicKey) getPublicKey(key), (RSAPrivateKey) getPrivateKey(key));
            case "RS384" -> Algorithm.RSA384((RSAPublicKey) getPublicKey(key), (RSAPrivateKey) getPrivateKey(key));
            case "RS512" -> Algorithm.RSA512((RSAPublicKey) getPublicKey(key), (RSAPrivateKey) getPrivateKey(key));
            case "PS256" ->
                    Algorithm.RSA256((RSAPublicKey) getPublicKey(key), (RSAPrivateKey) getPrivateKey(key)); // java-jwt handles PSS via different means if needed, but RSA256 is often used
            case "ES256" -> Algorithm.ECDSA256((ECPublicKey) getPublicKey(key), (ECPrivateKey) getPrivateKey(key));
            case "ES384" -> Algorithm.ECDSA384((ECPublicKey) getPublicKey(key), (ECPrivateKey) getPrivateKey(key));
            case "ES512" -> Algorithm.ECDSA512((ECPublicKey) getPublicKey(key), (ECPrivateKey) getPrivateKey(key));
            default -> throw new UnsupportedOperationException("Algorithm not supported: " + name);
        };
    }

    private byte[] getKeyBytes(Object key) {
        if (key instanceof String) return ((String) key).getBytes();
        if (key instanceof byte[]) return (byte[]) key;
        throw new IllegalArgumentException("Key must be String or byte[] for HMAC");
    }

    private Object getPublicKey(Object key) {
        if (key instanceof SubtleCryptoJS.CryptoKey) return ((SubtleCryptoJS.CryptoKey) key).key;
        if (key instanceof SubtleCryptoJS.KeyPair) return ((SubtleCryptoJS.KeyPair) key).publicKey.key;
        return key;
    }

    private Object getPrivateKey(Object key) {
        if (key instanceof SubtleCryptoJS.CryptoKey) return ((SubtleCryptoJS.CryptoKey) key).key;
        if (key instanceof SubtleCryptoJS.KeyPair) return ((SubtleCryptoJS.KeyPair) key).privateKey.key;
        return key;
    }
}
