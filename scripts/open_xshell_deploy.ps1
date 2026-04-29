param(
  [string]$AppDir = "/opt/housing-platform",
  [int]$BackendPort = 8080,
  [string]$XshellPath = ""
)

$ErrorActionPreference = "Stop"

function Resolve-XshellPath {
  param([string]$ExplicitPath)

  if ($ExplicitPath -and (Test-Path -LiteralPath $ExplicitPath)) {
    return (Resolve-Path -LiteralPath $ExplicitPath).Path
  }

  $shortcut = "C:\ProgramData\Microsoft\Windows\Start Menu\Programs\Xshell 8\Xshell.lnk"
  if (Test-Path -LiteralPath $shortcut) {
    $shell = New-Object -ComObject WScript.Shell
    $lnk = $shell.CreateShortcut($shortcut)
    if ($lnk.TargetPath -and (Test-Path -LiteralPath $lnk.TargetPath)) {
      return $lnk.TargetPath
    }
  }

  $registryPaths = @(
    "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*",
    "HKLM:\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*",
    "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*"
  )

  foreach ($registryPath in $registryPaths) {
    $app = Get-ItemProperty $registryPath -ErrorAction SilentlyContinue |
      Where-Object { $_.DisplayName -like "*Xshell*" -and $_.InstallLocation } |
      Select-Object -First 1

    if ($app) {
      $candidate = Join-Path $app.InstallLocation "Xshell.exe"
      if (Test-Path -LiteralPath $candidate) {
        return $candidate
      }
    }
  }

  throw "Xshell.exe was not found. Pass it explicitly: -XshellPath `"D:\path\Xshell.exe`""
}

$Root = Split-Path -Parent $PSScriptRoot
$ResolvedXshell = Resolve-XshellPath -ExplicitPath $XshellPath

$DeployCommand = @"
cd $AppDir
chmod +x deploy/remote_deploy.sh
APP_DIR=$AppDir BACKEND_PORT=$BackendPort bash deploy/remote_deploy.sh
"@

$DeployCommand | Set-Clipboard

Write-Host "Xshell found: $ResolvedXshell"
Write-Host "Deployment command copied to clipboard. Connect to your Linux server in Xshell, then paste and run:"
Write-Host ""
Write-Host $DeployCommand

Start-Process -FilePath $ResolvedXshell -WorkingDirectory (Split-Path -Parent $ResolvedXshell)
