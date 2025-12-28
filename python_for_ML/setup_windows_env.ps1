<# ------------------------------------

usage:

powershell -ExecutionPolicy Bypass -File .\setup_windows_env.ps1

prerequisites:
- have a NVIDIA GPU on your PC
- install the lattest drivers
- install the CUDA toolkit (not needed for gaming)
- install cuDNN (unzip in CUDA folder)
-then the script below will do the rest

   1. Installs Python 3.10 (winget / choco)
   2. Creates a venv called “venv-metal” in your HOME+.klik dir
   3. Activates that venv
   4. Upgrades pip
   5. Installs TensorFlow (CPU‑only on Windows)on Windows)
   6. Installs the rest of the Python ML deps
 ------------------------------------ #>

# ──────── 0 Utility helpers ────────
function Confirm-Command {
    param([string]$cmd)
    Write-Host "🛠️  $cmd" -ForegroundColor Cyan
}

# ─────── 1 Install Python 3.10 ───────
# Prefer winget (built‑in on Windows 10+), fall back to Chocolatey
if (Get-Command winget -ErrorAction SilentlyContinue) {
    Confirm-Command "winget install --id=Python.Python.3.10 --exact
--silent"
    winget install --id=Python.Python.3.10 --exact --silent
} elseif (Get-Command choco -ErrorAction SilentlyContinue) {
    Confirm-Command "choco install python310 -y"
    choco install python310 -y
} else {
    Write-Error "Neither winget nor choco was found. Please install
Python 3.10 manually from https://www.python.org/downloads/"
    exit 1
}

# Make sure the installer added python to the PATH
$env:Path = ([Environment]::GetEnvironmentVariable('Path', 'Machine') + ';'  + [Environment]::GetEnvironmentVariable('Path','User'))

$python = Get-Command python -ErrorAction Stop
Confirm-Command "python --version"
python --version   # Should print 3.10.x

# ─────── 2 Create the venv ───────
$venvDir = "$HOME\.klik\venv-metal"
Confirm-Command "python -m venv $venvDir"
python -m venv $venvDir

# ─────── 3 Activate it ────────
& "$venvDir\Scripts\Activate.ps1"

# ─────── Upgrade pip ────────
Confirm-Command "pip install -U pip"
pip install -U pip

# ─────── 5 TensorFlow ────────
Confirm-Command "pip install tensorflow"
pip install tensorflow
#by default this command will install GPU-enabled tensorflow IF THE DRIVERS ARE PRESENT

# ─────── 6 Install the rest of the ML stack ───────
Push-Location "$PSScriptRoot" -ErrorAction Stop
Confirm-Command "pip install -r requirements.txt"
pip install -r requirements.txt
Pop-Location

Write-Host "All done!  Your venv is ready at $venvDir"
