package com.example.stellar;

import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import java.util.List;
import java.util.Map;

public class StellarSourceConnector extends SourceConnector {
  private Map<String, String> config;

  @Override
  public String version() {
    return "1.0";
  }

  @Override
  public void start(Map<String, String> config) {
    this.config = config;
  }

  @Override
  public Class<? extends Task> taskClass() {
    return StellarFlowSourceTask.class;
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
        .define("stellar.horizon.url", Type.STRING, Importance.HIGH, "Stellar Horizon URL")
        .define("stellar.account.id", Type.STRING, Importance.HIGH, "Stellar Account ID")
        .define("stellar.transactions.topic", Type.STRING, Importance.HIGH, "Kafka Topic for transactions");
  }
}
