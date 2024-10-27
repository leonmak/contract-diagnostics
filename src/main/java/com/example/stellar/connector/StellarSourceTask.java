package com.example.stellar.connector;

import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.Collectors;

/**
 * StellarFlowSourceTask is a Kafka Connect SourceTask that subscribes to the horizon API transactions.
 * Contains the HTTP client initiating the streaming request, manages an internal buffer which is transferred on poll.
 */
public class StellarSourceTask extends SourceTask {
    private List<SourceRecord> buffer;
    private SubmissionPublisher<SourceRecord> publisher;
    private Map<String, StellarTxnSubscriber> accountSubscribers;

    private String txnTopic;
    private HttpClient httpClient;
    private List<String> accountIds;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> config) {
        this.buffer = new CopyOnWriteArrayList<>();
        this.publisher = new SubmissionPublisher<>();
        this.accountSubscribers = new HashMap<>();

        // Connect to your data source here
        // For example, establish an SSE connection
        String urlFormat = config.get("stellar.horizon.url-format");
        this.accountIds = Arrays.stream(config.get("stellar.account-ids").split(","))
                .collect(Collectors.toList());

        this.txnTopic = config.get("stellar.transactions.topic");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Create HttpRequest with SSE event stream
        var requestBuilder = HttpRequest.newBuilder()
                .header("Content-Type", "text/event-stream")
                .timeout(Duration.ofSeconds(10))
                .GET();

        var futures = accountIds.stream().map(accountId -> {
            Map<String, String> sourcePartition = Map.of("topic", txnTopic, "account_id", accountId);

            var sourceOffset = context.offsetStorageReader().offset(sourcePartition);
            var cursor = sourceOffset.getOrDefault("paging_token", "");
            String baseUrl = String.format(urlFormat, accountId);
            String urlWithParam = String.format("%s?order=desc&cursor=%s", baseUrl, cursor);
            System.out.printf("Connecting to : %s%n", urlWithParam);

            var uri = URI.create(urlWithParam);
            var request = requestBuilder.uri(uri).build();
            var subscriber = new StellarTxnSubscriber(accountId, txnTopic, buffer);
            accountSubscribers.put(accountId, subscriber);
            var handler = HttpResponse.BodyHandlers.fromLineSubscriber(subscriber);
            return httpClient.sendAsync(request, handler);
        }).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
    }

    @Override
    public List<SourceRecord> poll() {
        // Check the status of the subscriber
        boolean allDone = accountSubscribers.values().stream()
                .parallel()
                .allMatch(StellarTxnSubscriber::isComplete);
        // TODO Handle completion
        if (allDone) {
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
