#-----------------------------------------------------------------------
#  BuildAndPackage.ps1
#
#  1. Builds the fat JAR with Gradle (shadowJar)
#  2. Creates an MSI installer with jpackage
#
#  Run it with:
#      powershell -ExecutionPolicy Bypass -File .\jpackage_for_windows.ps1
#-----------------------------------------------------------------------

# -------------------------------------------------------
# Fail on the first error – makes debugging easier
# -------------------------------------------------------
$ErrorActionPreference = 'Stop'          # stop on any error

# -------------------------------------------------------
# CONFIG – tweak these values to match your project
# -------------------------------------------------------
$ProjectRoot      = Split-Path -Parent $MyInvocation.MyCommand.Path   # the folder that contains this script
$BuildDir         = Join-Path $ProjectRoot 'build'
$LibsDir          = Join-Path $BuildDir 'libs'                         # Gradle puts the JARs here
$OutputDir        = Join-Path $BuildDir 'jpackage'                     # jpackage writes there

# App metadata
$AppName         = 'Klik'
$AppVersion      = '1.0'
$Vendor          = 'Klik'
$Description     = 'Klik JavaFX application'

# Main‑class – adjust if your launcher class lives elsewhere
$MainClass       = 'klik.Klik_application'     # fully‑qualified class name

# The JAR that `shadowJar` produces
$JarName         = 'klik.jar'          # (adjust if the artifact has a different name)

# JavaFX modules – adjust the path if you installed JavaFX elsewhere
$JavaFXPath      = 'C:\Program Files\Java\javafx-sdk-25'     # <-- change if needed
$JavaModules     = "$env:JAVA_HOME\jmods;$JavaFXPath\lib"

# Modules that your app needs (comma‑separated, no spaces)
$AddModules      = @(
    'javafx.base',
    'javafx.graphics',
    'javafx.controls',
    'javafx.fxml',
    'javafx.media',
    'javafx.web'
)

# Windows icon – must be a .ico file
$IconPath        = Join-Path $ProjectRoot 'src\main\resources\icons\klik.ico'  # <-- provide your own

# Java options you want inside the installer
$JavaOptions     = '-Xmx2g'            # example; change if you need more memory

# -------------------------------------------------------
# Build the fat JAR (Gradle)
# -------------------------------------------------------
Write-Host " Building the fat JAR with Gradle ..."
& "$ProjectRoot\gradlew.bat" shadowJar   # the Gradle wrapper will do everything

# -------------------------------------------------------
# Run jpackage – create the MSI
# -------------------------------------------------------
Write-Host " Packaging with jpackage …"

$jpackageArgs = @(
    '--type', 'msi',
    '--name',      $AppName,
    '--app-version', $AppVersion,
    '--vendor',    $Vendor,
    '--description', $Description,
    '--input',     $LibsDir,
    '--main-jar',  $JarName,
    '--main-class', $MainClass,
    '--module-path', $JavaModules,
    '--add-modules', ($AddModules -join ','),
    '--icon',      $IconPath,
    '--java-options', $JavaOptions,
    '--dest',       $OutputDir           # folder where the MSI will appear
)

# Show the exact command for debugging
$cmd = "$env:JAVA_HOME\bin\jpackage.exe " + ($jpackageArgs -join ' ')

Write-Host "`n$cmd`n"

# Execute it
& "$env:JAVA_HOME\bin\jpackage.exe" @jpackageArgs

Write-Host "`MSI created in $OutputDir`n"