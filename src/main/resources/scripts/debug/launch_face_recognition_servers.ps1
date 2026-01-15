<#  ===================================================================
    Usage:
    powershell -ExecutionPolicy Bypass -File .\launch_face_recognition_servers.ps1 8000
    ================================================================== #>

param(
    [Parameter(Mandatory = $true, Position = 0)]
    [int]$UDP_PORT              # First argument – UDP monitoring port
)

# ------------------------------------------------------------------
# 1.  Activate the Python virtual‑environment that lives in
#     'home'/.klikr/venv
# ------------------------------------------------------------------
$VenvDir = Join-Path $HOME ".klikr\venv"

# Activate the venv
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


# ────────────────────── FaceNet embeddings ──────────────────────
$FacenetEmbeddingsPorts = @(8020, 8021)

foreach ($port in $FacenetEmbeddingsPorts) {
    Start-Process -FilePath python3 `
                  -ArgumentList "-c", "import FaceNet_embeddings_server;
FaceNet_embeddings_server.run_server($port,$UDP_PORT)" `
                  -WindowStyle Hidden
}

# ────────────────────── Face‑detection (MTCNN) ──────────────────────
$FaceDetectionPorts  = @(8040, 8041, 8042, 8043, 8044, 8045, 8046, 8047, 8048,
8049)
$FaceDetectorId      = "'MTCNN'"

foreach ($port in $FaceDetectionPorts) {
    Start-Process -FilePath python3 `
                  -ArgumentList "-c", "import MTCNN_face_detection_server;
MTCNN_face_detection_server.run_server($port,$FaceDetectorId,$UDP_PORT)" `
                  -WindowStyle Hidden
}

# ────────────────────── Face‑detection (haarcascade) ──────────────────────
$FaceDetectionPorts  = @(8080, 8081)
$FaceDetectorId      = "'haarcascade_frontalface_alt_tree.xml'"

foreach ($port in $FaceDetectionPorts) {
    Start-Process -FilePath python3 `
                  -ArgumentList "-c", "import haars_face_detection_server;
haars_face_detection_server.run_server($port,$FaceDetectorId,$UDP_PORT)" `
                  -WindowStyle Hidden
}

$FaceDetectionPorts  = @(8090, 8091)
$FaceDetectorId      = "'haarcascade_frontalface_alt_default.xml'"

foreach ($port in $FaceDetectionPorts) {
    Start-Process -FilePath python3 `
                  -ArgumentList "-c", "import haars_face_detection_server;
haars_face_detection_server.run_server($port,$FaceDetectorId,$UDP_PORT)" `
                  -WindowStyle Hidden
}

$FaceDetectionPorts  = @(8100, 8101)
$FaceDetectorId      = "'haarcascade_frontalface_alt1.xml'"

foreach ($port in $FaceDetectionPorts) {
    Start-Process -FilePath python3 `
                  -ArgumentList "-c", "import haars_face_detection_server;
haars_face_detection_server.run_server($port,$FaceDetectorId,$UDP_PORT)" `
                  -WindowStyle Hidden
}

$FaceDetectionPorts  = @(8110, 8111)
$FaceDetectorId      = "'haarcascade_frontalface_alt2.xml'"

foreach ($port in $FaceDetectionPorts) {
    Start-Process -FilePath python3 `
                  -ArgumentList "-c", "import haars_face_detection_server;
haars_face_detection_server.run_server($port,$FaceDetectorId,$UDP_PORT)" `
                  -WindowStyle Hidden
}
