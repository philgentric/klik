import sys
import json
import socket
import time
import uuid
import platform
import traceback
import urllib.parse
from http.server import HTTPServer, SimpleHTTPRequestHandler

# Server configuration
SERVER_UUID = str(uuid.uuid4())
MONITOR_PORT = None
TCP_PORT = None  # Default port, can be changed when calling run_server()

# ML libraries (initialized later)
tf = None
np = None
keras = None
model = None
preprocess_input = None

# Store startup diagnostics
STARTUP_DIAGNOSTICS = {
    "os": platform.platform(),
    "python_version": sys.version,
    "import_error": None,
    "model_load_error": None,
    "gpu_info": "Not checked"
}

def attempt_load_ml_libraries():
    """
    Attempts to import ML libraries and load the model.
    Updates STARTUP_DIAGNOSTICS with success/failure details.
    """
    global tf, np, keras, model, preprocess_input

    # A. Import ML libraries
    try:
        print("Loading TensorFlow/Keras libraries...")
        import tensorflow as tf_local
        import numpy as np_local
        import keras as keras_local
        from tensorflow.keras.models import Model
        from keras.applications import MobileNetV3Large
        from tensorflow.keras.applications.mobilenet_v3 import preprocess_input as pre_local

        # Assign to global variables
        tf = tf_local
        np = np_local
        keras = keras_local
        preprocess_input = pre_local

        # Check GPU availability
        gpus = tf.config.list_physical_devices('GPU')
        gpu_names = [d.name for d in gpus] if gpus else ["No GPU detected"]
        STARTUP_DIAGNOSTICS["gpu_info"] = f"Detected {len(gpus)} GPU(s): {gpu_names}"

    except Exception as e:
        STARTUP_DIAGNOSTICS["import_error"] = traceback.format_exc()
        print(f"CRITICAL: Failed to import ML libraries: {e}")
        return False  # Return False to indicate failure

    # B. Load the model
    try:
        print("Loading MobileNet Model...")
        base_model = MobileNetV3Large(
            include_top=False,
            pooling='avg',
            input_shape=(224, 224, 3),
            weights='imagenet'
        )
        model = Model(inputs=base_model.input, outputs=base_model.output)

        # Warmup the model with dummy data
        dummy = np.zeros((1, 224, 224, 3))
        model.predict(dummy, verbose=0)

    except Exception as e:
        STARTUP_DIAGNOSTICS["model_load_error"] = traceback.format_exc()
        print(f"CRITICAL: Failed to load model: {e}")
        return False  # Return False to indicate failure

    return True  # Return True if everything succeeded

# Initialize ML libraries at startup
ml_loaded_successfully = attempt_load_ml_libraries()

class EmbeddingGenerator(SimpleHTTPRequestHandler):
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
            "UUID": SERVER_UUID,
            "status": "healthy" if is_healthy else "critical_failure",
            "diagnostics": STARTUP_DIAGNOSTICS
        }

        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(response, indent=2).encode('utf-8'))

    def _process_image_request(self):
        """Process image embedding requests."""
        start_time = time.time()

        # Extract and validate image URL
        image_raw_url = self.path[1:]


        img = None

        try:
            # Decode URL and load image
            decoded_url = urllib.parse.unquote_plus(image_raw_url)
            img = keras.preprocessing.image.load_img(
                decoded_url,
                target_size=(224, 224)
            )

            # Preprocess image and generate embedding
            x = keras.preprocessing.image.img_to_array(img)
            x = np.expand_dims(x, axis=0)
            x = preprocess_input(x)
            feature_vector = model.predict(x)

            data = {'features': feature_vector.tolist()[0]}  # Convert numpy array to list for JSON

            #double_values = [np.float64(i) for i in x.flatten()]
            #print("values: "+str(double_values))
            #data = {'features': double_values}

            x = json.dumps(data)
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(x.encode('utf-8'))
            processing_time = (time.time() - start_time) * 1000  # Convert to milliseconds
            monitor_data = f"{SERVER_UUID},mobilenet,{image_raw_url},{processing_time:.3f}"

            # Send UDP monitoring message
            try:
                bytes_sent = EmbeddingGenerator.udp_socket.sendto(monitor_data.encode(), ('127.0.0.1', MONITOR_PORT))
            except Exception as e:
                print(f"Failed to send UDP message: {e}")


        except Exception as e:
            self.send_error(400, f"Error processing image: {str(e)}")
            print(f"Image processing error: {e}")

    def do_POST(self):
        """Handle POST requests (currently not implemented)."""
        self.send_error(405, "POST method not allowed")

class ReliableHTTPServer(HTTPServer):
    """HTTP server with improved reliability settings."""
    allow_reuse_address = True
    request_queue_size = 1024

def run_server(tcp_port=None, udp_port=None):
    """
    Run the server on the specified ports.

    Args:
        tcp_port: Port for HTTP traffic
        udp_port: Port for monitoring messages
    """
    global MONITOR_PORT
    MONITOR_PORT = udp_port

    print(f"Starting server on port {tcp_port}")
    if udp_port:
        print(f"Monitoring messages will be sent to UDP port {udp_port}")
    print("Diagnostics available at /health")

    server_address = ('127.0.0.1', tcp_port)
    httpd = ReliableHTTPServer(server_address, EmbeddingGenerator)

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down server...")
    except Exception as e:
        print(f"Server crashed: {e}")
        traceback.print_exc()
    finally:
        httpd.server_close()
        EmbeddingGenerator.udp_socket.close()
        print("Server stopped")
