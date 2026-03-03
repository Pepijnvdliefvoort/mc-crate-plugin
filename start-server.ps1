# Refresh PATH to use Java 21
$env:PATH = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

Write-Host "Starting Paper test server..." -ForegroundColor Green
.\gradlew.bat runServer
