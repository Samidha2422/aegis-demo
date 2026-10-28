import os
import wave
import struct
import math
import zlib

def create_wav(filepath, duration_sec=3.0, sample_rate=16000, freq=440.0):
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    num_samples = int(duration_sec * sample_rate)
    with wave.open(filepath, 'w') as wav_file:
        wav_file.setnchannels(1) # mono
        wav_file.setsampwidth(2) # 16-bit
        wav_file.setframerate(sample_rate)
        
        frames = bytearray()
        for i in range(num_samples):
            # 440Hz sine wave with slight decay
            value = int(32767.0 * 0.5 * math.sin(2.0 * math.pi * freq * (i / sample_rate)))
            frames.extend(struct.pack('<h', value))
        wav_file.writeframes(frames)
    print(f"Created WAV: {filepath} ({num_samples} samples, {sample_rate} Hz mono)")

def create_png(filepath, width=256, height=256, r=220, g=50, b=50):
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0) # filter type 0 (None)
        for x in range(width):
            # checkerboard/pattern border
            is_border = (x < 8 or x >= width - 8 or y < 8 or y >= height - 8)
            if is_border:
                raw_data.extend([255, 255, 255])
            elif (x // 16 + y // 16) % 2 == 0:
                raw_data.extend([r, g, b])
            else:
                raw_data.extend([max(0, r - 40), max(0, g - 40), max(0, b - 40)])
                
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    header = b'\x89PNG\r\n\x1a\n'
    ihdr = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
    idat = zlib.compress(raw_data)
    
    with open(filepath, 'wb') as f:
        f.write(header)
        f.write(chunk(b'IHDR', ihdr))
        f.write(chunk(b'IDAT', idat))
        f.write(chunk(b'IEND', b''))
    print(f"Created PNG: {filepath} ({width}x{height})")

def create_calls_csv(filepath):
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write("call_id,caller_id,callee_id,timestamp,duration_secs,status,simulated\n")
        # 40 calls between 02:00 AM and 04:00 AM
        for i in range(1, 41):
            minute = (i * 3) % 60
            hour = 2 if i <= 20 else 3
            time_str = f"2026-10-28T{hour:02d}:{minute:02d}:00Z"
            status = "answered" if i in [5, 22, 38] else "missed"
            duration = 14 if status == "answered" else 0
            f.write(f"call_{i:03d},unknown_stalker,meera,{time_str},{duration},{status},true\n")
    print(f"Created CSV: {filepath} (40 overnight calls)")

if __name__ == '__main__':
    # 1. Scenarios assets
    create_wav("scenarios/assets/synthetic_audio.wav")
    create_png("scenarios/assets/proxy_nsfw.png", r=180, g=40, b=40)
    create_png("scenarios/assets/screenshot_meme.png", r=40, g=120, b=180)
    create_calls_csv("scenarios/assets/overnight_calls.csv")

    # 2. Android app assets
    create_wav("android-app/app/src/main/assets/assets/synthetic_audio.wav")
    create_png("android-app/app/src/main/assets/assets/proxy_nsfw.png", r=180, g=40, b=40)
    create_png("android-app/app/src/main/assets/assets/screenshot_meme.png", r=40, g=120, b=180)
    create_calls_csv("android-app/app/src/main/assets/assets/overnight_calls.csv")
