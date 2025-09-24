#!/usr/bin/env bash
set -euo pipefail
TOPIC="${1:-demo-topic}"
docker exec -it kafka bash -lc "kafka-console-producer --bootstrap-server localhost:9092 --topic ${TOPIC}"
