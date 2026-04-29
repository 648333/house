param(
  [string]$JdbcUrl = "jdbc:mysql://127.0.0.1:3306/housing_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8",
  [string]$DbUser = "root",
  [string]$DbPassword = "031209",
  [string]$MysqlConnectorJar = "",
  [string]$Output = "frontend/public/spark-analytics"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Job = Join-Path $Root "integrations\spark\property_analytics.py"

$SparkSubmit = Get-Command spark-submit -ErrorAction SilentlyContinue
if (-not $SparkSubmit) {
  Write-Host "spark-submit was not found in PATH."
  Write-Host "Install Apache Spark, set SPARK_HOME, and add %SPARK_HOME%\bin to PATH."
  Write-Host "Then rerun: powershell -ExecutionPolicy Bypass -File scripts\run_spark_analytics.ps1 -MysqlConnectorJar C:\path\mysql-connector-j.jar"
  exit 1
}

$Args = @()
if ($MysqlConnectorJar) {
  if (-not (Test-Path -LiteralPath $MysqlConnectorJar)) {
    throw "MySQL connector jar not found: $MysqlConnectorJar"
  }
  $Args += @("--jars", $MysqlConnectorJar)
}

$Args += @(
  $Job,
  "--jdbc-url", $JdbcUrl,
  "--db-user", $DbUser,
  "--db-password", $DbPassword,
  "--output", (Join-Path $Root $Output)
)

& $SparkSubmit.Source @Args
