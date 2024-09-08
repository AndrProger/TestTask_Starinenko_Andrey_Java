import com.example.CrptApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrptApiTest {

    private OkHttpClient httpClientMock;
    private ObjectMapper objectMapperMock;
    private CrptApi crptApi;

    @BeforeEach
    void setUp() {
        httpClientMock = mock(OkHttpClient.class);
        objectMapperMock = mock(ObjectMapper.class);
        crptApi = new CrptApi(httpClientMock, objectMapperMock, TimeUnit.SECONDS, 10);
    }

    @Test
    void testCreateDocumentSuccess() throws Exception {
        // Given
        try (MockWebServer server = new MockWebServer()) {
            server.start();

            // Mocking server response
            MockResponse mockResponse = new MockResponse()
                    .setResponseCode(200)
                    .setBody("success");
            server.enqueue(mockResponse);

            // Adjust API URL for testing
            String mockUrl = server.url("/").toString();
            Mockito.doReturn(mockUrl).when(objectMapperMock).writeValueAsString(any(CrptApi.Document.class));

            // Mocking the HTTP response
            Call callMock = mock(Call.class);
            Response responseMock = new Response.Builder()
                    .request(new Request.Builder().url(mockUrl).build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create("success", MediaType.get("application/json")))
                    .build();

            when(callMock.execute()).thenReturn(responseMock);
            when(httpClientMock.newCall(any(Request.class))).thenReturn(callMock);

            // Document and signature
            CrptApi.Document document = new CrptApi.Document();
            String signature = "Bearer test-token";

            // When
            var response = crptApi.createDocument(document, signature);

            // Then
            verify(httpClientMock, times(1)).newCall(any(Request.class));
            assertEquals(200, responseMock.code());
            assertNotNull(responseMock.body());
            assertEquals("success", response);
        }
    }


    @Test
    void testCreateDocumentFailure() throws Exception {
        // Given
        try (MockWebServer server = new MockWebServer()) {
            server.start();

            // Mocking server response with an error (e.g., 400 Bad Request)
            MockResponse mockResponse = new MockResponse()
                    .setResponseCode(400)
                    .setBody("error");
            server.enqueue(mockResponse);

            // Adjust API URL for testing
            String mockUrl = server.url("/").toString();
            Mockito.doReturn(mockUrl).when(objectMapperMock).writeValueAsString(any(CrptApi.Document.class));

            // Mocking the HTTP response
            Call callMock = mock(Call.class);
            Response responseMock = new Response.Builder()
                    .request(new Request.Builder().url(mockUrl).build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(400)
                    .message("Bad Request")
                    .body(ResponseBody.create("error", MediaType.get("application/json")))
                    .build();

            when(callMock.execute()).thenReturn(responseMock);
            when(httpClientMock.newCall(any(Request.class))).thenReturn(callMock);

            // Document and signature
            CrptApi.Document document = new CrptApi.Document();
            String signature = "Bearer test-token";

            // When & Then
            Exception exception = assertThrows(IOException.class, () -> {
                crptApi.createDocument(document, signature);
            });

            // Assert that the correct exception was thrown
            assertEquals("Unexpected code Response{protocol=http/1.1, code=400, message=Bad Request, url=" + mockUrl + "}", exception.getMessage());
        }
    }

    @Test
    void testRequestLimitExceedWithThreeRequests() throws InterruptedException, IOException {
        // Mocking the HTTP response
        Call callMock = mock(Call.class);
        Response responseMock = new Response.Builder()
                .request(new Request.Builder().url("https://ismp.crpt.ru").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("success", MediaType.get("application/json")))
                .build();

        when(callMock.execute()).thenReturn(responseMock);
        when(httpClientMock.newCall(any(Request.class))).thenReturn(callMock);

        // Mocking objectMapper to return valid JSON string
        when(objectMapperMock.writeValueAsString(any(CrptApi.Document.class))).thenReturn("{\"doc_id\":\"123\"}");

        // Create CrptApi instance with a limit of 3 requests per second
        CrptApi limitedApi = new CrptApi(httpClientMock, objectMapperMock, TimeUnit.SECONDS, 3);

        // Mocking document and signature
        CrptApi.Document document = new CrptApi.Document();
        String signature = "Bearer test-token";

        // First three requests (within the limit)
        limitedApi.createDocument(document, signature);
        limitedApi.createDocument(document, signature);
        limitedApi.createDocument(document, signature);

        // Verify that 3 requests have been made
        verify(httpClientMock, times(3)).newCall(any(Request.class));

        // Fourth request should block until next window
        Thread.sleep(1000); // Simulate passage of time for the next time window

        // After the window resets, a new request should go through
        limitedApi.createDocument(document, signature);

        // Verify that the fourth request was made after the sleep
        verify(httpClientMock, times(4)).newCall(any(Request.class));
    }

    @Test
    void testRequestLimitExceedWithFourRequests() throws InterruptedException, IOException {
        // Mocking the HTTP response
        Call callMock = mock(Call.class);
        Response responseMock = new Response.Builder()
                .request(new Request.Builder().url("https://ismp.crpt.ru").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("success", MediaType.get("application/json")))
                .build();

        when(callMock.execute()).thenReturn(responseMock);
        when(httpClientMock.newCall(any(Request.class))).thenReturn(callMock);

        // Mocking objectMapper to return valid JSON string
        when(objectMapperMock.writeValueAsString(any(CrptApi.Document.class))).thenReturn("{\"doc_id\":\"123\"}");

        // Create CrptApi instance with a limit of 3 requests per second
        CrptApi limitedApi = new CrptApi(httpClientMock, objectMapperMock, TimeUnit.SECONDS, 3);

        // Mocking document and signature
        CrptApi.Document document = new CrptApi.Document();
        String signature = "Bearer test-token";

        long startTime = System.currentTimeMillis();


        // First three requests (within the limit)
        limitedApi.createDocument(document, signature);
        limitedApi.createDocument(document, signature);
        limitedApi.createDocument(document, signature);

        // Verify that 3 requests have been made
        verify(httpClientMock, times(3)).newCall(any(Request.class));


        // Immediately make the fourth request (should block until the next time window)
        limitedApi.createDocument(document, signature);

        // Measure time after the fourth request
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert that the delay is at least the duration of the rate limit window (1000ms)
        assertTrue(duration >= 1000, "The fourth request was not delayed correctly.");

        // Verify that the fourth request was made after the time window reset
        verify(httpClientMock, times(4)).newCall(any(Request.class));
    }


    @Test
    void testRequestLimitExceed() throws InterruptedException, IOException {
        // Mocking the HTTP response
        Call callMock = mock(Call.class);
        Response responseMock = new Response.Builder()
                .request(new Request.Builder().url("https://ismp.crpt.ru").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("success", MediaType.get("application/json")))
                .build();

        when(callMock.execute()).thenReturn(responseMock);
        when(httpClientMock.newCall(any(Request.class))).thenReturn(callMock);

        // Mocking objectMapper to return valid JSON string
        when(objectMapperMock.writeValueAsString(any(CrptApi.Document.class))).thenReturn("{\"doc_id\":\"123\"}");

        // Create CrptApi instance with a small limit
        CrptApi limitedApi = new CrptApi(httpClientMock, objectMapperMock, TimeUnit.SECONDS, 1);

        // Mocking document and signature
        CrptApi.Document document = new CrptApi.Document();
        String signature = "Bearer test-token";

        // First request
        limitedApi.createDocument(document, signature);
        verify(httpClientMock, times(1)).newCall(any(Request.class));

        // Second request should block until next window
        Thread.sleep(1000); // Simulate passage of time for the next time window
        limitedApi.createDocument(document, signature);
        verify(httpClientMock, times(2)).newCall(any(Request.class));
    }

    @Test
    void testInvalidRequestLimit() {
        // Given an invalid request limit (zero or negative)
        Exception exception = null;

        // When & Then
        try {
            new CrptApi(TimeUnit.SECONDS, 0);  // Invalid limit
        } catch (IllegalArgumentException e) {
            exception = e;
        }

        assert exception != null;
        assertEquals("Request limit must be a positive number.", exception.getMessage());
    }
}
