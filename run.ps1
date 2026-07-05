# Load environment variables from backend/.env
if (Test-Path "backend/.env") {
    Write-Host "Loading environment variables from backend/.env..." -ForegroundColor Green
    Get-Content "backend/.env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $key, $value = $line -split '=', 2
            [System.Environment]::SetEnvironmentVariable($key.Trim(), $value.Trim(), "Process")
        }
    }
} else {
    Write-Warning "backend/.env file not found. Using default values."
}

# Start the Spring Boot backend in a NEW PowerShell window
Write-Host "Starting RentHub Backend in a new window..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Write-Host 'Starting RentHub Backend...' -ForegroundColor Green; mvn spring-boot:run -f backend/pom.xml"

# Start the Vanilla HTML/CSS/JS frontend on port 3000
Write-Host "Starting RentHub Vanilla Frontend on http://localhost:3000..." -ForegroundColor Green
npx -y http-server frontend -p 3000 -c-1
