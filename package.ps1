# 배포본 생성 스크립트
# 사용:
#   powershell -ExecutionPolicy Bypass -File .\package.ps1            # app-image (JRE 내장 실행 폴더, 기본)
#   powershell -ExecutionPolicy Bypass -File .\package.ps1 -Type exe  # .exe 인스톨러 (WiX 3.x 필요)
#
# app-image : JRE 를 번들한 실행 폴더. 폴더 복사 = 설치, exe 더블클릭 = 실행. WiX 불필요.
# exe/msi   : 진짜 인스톨러(설치 마법사·시작메뉴·제거 등록). WiX 3.x(candle/light) 필요.
param([string]$Type = "app-image")  # app-image | exe | msi

$ErrorActionPreference = "Stop"

# jpackage 는 풀 JDK(14+)에만 있다(번들 justj 는 JRE라 없음). PATH/시스템 JDK 에서 탐색.
$jpackage = (Get-Command jpackage -ErrorAction SilentlyContinue).Source
if (-not $jpackage) {
    $jpackage = Get-ChildItem "C:\Program Files\Java" -Recurse -Filter "jpackage.exe" -ErrorAction SilentlyContinue |
                Select-Object -First 1 -ExpandProperty FullName
}
if (-not $jpackage) { Write-Host "[오류] jpackage.exe 없음(JDK 14+ 필요)." -ForegroundColor Red; exit 1 }
Write-Host "jpackage: $jpackage"

# 인스톨러(exe/msi)는 WiX 3.x(candle/light)가 PATH 에 있어야 한다. 설치돼 있으면 PATH 에 보강.
if ($Type -ne "app-image") {
    if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
        $wixBin = Get-ChildItem "C:\Program Files (x86)" -Directory -Filter "WiX Toolset*" -ErrorAction SilentlyContinue |
                  Sort-Object Name -Descending | Select-Object -First 1 | ForEach-Object { Join-Path $_.FullName "bin" }
        if ($wixBin -and (Test-Path (Join-Path $wixBin "candle.exe"))) {
            $env:PATH = "$wixBin;$env:PATH"
            Write-Host "WiX: $wixBin (PATH에 추가)"
        } else {
            Write-Host "[오류] WiX 3.x(candle/light)를 찾지 못했습니다. 'choco install wixtoolset' 후 다시 실행하세요." -ForegroundColor Red
            exit 1
        }
    }
}

$root = $PSScriptRoot

# 최신 jar 보장 — 빌드 먼저
powershell -ExecutionPolicy Bypass -File (Join-Path $root "build.ps1") | Out-Null
if ($LASTEXITCODE -ne 0) { Write-Host "[오류] 빌드 실패" -ForegroundColor Red; exit 1 }

$dest = Join-Path $root "package"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
$appDir = Join-Path $dest "egov-codegen-studio"
if ($Type -eq "app-image" -and (Test-Path $appDir)) { Remove-Item $appDir -Recurse -Force }

# jar 의 Main-Class 는 CLI(Main)이므로 GUI 진입점을 --main-class 로 덮어쓴다.
$jpArgs = @(
    "--type", $Type,
    "--name", "egov-codegen-studio",
    "--app-version", "1.0.0",
    "--vendor", "Hanbit",
    "--input", (Join-Path $root "dist"),
    "--main-jar", "egov-crud-gen.jar",
    "--main-class", "com.hanbit.egovgen.ui.GenGuiApp",
    "--dest", $dest,
    "--java-options", "-Dfile.encoding=UTF-8"
)
if ($Type -ne "app-image") {
    # 시작메뉴·바탕화면 바로가기, 설치경로 선택, 사용자 단위 설치(관리자 권한 불필요)
    $jpArgs += @("--win-menu", "--win-shortcut", "--win-dir-chooser", "--win-per-user-install")
}

& $jpackage @jpArgs
if ($LASTEXITCODE -ne 0) { Write-Host "[오류] jpackage 실패" -ForegroundColor Red; exit 1 }

if ($Type -eq "app-image") {
    Copy-Item (Join-Path $root "gen.properties") $appDir -Force -ErrorAction SilentlyContinue
    Write-Host ""
    Write-Host "완료(app-image): $appDir\egov-codegen-studio.exe" -ForegroundColor Green
    Write-Host "이 'egov-codegen-studio' 폴더를 통째로 복사하면 JRE 설치 없이 어디서든 실행됩니다."
} else {
    Write-Host ""
    Write-Host "완료(인스톨러): $dest\egov-codegen-studio-1.0.0.$Type" -ForegroundColor Green
    Write-Host "이 인스톨러를 실행하면 설치 마법사가 뜨고, 설치 후 시작메뉴/바탕화면에서 실행할 수 있습니다."
}
