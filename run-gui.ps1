# GUI 실행 래퍼 — Swing 화면을 띄웁니다.
# 사용 예:
#   .\run-gui.ps1
# (build.ps1 로 먼저 빌드해 두어야 합니다)
#
# CLI(run.ps1)와 같은 jar 를 쓰지만, jar 의 Main-Class 는 CLI(Main)이므로
# GUI 는 -cp 로 클래스를 직접 지정해 실행합니다. gen.properties 는 작업 폴더에서 읽으므로
# 어디서 실행하든 이 스크립트 위치를 작업 디렉터리로 고정합니다.

$ErrorActionPreference = "Stop"

# 번들 JDK 자동 탐색 — 환경에 맞게 $jdkRoot 만 바꾸면 됩니다.
# GUI 는 콘솔 없이 띄우기 위해 javaw.exe 를 우선 사용하고, 없으면 java.exe 로 대체합니다.
$jdkRoot = "C:\eGovFrameDev-5.0.1-Windows-64bit\eclipse\plugins"
$javaw = Get-ChildItem $jdkRoot -Recurse -Filter "javaw.exe" -ErrorAction SilentlyContinue |
         Select-Object -First 1 -ExpandProperty FullName
if (-not $javaw) {
    $javaw = Get-ChildItem $jdkRoot -Recurse -Filter "java.exe" -ErrorAction SilentlyContinue |
             Select-Object -First 1 -ExpandProperty FullName
}
if (-not $javaw) {
    Write-Host "[오류] javaw.exe/java.exe 를 찾지 못했습니다. run-gui.ps1 의 `$jdkRoot 경로를 확인하세요." -ForegroundColor Red
    exit 1
}

$jar = Join-Path $PSScriptRoot "dist\egov-crud-gen.jar"
if (-not (Test-Path $jar)) {
    Write-Host "[오류] dist\egov-crud-gen.jar 가 없습니다. 먼저 빌드하세요:" -ForegroundColor Red
    Write-Host "       powershell -ExecutionPolicy Bypass -File .\build.ps1" -ForegroundColor Yellow
    exit 1
}

# gen.properties 를 작업 폴더 기준으로 읽으므로 스크립트 위치로 이동
Set-Location $PSScriptRoot
& $javaw "-Dfile.encoding=UTF-8" -cp $jar dev.myoh.egovgen.ui.GenGuiApp
