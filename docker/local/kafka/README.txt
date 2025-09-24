[Kafka Local via Docker on macOS — Quick Start]

0) Requirements
   - Docker Desktop for Mac installed
   - Recommended resources: 2 CPUs, 2GB+ RAM in Docker Desktop settings

1) Start
   $ cd kafka-local
   $ docker compose up -d
   - Wait ~10-20s. Check logs if needed:  $ docker logs -f kafka

2) Kafka UI
   - Open: http://localhost:8080
   - Cluster name: local

3) Create topic (from host via container CLI)
   $ ./scripts/create-topic.sh demo-topic

4) Produce (interactive)
   $ ./scripts/produce.sh demo-topic
   > hello
   > world
   (Ctrl+C to exit)

5) Consume
   $ ./scripts/consume.sh demo-topic

6) From host with port 29092 (e.g., Spring Boot)
   bootstrap-servers: localhost:29092

7) Stop / Down
   $ ./scripts/stop.sh     # stops containers
   $ ./scripts/down.sh     # stops & removes (no volumes)

Troubleshooting
- If you see connection issues from host, ensure you use localhost:29092.
- If containers keep restarting, increase Docker Desktop RAM/CPU.
- If port collisions occur, change mapped ports in docker-compose.yml:
    kafka: "29092:29092" -> "39092:29092"
    ui:    "8080:8080"   -> "18080:8080"
