$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
mvn.cmd -q -DskipTests compile dependency:copy-dependencies -DincludeScope=runtime
if ($LASTEXITCODE -ne 0) { throw "La preparación de dependencias Maven falló." }
New-Item -ItemType Directory -Force out | Out-Null
$fuentes = @(Get-ChildItem src -Filter *.java -Recurse | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 --release 21 -cp "target\dependency\*" -d out $fuentes
if ($LASTEXITCODE -ne 0) { throw "La compilación falló." }
& java -cp out ar.arenasur.Pruebas
if ($LASTEXITCODE -ne 0) { throw "Fallaron las pruebas." }
