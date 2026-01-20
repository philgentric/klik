import cv2
import sys
import os
import json
import traceback
import numpy as np
from http.server import HTTPServer
from http.server import SimpleHTTPRequestHandler
import urllib.parse
import time
import socket
import uuid
import platform
from functools import partial

SERVER_UUID = str(uuid.uuid4())
MONITOR_PORT = None
TCP_PORT = None
CONFIG_NAME = None

# Global classifier
face_cascade = None

# Store startup diagnostics
STARTUP_DIAGNOSTICS = {
    "os": platform.platform(),
    "python_version": sys.version,
    "import_error": None,
    "model_load_error": None,
    "gpu_info": "Not checked (OpenCV)"
}

# Store runtime diagnostics
RUNTIME_DIAGNOSTICS = {
    "request_count": 0,
    "error_count": 0,
    "last_error": None,
    "last_error_time": None
}

def register_server():
    """Register server in ~/.klikr/.privacy_screen/face_recognition_server_registry."""
    try:
        home = os.path.expanduser("~")
        registry_dir = os.path.join(home, ".klikr", ".privacy_screen", "face_recognition_server_registry")
        os.makedirs(registry_dir, exist_ok=True)

        filename = f"{CONFIG_NAME}_{SERVER_UUID}.json"
        filepath = os.path.join(registry_dir, filename)

        data = {
            "name": "Haars",
            "sub-type": CONFIG_NAME,
            "port": TCP_PORT,
            "uuid": SERVER_UUID
        }

        with open(filepath, 'w') as f:
            json.dump(data, f)

        print(f"Registered server in {filepath}")
        return filepath
    except Exception as e:
        print(f"Failed to register server: {e}")
        return None

def unregister_server(filepath):
    """Remove registration file."""
    if filepath and os.path.exists(filepath):
        try:
            os.remove(filepath)
            print(f"Unregistered server (removed {filepath})")
        except Exception as e:
            print(f"Failed to unregister server: {e}")

REGISTRY_FILE = None
ml_loaded_successfully = False

def attempt_load_resources():
    global face_cascade, ml_loaded_successfully
    try:
        print(f"Loading Haar Cascade from: {CONFIG_NAME}")
        if not os.path.exists(CONFIG_NAME):
            raise FileNotFoundError(f"Cascade file not found: {CONFIG_NAME}")

        face_cascade = cv2.CascadeClassifier(CONFIG_NAME)
        if face_cascade.empty():
             raise IOError(f"Failed to load cascade classifier from {CONFIG_NAME}")

        print("Haar Cascade loaded successfully")
        ml_loaded_successfully = True
        return True
    except Exception as e:
        STARTUP_DIAGNOSTICS["model_load_error"] = str(e)
        print(f"CRITICAL: Failed to load resources: {e}")
        ml_loaded_successfully = False
        return False

