package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse(CONTENT_TYPE_JSON);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int requestLimit;
    private final long timeWindowMillis;
    private final Semaphore semaphore;
    private final Lock lock;
    private long nextWindowStartTime;
    private final ScheduledExecutorService scheduler;


    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this(new OkHttpClient(), new ObjectMapper(), timeUnit, requestLimit);
    }

    // Constructor with dependency injection for OkHttpClient and ObjectMapper
    public CrptApi(OkHttpClient httpClient, ObjectMapper objectMapper, TimeUnit timeUnit, int requestLimit) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Request limit must be a positive number.");
        }

        this.requestLimit = requestLimit;
        this.timeWindowMillis = timeUnit.toMillis(1);

        this.semaphore = new Semaphore(0);
        this.lock = new ReentrantLock();
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;

        this.scheduler = Executors.newScheduledThreadPool(1);

        scheduleRateLimitReset();
    }

    private void scheduleRateLimitReset() {
        scheduler.scheduleAtFixedRate(() -> {
            lock.lock();
            try {
                semaphore.drainPermits();
                semaphore.release(requestLimit);
                nextWindowStartTime = System.currentTimeMillis() + timeWindowMillis;
            } finally {
                lock.unlock();
            }
        }, 0, timeWindowMillis, TimeUnit.MILLISECONDS);
    }

    public String createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();

        String jsonBody = objectMapper.writeValueAsString(document);

        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Content-Type", CONTENT_TYPE_JSON)
                // Example: using signature for authorization
                .addHeader("Authorization", signature)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            return Objects.requireNonNull(response.body()).string();
        }
    }


    public void shutdown() {
        scheduler.shutdown();
    }

    public static class Document {
        @JsonProperty("description")
        public Description description;

        @JsonProperty("doc_id")
        public String documentId;

        @JsonProperty("doc_status")
        public String documentStatus;

        @JsonProperty("doc_type")
        public String documentType;

        @JsonProperty("importRequest")
        public boolean importRequest;

        @JsonProperty("owner_inn")
        public String ownerInn;

        @JsonProperty("participant_inn")
        public String participantInn;

        @JsonProperty("producer_inn")
        public String producerInn;

        @JsonProperty("production_date")
        public String productionDate;

        @JsonProperty("production_type")
        public String productionType;

        @JsonProperty("products")
        public Product[] products;

        @JsonProperty("reg_date")
        public String registrationDate;

        @JsonProperty("reg_number")
        public String registrationNumber;
    }

    public static class Description {
        @JsonProperty("participantInn")
        public String participantInn;
    }

    public static class Product {
        @JsonProperty("certificate_document")
        public String certificateDocument;

        @JsonProperty("certificate_document_date")
        public String certificateDocumentDate;

        @JsonProperty("certificate_document_number")
        public String certificateDocumentNumber;

        @JsonProperty("owner_inn")
        public String ownerInn;

        @JsonProperty("producer_inn")
        public String producerInn;

        @JsonProperty("production_date")
        public String productionDate;

        @JsonProperty("tnved_code")
        public String tnvedCode;

        @JsonProperty("uit_code")
        public String uitCode;

        @JsonProperty("uitu_code")
        public String uituCode;
    }
}
