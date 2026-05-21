import http.server
import os
import sys

port = 8124
# Accept port from env or argv for Claude Preview integration
if 'PORT' in os.environ:
    try:
        port = int(os.environ['PORT'])
    except ValueError:
        pass
if len(sys.argv) > 1:
    try:
        port = int(sys.argv[1])
    except ValueError:
        pass

print(f"Serving on port {port}")
http.server.test(HandlerClass=http.server.SimpleHTTPRequestHandler, port=port)
