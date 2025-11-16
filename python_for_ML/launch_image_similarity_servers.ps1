<#  ===================================================================
    Usage


    powershell -ExecutionPolicy Bypass -File .\launch_image_similarity_servers.ps1 8000 5000 5001 5002 .. etc

    This starts a UDP‑monitoring server on port 8000 and
    launches a MobileNet‑embedding server on each of the remaining HTTP
    ports (5000, 5001, 5002, …).

    ================================================================== #>

param(
    [Parameter(Mandatory = $true, Position = 0)]
    [int]$UDP_PORT,              # First argument – UDP monitoring port

    [Parameter(Mandatory = $true, Position = 1, ValueFromRemainingArguments = $true)]
    [int[]]$EMBEDDINGS_PORTS     # All remaining arguments – HTTP ports
)

# ------------------------------------------------------------------
# 1.  Activate the Python virtual‑environment that lives in
#     ~/venv-metal (on Windows ~ means %USERPROFILE%)
# ------------------------------------------------------------------
$VenvDir = Join-Path $HOME "venv-metal"

# Two ways to activate a venv – choose whichever is installed:
#   * activate.bat  – works in cmd.exe
#   * Activate.ps1  – works in PowerShell
$ActivateScript = Join-Path $VenvDir "Scripts\Activate.ps1"
if (Test-Path $ActivateScript) {
    & $ActivateScript          # Source the PowerShell script
}
else {
    Write-Error "Could not find Activate.ps1 in $VenvDir"
    exit 1
}

# ------------------------------------------------------------------
# 2.  Show the Python interpreter that will be used
# ------------------------------------------------------------------
python --version   # prints something like "Python 3.11.2"

# ------------------------------------------------------------------
# 3.  Start the servers
# ------------------------------------------------------------------
foreach ($Port in $EMBEDDINGS_PORTS) {
    # Launch each server in its own background job
    Start-Job -ScriptBlock {
        param($p, $udp)
        python -c "import MobileNet_embeddings_server;
MobileNet_embeddings_server.run_server($p, $udp)" | Out-Null
    } -ArgumentList $Port, $UDP_PORT | Out-Null

    Write-Host "Started MobileNet server on port $Port (UDP $UDP_PORT)"
}

# ------------------------------------------------------------------
# 4.  Keep the console alive until the user presses a key
# ------------------------------------------------------------------
Write-Host "`nAll servers are running. Press any key to exit."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

# ------------------------------------------------------------------
# 5.  (Optional) Clean up: stop all jobs that we started
# ------------------------------------------------------------------
Get-Job | Where-Object { $_.State -eq 'Running' } | Stop-Job | Out-Null