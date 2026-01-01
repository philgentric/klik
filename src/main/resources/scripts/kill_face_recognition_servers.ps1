<#  ================================================================
    Usage:
    powershell -ExecutionPolicy Bypass -File .\kill_face_recognition_servers.ps1
    ================================================================ #>
$ErrorActionPreference = 'Stop'

function Kill-Server {
    param (
        [Parameter(Mandatory)]
        [string]$Pattern
    )
    $procList = Get-CimInstance Win32_Process |
            Where-Object { $_.CommandLine -match
                    [regex]::Escape($Pattern) }
    if (-not $procList) {
        Write-Host "No $Pattern processes found."
        return
    }

    $pids = ($procList.ProcessId | Sort-Object) -join ' '
    Write-Host "Killing process(es):"
    Write-Host " $pids "
    Write-Host "for $Pattern"

    Stop-Process -Id $procList.ProcessId -Force -ErrorAction SilentlyContinue
}

Kill-Server -Pattern 'MTCNN_face_detection_server'
Kill-Server -Pattern 'haars_face_detection_server'
Kill-Server -Pattern 'FaceNet_embeddings_server'
