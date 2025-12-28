#  Face‑Net / Face‑Detection Server Killer

function Get-ProcessPidsForPort {
    param(
        [string] $NamePattern,
        [int]    $Port
    )

    $regex = [regex]::new("$NamePattern", 'IgnoreCase')

    Get-CimInstance Win32_Process |
    Where-Object {
    $_.CommandLine -and
    ($regex.IsMatch($_.CommandLine) -and
    $_.CommandLine -match "$Port")
    } |
    Select-Object -ExpandProperty ProcessId
}

$FaceNetPorts = @(8020, 8021)

foreach ($p in $FaceNetPorts) {
    $pids = Get-ProcessPidsForPort -NamePattern
    '_face_embeddings_server\.run_server' -Port $p

    if ($pids.Count) {
        Write-Host "FaceNet – killing process(es) $($pids -join ', ') for port $p" -ForegroundColor Yellow
        Stop-Process -Id $pids -Force -ErrorAction SilentlyContinue
    } else {
        Write-Host "FaceNet – No process(es) found for port $p" -ForegroundColor Green
    }
}


$FaceDetectionPorts = @(
    8040,8041,8042,8043,8044,8045,8046,8047,8048,8049,
    8080,8081,
    8090,8091,
    8100,8101,
    8110,8111
)

foreach ($p in $FaceDetectionPorts) {
    $pids = Get-ProcessPidsForPort -NamePattern '_face_detection_server\.run_server' -Port $p

    if ($pids.Count) {
        Write-Host "Face extraction – killing process(es) $($pids -join ', ') for port $p" -ForegroundColor Yellow
        Stop-Process -Id $pids -Force -ErrorAction SilentlyContinue
    } else {
        Write-Host "Face extraction – No process(es) found for port $p" -ForegroundColor Green
    }
}