<#  ================================================================
    Usage:
    powershell -ExecutionPolicy Bypass -File .\kill_image_similarity_servers.ps1
    ================================================================ #>
$ErrorActionPreference = 'Stop'

$procList = Get-CimInstance -ClassName Win32_Process |
        Where-Object { $_.CommandLine -match
                'MobileNet_embeddings_server' }
if (-not $procList) {
    Write-Host "Image similarity: No MobileNet_embeddings_server
processes found."
    exit 0
}
$pids = ($procList.ProcessId | Sort-Object) -join ' '
Write-Host "Killing process(es):"
Write-Host " $pids "
Write-Host "for MobileNet_embeddings_server"
foreach ($p in $procList) {
    Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
}
exit 0

