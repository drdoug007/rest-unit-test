package one.dastec.restunittest.services;

import one.dastec.restunittest.RestUnitTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = RestUnitTestApplication.class)
public class CryptoReproductionTest {

    @Autowired
    private RestTestService restTestService;

    @Test
    public void testHashAndHmac() {
        String content = """
                < {%
                    const hash = crypto.sha256()
                        .updateWithText("hello")
                        .digest().toHex();
                    client.log("DEBUG: hash=" + hash);
                    client.test("SHA-256 hash", () => {
                        client.assert(hash === "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", "Hash mismatch: " + hash);
                    });

                    const hmac = crypto.hmac.sha256()
                        .withTextSecret("secret")
                        .updateWithText("hello")
                        .digest().toHex();
                    client.log("DEBUG: hmac=" + hmac);
                    client.test("HMAC-SHA-256", () => {
                        client.assert(hmac === "88a52fcca9937d87b4494191d3570619e73f09ca13987bcacc3b0d604555845c", "HMAC mismatch: " + hmac);
                    });
                %}
                GET https://httpbin.org/get
                """;
        
        String report = restTestService.runTestWithContent("CryptoTest", content);
        System.out.println("[DEBUG_LOG] Hash report: " + report);
        assertTrue(report.contains("✅ SHA-256 hash"), "SHA-256 hash failed. Report: " + report);
        assertTrue(report.contains("✅ HMAC-SHA-256"), "HMAC-SHA-256 failed. Report: " + report);
    }

    @Test
    public void testSubtleCryptoRSA() {
        String content = """
                < {%
                    const keyPair = crypto.subtle.generateKey({
                                name: "RSA-PSS",
                                modulusLength: 2048,
                                publicExponent: new Uint8Array([1, 0, 1]),
                                hash: "SHA-256"
                            },
                            true,
                            ["sign", "verify"]);
                    const text = "Hello, HTTP Client Pre Script!!!";
                    const data = string2byteArray(text);
                    const signature = crypto.subtle.sign(
                            {
                                name: "RSA-PSS",
                                saltLength: 32
                            },
                            keyPair.privateKey,
                            data
                    );
                    const verified = crypto.subtle.verify(
                            {
                                name: "RSA-PSS",
                                saltLength: 32
                            },
                            keyPair.publicKey,
                            signature,
                            data);

                    client.test("RSA-PSS sign/verify", () => {
                        client.assert(verified === true, "Verification failed");
                    });
                %}
                GET https://httpbin.org/get
                """;
        
        String report = restTestService.runTestWithContent("SubtleRSATest", content);
        System.out.println("[DEBUG_LOG] RSA report: " + report);
        assertTrue(report.contains("✅ RSA-PSS sign/verify"), "RSA-PSS failed");
    }

    @Test
    public void testSubtleCryptoECDSA() {
        String content = """
                < {%
                    const base64publicKey = 'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEn8E8vCgnmyDISke4RQVt0uwhE0AFL61crfJ7gmKkLgISv+eV5zAB1GBVQ/mj/4bZO8yJnFCrNGILHN59aCEEfA=='
                    const base64privateKey = 'MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgXMpTO9h9dsmz9f9XfpvdUbU8PGt7ZMiN95Irv0XAgQyhRANCAASfwTy8KCebIMhKR7hFBW3S7CETQAUvrVyt8nuCYqQuAhK/55XnMAHUYFVD+aP/htk7zImcUKs0Ygsc3n1oIQR8'
                    
                    const privateKey = crypto.subtle.importKey(
                            'pkcs8',
                            Uint8Array.from(atob(base64privateKey), c => c.charCodeAt(0)),
                            { name: "ECDSA", namedCurve: "P-256" },
                            true,
                            ["sign"]
                    )

                    const msg = "Hello from HTTP Client!"
                    const signature = crypto.subtle.sign(
                            { name: "ECDSA", hash: "SHA-256" },
                            privateKey,
                            Uint8Array.from(msg, c => c.charCodeAt(0))
                    )

                    const publicKey = crypto.subtle.importKey(
                            'spki',
                            Uint8Array.from(atob(base64publicKey), c => c.charCodeAt(0)),
                            { name: "ECDSA", namedCurve: "P-256" },
                            true,
                            ["verify"]
                    )
                    const verificationResult = crypto.subtle.verify(
                            { name: "ECDSA", hash: "SHA-256" },
                            publicKey,
                            signature,
                            Uint8Array.from(msg, c => c.charCodeAt(0))
                    )
                    
                    client.test("ECDSA import/sign/verify", () => {
                        client.assert(verificationResult === true, "ECDSA Verification failed");
                    });
                %}
                GET https://httpbin.org/get
                """;
        
        String report = restTestService.runTestWithContent("SubtleECDSATest", content);
        System.out.println("[DEBUG_LOG] ECDSA report: " + report);
        assertTrue(report.contains("✅ ECDSA import/sign/verify"), "ECDSA failed");
    }

    @Test
    public void testJwt() {
        String content = """
                < {%
                    const payload = { sub: "1234567890", name: "John Doe", admin: true };
                    const secret = "secret";
                    const token = jwt.sign(payload, secret, { algorithm: "HS256" });
                    
                    client.test("JWT sign", () => {
                        client.assert(token !== null, "Token should not be null");
                    });
                    
                    const verified = jwt.verify(token, secret, { algorithm: "HS256" });
                    client.test("JWT verify", () => {
                        client.assert(verified === true, "JWT verification failed");
                    });
                    
                    const decoded = jwt.decode(token);
                    client.test("JWT decode", () => {
                        // client.log(JSON.stringify(decoded.payload));
                        client.assert(decoded.payload.name.value === "John Doe", "Decoded name mismatch");
                    });
                %}
                GET https://httpbin.org/get
                """;
        
        String report = restTestService.runTestWithContent("JwtTest", content);
        System.out.println("[DEBUG_LOG] JWT report: " + report);
        assertTrue(report.contains("✅ JWT sign"), "JWT sign failed");
        assertTrue(report.contains("✅ JWT verify"), "JWT verify failed");
        assertTrue(report.contains("✅ JWT decode"), "JWT decode failed");
    }
}
