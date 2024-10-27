package com.example.stellar.connector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

/**
 * Handler for new transaction events from the horizon streaming API,
 * Convert and store the new SourceRecords in an intermediate buffer.
 **/
public class StellarTxnSubscriber implements Flow.Subscriber<String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final List<SourceRecord> buffer;
    private final String accountId;
    private final String txnTopic;

    private Flow.Subscription subscription;
    private boolean isComplete = false;

    public StellarTxnSubscriber(String accountId, String txnTopic, List<SourceRecord> buffer) {
        this.accountId = accountId;
        this.txnTopic = txnTopic;
        this.buffer = buffer;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(String event) {
        try {
            // Parse the SSE event
            TypeReference<Map<String,Object>> mapTypeReference = new TypeReference<>() {
            };
            Map<String, Object> eventData = OBJECT_MAPPER.readValue(event, mapTypeReference);
            // Create source partition and offset
            Map<String, String> sourcePartition = Map.of("topic", txnTopic, "account_id", accountId);
            String pagingToken = (String) eventData.get("pagingToken");
            Map<String, Object> sourceOffset = Map.of("paging_token", pagingToken);

            // TODO: do some kind of conversion, store as string for now
            SourceRecord sourceRecord = new SourceRecord(
                    sourcePartition,
                    sourceOffset,
                    txnTopic,
                    null, // partition will be determined by Kafka
                    Schema.STRING_SCHEMA,
                    "acct="+accountId+":pg="+pagingToken,
                    Schema.STRING_SCHEMA,
                    event,
                    System.currentTimeMillis()
            );

            // Add the SourceRecord to the buffer
            // Assuming you have a method to add records to the buffer
            buffer.add(sourceRecord);
        } catch (Exception e) {
            // Log the error and potentially handle it
            // For example, you might want to skip malformed events
            System.err.println("Error processing SSE event: " + e.getMessage());
        }

        // Request the next item
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        // Handle error
        isComplete = true;
    }

    @Override
    public void onComplete() {
        isComplete = true;
    }

    public boolean isComplete() {
        return isComplete;
    }
}