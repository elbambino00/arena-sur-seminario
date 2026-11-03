$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (Test-Path .env) {
    Get-Content .env | Where-Object { $_ -match '^\s*[^#][^=]*=' } | ForEach-Object {
        $partes = $_ -split '=', 2
        [Environment]::SetEnvironmentVariable($partes[0].Trim(), $partes[1].Trim())
    }
}
if ([string]::IsNullOrWhiteSpace($env:ARENA_SUR_DB_URL)) {
    throw "Falta ARENA_SUR_DB_URL. Configurá arena-sur-java/.env antes de iniciar JavaFX."
}
mvn.cmd -q javafx:run
