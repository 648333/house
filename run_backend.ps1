# Set Maven version and download URL
$mavenVersion = "3.9.6"
$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
$mavenDir = "maven-tools"
$mavenBin = "$mavenDir\apache-maven-$mavenVersion\bin\mvn.cmd"

# Create tools directory if it doesn't exist
if (-not (Test-Path $mavenDir)) {
    Write-Host "Creating $mavenDir directory..."
    New-Item -ItemType Directory -Force -Path $mavenDir
}

# Check if Maven is already downloaded
if (-not (Test-Path $mavenBin)) {
    Write-Host "Downloading Maven $mavenVersion..."
    $zipFile = "$mavenDir\maven.zip"
    Invoke-WebRequest -Uri $mavenUrl -OutFile $zipFile
    
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $zipFile -DestinationPath $mavenDir -Force
    
    # Cleanup zip file
    Remove-Item $zipFile
    Write-Host "Maven installed successfully!"
} else {
    Write-Host "Maven is already installed."
}

# Run the backend
Write-Host "Starting Spring Boot Backend..."
Set-Location backend
$jvmCompatOpts = "--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow"
$env:MAVEN_OPTS = "$jvmCompatOpts $env:MAVEN_OPTS".Trim()

if (Test-Path "..\backend.local.env.ps1") {
    Write-Host "Loading local backend environment from backend.local.env.ps1..."
    . "..\backend.local.env.ps1"
}

if (-not $env:DB_PASSWORD) {
    Write-Host "DB_PASSWORD is not set. Please set DB_URL / DB_USERNAME / DB_PASSWORD before starting the backend." -ForegroundColor Yellow
    Write-Host "Example:" -ForegroundColor Yellow
    Write-Host '$env:DB_USERNAME="root"' -ForegroundColor Yellow
    Write-Host '$env:DB_PASSWORD="your-mysql-password"' -ForegroundColor Yellow
    Write-Host '$env:DB_URL="jdbc:mysql://localhost:3306/housing_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci"' -ForegroundColor Yellow
}

& "..\$mavenBin" spring-boot:run
