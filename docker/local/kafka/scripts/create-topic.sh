#!/usr/bin/env bash
set -euo pipefail
TOPIC="${1:-demo-topic}"
docker exec -it kafka bash -lc "kafka-topics --bootstrap-server localhost:9092 --create --topic ${TOPIC} --partitions 1 --replication-factor 1 || true"
docker exec -it kafka bash -lc "kafka-topics --bootstrap-server localhost:9092 --describe --topic ${TOPIC}"
