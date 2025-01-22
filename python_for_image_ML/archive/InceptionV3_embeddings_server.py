import json
import re
import socket

from http.server import HTTPServer
from http.server import SimpleHTTPRequestHandler
import urllib.parse
import tensorflow as tf
import numpy as np
import keras

from keras.applications.inception_v3 import InceptionV3
from keras.models import Model
from keras.layers import Dense, GlobalAveragePooling2D

# create the base pre-trained model
base_model = InceptionV3(weights='imagenet', include_top=False)
#inceptionV3 = tf.keras.applications.InceptionV3(weights='imagenet', include_top=False)

class EmbeddingGenerator(SimpleHTTPRequestHandler):
    def do_GET(self):
        image_raw_url = self.path[1:]
        #print("going to open image_raw_url:    "+image_raw_url)
        decoded_url = urllib.parse.unquote_plus(image_raw_url)

        #print("decoded url:"+decoded_url)
        #img = keras.utils.load_img(decoded_url, target_size=(224, 224))
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
        #x = preprocess_input(x)
        #model = vgg19
        #model = inceptionV3
        feature_vector = base_model.predict(x)
        #print("feature_vector: "+str(feature_vector))
        print("inceptionV3 feature vector size: "+str(feature_vector.size))

        double_values = [np.float64(i) for i in x.flatten()]

        #print("values: "+str(double_values))
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
    httpd.socket.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
    httpd.socket.listen(1024)
    print("Starting local InceptionV3 IMAGE EMBEDDINGS server on port: "+str(port))
    httpd.serve_forever()
