#/usr/bin/python
# -*- coding: utf-8 -*-
import sys
import BaseHTTPServer  
from BaseHTTPServer import BaseHTTPRequestHandler

"""
搭配GS_Net使用的服务端代码
简单的将请求体原样返回，以便通过Burp进行修改
使用方式：python2 local-http-server.py [端口]

This is a server that cooperates with GS_Net.
It simplely send request body back, so you can intercept it with Burp.
Usage: python2 local-http-server.py [port]
"""

ServerClass = BaseHTTPServer.HTTPServer
Protocol = "HTTP/1.0"  
  
class PostHandler(BaseHTTPRequestHandler):
    def _writeheaders(self):
        print self.path
        print self.headers
        self.send_response(200);
        self.send_header('Content-type','text/html');
        self.end_headers()

    def do_POST(self):
        # Parse the form data posted
        (host,port) = self.client_address
        print 'connect from %s:%s' % (host,port)

        self._writeheaders()
        length = self.headers.getheader('content-length');
        nbytes = int(length)
        data = self.rfile.read(nbytes)
        self.wfile.write(data)


if sys.argv[1:]:  
    port = int(sys.argv[1])  
else:  
    port = 8000

server_address = ('', port)  
  
httpd = ServerClass(server_address, PostHandler)
  
sa = httpd.socket.getsockname()  
print "Serving HTTP on", sa[0], "port", sa[1], "..."

while True:
    try:
        httpd.serve_forever()
    except:
        print 'error'

