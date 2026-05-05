package com.ddib.monolith.support.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddib.monolith.support.security.JwtTokenProvider;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthSupportIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldAllowPublicEndpointWithoutToken() throws IOException, InterruptedException {
        HttpResponse<String> response = send("/api/public/ping", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"scope\":\"public\"");
    }

    @Test
    void shouldRejectProtectedEndpointWithoutToken() throws IOException, InterruptedException {
        HttpResponse<String> response = send("/api/private/ping", null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"code\":\"COMMON_401\"");
    }

    @Test
    void shouldResolveAuthenticatedUserFromToken() throws IOException, InterruptedException {
        String token = jwtTokenProvider.generateAccessToken(7L, "tester", "USER");

        HttpResponse<String> response = send("/api/private/ping", token);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"userId\":7");
        assertThat(response.body()).contains("\"userName\":\"tester\"");
        assertThat(response.body()).contains("\"userRole\":\"USER\"");
    }

    private HttpResponse<String> send(String path, String token) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET();

        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
