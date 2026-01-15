import cv2
import sys
import traceback
import numpy as np
from http.server import HTTPServer
from http.server import SimpleHTTPRequestHandler
import urllib.parse
from mtcnn import MTCNN
import time
import socket
import uuid
import json
import os

SERVER_UUID = str(uuid.uuid4())  # Generate a unique ID for this server instance
MONITOR_PORT = None

def register_server(port, uuid_str):
    """Register server in ~/.klikr/.privacy_screen/face_recognition_server_registry."""
    try:
        home = os.path.expanduser("~")
        registry_dir = os.path.join(home, ".klikr", ".privacy_screen", "face_recognition_server_registry")
        os.makedirs(registry_dir, exist_ok=True)

        filename = f"MTCNN_{uuid_str}.json"
        filepath = os.path.join(registry_dir, filename)

        data = {
            "name": "MTCNN",
            "port": port,
            "uuid": uuid_str
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

class MTCNN_FaceDetectionHandler(SimpleHTTPRequestHandler):

    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    detector = MTCNN()

    def __init__(self, *args, **kwargs):
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

class ReliableHTTPServer(HTTPServer):
    allow_reuse_address = True  # Solves "Address already in use" on restart
    request_queue_size = 1024   # Sets the listen backlog correctly from the start

def run_server(monitor_udp_port):
    global MONITOR_PORT, REGISTRY_FILE
    MONITOR_PORT = monitor_udp_port
    print("Starting local MTCNN FACE DETECTION server")

    # Bind to ephemeral port to avoid fragile fixed port assignments
    server_address = ('127.0.0.1', 0)
    httpd = ReliableHTTPServer(server_address, MTCNN_FaceDetectionHandler)
    chosen_port = httpd.server_address[1]

    print(f"Bound MTCNN face detection server to TCP port: {chosen_port}")

    # Register server using the real bound port
    REGISTRY_FILE = register_server(chosen_port, SERVER_UUID)

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
        udp_port = int(sys.argv[1])

        run_server(udp_port)
    except ValueError:
        print("Error: Ports must be integers")
        sys.exit(1)