class FaceDetectionHandler(SimpleHTTPRequestHandler):
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    def do_POST(self):
        pass

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

    def do_GET(self):

        # Health check endpoint
        if self.path == '/health':
            self._handle_health_check()
            return

        if not ml_loaded_successfully:
             self.send_error(503, "Server improperly configured. Check /health endpoint for details.")
             return

        RUNTIME_DIAGNOSTICS["request_count"] += 1
        start_time = time.time()
        image_raw_url = self.path[1:]

        try:
            #decoded_url = urllib.parse.unquote(image_raw_url)
            decoded_url = urllib.parse.unquote_plus(image_raw_url)

            if not os.path.exists(decoded_url):
                 error_msg = f"File not found: {decoded_url}"
                 RUNTIME_DIAGNOSTICS["error_count"] += 1
                 RUNTIME_DIAGNOSTICS["last_error"] = error_msg
                 RUNTIME_DIAGNOSTICS["last_error_time"] = time.time()
                 self.send_error(404, error_msg)
                 return

            # Create a dummy image
            img = cv2.imread(decoded_url, cv2.IMREAD_COLOR)
            if img is None:
                 error_msg = f"Failed to load image: {decoded_url}"
                 RUNTIME_DIAGNOSTICS["error_count"] += 1
                 RUNTIME_DIAGNOSTICS["last_error"] = error_msg
                 RUNTIME_DIAGNOSTICS["last_error_time"] = time.time()
                 self.send_error(404, error_msg)
                 return

            # Convert the image to grayscale
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            #normalize
            gray = cv2.normalize(gray, None, 0.0, 255.0,cv2.NORM_MINMAX)

            # Detect faces in the image using the Haar cascade classifier
            faces, rejectLevels, levelWeights = face_cascade.detectMultiScale3(gray, scaleFactor=1.1, minNeighbors=5, outputRejectLevels=True)

            return_whole_image = False
            if (return_whole_image):
                # ... same ...
                pass
            else:
                 if len(faces) > 0:
                    max_weight_index = np.argmax(levelWeights)
                    max_confidence = levelWeights[max_weight_index]

                    if (max_confidence < 0.004):
                        print(" max_confidence too small "+ str(max_confidence))
                    else:
                        x, y, w, h = faces[max_weight_index]
                        roi = gray[y:y+h, x:x+w]

                        ret, out = cv2.imencode('.png', roi)
                        self.send_response(200)
                        self.send_header('Content-type', 'image/png')
                        self.end_headers()
                        self.wfile.write(out.tobytes())
                 else:
                    # No faces detected in the image
                    pass

            processing_time = (time.time() - start_time)*1000  # Convert to milliseconds
            # Send monitoring data
            monitor_data = f"{SERVER_UUID},haar_face_detection,{CONFIG_NAME},{processing_time:.3f}"
            try:
                bytes_sent = self.udp_socket.sendto(monitor_data.encode(), ('127.0.0.1', MONITOR_PORT))
                print(f"UDP sent {bytes_sent} bytes to 127.0.0.1:{MONITOR_PORT}: {monitor_data}")
            except Exception as e:
                print(f"UDP send error: {e}")

        except Exception as e:
            error_msg = str(e)
            RUNTIME_DIAGNOSTICS["error_count"] += 1
            RUNTIME_DIAGNOSTICS["last_error"] = error_msg
            RUNTIME_DIAGNOSTICS["last_error_time"] = time.time()
            self.send_error(500, f"Internal error: {error_msg}")
            print(f"Error: {e}")
            traceback.print_exc()

    def _handle_health_check(self):
        """Handle health check requests."""
        is_healthy = (
            STARTUP_DIAGNOSTICS["import_error"] is None and
            STARTUP_DIAGNOSTICS["model_load_error"] is None
        )

        response = {
            "name": "Haars",
            "sub-type": CONFIG_NAME,
            "port": TCP_PORT,
            "uuid": SERVER_UUID,
            "status": "healthy" if is_healthy else "critical_failure",
            "diagnostics": STARTUP_DIAGNOSTICS,
            "runtime": RUNTIME_DIAGNOSTICS
        }

        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(response, indent=2).encode('utf-8'))

class ReliableHTTPServer(HTTPServer):
    allow_reuse_address = True  # Solves "Address already in use" on restart
    request_queue_size = 1024   # Sets the listen backlog correctly from the start

def run_server():
    global REGISTRY_FILE, TCP_PORT

    attempt_load_resources()

    print("Starting local HAARS FACE DETECTION server with config: "+CONFIG_NAME)

    # Bind to ephemeral port to avoid fragile fixed port assignments
    server_address = ('127.0.0.1', 0)
    httpd = ReliableHTTPServer(server_address, FaceDetectionHandler)
    TCP_PORT = httpd.server_address[1]

    print(f"Bound HAARS face detection server to TCP port: {TCP_PORT}")

    # Register server using the real bound port
    REGISTRY_FILE = register_server()

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down server...")
    except Exception as e:
        print(f"Server crashed: {e}")
        traceback.print_exc()
        sys.exit(1)
    finally:
        if REGISTRY_FILE:
            unregister_server(REGISTRY_FILE)
        httpd.server_close()
        FaceDetectionHandler.udp_socket.close()
        print("Server stopped")

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python haars_face_detection_server.py <config_name> <udp_port>")
        time.sleep(1)
        sys.exit(1)

    try:
        CONFIG_NAME = sys.argv[1].replace("'", "")
        MONITOR_PORT = int(sys.argv[2])

        run_server()
    except ValueError:
        print("Error: Ports must be integers")
        sys.exit(1)