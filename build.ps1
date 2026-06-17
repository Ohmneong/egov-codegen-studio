# eGov CRUD 코드 제너레이터 빌드 스크립트 (외부 의존성 0, 번들 JDK 사용)
# 사용: powershell -ExecutionPolicy Bypass -File build.ps1

$ErrorActionPreference = "Stop"

# 번들 JDK 자동 탐색 (eGovFrameDev 내 justj openjdk). 환경에 맞게 조정 가능.
$jdkRoot = "C:\eGovFrameDev-5.0.1-Windows-64bit\eclipse\plugins"
$javac = Get-ChildItem $jdkRoot -Recurse -Filter "javac.exe" -ErrorAction SilentlyContinue |
         Select-Object -First 1 -ExpandProperty FullName
$jarExe = Get-ChildItem $jdkRoot -Recurse -Filter "jar.exe" -ErrorAction SilentlyContinue |
          Select-Object -First 1 -ExpandProperty FullName

if (-not $javac) { Write-Error "javac.exe 를 찾지 못했습니다. JDK 경로를 확인하세요."; exit 1 }
Write-Host "javac: $javac"

$root = $PSScriptRoot
$srcDir = Join-Path $root "src"
$buildDir = Join-Path $root "build\classes"
$distDir = Join-Path $root "dist"
$jarPath = Join-Path $distDir "egov-crud-gen.jar"

# 클린 & 디렉터리 준비
if (Test-Path (Join-Path $root "build")) { Remove-Item (Join-Path $root "build") -Recurse -Force }
New-Item -ItemType Directory -Force -Path $buildDir | Out-Null
New-Item -ItemType Directory -Force -Path $distDir | Out-Null

# 소스 목록 → 임시 파일 (인자 길이 회피)
$sources = Get-ChildItem $srcDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
$srcList = Join-Path $root "build\sources.txt"
# BOM 없이 기록 (javac @argfile 은 BOM을 인식하지 못함)
[System.IO.File]::WriteAllLines($srcList, $sources)

# 컴파일
Write-Host "컴파일 중... ($($sources.Count) 파일)"
& $javac -encoding UTF-8 -d $buildDir "@$srcList"
if ($LASTEXITCODE -ne 0) { Write-Error "컴파일 실패"; exit 1 }

# JAR 패키징 (Main-Class 매니페스트)
Write-Host "JAR 생성 중..."
& $jarExe --create --file $jarPath --main-class com.hanbit.egovgen.Main -C $buildDir .
if ($LASTEXITCODE -ne 0) { Write-Error "JAR 생성 실패"; exit 1 }

Write-Host ""
Write-Host "빌드 완료: $jarPath"
Write-Host "실행 예: java -jar `"$jarPath`" --ddl sample\sample.sql --config gen.properties"
