<#
  Mostra o hash BCrypt (custo 12) de uma senha, para a variável APP_PASSWORD_HASH.
  A senha é digitada oculta e nunca é gravada nem exibida; só o hash aparece.
  Usa o codificador do próprio Spring Security do repositório Maven local (rode "mvn test" em backend/ uma vez antes).

    powershell -ExecutionPolicy Bypass -File backend\scripts\bcrypt-hash.ps1
#>
$m2 = Join-Path $env:USERPROFILE '.m2\repository'
$crypto = Get-ChildItem "$m2\org\springframework\security\spring-security-crypto" -Recurse -Filter 'spring-security-crypto-*.jar' -ErrorAction SilentlyContinue |
    Where-Object Name -notmatch 'sources|javadoc' | Sort-Object FullName | Select-Object -Last 1
if (-not $crypto) { Write-Host 'spring-security-crypto não encontrado: rode "mvn test" em backend/ primeiro.' -ForegroundColor Red; exit 1 }
$jcl = Get-ChildItem "$m2\org\springframework\spring-jcl" -Recurse -Filter 'spring-jcl-*.jar' -ErrorAction SilentlyContinue | Select-Object -Last 1

$secure = Read-Host 'Senha do Avisa Aí (oculta)' -AsSecureString
$again  = Read-Host 'Repita' -AsSecureString
$plain  = [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
$plain2 = [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($again))
if ($plain -ne $plain2) { Write-Host 'As senhas são diferentes.' -ForegroundColor Red; exit 1 }
if ($plain.Length -lt 10) { Write-Host 'Use pelo menos 10 caracteres.' -ForegroundColor Red; exit 1 }

$src = Join-Path $env:TEMP "BcryptHash$PID.java"
Set-Content $src 'public class BcryptHash { public static void main(String[] a) { System.out.print(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12).encode(System.getenv("BCRYPT_INPUT"))); } }'
$cp = $crypto.FullName + $(if ($jcl) { ';' + $jcl.FullName } else { '' })
$env:BCRYPT_INPUT = $plain
try { $hash = & java -cp $cp $src 2>$null } finally { $env:BCRYPT_INPUT = $null; Remove-Item $src -ErrorAction SilentlyContinue }

if ($hash -notmatch '^\$2a\$12\$.{53}$') { Write-Host 'Falha ao gerar o hash.' -ForegroundColor Red; exit 1 }
Write-Host ''
Write-Host 'APP_PASSWORD_HASH (cole no arquivo config-local.ps1 ou nas variáveis de ambiente):' -ForegroundColor Green
Write-Host $hash
