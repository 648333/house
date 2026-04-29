# Integrations

This project includes optional integration files for Spark analytics and Xshell-based deployment.

## Spark

Current local check found no `spark-submit` command in PATH. To enable Spark:

1. Install Apache Spark.
2. Set `SPARK_HOME`.
3. Add `%SPARK_HOME%\bin` to PATH.
4. Download `mysql-connector-j` and pass the jar path when running the job.

Run from the project root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run_spark_analytics.ps1 -MysqlConnectorJar C:\path\mysql-connector-j.jar
```

The Spark job reads MySQL tables:

- `properties`
- `support_tickets`

It writes JSON reports into:

```text
frontend/public/spark-analytics
```

Reports:

- `price_by_type`
- `ticket_pressure`
- `hot_tags`

## Xshell

Xshell is an SSH client. It is not embedded into the web app; use it to connect to a Linux server and run deployment commands.

This machine has Xshell 8 installed at:

```text
D:\虚拟机\Xshell.exe
```

Open Xshell and copy the deployment command automatically:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\open_xshell_deploy.ps1
```

Suggested flow:

```bash
cd /opt/housing-platform
chmod +x deploy/remote_deploy.sh
APP_DIR=/opt/housing-platform BACKEND_PORT=8080 bash deploy/remote_deploy.sh
```

Use `deploy/xshell.env.example` as the deployment parameter reference.
