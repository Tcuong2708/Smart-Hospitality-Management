param(
    [Parameter(Mandatory = $true)]
    [string]$PrivateKeyPath,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9]{4}$')]
    [string]$Last4,

    [string]$OutputPath = '.\nfc-demo-payload.json',

    [ValidateRange(1, 365)]
    [int]$ValidDays = 30
)

$resolvedPrivateKey = (Resolve-Path -LiteralPath $PrivateKeyPath).Path
$privatePem = [System.IO.File]::ReadAllText($resolvedPrivateKey)
$rsa = [System.Security.Cryptography.RSA]::Create()

try {
    $rsa.ImportFromPem($privatePem)
    $issuedAt = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $expiresAt = [DateTimeOffset]::UtcNow.AddDays($ValidDays).ToUnixTimeSeconds()
    $tokenId = [Guid]::NewGuid().ToString('N')
    $issuer = 'MAY_HOTEL_DEMO'
    $canonical = "1|$tokenId|$Last4|$issuedAt|$expiresAt|$issuer"
    $signatureBytes = $rsa.SignData(
        [Text.Encoding]::UTF8.GetBytes($canonical),
        [Security.Cryptography.HashAlgorithmName]::SHA256,
        [Security.Cryptography.RSASignaturePadding]::Pkcs1
    )

    $payload = [ordered]@{
        version = '1'
        tokenId = $tokenId
        last4 = $Last4
        issuedAt = $issuedAt
        expiresAt = $expiresAt
        issuer = $issuer
        signature = [Convert]::ToBase64String($signatureBytes)
    } | ConvertTo-Json -Compress

    [System.IO.File]::WriteAllText(
        [System.IO.Path]::GetFullPath($OutputPath),
        $payload,
        [Text.UTF8Encoding]::new($false)
    )

    $publicKeyBase64 = [Convert]::ToBase64String($rsa.ExportSubjectPublicKeyInfo())
    Write-Output "Đã tạo payload NDEF: $([System.IO.Path]::GetFullPath($OutputPath))"
    Write-Output "NFC_DEMO_PUBLIC_KEY_BASE64=$publicKeyBase64"
} finally {
    $rsa.Dispose()
}
