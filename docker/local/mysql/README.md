# mysql-local (Docker Compose)

## Quick start
```bash
cd mysql-local
docker compose up -d
# or: docker-compose up -d
```

Wait for health to be `healthy`:
```bash
docker ps
docker logs -f mysql-local
```

Connect:
```bash
mysql -h localhost -P 3306 -uorder -porder1234 order_local
```

Reinitialize (rerun init SQLs):
```bash
docker compose down
docker volume rm -f mysql_data
docker compose up -d
```

## What you get
- MySQL 8.3 on port 3306
- DB: order_local
- User: order / Password: order1234 (root: root)
- UTF-8 defaults, InnoDB, timezone Asia/Seoul
- `init/01_schema.sql` + `init/02_seed.sql` auto-applied on first init
