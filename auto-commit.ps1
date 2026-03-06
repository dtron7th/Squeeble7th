# Auto-commit and push script for Windows
# Save as auto-commit.ps1

Write-Host "🤖 Starting auto-commit process..." -ForegroundColor Green

# Navigate to your repo directory
Set-Location "C:\Users\dtron\OneDrive\Documents\GitHub\Squeeble7th"

# Check for changes
$changes = git status --porcelain
if (-not $changes) {
    Write-Host "No changes to commit." -ForegroundColor Yellow
    exit
}

# Add all changes
Write-Host "📝 Staging changes..." -ForegroundColor Blue
git add .

# Get timestamp
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

# Commit changes
Write-Host "💾 Committing changes..." -ForegroundColor Blue
git commit -m "Auto-commit: $timestamp"

# Push to GitHub
Write-Host "🚀 Pushing to GitHub..." -ForegroundColor Blue
try {
    git push origin main
    Write-Host "✅ Auto-pushed to GitHub successfully!" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to push to GitHub. Check connection or run 'git push' manually." -ForegroundColor Red
}

Write-Host "🎉 Auto-commit process completed!" -ForegroundColor Green
