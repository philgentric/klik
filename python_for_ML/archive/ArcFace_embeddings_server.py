import json
import re
from http.server import HTTPServer
from http.server import SimpleHTTPRequestHandler
import urllib.parse
import numpy as np
from insightface.app import FaceAnalysis

class EmbeddingGenerator(SimpleHTTPRequestHandler):

    fa = FaceAnalysis(name='arcface', root='/path/to/model/weights')

    def do_GET(self):
        image_path = self.path[1:]
        decoded_url = urllib.parse.unquote_plus(image_raw_url)
        img = keras.utils.load_img(decoded_url, target_size=(160, 160))  # FaceNet uses 160x160 input size
        # Preprocess the image
        aligned_img, _ = fa.align_crop(img)
        # Extract face embeddings using ArcFace
        feature_vector = fa.get_embedding(aligned_img)
        double_values = feature_vector.detach().numpy().flatten().tolist()
        data = {'features': double_values}
        x = json.dumps(data)
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(x.encode('utf-8'))

    def do_POST(self):
        pass

def extract_double_values(feature_vector):
    double_values = []
    for element in np.nditer(feature_vector):
        if isinstance(element, float):  # Check if the element is a float
            double_values.append(element)
    return double_values

def parse_feature_vector(s):

    values = []
    num_str = ''
    for char in s:
        if char.isdigit():
            num_str += char
        elif char == '.' and not num_str:
            num_str += char
        elif num_str:
            values.append(float(num_str))
            num_str = ''
    if num_str:
        values.append(float(num_str))
        return values

def run_server(port):
    server_address = ('localhost', port)
    httpd = HTTPServer(server_address, EmbeddingGenerator)
    print("Starting local ArcFace IMAGE EMBEDDINGS server on port: "+str(port))
    httpd.serve_forever()
