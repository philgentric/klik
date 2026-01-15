import json
import socket
import time
import uuid
import urllib.parse

from http.server import HTTPServer, SimpleHTTPRequestHandler

import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Model
from tensorflow.keras.applications import MobileNetV3Large
from tensorflow.keras.applications.mobilenet_v3 import preprocess_input
from tensorflow.keras.utils import load_img, img_to_array



SERVER_UUID = str(uuid.uuid4())  # Generate a unique ID for this server instance
MONITOR_PORT = None

base_model = MobileNetV3Large(include_top=False, pooling='avg', input_shape=(224, 224, 3),weights='imagenet')
model = Model(inputs=base_model.input, outputs=base_model.output)

class EmbeddingGenerator(SimpleHTTPRequestHandler):
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    def do_GET(self):
        # --- Health Check Endpoint ---
        if self.path == '/health':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            # Return status and the UUID
            response = json.dumps({
                "status": "UP",
                "uuid": SERVER_UUID,
                "service": "MobileNet_embeddings"
            })
            self.wfile.write(response.encode('utf-8'))
            return
        # ----------------------------------

        start_time = time.time()
        image_raw_url = self.path[1:]
        #print("going to open image_raw_url:    "+image_raw_url)
        decoded_url = urllib.parse.unquote_plus(image_raw_url)

        #print("decoded url:"+decoded_url)
        img = None
        try:
            img = load_img(decoded_url, target_size=(224, 224))
        except Exception as e:
            print(f"error loading image: {decoded_url} {e}")
            return

        #plt.imshow(img)
        #plt.title('SERVER SIDE INPUT Face')
        #plt.show()

        x = img_to_array(img)
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
    allow_reuse_address = True  # Solves "Address already in use" on restart
    request_queue_size = 1024   # Sets the listen backlog correctly from the start

def run_server(tcp_port, udp_port):
    global MONITOR_PORT
    MONITOR_PORT = udp_port
    print("Starting local MobileNet EMBEDDINGS server on TCP port: " + str(tcp_port))
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
