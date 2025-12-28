# --------------------------------------------------------------------
# Virtual environment activation script for PowerShell
# --------------------------------------------------------------------
# 1. Resolve the directory that contains this script (the venv root).
$here = Split-Path -Parent $MyInvocation.MyCommand.Definition

# 2. Tell the shell that we are inside a virtual environment.
$env:VIRTUAL_ENV = $here

# 3. Prepend the venv's Scripts directory to PATH so that the right
#    python, pip, etc. are found.
$env:PATH = Join-Path -Path $here -ChildPath 'Scripts' `
            + [IO.Path]::PathSeparator + $env:PATH

# 4. (Optional) You can expose helper functions that automatically
#    invoke the venv’s python/pip executables.
function py  { & "$here\Scripts\python.exe" @args }
function pip { & "$here\Scripts\pip.exe" @args }

# 5. (Optional) Change the prompt so you can see that you are in a
#    virtual environment.  This line can be commented out if you
#    don’t want it.
# $global:prompt = "($($env:VIRTUAL_ENV.split('\')[-1])) " + $global:prompt