import cv2
import sys
import os
import json
import traceback
import numpy as np
from http.server import HTTPServer
from http.server import SimpleHTTPRequestHandler
import urllib.parse
import time
import socket
import uuid
from functools import partial

SERVER_UUID = str(uuid.uuid4())
MONITOR_PORT = None
TCP_PORT = None
CONFIG_NAME = None

def register_server():
    """Register server in ~/.klikr/.privacy_screen/face_recognition_server_registry."""
    try:
        home = os.path.expanduser("~")
        registry_dir = os.path.join(home, ".klikr", ".privacy_screen", "face_recognition_server_registry")
        os.makedirs(registry_dir, exist_ok=True)

        filename = f"{CONFIG_NAME}_{SERVER_UUID}.json"
        filepath = os.path.join(registry_dir, filename)

        data = {
            "name": "Haars",
            "sub-type": CONFIG_NAME,
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

# Dictionary to map config number to face cascade classifier file
class FaceDetectionHandler(SimpleHTTPRequestHandler):
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    def do_POST(self):
        pass

    def __init__(self, request, client_address, server):
        print("Haars: "+CONFIG_NAME)
        self.face_cascade = cv2.CascadeClassifier(CONFIG_NAME)
        super().__init__(request, client_address, server)

    def do_GET(self):

        # Health check endpoint
        if self.path == '/health':
            response = {
                "name": "Haars",
                "sub-type": CONFIG_NAME,
                "port": TCP_PORT,
                "uuid": SERVER_UUID,
                "status": "healthy"
            }

            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(response, indent=2).encode('utf-8'))
            return

        start_time = time.time()
        image_raw_url = self.path[1:]
        #print("going to open image_raw_url:    "+image_raw_url)

        #decoded_url = urllib.parse.unquote(image_raw_url)
        decoded_url = urllib.parse.unquote_plus(image_raw_url)

        #print("decoded url:"+decoded_url)
        # Create a dummy image (you can replace this with your own video feed)
        img = cv2.imread(decoded_url, cv2.IMREAD_COLOR)
        # debug:
        #cv2.imshow('SERVER SIDE Face Detector INPUT image',img)
        #cv2.waitKey(0)
        #cv2.destroyAllWindows()


        # Convert the image to grayscale
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        # debug:
        #cv2.imshow('SERVER SIDE Face Detector GRAY image',gray)
        #cv2.waitKey(0)
        #cv2.destroyAllWindows()


        #normalize
        gray = cv2.normalize(gray, None, 0.0, 255.0,cv2.NORM_MINMAX)

        #print("image has been converted to grayscale, going to call face detection")


        # Detect faces in the image using the Haar cascade classifier
        faces, rejectLevels, levelWeights = self.face_cascade.detectMultiScale3(gray, scaleFactor=1.1, minNeighbors=5, outputRejectLevels=True)
        #print("detected face count: "+str(len(faces)))
        #print("rejectLevels: "+str(rejectLevels))
        #print("levelWeights: "+str(levelWeights))

        return_whole_image = False
        if (return_whole_image):
            print("whole image")

            # Draw rectangles around detected faces
            for (x, y, w, h) in faces:
                print("face detected at x: "+str(x)+", y: "+str(y)+", w: "+str(w)+", h: "+str(h))
                cv2.rectangle(gray, (x, y), (x+w, y+h), (0, 255, 0), 2)

            # Convert the output image to JPEG format
            ret, output = cv2.imencode('.png', gray)
            self.send_response(200)
            self.send_header('Content-type', 'image/png')
            self.end_headers()
            self.wfile.write(output.tobytes())
        else:

            if len(faces) > 0:

                #for i, face in enumerate(faces):
                #    rejectLevel = rejectLevels[i]
                #    levelWeight = levelWeights[i]
                #    print(f"Face {i}: Reject Level={rejectLevel:.2f}, Weight={levelWeight:.2f}")

                max_weight_index = np.argmax(levelWeights)
                max_confidence = levelWeights[max_weight_index]

                #for (x, y, w, h) in faces:
                #    print("face detected at x: "+str(x)+", y: "+str(y)+", w: "+str(w)+", h: "+str(h))

                if (max_confidence < 0.004):
                    print(" max_confidence too small "+ str(max_confidence))
                else:
                    # Extract the ROI of the most plausible face and return it as a JPEG image
                    x, y, w, h = faces[max_weight_index]
                    roi = gray[y:y+h, x:x+w]
                    #print("face detected at x: "+str(x)+", y: "+str(y)+", w: "+str(w)+", h: "+str(h))

                    # debug:
                    #cv2.imshow('SERVER SIDE Face Detector DETECTED gray Face',roi)
                    #cv2.waitKey(0)
                    #cv2.destroyAllWindows()

                    ret, out = cv2.imencode('.png', roi)
                    self.send_response(200)
                    self.send_header('Content-type', 'image/png')
                    self.end_headers()
                    self.wfile.write(out.tobytes())
            else:
                # No faces detected in the image
                pass
        processing_time = (time.time() - start_time)*1000  # Convert to milliseconds
        # Send monitoring data
        monitor_data = f"{SERVER_UUID},haar_face_detection,{CONFIG_NAME},{processing_time:.3f}"
        try:
            bytes_sent = self.udp_socket.sendto(monitor_data.encode(), ('127.0.0.1', MONITOR_PORT))
            print(f"UDP sent {bytes_sent} bytes to 127.0.0.1:{MONITOR_PORT}: {monitor_data}")
        except Exception as e:
            print(f"UDP send error: {e}")


from functools import partial

class ReliableHTTPServer(HTTPServer):
    allow_reuse_address = True  # Solves "Address already in use" on restart
    request_queue_size = 1024   # Sets the listen backlog correctly from the start

def run_server():
    global REGISTRY_FILE, TCP_PORT
    print("Starting local HAARS FACE DETECTION server with config: "+CONFIG_NAME)

    # Bind to ephemeral port to avoid fragile fixed port assignments
    server_address = ('127.0.0.1', 0)
    httpd = ReliableHTTPServer(server_address, FaceDetectionHandler)
    TCP_PORT = httpd.server_address[1]

    print(f"Bound HAARS face detection server to TCP port: {TCP_PORT}")

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
        FaceDetectionHandler.udp_socket.close()
        print("Server stopped")

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python haars_face_detection_server.py <config_name> <udp_port>")
        time.sleep(1)
        sys.exit(1)

    try:
        CONFIG_NAME = sys.argv[1].replace("'", "")
        MONITOR_PORT = int(sys.argv[2])

        run_server()
    except ValueError:
        print("Error: Ports must be integers")
        sys.exit(1)
