# contract-diagnostics

Analysis for soroban smart contracts

## Source Connector
- subscribes to horizon transaction streaming endpoint for transactions and adds to topic
- added to plugins in docker-compose
  - run `mvn install` for jar to be in `target/data/connector`
  - mounts to `/usr/java/share/connector` for discovery [http://localhost:8083/connector-plugins]()
  - install connector to run `curl -X POST -H "Content-Type: application/json" --data @conf/stellar-source-connector.json http://localhost:8083/connectors`

## Run App
- generate the connector jar with `mvn install`
- start brokers, connect, schema registry, stellar localnet with `docker compose up -d`
- create topics with `bash conf/create-topics.sh`
- run `StellarStreamsApp`: connects to the source connector
- run source connector above to populate source topic