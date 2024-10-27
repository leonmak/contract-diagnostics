package com.example.stellar.connector;

import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import java.util.List;
import java.util.Map;

/**
 * StellarSourceConnector is a Kafka Connect connector configures the SourceTask
 * that reads transactions from the Stellar network and publishes them to a Kafka topic for analysis.
 **/
// TODO separate out into another maven module
public class StellarSourceConnector extends SourceConnector {
  private Map<String, String> config;

  @Override
  public String version() {
    return "1.1";
  }

  @Override
  public void start(Map<String, String> config) {
    this.config = config;
  }

  @Override
  public Class<? extends Task> taskClass() {
    return StellarSourceTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    return List.of(config);
  }

  @Override
  public void stop() {}

  @Override
  public ConfigDef config() {
    return new ConfigDef()
        .define("stellar.horizon.url-format", Type.STRING, Importance.HIGH, "Stellar Horizon URL with account param")
        .define("stellar.account-ids", Type.STRING, Importance.HIGH, "Stellar Account IDs being monitored")
        .define("stellar.transactions.topic", Type.STRING, Importance.HIGH, "Kafka Topic for transactions");
  }
}
