package com.example.stellar.connector;

import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SubmissionPublisher;

public class StellarFlowSourceTask extends SourceTask {
    private SubmissionPublisher<SourceRecord> publisher;
    private List<SourceRecord> buffer;
    private TxnSubscriber subscriber;

    private String uri;
    private String txnTopic;
    private HttpClient httpClient;

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
        System.out.printf("Connected to : %s%n", url);

        // Create HttpRequest with SSE event stream
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/event-stream")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        // fill up buffer, cleared on poll
        httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.fromLineSubscriber(subscriber))
                .thenAccept(response -> System.out.println("Response status code: " + response.statusCode()));
    }

    @Override
    public List<SourceRecord> poll() {
        // Check the status of the subscriber
        if (subscriber.isComplete()) {
            // Handle completion
            return null;
        }
        // TODO add some metrics on the buffer size, httpClient, etc
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
