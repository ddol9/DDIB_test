package com.ddib.monolith.performance;

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

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PerformanceReadPathIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void shouldReadPerformanceListDetailAndSeatInfo() throws IOException, InterruptedException {
        HttpResponse<String> listResponse = sendGet("/api/ticketing/performances");
        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.body()).contains("DDIB Launch Concert");

        HttpResponse<String> detailResponse = sendGet("/api/ticketing/performances/1");
        assertThat(detailResponse.statusCode()).isEqualTo(200);
        assertThat(detailResponse.body()).contains("\"performanceId\":1");
        assertThat(detailResponse.body()).contains("\"options\"");

        HttpResponse<String> seatResponse = sendGet("/api/ticketing/performances/1/options/1/seats");
        assertThat(seatResponse.statusCode()).isEqualTo(200);
        assertThat(seatResponse.body()).contains("\"seatConfiguration\"");
        assertThat(seatResponse.body()).contains("\"seats\"");
    }

    private HttpResponse<String> sendGet(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
