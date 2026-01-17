import sys
import os
import json
import socket
import time
import uuid
import platform
import traceback
import urllib.parse
try:
    from http.server import ThreadingHTTPServer as BaseHTTPServer
except ImportError:
    from http.server import HTTPServer as BaseHTTPServer
from http.server import SimpleHTTPRequestHandler

# Server configuration
SERVER_UUID = str(uuid.uuid4())
MONITOR_PORT = None
TCP_PORT = None

# ML libraries (initialized later)
torch = None
tf = None
np = None
keras = None
face_model = None

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

        filename = f"FaceNet_{SERVER_UUID}.json"
        filepath = os.path.join(registry_dir, filename)

        data = {
            "name": "FaceNet",
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
    """
    Attempts to import ML libraries and load the model.
    Updates STARTUP_DIAGNOSTICS with success/failure details.
    """
    global torch, tf, np, keras, face_model

    # A. Import ML libraries
    try:
        print("Loading PyTorch & TensorFlow/Keras libraries...")
        import torch as torch_local
        import tensorflow as tf_local
        import numpy as np_local

        # import keras as keras_local # OLD: compatibility issues
        from tensorflow import keras as keras_local

        from facenet_pytorch import InceptionResnetV1 as IRV1

        # Assign to global variables
        torch = torch_local
        tf = tf_local
        np = np_local
        keras = keras_local

        # Check GPU availability (PyTorch)
        if torch.cuda.is_available():
            device = "cuda"
        elif hasattr(torch.backends, "mps") and torch.backends.mps.is_available():
            device = "mps"
        else:
            device = "cpu"
        STARTUP_DIAGNOSTICS["gpu_info"] = f"PyTorch device: {device}"

    except Exception as e:
        STARTUP_DIAGNOSTICS["import_error"] = traceback.format_exc()
        print(f"CRITICAL: Failed to import ML libraries: {e}")
        return False

    # B. Load the model
    try:
        print("Loading FaceNet Model (InceptionResnetV1)...")
        face_model = IRV1(pretrained='vggface2').eval()
        print("FaceNet vggface2 model loaded")

    except Exception as e:
        STARTUP_DIAGNOSTICS["model_load_error"] = traceback.format_exc()
        print(f"CRITICAL: Failed to load model: {e}")
        return False

    return True

# Initialize ML libraries at startup
ml_loaded_successfully = attempt_load_ml_libraries()

class Facenet_Embeddings_Generator(SimpleHTTPRequestHandler):
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    def do_GET(self):
        """Handle GET requests for image embeddings and health checks."""
        try:
            # Health check endpoint
            if self.path == '/health':
                self._handle_health_check()
                return

            # Check if ML libraries are loaded
            if not ml_loaded_successfully:
                self.send_error(503, "Server improperly configured. Check /health endpoint for details.")
                return

            # Process image embedding request
            self._process_image_request()

        except Exception as e:
            self.send_error(500, f"Internal server error: {str(e)}")
            print(f"Error processing request: {e}")
            traceback.print_exc()

    def _handle_health_check(self):
        """Handle health check requests."""
        is_healthy = (
            STARTUP_DIAGNOSTICS["import_error"] is None and
            STARTUP_DIAGNOSTICS["model_load_error"] is None
        )

        response = {
            "name": "FaceNet",
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

    def _process_image_request(self):
        """Process image embedding requests."""
        RUNTIME_DIAGNOSTICS["request_count"] += 1
        start_time = time.time()

        image_raw_url = self.path[1:]

        try:
            decoded_url = urllib.parse.unquote_plus(image_raw_url)

            # Explicit file check
            if not os.path.exists(decoded_url):
                 error_msg = f"File not found: {decoded_url}"
                 print(f"ERROR: {error_msg}")
                 RUNTIME_DIAGNOSTICS["error_count"] += 1
                 RUNTIME_DIAGNOSTICS["last_error"] = error_msg
                 RUNTIME_DIAGNOSTICS["last_error_time"] = time.time()
                 self.send_error(404, error_msg)
                 return

            # Load and preprocess image
            # FaceNet uses 160x160 input size
            img = keras.preprocessing.image.load_img(decoded_url, target_size=(160, 160))

            x = keras.preprocessing.image.img_to_array(img)
            x = np.expand_dims(x, axis=0)
            x = x / 255.0  # Normalize to [0, 1] range, for faceNet

            # Convert the input data to a PyTorch tensor
            x_tensor = torch.tensor(x)
            x_tensor = x_tensor.permute(0, 3, 1, 2)   # permute the axes to (batch_size, channels, height, width)

            with torch.no_grad():
                feature_vector = face_model.forward(x_tensor)

            # Convert the tensor to a NumPy array and flatten it
            double_values = feature_vector.detach().numpy().flatten().tolist()
            data = {'features': double_values}

            response_json = json.dumps(data)
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(response_json.encode('utf-8'))

            processing_time = (time.time() - start_time)*1000  # Convert to milliseconds
            monitor_data = f"{SERVER_UUID},facenet_embeddings,facenet,{processing_time:.3f}"

            # Send UDP monitoring message
            if MONITOR_PORT:
                try:
                    bytes_sent = Facenet_Embeddings_Generator.udp_socket.sendto(monitor_data.encode(), ('127.0.0.1', MONITOR_PORT))
                    print(f"UDP sent {bytes_sent} bytes to 127.0.0.1:{MONITOR_PORT}: {monitor_data}")
                except Exception as e:
                    print(f"UDP send error: {e}")

        except Exception as e:
            error_msg = str(e)
            RUNTIME_DIAGNOSTICS["error_count"] += 1
            RUNTIME_DIAGNOSTICS["last_error"] = error_msg
            RUNTIME_DIAGNOSTICS["last_error_time"] = time.time()

            self.send_error(400, f"Error processing image: {error_msg}")
            print(f"Image processing error: {e}")

    def do_POST(self):
        """Handle POST requests (currently not implemented)."""
        self.send_error(405, "POST method not allowed")

class ReliableHTTPServer(BaseHTTPServer):
    """HTTP server with improved reliability settings."""
    allow_reuse_address = True
    request_queue_size = 1024

def run_server():
    global REGISTRY_FILE, TCP_PORT
    print(f"Starting local FaceNet FACE EMBEDDINGS server")
    print(f"Monitoring messages will be sent to UDP port {MONITOR_PORT}")
    print("Diagnostics available at /health")

    # Bind to an ephemeral port to avoid fragile fixed port assignments
    server_address = ('127.0.0.1', 0)
    httpd = ReliableHTTPServer(server_address, Facenet_Embeddings_Generator)
    TCP_PORT = httpd.server_address[1]

    print(f"Bound FaceNet embeddings server to TCP port: {TCP_PORT}")

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
        Facenet_Embeddings_Generator.udp_socket.close()
        print("Server stopped")

if __name__ == '__main__':
    # Parse command line arguments
    if len(sys.argv) != 2:
        print("Usage: python FaceNet_embeddings_server.py [udp_port]")
        time.sleep(1)
        sys.exit(1)

    try:
        MONITOR_PORT = int(sys.argv[1])
        run_server()
    except ValueError:
        print("Error: Ports must be integers")
        sys.exit(1)
