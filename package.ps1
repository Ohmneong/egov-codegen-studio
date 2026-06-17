# 설치본(자체포함 실행 폴더, app-image) 생성 스크립트
# 사용: powershell -ExecutionPolicy Bypass -File .\package.ps1
# 결과: package\egov-codegen-studio\egov-codegen-studio.exe  (JRE 내장, 더블클릭 실행)
#
# 왜 app-image 인가: .msi/.exe 인스톨러는 WiX 가 필요한데 환경에 없다.
# app-image 는 JRE 를 통째로 번들한 실행 폴더라, 폴더만 복사하면 Java 설치 없이 어디서든 돈다.
# (폐쇄망 USB 반입에 적합 — 인스톨러보다 단순)

$ErrorActionPreference = "Stop"

# jpackage 는 풀 JDK(14+)에만 있다. 번들 justj 는 JRE라 없으므로 PATH/시스템 JDK 에서 탐색.
$jpackage = (Get-Command jpackage -ErrorAction SilentlyContinue).Source
if (-not $jpackage) {
    $jpackage = Get-ChildItem "C:\Program Files\Java" -Recurse -Filter "jpackage.exe" -ErrorAction SilentlyContinue |
                Select-Object -First 1 -ExpandProperty FullName
}
if (-not $jpackage) {
    Write-Host "[오류] jpackage.exe 를 찾지 못했습니다. JDK 14+ 가 필요합니다(번들 JRE에는 없음)." -ForegroundColor Red
    exit 1
}
Write-Host "jpackage: $jpackage"

$root = $PSScriptRoot

# 최신 jar 보장 — 빌드 먼저 수행
powershell -ExecutionPolicy Bypass -File (Join-Path $root "build.ps1") | Out-Null
if ($LASTEXITCODE -ne 0) { Write-Host "[오류] 빌드 실패" -ForegroundColor Red; exit 1 }

$dest = Join-Path $root "package"
$appDir = Join-Path $dest "egov-codegen-studio"
if (Test-Path $appDir) { Remove-Item $appDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $dest | Out-Null

# jar 의 Main-Class 는 CLI(Main)이므로, GUI 진입점을 --main-class 로 덮어쓴다.
& $jpackage --type app-image `
    --name "egov-codegen-studio" `
    --app-version "1.0.0" `
    --vendor "Hanbit" `
    --input (Join-Path $root "dist") `
    --main-jar "egov-crud-gen.jar" `
    --main-class "com.hanbit.egovgen.ui.GenGuiApp" `
    --dest $dest `
    --java-options "-Dfile.encoding=UTF-8"
if ($LASTEXITCODE -ne 0) { Write-Host "[오류] jpackage 실패" -ForegroundColor Red; exit 1 }

# 기본 설정 파일을 실행 폴더에 동봉(없어도 GUI 는 내장 기본값으로 뜬다)
Copy-Item (Join-Path $root "gen.properties") $appDir -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "완료: $appDir\egov-codegen-studio.exe" -ForegroundColor Green
Write-Host "이 'egov-codegen-studio' 폴더를 통째로 복사하면 JRE 설치 없이 어디서든 실행됩니다."
