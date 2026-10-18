package com.buildat.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

final class ProviderHttp {
    private ProviderHttp() { }
    static SimpleClientHttpRequestFactory requestFactory() {
        var factory = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                // Never forward a Bearer token or obtain signing keys from a redirected origin.
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return factory;
    }
}
