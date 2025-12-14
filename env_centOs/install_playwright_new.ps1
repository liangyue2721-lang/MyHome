# PowerShell script to install Playwright
# Set execution policy for current process
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force

# Define progress function
function Show-Progress($activity, $percent) {
    Write-Progress -Activity $activity -Status "$percent% Complete" -PercentComplete $percent
}

# Print header
Write-Host "==============================" -ForegroundColor Cyan
Write-Host " Starting Playwright Installation " -ForegroundColor Yellow
Write-Host "==============================" -ForegroundColor Cyan

# Check if Python is installed
Show-Progress "Checking Python environment..." 10
if (Get-Command python -ErrorAction SilentlyContinue) {
    $pyVersion = python --version 2>&1
    Write-Host "✅ Python detected: $pyVersion" -ForegroundColor Green
} else {
    Write-Host "❌ Python not found. Please install Python 3.10 or higher." -ForegroundColor Red
    exit 1
}

# Check if pip is available
Show-Progress "Checking pip..." 25
if (Get-Command pip -ErrorAction SilentlyContinue) {
    Write-Host "✅ pip is available." -ForegroundColor Green
} else {
    Write-Host "❌ pip not found. Make sure Python was installed with 'Add Python to PATH' option." -ForegroundColor Red
    exit 1
}

# Upgrade pip
Show-Progress "Upgrading pip..." 40
try {
    python -m pip install --upgrade pip -q
    Write-Host "✅ pip upgraded successfully." -ForegroundColor Green
} catch {
    Write-Host "⚠️ Failed to upgrade pip: $_" -ForegroundColor Yellow
}

# Install Playwright
Show-Progress "Installing Playwright..." 60
try {
    pip install playwright -q
    Write-Host "✅ Playwright installed successfully." -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to install Playwright: $_" -ForegroundColor Red
    exit 1
}

# Install browser drivers
Show-Progress "Installing browser drivers..." 80
try {
    python -m playwright install
    Write-Host "✅ Browser drivers installed successfully." -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to install browser drivers: $_" -ForegroundColor Red
    exit 1
}

# Finish
Show-Progress "Installation complete!" 100
Write-Host "==============================" -ForegroundColor Cyan
Write-Host "✅ Playwright environment installed successfully!" -ForegroundColor Green
Write-Host " You can now run: python -m playwright codegen" -ForegroundColor Yellow
Write-Host "==============================" -ForegroundColor Cyan