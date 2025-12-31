<#  ================================================================
    Usage:
    powershell -ExecutionPolicy Bypass -File .\kill_image_similarity_servers.ps1
    ================================================================ #>

$ModulePattern = 'MobileNet_embeddings_server\.run_server'


$procInfo = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match "$ModulePattern.*$port" }

if (-not $procInfo) {
    Write-Host 'Image similarity: No MobileNet_embeddings_server processes found.'
    exit 0
}

$pids = $procInfo.ProcessId -join ', '
Write-Host "Image similarity: Killing process(es) $pids for MobileNet_embeddings_server"

# `Stop-Process` uses the native Windows API – it accepts an array of IDs.
Stop-Process -Id $procInfo.ProcessId -Force -ErrorAction SilentlyContinue

