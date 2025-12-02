import json
import socket
from pathlib import Path
from http.server import HTTPServer, SimpleHTTPRequestHandler
import urllib.parse
import time
import uuid
import chromaprint
import acoustid
import numpy as np
import wave
import os
import subprocess


SERVER_UUID = str(uuid.uuid4())  # Generate a unique ID for this server instance
MONITOR_PORT = None

"""
def get_fingerprint(wav_path):
    with wave.open(wav_path, "rb") as wf:
        sample_rate = wf.getframerate()
        num_channels = wf.getnchannels()
        num_frames = wf.getnframes()
        audio_data = wf.readframes(num_frames)
        # Convert bytes to 16-bit PCM numpy array
        audio_array = np.frombuffer(audio_data, dtype=np.int16)
        # If stereo, flatten to interleaved
        if num_channels == 2:
            audio_array = audio_array.reshape(-1, 2)
        # Flatten to 1D for chromaprint
        audio_array = audio_array.flatten()
        # Convert to bytes for chromaprint
        pcm_bytes = audio_array.tobytes()

    fp = chromaprint.Fingerprinter()
    fp.start(sample_rate, num_channels)
    fp.feed(pcm_bytes)
    fp.finish()
    fingerprint = fp.fingerprint()
    return fingerprint

"""

def compute_embedding(audio_path: Path) -> list[int]:
    if not audio_path.exists() or not audio_path.is_file():
        raise FileNotFoundError(f"Audio file not found: {audio_path}")

    trash_dir = os.path.expanduser("~/.klik/klik_privacy_screen/klik_trash")
    os.makedirs(trash_dir, exist_ok=True)
    base = os.path.splitext(os.path.basename(audio_path))[0]
    wav_path = os.path.join(trash_dir, f"{base}.wav")
    cmd = [
        "ffmpeg",
        "-y",  # overwrite output file if exists
        "-i", audio_path,
        "-acodec", "pcm_s16le",
        "-ac", "2",
        "-ar", "44100",
        wav_path
    ]
    try:
        subprocess.run(cmd, check=True, stderr=subprocess.PIPE)
    except subprocess.CalledProcessError as e:
        raise RuntimeError(f"ffmpeg error: {e.stderr.decode()}")

    #return get_fingerprint(output_path)
    print("computing chromaprint fingerprint for: "+wav_path)


    result = subprocess.run(
        ["fpcalc", "-json", wav_path],
        capture_output=True, text=True, check=True
    )
    output = json.loads(result.stdout)
    fingerprint = output["fingerprint"]
    os.remove(output_path)
    return fingerprint



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
        monitor_data = f"{SERVER_UUID},chromaprint,{decoded_url},{processing_time:.3f}"
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

    print("Starting local chromaprint EMBEDDINGS server on TCP port: "+str(tcp_port))
    server_address = ('localhost', tcp_port)
    httpd = HTTPServer(server_address, EmbeddingGenerator)
    httpd.socket.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
    httpd.socket.listen(1024)
    httpd.serve_forever()
