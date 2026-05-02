# Windows AGP バグ対処テスト実行スクリプト
# Gradle デーモン停止 → クリーン PATH → テスト実行

param([string]$Task = "testDebugUnitTest")

# Gradle デーモンを停止
Write-Host "Gradle デーモンを停止中..."
.\gradlew.bat --stop 2>&1 | Out-Null

# PATH からスペースを含むパスを除去
$cleanPath = ($env:PATH -split ';' | Where-Object { $_ -notmatch ' ' }) -join ';'
$env:PATH = $cleanPath

Write-Host "テスト実行: $Task"
.\gradlew.bat $Task --info 2>&1
