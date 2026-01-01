import json
#import re
import socket

from http.server import HTTPServer, SimpleHTTPRequestHandler
import urllib.parse
import tensorflow as tf
import numpy as np
import keras
from tensorflow.keras.models import Model
#from keras.applications import MobileNetV2
#from tensorflow.keras.applications.mobilenet_v2 import preprocess_input
from keras.applications import MobileNetV3Large
from tensorflow.keras.applications.mobilenet_v3 import preprocess_input

import socket
import time
import uuid

SERVER_UUID = str(uuid.uuid4())  # Generate a unique ID for this server instance
MONITOR_PORT = None

#base_model = MobileNetV2(include_top=False, pooling='avg', input_shape=(224, 224, 3),weights='imagenet')
base_model = MobileNetV3Large(include_top=False, pooling='avg', input_shape=(224, 224, 3),weights='imagenet')
#model = Model(inputs=base_model.input, outputs=base_model.get_layer('block4_pool').output)
model = Model(inputs=base_model.input, outputs=base_model.output)

class EmbeddingGenerator(SimpleHTTPRequestHandler):
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    def do_GET(self):
        start_time = time.time()
        image_raw_url = self.path[1:]
        #print("going to open image_raw_url:    "+image_raw_url)
        decoded_url = urllib.parse.unquote_plus(image_raw_url)

        #print("decoded url:"+decoded_url)
        img = None
        try:
            img = keras.utils.load_img(decoded_url, target_size=(224, 224))
        except Exception as e:
            print(f"error loading image: {decoded_url} {e}")
            return

        #plt.imshow(img)
        #plt.title('SERVER SIDE INPUT Face')
        #plt.show()

        x = keras.utils.img_to_array(img)
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


def run_server(tcp_port,udp_port):
    global MONITOR_PORT  # Need to modify the global variable
    MONITOR_PORT = udp_port

    print("Starting local MobileNet EMBEDDINGS server on TCP port: "+str(tcp_port))
    server_address = ('127.0.0.1', tcp_port)
    httpd = HTTPServer(server_address, EmbeddingGenerator)
    httpd.socket.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
    httpd.socket.listen(1024)
    httpd.serve_forever()
