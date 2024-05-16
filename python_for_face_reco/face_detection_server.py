import cv2
from http.server import HTTPServer
from http.server import SimpleHTTPRequestHandler
import urllib.parse
import numpy as np



# Dictionary to map config number to face cascade classifier file

# haarcascade_frontalface_alt_tree.xml has higher precision, detects less faces
# haarcascade_frontalface_default.xml has higher recall,  more false positives


class FaceDetectionHandler(SimpleHTTPRequestHandler):

    FACE_CASCADE_FILES = {
        1: 'haarcascade_frontalface_default.xml',
        2: 'haarcascade_frontalface_alt.xml',
        3: 'haarcascade_frontalface_alt2.xml',
        4: 'haarcascade_frontalface_alt_tree.xml',

        # Add more entries for other config numbers
    }


    def __init__(self, config_number, *args, **kwargs):
        self.config_number = config_number
        self.face_cascade = cv2.CascadeClassifier(self.FACE_CASCADE_FILES[self.config_number])
        super().__init__(*args, **kwargs)

    def do_GET(self):

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
        #rejectLevels = []
        #levelWeights = []
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


    def do_POST(self):
        pass

from functools import partial

def run_server(port, config_number):
    server_address = ('localhost', port)
    handler = partial(FaceDetectionHandler, config_number)
    httpd = HTTPServer(server_address, handler)
    print("Starting local FACE DETECTION server on port "+str(port)+ " with config: "+str(config_number))
    httpd.serve_forever()
