$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $maven) {
    throw "No se encontró mvn.cmd en PATH. Instalá Maven o agregalo a PATH."
}

& $maven.Source -q -DskipTests test-compile dependency:copy-dependencies -DincludeScope=runtime
if ($LASTEXITCODE -ne 0) { throw "La compilación Maven falló." }

& java -cp "target\classes;target\test-classes;target\dependency\*" ar.arenasur.IntegracionReservaJdbc
if ($LASTEXITCODE -ne 0) { throw "La integración transaccional de reservas falló." }
