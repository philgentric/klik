<#  ================================================================
    Usage:
    powershell -ExecutionPolicy Bypass -File .\launch_image_similarity_servers.ps1 8000 5000 5001 5002 ...
    ================================================================ #>

param(
    [Parameter(Mandatory = $true, Position = 0)]
    [int]$UDP_PORT,              # First argument – UDP monitoring port

    [Parameter(Mandatory = $true, Position = 1, ValueFromRemainingArguments = $true)]
    [int[]]$EMBEDDINGS_PORTS     # All remaining arguments – HTTP ports
)

# --------------------------------------------------------------
# 1. Activate the Python virtual‑environment that lives in
#    'home'/.klik/venv-metal
# --------------------------------------------------------------
$VenvDir = Join-Path $HOME ".klik\venv-metal"

$ActivateScript = Join-Path $VenvDir "Scripts\Activate.ps1"
if (Test-Path $ActivateScript) {
    & $ActivateScript          # Source the PowerShell script
}
else {
    Write-Error "Could not find Activate.ps1 in $VenvDir"
    exit 1
}

python --version
foreach ($Port in $EMBEDDINGS_PORTS) {
    $job = Start-Job -ScriptBlock {
        param($p, $udp)
        python -c "import MobileNet_embeddings_server;
MobileNet_embeddings_server.run_server($p, $udp)" `  | Out-Null
    } -ArgumentList $Port, $UDP_PORT

    Write-Host "Started MobileNet server on port $Port (sending UDP monitoring: $UDP_PORT) – Job ID: $($job.Id)"
}
