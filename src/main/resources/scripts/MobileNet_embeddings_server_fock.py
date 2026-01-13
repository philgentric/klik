import sys
import json
import socket
import time
import uuid
import platform
import traceback
import urllib.parse
from http.server import HTTPServer, SimpleHTTPRequestHandler

SERVER_UUID = str(uuid.uuid4())
MONITOR_PORT = None
# These hold the imported modules/models later
tf = None
np = None
keras = None
model = None
preprocess_input = None

# We store startup errors here instead of crashing
STARTUP_DIAGNOSTICS = {
    "os": platform.platform(),
    "python_version": sys.version,
    "import_error": None,     # Will contain string trace if imports fail
    "model_load_error": None, # Will contain string trace if model creation fails
    "gpu_info": "Not checked"
}

# --- 2. Safe Loading Function ---
def attempt_load_ml_libraries():
    """
    Attempts to import heavy libs and load model.
    Updates STARTUP_DIAGNOSTICS with success or failure details.
    """
    global tf, np, keras, model, preprocess_input

    # A. IMPORT STEP
    try:
        # We import inside the function to prevent script crash at startup
        global tf, np, keras, model, preprocess_input
        print("Loading TensorFlow/Keras libraries...")
        import tensorflow as tf_local
        import numpy as np_local
        import keras as keras_local
        from tensorflow.keras.models import Model
        from keras.applications import MobileNetV3Large
        from tensorflow.keras.applications.mobilenet_v3 import preprocess_input as pre_local

        # specific imports successful, assign to globals
        tf = tf_local
        np = np_local
        keras = keras_local
        preprocess_input = pre_local

        # Check GPU status immediately
        gpus = tf.config.list_physical_devices('GPU')
        STARTUP_DIAGNOSTICS["gpu_info"] = f"Detected {len(gpus)} GPU(s): {[d.name for d in gpus]}"

    except Exception:
        STARTUP_DIAGNOSTICS["import_error"] = traceback.format_exc()
        print("CRITICAL: Failed to import ML libraries.")
        return # Stop here if unrelated imports failed

    # B. MODEL LOAD STEP
    try:
        print("Loading MobileNet Model...")
        base_model = MobileNetV3Large(include_top=False, pooling='avg', input_shape=(224, 224, 3), weights='imagenet')
        model = Model(inputs=base_model.input, outputs=base_model.output)

        # Warmup (optional, catches CUDA errors early)
        dummy = np.zeros((1, 224, 224, 3))
        model.predict(dummy, verbose=0)

    except Exception:
        STARTUP_DIAGNOSTICS["model_load_error"] = traceback.format_exc()
        print("CRITICAL: Failed to load model.")

# Call the loader immediately, but the server will run even if this fails
attempt_load_ml_libraries()


class EmbeddingGenerator(SimpleHTTPRequestHandler):
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    def do_GET(self):
        # --- Diagnostic Endpoint ---
        if self.path == '/health':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()

            # Decide overall health status
            is_healthy = (STARTUP_DIAGNOSTICS["import_error"] is None and
                          STARTUP_DIAGNOSTICS["model_load_error"] is None)

            response = {
                "status": "healthy" if is_healthy else "critical_failure",
                "diagnostics": STARTUP_DIAGNOSTICS
            }
            self.wfile.write(json.dumps(response, indent=2).encode('utf-8'))
            return

        # --- Actual Logic ---
        # If imports failed, we can't process images. Fail gracefully.
        if STARTUP_DIAGNOSTICS["import_error"] or STARTUP_DIAGNOSTICS["model_load_error"]:
             self.send_error(503, "Server improperly configured. Check /health endpoint for install errors.")
             return

        # ... (Your existing image processing code goes here) ...
        # Since 'keras', 'np', 'model' are globals now, use them directly.
        # Ensure you use 'global model' logic if needed, but reading globals is fine.

        start_time = time.time()
        image_raw_url = self.path[1:]

        #print("going to open image_raw_url:    "+image_raw_url)
        decoded_url = urllib.parse.unquote_plus(image_raw_url)

        #print("decoded url:"+decoded_url)
        img = None
        try:
            img = keras.load_img(decoded_url, target_size=(224, 224))
        except Exception as e:
            print(f"error loading image: {decoded_url} {e}")
            return

        #plt.imshow(img)
        #plt.title('SERVER SIDE INPUT Face')
        #plt.show()

        x = keras.img_to_array(img)
        x = np.expand_dims(x, axis=0)
        x = preprocess_input(x)
        feature_vector = model.predict(x)
        #print("feature_vector: "+str(feature_vector))
        # size is 62720 for MobileNetV2 "full" and reduced to 1280 with 'pooling=avg'
        # and 960 with MobileNetV3Large
        #print("MobileNet EMBEDDINGS feature vector size: "+str(feature_vector.size))

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
        try:
            bytes_sent = self.udp_socket.sendto(monitor_data.encode(), ('127.0.0.1', MONITOR_PORT))
            #print(f"UDP sent {bytes_sent} bytes to 127.0.0.1:{MONITOR_PORT}: {monitor_data}")
        except Exception as e:
            print(f"UDP send error: {e}")

    def do_POST(self):
        pass




class ReliableHTTPServer(HTTPServer):
    allow_reuse_address = True
    request_queue_size = 1024

def run_server(tcp_port, udp_port):
    global MONITOR_PORT
    MONITOR_PORT = udp_port
    print(f"Starting server on port {tcp_port}. Diagnostics available at /health")
    server_address = ('127.0.0.1', tcp_port)
    httpd = ReliableHTTPServer(server_address, EmbeddingGenerator)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    except Exception as e:
        print(f"server crashed: {e}")
    finally:
        httpd.server_close()

