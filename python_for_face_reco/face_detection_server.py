import cv2
from http.server import HTTPServer
from http.server import SimpleHTTPRequestHandler
import urllib.parse


# Load the Haar cascade classifier for face detection
face_cascade = cv2.CascadeClassifier('haarcascade_frontalface_default.xml')

class FaceDetectionHandler(SimpleHTTPRequestHandler):
    def do_GET(self):

        image_raw_url = self.path[1:]
        print("going to open image_raw_url:    "+image_raw_url)

        #decoded_url = urllib.parse.unquote(image_raw_url)
        decoded_url = urllib.parse.unquote_plus(image_raw_url)

        print("decoded url:"+decoded_url)
        # Create a dummy image (you can replace this with your own video feed)
        img = cv2.imread(decoded_url, cv2.IMREAD_COLOR)

        #plt.imshow(img)
        #plt.title('SERVER SIDE INPUT Face')
        #plt.show()

        # Convert the image to grayscale
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        print("image has been converted to grayscale, going to call face detection")

        # Detect faces in the image using the Haar cascade classifier
        faces = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5)
        print("detected face count: "+str(len(faces)))

        return_whole_image = False
        if (return_whole_image):
            print("whole image")
            # Draw rectangles around detected faces
            for (x, y, w, h) in faces:
                print("face detected at x: "+str(x)+", y: "+str(y)+", w: "+str(w)+", h: "+str(h))
                cv2.rectangle(gray, (x, y), (x+w, y+h), (0, 255, 0), 2)

            # Convert the output image to JPEG format
            ret, output = cv2.imencode('.jpg', gray)
            self.send_response(200)
            self.send_header('Content-type', 'image/jpeg')
            self.end_headers()
            self.wfile.write(output.tobytes())
        else:
            print("just one face")

            if len(faces) > 0:
                # Calculate the confidence score for each face
                confidences = [face[3] for face in faces]

                # Find the index of the face with the highest confidence score
                max_confidence_index = confidences.index(max(confidences))
                print("max_confidence: "+str(max(confidences)))

                # Extract the ROI of the most plausible face and return it as a JPEG image
                x, y, w, h = faces[max_confidence_index]
                roi = gray[y:y+h, x:x+w]
                print("face detected at x: "+str(x)+", y: "+str(y)+", w: "+str(w)+", h: "+str(h))
                print("returning face image... of size: "+str(roi.shape))
                #plt.imshow(roi, cmap='gray')
                #plt.title('SERVER SIDE Detected Face')
                #plt.show()
                ret, out = cv2.imencode('.jpg', roi)
                self.send_response(200)
                self.send_header('Content-type', 'image/jpeg')
                self.end_headers()
                self.wfile.write(out.tobytes())
            else:
                # No faces detected in the image
                pass


    def do_POST(self):
        pass

def run_server():
    server_address = ('localhost', 8000)
    httpd = HTTPServer(server_address, FaceDetectionHandler)
    print("Starting local server on port 8000...")
    httpd.serve_forever()

if __name__ == '__main__':
    run_server()