$ErrorActionPreference = "Stop"
Set-Location "E:\AI\trae\trae project\daojishi\backend"
$jvmCompatOpts = "--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow"
$env:MAVEN_OPTS = "$jvmCompatOpts $env:MAVEN_OPTS".Trim()
if (Test-Path "E:\AI\trae\trae project\daojishi\backend.local.env.ps1") {
    . "E:\AI\trae\trae project\daojishi\backend.local.env.ps1"
}
& "E:\AI\trae\trae project\daojishi\maven-tools\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run 1>> "E:\AI\trae\trae project\daojishi\backend.dev.log" 2>> "E:\AI\trae\trae project\daojishi\backend.dev.err.log"
