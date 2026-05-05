package com.ddib.monolith.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddib.monolith.queue.application.QueueService;
import com.ddib.monolith.seat.application.SeatLockService;
import com.ddib.monolith.seat.domain.SeatStore;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PaymentBookingFlowIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private QueueService queueService;

    @Autowired
    private SeatLockService seatLockService;

    @Autowired
    private SeatStore seatStore;

    @Test
    void shouldCompletePaymentAndProjectReservation() throws IOException, InterruptedException {
        String accessToken = extractJsonField(sendPost("/api/auth/test/login?userId=1", null).body(), "accessToken");

        HttpResponse<String> queueRegister = sendPost(
                "/api/queue/in",
                "{\"performanceId\":1,\"optionId\":1}",
                accessToken
        );
        assertThat(queueRegister.statusCode()).isEqualTo(200);

        String queueToken = queueService.getStatus(1L, 1L, 1L).getQueueToken();
        assertThat(queueToken).isNotBlank();

        seatLockService.lockSeats(1L, 1L, 1L, queueToken, List.of(1L, 2L));

        HttpResponse<String> prepareResponse = sendPost(
                "/api/payments/prepare",
                """
                {
                  "tokenId":"%s",
                  "performanceId":1,
                  "optionId":1,
                  "amount":300000,
                  "seatIds":["1","2"]
                }
                """.formatted(queueToken),
                accessToken
        );
        assertThat(prepareResponse.statusCode()).isEqualTo(200);
        String orderId = extractJsonField(prepareResponse.body(), "orderId");

        HttpResponse<String> confirmResponse = sendPost(
                "/api/payments/confirm",
                """
                {
                  "orderId":"%s",
                  "paymentKey":"pg-test-key",
                  "amount":300000
                }
                """.formatted(orderId),
                accessToken
        );
        assertThat(confirmResponse.statusCode()).isEqualTo(200);
        assertThat(confirmResponse.body()).contains("\"status\":\"SUCCESS\"");

        HttpResponse<String> confirmAgainResponse = sendPost(
                "/api/payments/confirm",
                """
                {
                  "orderId":"%s",
                  "paymentKey":"pg-test-key",
                  "amount":300000
                }
                """.formatted(orderId),
                accessToken
        );
        assertThat(confirmAgainResponse.statusCode()).isEqualTo(200);
        assertThat(confirmAgainResponse.body()).contains("\"idempotent\":true");

        HttpResponse<String> paymentStatusResponse = sendGet("/api/payments/" + orderId, accessToken);
        assertThat(paymentStatusResponse.statusCode()).isEqualTo(200);
        assertThat(paymentStatusResponse.body()).contains("\"status\":\"SUCCESS\"");
        assertThat(paymentStatusResponse.body()).contains("\"seatLabels\":[\"A-1\",\"A-2\"]");

        HttpResponse<String> reservationResponse = sendGet("/api/reservations/my?type=ALL&size=10", accessToken);
        assertThat(reservationResponse.statusCode()).isEqualTo(200);
        assertThat(reservationResponse.body()).contains("\"performanceTitle\":\"DDIB Launch Concert\"");
        assertThat(reservationResponse.body()).contains("\"seatPos\":\"A-1\"");
        assertThat(reservationResponse.body()).contains("\"seatPos\":\"A-2\"");

        assertThat(seatStore.getSoldSeats(1L, 1L)).containsExactlyInAnyOrder(1L, 2L);
        assertThat(seatStore.getOccupiedSeats(1L, 1L)).doesNotContain(1L, 2L);
    }

    private HttpResponse<String> sendGet(String path, String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(String path, String body, String token) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .POST(body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header(HttpHeaders.CONTENT_TYPE, "application/json");
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(String path, String body) throws IOException, InterruptedException {
        return sendPost(path, body, null);
    }

    private String extractJsonField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\":\"";
        int start = json.indexOf(pattern);
        int valueStart = start + pattern.length();
        int valueEnd = json.indexOf('"', valueStart);
        return json.substring(valueStart, valueEnd);
    }
}
