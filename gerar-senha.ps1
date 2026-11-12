# Gera uma senha fixa para o usuário admin do Avisa Aí, grava o hash em config-local.ps1
# e a senha em NOTAS_acesso_avisa_ai.txt (os dois ficam fora do Git). Rode de novo para trocar a senha.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
$bytes = New-Object byte[] 14
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$pwd = -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })

$m2 = Join-Path $env:USERPROFILE '.m2\repository'
$crypto = Get-ChildItem "$m2\org\springframework\security\spring-security-crypto" -Recurse -Filter 'spring-security-crypto-*.jar' |
    Where-Object Name -notmatch 'sources|javadoc' | Sort-Object FullName | Select-Object -Last 1
$jcl = Get-ChildItem "$m2\org\springframework\spring-jcl" -Recurse -Filter 'spring-jcl-*.jar' | Select-Object -Last 1
$src = Join-Path $env:TEMP "BcryptHash$PID.java"
Set-Content $src 'public class BcryptHash { public static void main(String[] a) { System.out.print(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12).encode(System.getenv("BCRYPT_INPUT"))); } }'
$cp = $crypto.FullName + $(if ($jcl) { ';' + $jcl.FullName } else { '' })
$env:BCRYPT_INPUT = $pwd
try { $hash = & java -cp $cp $src 2>$null } finally { $env:BCRYPT_INPUT = $null; Remove-Item $src -ErrorAction SilentlyContinue }
if ($hash -notmatch '^\$2a\$12\$.{53}$') { throw 'Falha ao gerar o hash.' }

$utf8 = New-Object Text.UTF8Encoding $true
[IO.File]::WriteAllText((Join-Path $root 'config-local.ps1'), "`$env:APP_USERNAME = 'admin'`r`n`$env:APP_PASSWORD_HASH = '$hash'`r`n", $utf8)
$notes = @"
ACESSO AO AVISA AÍ (arquivo pessoal: não compartilhe, não envie ao GitHub)
Gerado em: $(Get-Date -Format 'dd/MM/yyyy HH:mm')

Endereço no PC:      http://localhost:8080
Endereço no celular: http://IP-DO-PC:8080 (mesma rede Wi-Fi; o iniciar.ps1 mostra o IP)
Usuário:             admin
Senha:               $pwd

Para trocar a senha: rode gerar-senha.ps1 de novo e reinicie (feche a janela do servidor e rode iniciar.ps1).
"@
[IO.File]::WriteAllText((Join-Path $root 'NOTAS_acesso_avisa_ai.txt'), $notes, $utf8)
'ok'
