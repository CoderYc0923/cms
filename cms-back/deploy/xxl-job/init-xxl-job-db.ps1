# Init xxl_job database (create DB + download SQL + import + verify)
# Usage (from cms-back):
#   .\deploy\xxl-job\init-xxl-job-db.ps1

$ErrorActionPreference = "Stop"

$RootDir   = Resolve-Path (Join-Path $PSScriptRoot "../../")
$EnvFile   = Join-Path $RootDir ".env"
$SqlFile   = Join-Path $PSScriptRoot "tables_xxl_job.sql"

function Get-EnvValue($key, $default) {
    if (-not (Test-Path $EnvFile)) { return $default }
    foreach ($line in Get-Content $EnvFile) {
        if ($line -match "^\s*$key=(.*)$") { return $Matches[1].Trim() }
    }
    return $default
}

$MysqlContainer = "cms-mysql"
$RootPassword   = Get-EnvValue "MYSQL_ROOT_PASSWORD" ""
$XxlVersion     = Get-EnvValue "XXL_JOB_ADMIN_IMAGE_TAG" "2.4.2"

if ([string]::IsNullOrWhiteSpace($RootPassword)) {
    Write-Error "Set MYSQL_ROOT_PASSWORD in cms-back/.env"
}

$running = docker ps --filter "name=$MysqlContainer" --format "{{.Names}}"
if (-not $running) {
    Write-Error "Container $MysqlContainer is not running. Run: docker compose up -d db"
}

$exists = docker exec $MysqlContainer mysql -uroot -p"$RootPassword" -N -e `
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='xxl_job' AND table_name='xxl_job_group';"
if ($exists.Trim() -eq "1") {
    Write-Host "[skip] xxl_job_group already exists, skip init"
    exit 0
}

Write-Host "[1/4] CREATE DATABASE xxl_job ..."
docker exec -i $MysqlContainer mysql -uroot -p"$RootPassword" -e `
    "CREATE DATABASE IF NOT EXISTS xxl_job DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

Write-Host "[2/4] Download tables_xxl_job.sql (tag=$XxlVersion) ..."
$url = "https://raw.githubusercontent.com/xuxueli/xxl-job/$XxlVersion/doc/db/tables_xxl_job.sql"
Invoke-WebRequest -Uri $url -OutFile $SqlFile

Write-Host "[3/4] Import SQL ..."
# Use cmd redirect to preserve UTF-8 (PowerShell pipe may corrupt Chinese comments)
$importCmd = "docker exec -i $MysqlContainer mysql --default-character-set=utf8mb4 -uroot -p$RootPassword xxl_job < `"$SqlFile`""
cmd /c $importCmd
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to import tables_xxl_job.sql"
}

Write-Host "[4/4] Verify ..."
docker exec -i $MysqlContainer mysql -uroot -p"$RootPassword" -e "USE xxl_job; SHOW TABLES;"

Write-Host "Done. Run: docker compose up -d xxl-job-admin"
Write-Host "Admin UI: http://127.0.0.1:8088/xxl-job-admin (admin / 123456)"
