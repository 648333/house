$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$frontendDir = Join-Path $projectRoot "frontend"
$backendDir = Join-Path $projectRoot "backend"
$mavenCmd = Join-Path $projectRoot "maven-tools\apache-maven-3.9.6\bin\mvn.cmd"
$javaCmd = (Get-Command java).Source
$backendPom = Join-Path $backendDir "pom.xml"
$backendJar = Join-Path $backendDir "target\housing-platform-0.0.1-SNAPSHOT.jar"
$frontendUrl = "http://127.0.0.1:5173"
$backendLog = Join-Path $projectRoot "backend.dev.log"
$backendErrLog = Join-Path $projectRoot "backend.dev.err.log"
$jvmCompatOpts = "--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow"
$localBackendEnv = Join-Path $projectRoot "backend.local.env.ps1"

function Test-PortListening {
    param([int]$Port)

    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return $null -ne $connection
}

function Wait-ForPort {
    param(
        [int]$Port,
        [int]$TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortListening -Port $Port) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

Write-Host ""
Write-Host "=== Warm Home Demo Launcher ===" -ForegroundColor Cyan
Write-Host "Project: $projectRoot"
Write-Host ""

if (-not (Test-Path $frontendDir)) {
    throw "Frontend folder not found: $frontendDir"
}

if (-not (Test-Path $backendDir)) {
    throw "Backend folder not found: $backendDir"
}

if (-not (Test-Path $mavenCmd)) {
    throw "Maven command not found: $mavenCmd"
}

if (Test-Path $localBackendEnv) {
    Write-Host "Loading backend.local.env.ps1..." -ForegroundColor DarkCyan
    . $localBackendEnv
}

if (-not (Test-PortListening -Port 8080)) {
    Write-Host "Starting backend on port 8080..." -ForegroundColor Yellow
    if (Test-Path $backendLog) { Remove-Item $backendLog -Force }
    if (Test-Path $backendErrLog) { Remove-Item $backendErrLog -Force }
    Write-Host "Packaging backend..." -ForegroundColor DarkCyan
    $env:MAVEN_OPTS = "$jvmCompatOpts $env:MAVEN_OPTS".Trim()
    & $mavenCmd -f $backendPom -DskipTests package | Out-Null

    if (-not (Test-Path $backendJar)) {
        throw "Backend jar not found: $backendJar"
    }

    $env:JAVA_TOOL_OPTIONS = "$jvmCompatOpts $env:JAVA_TOOL_OPTIONS".Trim()
    Start-Process -FilePath $javaCmd `
        -ArgumentList "-jar `"$backendJar`"" `
        -WorkingDirectory $backendDir `
        -RedirectStandardOutput $backendLog `
        -RedirectStandardError $backendErrLog | Out-Null

    if (Wait-ForPort -Port 8080 -TimeoutSeconds 90) {
        Write-Host "Backend started successfully." -ForegroundColor Green
    } else {
        Write-Host "Backend did not start successfully." -ForegroundColor Red
        Write-Host "Check logs:" -ForegroundColor Yellow
        Write-Host "  $backendLog"
        Write-Host "  $backendErrLog"
        throw "Backend startup failed."
    }
} else {
    Write-Host "Backend already running on port 8080." -ForegroundColor Green
}

if (-not (Test-PortListening -Port 5173)) {
    Write-Host "Starting frontend on port 5173..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command", "Set-Location '$frontendDir'; npm run dev"
    ) -WorkingDirectory $frontendDir | Out-Null
} else {
    Write-Host "Frontend already running on port 5173." -ForegroundColor Green
}

Write-Host ""
Write-Host "Waiting for frontend, then opening demo page..." -ForegroundColor Cyan
[void](Wait-ForPort -Port 5173 -TimeoutSeconds 20)
Start-Sleep -Seconds 2
Start-Process $frontendUrl | Out-Null

Write-Host "Done." -ForegroundColor Green
Write-Host ""
