#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Server unduhan Aplikasi 7 KAIH — melayani APK & bagian-bagiannya.
Mendukung Range request (untuk file besar / resume download).
Penggunaan: python3 download-server.py [port]
"""
import http.server
import os
import re
import sys

ROOT = os.path.abspath(os.path.dirname(os.path.abspath(__file__)))
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8080

ATTACH = (".apk", ".keystore", ".part1", ".part2", ".txt", ".zip", ".jar")


class Handler(http.server.BaseHTTPRequestHandler):
    server_version = "KAIHDownload/1.0"

    def log_message(self, fmt, *args):
        sys.stderr.write("%s %s\n" % (self.address_string(), fmt % args))

    def _resolve(self):
        path = self.path.split("?", 1)[0].lstrip("/")
        full = os.path.realpath(os.path.join(ROOT, path))
        if not full.startswith(ROOT):
            return None
        if os.path.isdir(full):
            full = os.path.join(full, "index.html")
        if not os.path.isfile(full):
            return None
        return full

    def _send(self, with_body=True):
        full = self._resolve()
        if full is None:
            self.send_error(404, "Berkas tidak ditemukan")
            return
        size = os.path.getsize(full)
        rng = self.headers.get("Range")
        start, end = 0, size - 1
        if rng:
            m = re.match(r"bytes=(\d*)-(\d*)", rng)
            if m:
                s, e = m.group(1), m.group(2)
                if s:
                    start = int(s)
                if e:
                    end = min(int(e), size - 1)
        if start > end or start >= size:
            self.send_response(416)
            self.send_header("Content-Range", "bytes */%d" % size)
            self.end_headers()
            return
        length = end - start + 1
        name = os.path.basename(full)
        ext = os.path.splitext(name)[1].lower()
        if name.endswith(".apk"):
            ctype = "application/vnd.android.package-archive"
        elif name.endswith(".html") or name.endswith(".htm"):
            ctype = "text/html; charset=utf-8"
        else:
            ctype = "application/octet-stream"
        self.send_response(206 if rng else 200)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(length))
        self.send_header("Accept-Ranges", "bytes")
        if rng:
            self.send_header("Content-Range", "bytes %d-%d/%d" % (start, end, size))
        if ext in ATTACH:
            self.send_header(
                "Content-Disposition", 'attachment; filename="%s"' % name
            )
        self.end_headers()
        if with_body:
            with open(full, "rb") as f:
                f.seek(start)
                remaining = length
                while remaining > 0:
                    chunk = f.read(min(1024 * 1024, remaining))
                    if not chunk:
                        break
                    try:
                        self.wfile.write(chunk)
                    except (BrokenPipeError, ConnectionResetError):
                        break
                    remaining -= len(chunk)

    def do_GET(self):
        self._send(with_body=True)

    def do_HEAD(self):
        self._send(with_body=False)


if __name__ == "__main__":
    print("Menyajikan %s pada port %d" % (ROOT, PORT))
    http.server.ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
