# ECS deployment setup

This directory contains templates for the one-time ECS setup used by `.github/workflows/deploy-ecs.yml`.

## 1. Prepare the server

Install Java 17, Nginx, MySQL, and Qdrant before the first deploy. The deployment workflow assumes the SSH user is `root`, so create the app directories as root:

```bash
mkdir -p /opt/aiops/releases /opt/aiops/config /opt/aiops/run /opt/aiops/logs
```

The workflow starts the Java app directly with `nohup`, stores its PID in `/opt/aiops/run/aiops.pid`, and runs `nginx -t && nginx -s reload` as root.

## 2. Add production config

Copy `application-prod.example.yml` to `/opt/aiops/config/application-prod.yml`, then set the real MySQL, Qdrant, and CORS values:

```bash
cp application-prod.example.yml /opt/aiops/config/application-prod.yml
chmod 600 /opt/aiops/config/application-prod.yml
```

Keep this real file on ECS only. Do not commit production credentials to Git.

## 3. Install Nginx config

```bash
cp nginx-aiops.conf /etc/nginx/conf.d/aiops.conf
nginx -t
nginx -s reload
```

The Java process starts during the first GitHub Actions deployment after `/opt/aiops/current` is created.

## 4. Configure GitHub Secrets

Set these repository secrets:

- `ECS_HOST`: ECS public IP
- `ECS_USER`: set this to `root`
- `ECS_SSH_PRIVATE_KEY`: private key for that user
- `ECS_PORT`: SSH port, optional, defaults to `22`

Push to `master` to deploy code. For config-only changes, edit `/opt/aiops/config/application-prod.yml` on ECS and run the workflow manually from GitHub Actions.

## Manual process control

Check the app process and logs:

```bash
cat /opt/aiops/run/aiops.pid
tail -f /opt/aiops/logs/aiops.log
curl http://127.0.0.1:8080/actuator/health
```

Stop the app manually:

```bash
kill "$(cat /opt/aiops/run/aiops.pid)"
rm -f /opt/aiops/run/aiops.pid
```

## Rollback

The workflow keeps the latest 5 releases under `/opt/aiops/releases`. To roll back:

```bash
ls -lt /opt/aiops/releases
ln -sfn /opt/aiops/releases/<previous-release-id> /opt/aiops/current
kill "$(cat /opt/aiops/run/aiops.pid)" || true
nohup java -jar /opt/aiops/current/backend/app.jar \
  --spring.profiles.active=prod \
  --spring.config.additional-location=file:/opt/aiops/config/application-prod.yml \
  > /opt/aiops/logs/aiops.log 2>&1 &
echo "$!" > /opt/aiops/run/aiops.pid
```
