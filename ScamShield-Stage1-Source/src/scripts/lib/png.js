"use strict";
// Minimal PNG reader for the test fixtures: 8-bit grayscale, RGB or RGBA, non-interlaced.
const zlib = require("node:zlib");

function decodePng(buf) {
  const sig = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  if (!buf.subarray(0, 8).equals(sig)) throw new Error("not a PNG file");
  let pos = 8, width = 0, height = 0, depth = 0, ctype = 0, interlace = 0;
  const idat = [];
  while (pos < buf.length) {
    const len = buf.readUInt32BE(pos);
    const type = buf.toString("ascii", pos + 4, pos + 8);
    const data = buf.subarray(pos + 8, pos + 8 + len);
    if (type === "IHDR") {
      width = data.readUInt32BE(0); height = data.readUInt32BE(4);
      depth = data[8]; ctype = data[9]; interlace = data[12];
    } else if (type === "IDAT") idat.push(data);
    else if (type === "IEND") break;
    pos += 12 + len;
  }
  if (depth !== 8 || interlace !== 0) throw new Error("unsupported PNG variant");
  const channels = { 0: 1, 2: 3, 6: 4 }[ctype];
  if (!channels) throw new Error("unsupported PNG colour type " + ctype);
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const stride = width * channels;
  const px = Buffer.alloc(height * stride);
  for (let y = 0; y < height; y++) {
    const ft = raw[y * (stride + 1)];
    const src = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1));
    for (let x = 0; x < stride; x++) {
      const a = x >= channels ? px[y * stride + x - channels] : 0;
      const b = y > 0 ? px[(y - 1) * stride + x] : 0;
      const c = x >= channels && y > 0 ? px[(y - 1) * stride + x - channels] : 0;
      let v = src[x];
      if (ft === 1) v += a;
      else if (ft === 2) v += b;
      else if (ft === 3) v += (a + b) >> 1;
      else if (ft === 4) {
        const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
        v += pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
      } else if (ft !== 0) throw new Error("bad PNG filter " + ft);
      px[y * stride + x] = v & 255;
    }
  }
  const rgba = new Uint8ClampedArray(width * height * 4);
  for (let i = 0; i < width * height; i++) {
    if (channels === 1) { const g = px[i]; rgba.set([g, g, g, 255], i * 4); }
    else if (channels === 3) rgba.set([px[i * 3 + 2 - 2], px[i * 3 + 1], px[i * 3 + 2], 255], i * 4);
    else rgba.set([px[i * 4], px[i * 4 + 1], px[i * 4 + 2], px[i * 4 + 3]], i * 4);
  }
  return { width, height, data: rgba };
}
module.exports = { decodePng };
