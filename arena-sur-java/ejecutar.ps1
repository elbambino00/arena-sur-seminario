$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force out | Out-Null
$fuentes = @(Get-ChildItem src/main/java -Filter *.java -Recurse | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 --release 17 -d out $fuentes
if ($LASTEXITCODE -ne 0) { throw "La compilación falló." }
& java -cp out ar.arenasur.consola.Main
