import cv2
import numpy as np
from http.server import HTTPServer
from http.server import SimpleHTTPRequestHandler
import urllib.parse
from mtcnn import MTCNN
import time
import socket
import uuid
from functools import partial


SERVER_UUID = str(uuid.uuid4())  # Generate a unique ID for this server instance
MONITOR_PORT = None

#https://github.com/ipazc/mtcnn/blob/master/example.py

class MTCNN_FaceDetectionHandler(SimpleHTTPRequestHandler):

    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    detector = MTCNN()

    def __init__(self, config_name, *args, **kwargs):
        if  config_name ==  "MTCNN":
            print("MTCNN")
        else :
            print("❌ FATAL: config  not supported "+config_name)

        self.config_name = config_name
        super().__init__(*args, **kwargs)

    def do_GET(self):

        start_time = time.time()
        image_raw_url = self.path[1:]
        #print("going to open image_raw_url:    "+image_raw_url)

        #decoded_url = urllib.parse.unquote(image_raw_url)
        decoded_url = urllib.parse.unquote_plus(image_raw_url)

        #print("decoded url:"+decoded_url)
        # Create a dummy image (you can replace this with your own video feed)
        img = cv2.imread(decoded_url, cv2.IMREAD_COLOR)
        #img2 = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

        result = self.detector.detect_faces(img)

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

    def do_POST(self):
        pass

from functools import partial

def run_server(port, config_number, monitor_udp_port):
    global MONITOR_PORT
    MONITOR_PORT = monitor_udp_port

    print("Starting local MTCNN FACE DETECTION server on port "+str(port)+ " with config: "+str(config_number))
    server_address = ('127.0.0.1', port)
    handler = partial(MTCNN_FaceDetectionHandler, config_number)
    httpd = HTTPServer(server_address, handler)
    httpd.serve_forever()
