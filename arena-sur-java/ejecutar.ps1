$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (Test-Path .env) {
    Get-Content .env | Where-Object { $_ -match '^\s*[^#][^=]*=' } | ForEach-Object {
        $partes = $_ -split '=', 2
        [Environment]::SetEnvironmentVariable($partes[0].Trim(), $partes[1].Trim())
    }
}
if ([string]::IsNullOrWhiteSpace($env:ARENA_SUR_DB_URL)) {
    Write-Warning "ARENA_SUR_DB_URL no está configurada. La aplicación se ejecutará en modo memoria y perderá los datos al salir."
}
mvn.cmd -q -DskipTests compile dependency:copy-dependencies -DincludeScope=runtime
if ($LASTEXITCODE -ne 0) { throw "La preparación de dependencias Maven falló." }
New-Item -ItemType Directory -Force out | Out-Null
$fuentes = @(Get-ChildItem src/main/java -Filter *.java -Recurse | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 --release 21 -d out $fuentes
if ($LASTEXITCODE -ne 0) { throw "La compilación falló." }
& java -cp "out;target\dependency\*" ar.arenasur.consola.Main
