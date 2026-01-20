import cv2
import sys
import traceback
import numpy as np
from http.server import HTTPServer
from http.server import SimpleHTTPRequestHandler
import urllib.parse
import time
import socket
import uuid
import json
import os
import platform

SERVER_UUID = str(uuid.uuid4())  # Generate a unique ID for this server instance
MONITOR_PORT = None
TCP_PORT = None

# Global detector
detector = None

# Store startup diagnostics
STARTUP_DIAGNOSTICS = {
    "os": platform.platform(),
    "python_version": sys.version,
    "import_error": None,
    "model_load_error": None,
    "gpu_info": "Not checked"
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

        filename = f"MTCNN_{SERVER_UUID}.json"
        filepath = os.path.join(registry_dir, filename)

        data = {
            "name": "MTCNN",
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

def attempt_load_ml_libraries():
    global detector
    try:
        print("Loading MTCNN library...")
        from mtcnn import MTCNN

        print("Loading MTCNN Detector...")
        detector = MTCNN()
        print("MTCNN Detector loaded")

    except Exception as e:
        STARTUP_DIAGNOSTICS["import_error"] = traceback.format_exc()
        STARTUP_DIAGNOSTICS["model_load_error"] = str(e)
        print(f"CRITICAL: Failed to load MTCNN: {e}")
        return False
    return True

# Initialize ML libraries at startup
ml_loaded_successfully = attempt_load_ml_libraries()

class MTCNN_FaceDetectionHandler(SimpleHTTPRequestHandler):

    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

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
        #print("going to open image_raw_url:    "+image_raw_url)

        try:
            #decoded_url = urllib.parse.unquote(image_raw_url)
            decoded_url = urllib.parse.unquote_plus(image_raw_url)

            #print("decoded url:"+decoded_url)

            if not os.path.exists(decoded_url):
                 error_msg = f"File not found: {decoded_url}"
                 RUNTIME_DIAGNOSTICS["error_count"] += 1
                 RUNTIME_DIAGNOSTICS["last_error"] = error_msg
                 RUNTIME_DIAGNOSTICS["last_error_time"] = time.time()
                 self.send_error(404, error_msg)
                 return

            # Create a dummy image (you can replace this with your own video feed)
            img = cv2.imread(decoded_url, cv2.IMREAD_COLOR)
            if img is None:
                 error_msg = f"Failed to load image: {decoded_url}"
                 RUNTIME_DIAGNOSTICS["error_count"] += 1
                 RUNTIME_DIAGNOSTICS["last_error"] = error_msg
                 RUNTIME_DIAGNOSTICS["last_error_time"] = time.time()
                 self.send_error(404, error_msg)
                 return
            #img2 = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

            result = detector.detect_faces(img)

            if result:  # Check if at least one face is detected
                bounding_box = result[0]['box']
                x, y, w, h = bounding_box
                roi = img[y:y+h, x:x+w]
                print("face detected by MTCNN at x: "+str(x)+", y: "+str(y)+", w: "+str(w)+", h: "+str(h))

                # debug:
                #cv2.imshow('SERVER SIDE Face Detector DETECTED color Face',roi)
                #cv2.waitKey(0)
                #cv2.destroyAllWindows()

                ret, out = cv2.imencode('.png', roi)
                self.send_response(200)
                self.send_header('Content-type', 'image/png')
                self.end_headers()
                self.wfile.write(out.tobytes())

                processing_time = (time.time() - start_time)*1000
                monitor_data = f"{SERVER_UUID},mtcnn_detection,mtcnn,{processing_time:.3f}"
                try:
                    bytes_sent = self.udp_socket.sendto(monitor_data.encode(), ('127.0.0.1', MONITOR_PORT))
                    print(f"UDP sent {bytes_sent} bytes to 127.0.0.1:{MONITOR_PORT}: {monitor_data}")
                except Exception as e:
                    print(f"UDP send error: {e}")

            else:
                print("No faces detected by MTCNN")
                pass

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
            "name": "MTCNN",
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

    def do_POST(self):
        pass

class ReliableHTTPServer(HTTPServer):
    allow_reuse_address = True  # Solves "Address already in use" on restart
    request_queue_size = 1024   # Sets the listen backlog correctly from the start

def run_server():
    global TCP_PORT, REGISTRY_FILE
    print("Starting local MTCNN FACE DETECTION server")

    # Bind to ephemeral port to avoid fragile fixed port assignments
    server_address = ('127.0.0.1', 0)
    httpd = ReliableHTTPServer(server_address, MTCNN_FaceDetectionHandler)
    TCP_PORT = httpd.server_address[1]

    print(f"Bound MTCNN face detection server to TCP port: {TCP_PORT}")

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
        MTCNN_FaceDetectionHandler.udp_socket.close()
        print("Server stopped")

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print(f"FATAL! Arguments received: {sys.argv[1:]}")  # Show only the parameters (excluding script name)
        print("Usage: python MTCNN_face_detection_server.py <udp_port>")
        time.sleep(1)
        sys.exit(1)

    try:
        MONITOR_PORT = int(sys.argv[1])
        run_server()
    except ValueError:
        print("Error: Ports must be integers")
        sys.exit(1)