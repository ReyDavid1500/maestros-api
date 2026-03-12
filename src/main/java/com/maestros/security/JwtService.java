package com.maestros.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.maestros.exception.InvalidTokenException;
import com.maestros.model.postgres.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    @Value("${JWT_PRIVATE_KEY}")
    private String privateKeyBase64;

    @Value("${JWT_PUBLIC_KEY}")
    private String publicKeyBase64;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private Algorithm algorithm;

    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() throws Exception {
        this.privateKey = decodePrivateKey(privateKeyBase64);
        this.publicKey = decodePublicKey(publicKeyBase64);
        this.algorithm = Algorithm.RSA256(publicKey, privateKey);
    }

    /**
     * Generates a short-lived access token (1 hour) containing userId, role, jti,
     * iat and exp.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return JWT.create()
                .withClaim("userId", user.getId().toString())
                .withClaim("role", user.getRole().name())
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(ACCESS_TOKEN_TTL)))
                .sign(algorithm);
    }

    /**
     * Generates a long-lived refresh token (30 days) and stores the jti → userId
     * mapping
     * in Redis with a matching TTL so the token can be revoked server-side.
     */
    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        return JWT.create()
                .withClaim("userId", user.getId().toString())
                .withJWTId(jti)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(REFRESH_TOKEN_TTL)))
                .sign(algorithm);
    }

    /**
     * Verifies the token's signature, expiration, and that its jti is not
     * blacklisted.
     *
     * @return the decoded JWT payload
     * @throws InvalidTokenException if verification fails for any reason
     */
    public DecodedJWT validateToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT decoded = verifier.verify(token);

            if (isTokenRevoked(decoded.getId())) {
                throw new InvalidTokenException("Token has been revoked");
            }

            return decoded;
        } catch (JWTVerificationException e) {
            throw new InvalidTokenException("Invalid or expired token: " + e.getMessage());
        }
    }

    /**
     * Extracts the userId claim from the token payload without verifying the
     * signature.
     */
    public String extractUserId(String token) {
        return JWT.decode(token).getClaim("userId").asString();
    }

    /**
     * Adds the token's jti to the Redis blacklist with a TTL equal to the token's
     * remaining
     * lifetime, effectively invalidating it for all future requests.
     */
    public void revokeToken(String token) {
        DecodedJWT decoded = JWT.decode(token);
        String jti = decoded.getId();
        Instant expiry = decoded.getExpiresAtAsInstant();
        long ttlSeconds = Duration.between(Instant.now(), expiry).getSeconds();

        if (ttlSeconds > 0) {
            blacklist.put(jti, expiry);
        }
    }

    /**
     * Returns true if the given jti appears in the Redis blacklist.
     */
    public boolean isTokenRevoked(String jti) {
        Instant expiry = blacklist.get(jti);
        if (expiry == null)
            return false;
        if (Instant.now().isAfter(expiry)) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Key decoding helpers
    // -------------------------------------------------------------------------

    /**
     * Decodes a base64-encoded PEM private key. Accepts both PKCS#8 ("BEGIN PRIVATE
     * KEY")
     * and PKCS#1 ("BEGIN RSA PRIVATE KEY") formats; PKCS#1 is automatically
     * wrapped.
     */
    private RSAPrivateKey decodePrivateKey(String base64Pem) throws Exception {
        String pem = new String(Base64.getDecoder().decode(base64Pem)).trim();
        byte[] der;
        if (pem.contains("BEGIN RSA PRIVATE KEY")) {
            byte[] pkcs1 = extractPemDer(pem, "-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----");
            der = pkcs1ToPkcs8(pkcs1);
        } else {
            der = extractPemDer(pem, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
        }
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private RSAPublicKey decodePublicKey(String base64Pem) throws Exception {
        String pem = new String(Base64.getDecoder().decode(base64Pem)).trim();
        byte[] der = extractPemDer(pem, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private static byte[] extractPemDer(String pem, String header, String footer) {
        String base64 = pem.replace(header, "").replace(footer, "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Wraps raw PKCS#1 RSA private key DER bytes into a PKCS#8 PrivateKeyInfo
     * structure
     * so that the standard Java {@code KeyFactory} can parse it.
     */
    private static byte[] pkcs1ToPkcs8(byte[] pkcs1) {
        // AlgorithmIdentifier: SEQUENCE { OID rsaEncryption(1.2.840.113549.1.1.1), NULL
        // }
        byte[] algorithmIdentifier = {
                0x30, 0x0D,
                0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,
                0x05, 0x00
        };
        byte[] version = { 0x02, 0x01, 0x00 }; // INTEGER 0
        byte[] privateKeyOctetString = derTag(0x04, pkcs1); // OCTET STRING { pkcs1 }
        byte[] inner = concat(version, algorithmIdentifier, privateKeyOctetString);
        return derTag(0x30, inner); // outer SEQUENCE
    }

    private static byte[] derTag(int tag, byte[] content) {
        byte[] lengthBytes = derEncodeLength(content.length);
        byte[] result = new byte[1 + lengthBytes.length + content.length];
        result[0] = (byte) tag;
        System.arraycopy(lengthBytes, 0, result, 1, lengthBytes.length);
        System.arraycopy(content, 0, result, 1 + lengthBytes.length, content.length);
        return result;
    }

    private static byte[] derEncodeLength(int length) {
        if (length < 128) {
            return new byte[] { (byte) length };
        } else if (length < 256) {
            return new byte[] { (byte) 0x81, (byte) length };
        } else {
            return new byte[] { (byte) 0x82, (byte) (length >> 8), (byte) (length & 0xFF) };
        }
    }

    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays)
            total += a.length;
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, offset, a.length);
            offset += a.length;
        }
        return result;
    }
}
