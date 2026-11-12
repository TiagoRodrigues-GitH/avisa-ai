<#
  Inicia o Avisa Aí no PC: compila o app (se ainda não foi) e sobe a API, que também entrega o app.
  Opcional: crie "config-local.ps1" ao lado deste arquivo (fica fora do Git) com, por exemplo:
      $env:APP_PASSWORD_HASH = '$2a$12$...'   # gerado por backend\scripts\bcrypt-hash.ps1
      $env:APP_DEMO_DATA = 'false'
#>
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$local = Join-Path $root 'config-local.ps1'
if (Test-Path $local) { . $local }

$dist = Join-Path $root 'frontend\dist\frontend\browser\index.html'
if (-not (Test-Path $dist)) {
    Write-Host 'Compilando o app (primeira vez)...' -ForegroundColor Cyan
    Push-Location (Join-Path $root 'frontend'); npm install; npm run build; Pop-Location
}

# Java 25 no Windows: a pasta TEMP com espaço no caminho impede o Tomcat de abrir a porta.
$tmp = 'C:\Temp'; New-Item -ItemType Directory -Force $tmp | Out-Null

# IP da placa de rede com gateway (Wi-Fi/cabo de verdade, não adaptadores virtuais)
$ip = (Get-NetIPConfiguration -ErrorAction SilentlyContinue | Where-Object { $_.IPv4DefaultGateway } | Select-Object -First 1).IPv4Address.IPAddress
Write-Host ''
Write-Host 'Avisa Aí:' -ForegroundColor Green
Write-Host '  neste PC:   http://localhost:8080'
if ($ip) { Write-Host "  no celular: http://${ip}:8080  (mesma rede Wi-Fi)" }
Write-Host '  API:        http://localhost:8080/swagger-ui.html'
Write-Host ''

Push-Location (Join-Path $root 'backend')
mvn -q spring-boot:run "-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=$tmp"
Pop-Location
