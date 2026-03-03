# Refresh PATH to use Java 21
$env:PATH = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

Write-Host "Building plugin..." -ForegroundColor Cyan
.\gradlew.bat jar copyToServer

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nPlugin built and copied to run/plugins/" -ForegroundColor Green
    Write-Host "Now type in the server console: " -ForegroundColor Yellow -NoNewline
    Write-Host "reload confirm" -ForegroundColor White
} else {
    Write-Host "`nBuild failed!" -ForegroundColor Red
}
