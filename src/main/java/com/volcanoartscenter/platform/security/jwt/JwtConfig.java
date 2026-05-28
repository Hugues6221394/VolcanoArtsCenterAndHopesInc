package com.volcanoartscenter.platform.security.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * RS256 keypair wiring for the partner JWT issuer + verifier.
 *
 * <p>Production: provide {@code platform.security.jwt.private-key} and
 * {@code platform.security.jwt.public-key} as PEM-encoded strings (no headers
 * needed — newlines optional, base64 body is what matters).
 *
 * <p>Dev: if both env vars are blank, an ephemeral RSA-2048 keypair is generated
 * at startup so the app boots. Tokens issued during one boot won't verify after
 * a restart — fine for local development, never for production.
 */
@Configuration
@Slf4j
public class JwtConfig {

    private static final String DEFAULT_KID = "partner-jwt-key";

    @Bean
    public RSAKey partnerRsaKey(
            @Value("${platform.security.jwt.private-key:}") String privateKeyPem,
            @Value("${platform.security.jwt.public-key:}") String publicKeyPem,
            @Value("${platform.security.jwt.key-id:" + DEFAULT_KID + "}") String keyId) {

        if ((privateKeyPem == null || privateKeyPem.isBlank())
                || (publicKeyPem == null || publicKeyPem.isBlank())) {
            log.warn("Partner JWT keypair not configured — generating an ephemeral RSA-2048 keypair. "
                    + "Set platform.security.jwt.private-key / public-key in production.");
            KeyPair kp = generateRsa();
            return new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                    .privateKey((RSAPrivateKey) kp.getPrivate())
                    .keyID(keyId == null || keyId.isBlank() ? UUID.randomUUID().toString() : keyId)
                    .build();
        }
        try {
            RSAPrivateKey priv = parsePrivate(privateKeyPem);
            RSAPublicKey pub = parsePublic(publicKeyPem);
            return new RSAKey.Builder(pub)
                    .privateKey(priv)
                    .keyID(keyId == null || keyId.isBlank() ? DEFAULT_KID : keyId)
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse partner JWT keypair: " + ex.getMessage(), ex);
        }
    }

    @Bean
    public JWKSource<SecurityContext> partnerJwkSource(RSAKey partnerRsaKey) {
        JWKSet set = new JWKSet(partnerRsaKey);
        return new ImmutableJWKSet<>(set);
    }

    @Bean
    public JwtEncoder partnerJwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder partnerJwtDecoder(RSAKey partnerRsaKey) throws Exception {
        return NimbusJwtDecoder.withPublicKey(partnerRsaKey.toRSAPublicKey()).build();
    }

    private KeyPair generateRsa() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            return g.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("RSA keypair generation failed", ex);
        }
    }

    private RSAPrivateKey parsePrivate(String pem) throws Exception {
        byte[] der = Base64.getDecoder().decode(stripPem(pem));
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private RSAPublicKey parsePublic(String pem) throws Exception {
        byte[] der = Base64.getDecoder().decode(stripPem(pem));
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private String stripPem(String pem) {
        return pem.replaceAll("-----[A-Z ]+-----", "")
                  .replaceAll("\\s+", "");
    }
}
