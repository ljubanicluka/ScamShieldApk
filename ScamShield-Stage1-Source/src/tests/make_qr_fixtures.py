#!/usr/bin/env python3
"""Regenerates tests/fixtures/qr/*.png (needs opencv-python and numpy). Deterministic.
The PNGs are committed, so CI does not need OpenCV; run this only to change fixtures."""
import json, os
import cv2, numpy as np

out = os.path.join(os.path.dirname(__file__), "fixtures", "qr")
os.makedirs(out, exist_ok=True)
enc = cv2.QRCodeEncoder.create()

def qr(text, module=8, border=4):
    m = enc.encode(text)                       # 1 px per module, includes a quiet zone
    m = cv2.copyMakeBorder(m, border, border, border, border, cv2.BORDER_CONSTANT, value=255)
    return cv2.resize(m, None, fx=module, fy=module, interpolation=cv2.INTER_NEAREST)

def save(name, img):
    cv2.imwrite(os.path.join(out, name), img)

payloads = {
    "url_basic.png": "https://example.com/pay?id=123",
    "wifi.png": "WIFI:T:WPA;S:CafeNet;P:secret123;;",
    "ips_payment.png": "K:PR|V:01|C:1|R:160000000000000000|N:Test Primalac|I:RSD1500,00|SF:289|S:Racun 12",
    "sms.png": "SMSTO:+38160111222:Pozovi me",
}
for name, text in payloads.items():
    save(name, qr(text))

# Two codes side by side
a, b = qr("https://first.example/a"), qr("https://second.example/b")
h = max(a.shape[0], b.shape[0])
pad = lambda x: cv2.copyMakeBorder(x, 0, h - x.shape[0], 0, 0, cv2.BORDER_CONSTANT, value=255)
save("multi.png", np.hstack([pad(a), pad(b)]))

# Rotated 30 degrees on white
base = qr("https://rotated.example/r", module=6)
hh, ww = base.shape
M = cv2.getRotationMatrix2D((ww / 2, hh / 2), 30, 1.0)
save("rotated.png", cv2.warpAffine(base, M, (ww, hh), borderValue=255))

# Small code inside a large, busy screenshot-like image
canvas = np.full((1200, 1600), 245, np.uint8)
rng = np.random.default_rng(7)
for _ in range(60):
    x, y = int(rng.integers(0, 1500)), int(rng.integers(0, 1100))
    cv2.rectangle(canvas, (x, y), (x + int(rng.integers(20, 200)), y + int(rng.integers(5, 40))), int(rng.integers(120, 220)), -1)
small = qr("https://small.example/in-a-big-image", module=3)
canvas[700:700 + small.shape[0], 900:900 + small.shape[1]] = small
save("small_in_large.png", canvas)

# Slightly blurred
save("blurred.png", cv2.GaussianBlur(qr("https://blurred.example/b", module=6), (5, 5), 1.2))

# Light-on-dark (inverted)
save("inverted.png", 255 - qr("https://inverted.example/i"))

# No QR code: gradient plus noise
g = np.tile(np.linspace(40, 220, 400, dtype=np.uint8), (400, 1))
g = cv2.add(g, rng.integers(0, 30, g.shape, dtype=np.uint8))
save("no_qr.png", g)

# Tight crop: no quiet zone at all
save("no_quiet_zone.png", qr("https://tight.example/t", border=0))

json.dump({
    "url_basic.png": [payloads["url_basic.png"]],
    "wifi.png": [payloads["wifi.png"]],
    "ips_payment.png": [payloads["ips_payment.png"]],
    "sms.png": [payloads["sms.png"]],
    "multi.png": ["https://first.example/a", "https://second.example/b"],
    "rotated.png": ["https://rotated.example/r"],
    "small_in_large.png": ["https://small.example/in-a-big-image"],
    "blurred.png": ["https://blurred.example/b"],
    "inverted.png": ["https://inverted.example/i"],
    "no_qr.png": [],
    "no_quiet_zone.png": ["https://tight.example/t"],
}, open(os.path.join(out, "expected.json"), "w"), indent=2)
print("written", len(os.listdir(out)), "files")
