#!/bin/sh

KAFKA_BROKER="localhost:9092"
TOPICS=( "stellar-transactions" "test" )

for TOPIC in "${TOPICS[@]}"; do
  kafka-topics.sh --create --if-not-exists --bootstrap-server $KAFKA_BROKER --replication-factor 1 --partitions 1 --topic $TOPIC
done