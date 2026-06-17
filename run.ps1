# 실행 래퍼 — java 경로를 자동으로 찾아 생성기를 돌립니다.
# 사용 예:
#   .\run.ps1 --ddl sample\my.sql --config gen.properties
#   .\run.ps1 --ddl sample\my.sql --config gen.properties --idgnr
# (build.ps1 로 먼저 빌드해 두어야 합니다)

$ErrorActionPreference = "Stop"

# 번들 JDK(java.exe) 자동 탐색 — 환경에 맞게 $jdkRoot 만 바꾸면 됩니다.
$jdkRoot = "C:\eGovFrameDev-5.0.1-Windows-64bit\eclipse\plugins"
$java = Get-ChildItem $jdkRoot -Recurse -Filter "java.exe" -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
if (-not $java) {
    Write-Host "[오류] java.exe 를 찾지 못했습니다. run.ps1 의 `$jdkRoot 경로를 확인하세요." -ForegroundColor Red
    exit 1
}

$jar = Join-Path $PSScriptRoot "dist\egov-crud-gen.jar"
if (-not (Test-Path $jar)) {
    Write-Host "[오류] dist\egov-crud-gen.jar 가 없습니다. 먼저 빌드하세요:" -ForegroundColor Red
    Write-Host "       powershell -ExecutionPolicy Bypass -File .\build.ps1" -ForegroundColor Yellow
    exit 1
}

# 입력한 옵션(--ddl 등)을 그대로 생성기에 전달
& $java "-Dfile.encoding=UTF-8" -jar $jar @args
