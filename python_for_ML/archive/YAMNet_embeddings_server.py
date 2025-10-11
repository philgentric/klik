import json
import socket
import threading
from pathlib import Path

from http.server import HTTPServer, SimpleHTTPRequestHandler
import urllib.parse
import time
import uuid
import librosa
import numpy as np
import tensorflow as tf
import kagglehub


SERVER_UUID = str(uuid.uuid4())  # Generate a unique ID for this server instance
MONITOR_PORT = None
_MODEL = None
_MODEL_LOCK = threading.Lock()

def get_model():
    global _MODEL
    if _MODEL is None:
        with _MODEL_LOCK:
            if _MODEL is None:
                model_path = kagglehub.model_download("google/yamnet/tensorFlow2/yamnet")
                print(f"Downloaded YAMNet model from: {model_path}")
                _MODEL = tf.keras.models.load_model(model_path)
    return _MODEL

def compute_embedding(audio_path: Path) -> list[float]:
    if not audio_path.exists() or not audio_path.is_file():
        raise FileNotFoundError(f"Audio file not found: {audio_path}")
    # Load mono 16kHz audio
    y, sr = librosa.load(str(audio_path), sr=16000, mono=True)
    model = get_model()
    waveform = tf.convert_to_tensor(y, dtype=tf.float32)
    scores, embeddings, spectrogram = model(waveform)
    vec = tf.reduce_mean(embeddings, axis=0).numpy()
    norm = np.linalg.norm(vec)
    if norm > 0:
        vec = vec / norm
    return [float(x) for x in vec]



class EmbeddingGenerator(SimpleHTTPRequestHandler):
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    def do_GET(self):
        start_time = time.time()
        try:
            song_raw_url = self.path[1:]
            #print("going to open song_raw_url:    "+song_raw_url)
            decoded_url = urllib.parse.unquote_plus(song_raw_url)
            #print("decoded url:"+decoded_url)
            audio_path = Path(decoded_url).resolve()
            vector = compute_embedding(audio_path)
            payload = {"features": vector}
            data = json.dumps(payload).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
        except FileNotFoundError as e:
            self.send_error(404, str(e))
        except Exception as e:
            self.send_error(500, f"Internal error: {e}")

        processing_time = (time.time() - start_time) * 1000  # Convert to milliseconds
        monitor_data = f"{SERVER_UUID},yamnet,{decoded_url},{processing_time:.3f}"
        try:
            bytes_sent = self.udp_socket.sendto(monitor_data.encode(), ('localhost', MONITOR_PORT))
            #print(f"UDP sent {bytes_sent} bytes to localhost:{MONITOR_PORT}: {monitor_data}")
        except Exception as e:
            print(f"UDP send error: {e}")


    def do_POST(self):
        pass


def run_server(tcp_port,udp_port):
    global MONITOR_PORT  # Need to modify the global variable
    MONITOR_PORT = udp_port

    print("Starting local YAMNet EMBEDDINGS server on TCP port: "+str(tcp_port))
    server_address = ('localhost', tcp_port)
    httpd = HTTPServer(server_address, EmbeddingGenerator)
    httpd.socket.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
    httpd.socket.listen(1024)
    httpd.serve_forever()
