package com.example.stellar;

import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.CopyOnWriteArrayList;

public class StellarFlowSourceTask extends SourceTask {
    private SubmissionPublisher<SourceRecord> publisher;
    private List<SourceRecord> buffer;
    private TxnSubscriber subscriber;

    private String uri;
    private String txnTopic;
    private HttpClient httpClient;
    private CompletableFuture<Void> sseFuture;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> config) {
        this.buffer = new CopyOnWriteArrayList<>();
        this.publisher = new SubmissionPublisher<>();
        this.subscriber = new TxnSubscriber(this.txnTopic, this.buffer);

        // Connect to your data source here
        // For example, establish an SSE connection
        this.uri = config.get("stellar.horizon.url");
        this.txnTopic = config.get("stellar.transactions.topic");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        var sourcePartition = Map.of("topic", txnTopic);
        var sourceOffset = context.offsetStorageReader().offset(sourcePartition);
        var cursor = sourceOffset.getOrDefault("position", "");
        String url = String.format("%s?cursor=%s", uri, cursor);

        // Create HttpRequest with SSE event stream
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/event-stream")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        // fill up buffer, cleared on poll
        var responseBodyHandler = HttpResponse.BodyHandlers.fromLineSubscriber(subscriber);
        sseFuture = httpClient
                .sendAsync(request, responseBodyHandler)
                .thenAccept(response -> System.out.println("Response status code: " + response.statusCode()));

        // Keep the main thread alive
        sseFuture.join();
    }

    // TODO add some metrics on the buffer size etc
    @Override
    public List<SourceRecord> poll() {
        // Check the status of the subscriber
        if (subscriber.isComplete()) {
            // Handle completion
            return null;
        }
        List<SourceRecord> records = new ArrayList<>(buffer);
        buffer.clear();
        return records;
    }

    @Override
    public void stop() {
        if (publisher != null) {
            publisher.close();
        }
    }
}
