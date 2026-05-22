# PowerShell Build & Launch Script: Automated Time Table Generation System

$ErrorActionPreference = "Stop"

# 1. Clean and prepare output directories
Write-Host "=============================================" -ForegroundColor Magenta
Write-Host "   Time Table Management: Project Compilation Engine   " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Magenta

if (-not (Test-Path "lib/mongo-java-driver-3.12.14.jar")) {
    Write-Host "[-] MongoDB Java Driver missing! Attempting download..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path "lib" | Out-Null
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/mongodb/mongo-java-driver/3.12.14/mongo-java-driver-3.12.14.jar" -OutFile "lib/mongo-java-driver-3.12.14.jar"
    Write-Host "[+] Driver downloaded successfully!" -ForegroundColor Green
} else {
    Write-Host "[+] Verified MongoDB Driver JAR: lib/mongo-java-driver-3.12.14.jar" -ForegroundColor Green
}

if (Test-Path "bin") {
    Remove-Item -Recurse -Force "bin" | Out-Null
}
New-Item -ItemType Directory -Force -Path "bin" | Out-Null

# 2. Gather all Java files
Write-Host "[*] Indexing source code..." -ForegroundColor Cyan
$sources = Get-ChildItem -Path "backend" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName

if ($sources.Count -eq 0) {
    Write-Host "[-] Error: No Java source files found in 'backend/'!" -ForegroundColor Red
    Exit 1
}

Write-Host "[*] Compiling $($sources.Count) Java classes..." -ForegroundColor Cyan
try {
    # Run javac with class paths
    javac -cp "lib/mongo-java-driver-3.12.14.jar" -d bin -sourcepath backend $sources
    Write-Host "[+] Compilation successful! Output generated in 'bin/'" -ForegroundColor Green
} catch {
    Write-Host "[-] Compilation failed! Review syntax errors above." -ForegroundColor Red
    Exit 1
}

# 3. Launch Main Application
Write-Host "[*] Launching Automated Time Table System..." -ForegroundColor Magenta
Write-Host "---------------------------------------------" -ForegroundColor Gray
java -cp "bin;lib/mongo-java-driver-3.12.14.jar" com.timetable.Main
