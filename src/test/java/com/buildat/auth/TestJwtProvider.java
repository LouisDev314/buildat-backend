package com.buildat.auth;

import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;

// Local synthetic provider only. No Supabase endpoint, credentials or user data.
public final class TestJwtProvider implements AutoCloseable {
    public static final String ISSUER = "https://test.invalid/auth/v1";
    public final RSAKey first = key("first");
    public final RSAKey second = key("second");
    public volatile String jwks = new JWKSet(first.toPublicJWK()).toString();
    public volatile int status = 200;
    public final AtomicInteger requests = new AtomicInteger();
    private final HttpServer server;
    public TestJwtProvider() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            requests.incrementAndGet();
            byte[] body = jwks.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
    }
    public URI uri() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/jwks"); }
    public String token(UUID subject) { return token(first, claims(subject), JWSAlgorithm.RS256); }
    public static Map<String, Object> claims(UUID subject) {
        var now = Instant.now();
        var claims = new HashMap<String, Object>();
        claims.put("iss", ISSUER); claims.put("aud", "authenticated"); claims.put("sub", subject.toString());
        claims.put("session_id", UUID.randomUUID().toString()); claims.put("is_anonymous", false);
        claims.put("role", "authenticated"); claims.put("iat", Date.from(now)); claims.put("exp", Date.from(now.plusSeconds(600)));
        return claims;
    }
    public static RSAKey key(String id) {
        try { return new RSAKeyGenerator(2048).keyID(id).generate(); }
        catch (JOSEException exception) { throw new IllegalStateException(exception); }
    }
    public static String token(RSAKey key, Map<String, Object> claims, JWSAlgorithm algorithm) {
        try {
            var builder = new JWTClaimsSet.Builder(); claims.forEach(builder::claim);
            var jwt = new SignedJWT(new JWSHeader.Builder(algorithm).keyID(key.getKeyID()).build(), builder.build());
            jwt.sign(new RSASSASigner(key)); return jwt.serialize();
        } catch (JOSEException exception) { throw new IllegalStateException(exception); }
    }
    @Override public void close() { server.stop(0); }
}
