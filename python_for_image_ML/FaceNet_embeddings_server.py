import json
import re
import torch
from http.server import HTTPServer
from http.server import SimpleHTTPRequestHandler
import urllib.parse
import tensorflow as tf
import keras
import numpy as np
from keras.models import Model
from facenet_pytorch import InceptionResnetV1
import socket
import time
import uuid

SERVER_UUID = str(uuid.uuid4())
MONITOR_PORT = None

class Facenet_Embeddings_Generator(SimpleHTTPRequestHandler):
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    model = InceptionResnetV1(pretrained='vggface2')  # Load FaceNet model only once
    print("FaceNet Embeddings server started: vggface2 model loaded")

    def do_GET(self):
        start_time = time.time()
        image_raw_url = self.path[1:]
        #print("FaceNet Embeddings server, going to open image_raw_url:    "+image_raw_url)
        decoded_url = urllib.parse.unquote_plus(image_raw_url)

        img = keras.utils.load_img(decoded_url, target_size=(160, 160))  # FaceNet uses 160x160 input size
        #plt.imshow(img)
        #plt.title('SERVER SIDE INPUT Face')
        #plt.show()

        x = keras.utils.img_to_array(img)
        x = np.expand_dims(x, axis=0)
        x = x / 255.0  # Normalize to [0, 1] range, for faceNet

        # Convert the input data to a PyTorch tensor
        x_tensor = torch.tensor(x)
        x_tensor = x_tensor.permute(0, 3, 1, 2)   # permute the axes to (batch_size, channels, height, width)

        # Pass the input data through the model using the forward method
        self.model.eval()

        with torch.no_grad():
            feature_vector = self.model.forward(x_tensor)

        #print("FaceNet feature vector: " + str(feature_vector))

        #print("FaceNet EMBEDDINGS feature vector size: "+str(feature_vector.size()))


        # Convert the tensor to a NumPy array and flatten it
        double_values = feature_vector.detach().numpy().flatten().tolist()
        data = {'features': double_values}
        x = json.dumps(data)
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(x.encode('utf-8'))

        processing_time = (time.time() - start_time)*1000  # Convert to milliseconds
        monitor_data = f"{SERVER_UUID},facenet_embeddings,facenet,{processing_time:.3f}"
        try:
            bytes_sent = self.udp_socket.sendto(monitor_data.encode(), ('localhost', MONITOR_PORT))
            print(f"UDP sent {bytes_sent} bytes to localhost:{MONITOR_PORT}: {monitor_data}")
        except Exception as e:
            print(f"UDP send error: {e}")

def do_POST(self):
        pass

def run_server(tcp_port, udp_port):
    global MONITOR_PORT
    MONITOR_PORT = udp_port
    print("Starting local FaceNet FACE EMBEDDINGS server on TCP port: "+str(tcp_port))
    server_address = ('localhost', tcp_port)
    httpd = HTTPServer(server_address, Facenet_Embeddings_Generator)
    httpd.serve_forever()
