package com.ddib.monolith.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthReadPathIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void shouldLoginSeedUserAndReadMe() throws IOException, InterruptedException {
        HttpResponse<String> loginResponse = sendPost("/api/auth/test/login?userId=1", null);
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        assertThat(loginResponse.body()).contains("\"accessToken\"");

        String token = extractJsonField(loginResponse.body(), "accessToken");
        HttpResponse<String> meResponse = sendGet("/api/users/me", token);

        assertThat(meResponse.statusCode()).isEqualTo(200);
        assertThat(meResponse.body()).contains("\"userId\":1");
        assertThat(meResponse.body()).contains("\"nickname\":");
    }

    @Test
    void shouldUpdateNickname() throws IOException, InterruptedException {
        String token = extractJsonField(sendPost("/api/auth/test/login?userId=1", null).body(), "accessToken");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/users/me"))
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"nickname\":\"Updated Tester\"}"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"nickname\":\"Updated Tester\"");
    }

    private HttpResponse<String> sendGet(String path, String token) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET();
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .POST(body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            builder.header(HttpHeaders.CONTENT_TYPE, "application/json");
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String extractJsonField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\":\"";
        int start = json.indexOf(pattern);
        int valueStart = start + pattern.length();
        int valueEnd = json.indexOf('"', valueStart);
        return json.substring(valueStart, valueEnd);
    }
}
