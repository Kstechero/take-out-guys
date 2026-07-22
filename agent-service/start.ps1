param(
    [string]$HostName = "127.0.0.1",
    [int]$Port = 8000,
    [switch]$NoReload
)

$ErrorActionPreference = "Stop"

$ServiceRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

if (-not (Test-Path (Join-Path $ServiceRoot "app\main.py"))) {
    Write-Error "Cannot find agent-service app at $ServiceRoot"
}

$python = Get-Command python -ErrorAction SilentlyContinue
if ($null -eq $python) {
    Write-Error "Python is not available in PATH. Install Python 3.11+ or add it to PATH."
}

Push-Location $ServiceRoot
try {
    Write-Host "Starting Agent Service at http://$HostName`:$Port"
    Write-Host "Health check: http://$HostName`:$Port/health"
    Write-Host "Press Ctrl+C to stop."

    $uvicornArgs = @(
        "-m",
        "uvicorn",
        "app.main:app",
        "--host",
        $HostName,
        "--port",
        "$Port"
    )
    if (-not $NoReload) {
        $uvicornArgs += "--reload"
    }

    & python @uvicornArgs
}
finally {
    Pop-Location
}
