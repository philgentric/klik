<#  ================================================================
    Usage:
    powershell -ExecutionPolicy Bypass -File .\launch_image_similarity_servers.ps1 65432 8200

    ================================================================ #>

param(
    [Parameter(Mandatory = $true, Position = 0)]
    [int]$UDP_PORT,              # First argument – UDP monitoring port

    [Parameter(Mandatory = $true, Position = 1, ValueFromRemainingArguments = $true)]
    [int[]]$EMBEDDINGS_PORTS     # All remaining arguments – HTTP ports
)

# --------------------------------------------------------------
# 1. Activate the Python virtual‑environment that lives in
#    'home'/.klikr/venv
# --------------------------------------------------------------
$VenvDir = Join-Path $HOME ".klikr\venv"

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
    Write-Host "Started MobileNet server on port $Port"

    $PyCode = "import MobileNet_embeddings_server;MobileNet_embeddings_server.run_server($Port,$UDP_PORT)"

    Start-Process cmd -ArgumentList "/k python -c `"$PyCode`""
}

