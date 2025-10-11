import json
import socket
import threading
from pathlib import Path

from http.server import HTTPServer, SimpleHTTPRequestHandler
import urllib.parse
import time
import uuid
import librosa, numpy as np, openl3

SERVER_UUID = str(uuid.uuid4())  # Generate a unique ID for this server instance
MONITOR_PORT = None
_MODEL = None
_MODEL_LOCK = threading.Lock()

def get_model():
    global _MODEL
    if _MODEL is None:
        with _MODEL_LOCK:
            if _MODEL is None:
                # Adjust parameters as needed
                _MODEL = openl3.core.load_audio_embedding_model(
                    input_repr="mel256",
                    content_type="music",      # or "env"
                    embedding_size=512
                )
    return _MODEL

def compute_embedding(audio_path: Path) -> list[float]:
    if not audio_path.exists() or not audio_path.is_file():
        raise FileNotFoundError(f"Audio file not found: {audio_path}")
    # Load mono audio (OpenL3 expects 48k by default, but will resample)
    y, sr = librosa.load(str(audio_path), sr=48000, mono=True)
    model = get_model()
    emb, ts = openl3.get_audio_embedding(
        y, sr,
        input_repr="mel256",
        content_type="music",
        embedding_size=512,
        model=model
    )
    vec = emb.mean(axis=0)  # Temporal mean pooling
    norm = np.linalg.norm(vec)
    if norm > 0:
        vec = vec / norm
    # Convert to native Python floats (doubles)
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
        monitor_data = f"{SERVER_UUID},mobilenet,{image_raw_url},{processing_time:.3f}"
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

    print("Starting local OpenL3 EMBEDDINGS server on TCP port: "+str(tcp_port))
    server_address = ('localhost', tcp_port)
    httpd = HTTPServer(server_address, EmbeddingGenerator)
    httpd.socket.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
    httpd.socket.listen(1024)
    httpd.serve_forever()
