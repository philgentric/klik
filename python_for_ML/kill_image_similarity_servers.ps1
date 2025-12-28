<#  ================================================================
    Usage:
    powershell -ExecutionPolicy Bypass -File .\kill_image_similarity_servers.ps1
    ================================================================ #>

param(
    [int[]]$Ports = 8200..8233
)

$ModulePattern = 'MobileNet_embeddings_server\.run_server'

foreach ($port in $Ports) {
    $procs = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match "$ModulePattern.*$port" }

    if ($procs) {
        $pids = $procs.ProcessId
        Write-Host "Image similarity: killing process(es) $($pids -join ', ') on port $port"
        Stop-Process -Id $pids -Force
    }
    else {
        Write-Host "Image similarity: no process found on port $port"
    }
}