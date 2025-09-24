# redis-local

Dockerized Redis for local development.

## Structure
```
redis-local/
├─ .env
├─ docker-compose.yml
├─ conf/
│  └─ redis.conf
├─ data/
└─ scripts/
   └─ redis-cli.sh
```

## Quick start
```bash
cd redis-local
docker compose up -d
docker compose ps
```

### Health check
```bash
docker compose exec redis redis-cli ping
# -> PONG
```

### With password (optional)
1) Edit `.env` and set `REDIS_PASSWORD=changeme`
2) Recreate:
```bash
docker compose down
docker compose up -d
docker compose exec redis redis-cli -a changeme ping
```

## Spring/Redisson wiring (recommended)
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    # password: changeme

lock:
  redisson:
    enabled: true
    address: redis://${spring.redis.host}:${spring.redis.port}
    database: 10
    wait-time: 3000
    lease-time: 10000
    retry-interval: 150
```

## Reset all data
```bash
docker compose down -v
```
