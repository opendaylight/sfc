#!/usr/bin/env python

## The script is also python3 compatible
## #!/usr/bin/env python3

import sys
import time

if sys.version_info[0] == 3:
    from http.server import BaseHTTPRequestHandler
    from http.server import HTTPServer
else:
    from BaseHTTPServer import BaseHTTPRequestHandler
    from BaseHTTPServer import HTTPServer

#HOST_NAME = '127.0.0.1' # 'example.net'
HOST_NAME = ""
PORT_NUMBER = 8000

class MyHandler(BaseHTTPRequestHandler):
    def do_HEAD(s):
        s.send_response(200)
        s.send_header("Content-type", "text/html")
        s.end_headers()
    def do_GET(s):
        ## Respond to a GET request
        previousHeaderEnrichment = \
            str(s.headers.getheader('HHE'))
        parse_hhe = previousHeaderEnrichment.replace('#' * len("HHE: \r\n"), '#', 255)
        parse_hhe = parse_hhe.replace('##', '#', 255)
        parse_hhe = parse_hhe.replace('##', '#', 255)
        body = 'BODY:\n' + parse_hhe

        http_payload = b''
        ## Send manually to assure the messages is sent in one packet
        http_payload = (
            'HTTP/1.0 200 OK\r\n'
            #+ 'Server: ' + s.version_string() + '\r\n'
            #+ 'Date: ' + s.date_time_string() + '\r\n'
            + 'Content-type: text/plain\r\n'
            #+ 'Content-type: text/html\r\n'
            + 'Connection: close\r\n'
            #+ 'Connection: keep-alive\r\n'
            + 'Content-lenght: ' + str(len(body)) + '\r\n'
            + 'HHE_1: ' + parse_hhe + '\r\n'
            + '\r\n'
            + body)

        s.wfile.write(http_payload)

if __name__ == '__main__':
    server_class = HTTPServer
    httpd = server_class((HOST_NAME, PORT_NUMBER), MyHandler)
    print(("{} Server Starts - {}:{}".format(time.asctime(), HOST_NAME, PORT_NUMBER)))
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()
    print(("{} Server Stops - {}:{}".format(time.asctime(), HOST_NAME, PORT_NUMBER)))
