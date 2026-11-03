$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (Test-Path .env) {
    Get-Content .env | Where-Object { $_ -match '^\s*[^#][^=]*=' } | ForEach-Object {
        $partes = $_ -split '=', 2
        [Environment]::SetEnvironmentVariable($partes[0].Trim(), $partes[1].Trim())
    }
}

$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $maven) {
    throw "No se encontró mvn.cmd en PATH. Instalá Maven o agregalo a PATH."
}

& $maven.Source -q -DskipTests compile dependency:copy-dependencies -DincludeScope=runtime
if ($LASTEXITCODE -ne 0) { throw "La compilación Maven falló." }

& java -cp "target\classes;target\dependency\*" ar.arenasur.config.VerificarConexion
if ($LASTEXITCODE -ne 0) { throw "La verificación de conexión falló." }
