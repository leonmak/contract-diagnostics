# contract-diagnostics

Analysis for soroban smart contracts


## Run App
- generate the connector jar with `mvn install`
- start brokers, connect, schema registry, stellar localnet with `docker compose up -d`
- create topics with `bash conf/create-topics.sh`
- run `StellarStreamsApp`: connects to the source connector
- run source connector above to populate source topic

## Update Actions

### Source Task
- `SourceTask` subscribes to horizon transaction streaming endpoint for transactions and adds to topic
- `.jar` file is mounted to plugins folder set in docker-compose
  - run `mvn install` for jar to be in `target/data/connector`discovery in
  - connector class in connect logs after `/usr/java/share/connector`
  - check in [http://localhost:8083/connector-plugins]()

### Source Connector
- to change the config, can just delete then create the connector again
  - Delete: `curl -X DELETE http://localhost:8083/connectors/stellar-source-connector`
  - Create: `curl -X POST -H "Content-Type: application/json" --data @conf/stellar-source-connector.json http://localhost:8083/connectors`
  - check in [http://localhost:8083/connectors/stellar-source-connector/status]()
