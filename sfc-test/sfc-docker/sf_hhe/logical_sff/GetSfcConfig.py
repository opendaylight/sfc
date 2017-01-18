#!/usr/bin/env python3

import http.client
import argparse
import sys
import json

def main():
    _ip = "127.0.0.1"
    _port = "8181"

    parser = argparse.ArgumentParser(description='Python3 script to send HTTP GET request (by default to 127.0.0:8000)', prog='HttpClient.py')
    parser.add_argument('-ip', '--ipaddress',
        help='Controller IP address where is the controller (default: '
            + _ip + ')' )
    parser.add_argument('-p', '--port',
        help='Specify the port in with the Controller listen RestConf (default: '
            + _port + ')' )
    parser.add_argument('-v', '--verbose', action='store_true', default=False,
        help='Print complete information')

    args = parser.parse_args()
    if args.ipaddress is not None:
        _ip = args.ipaddress
    if args.port is not None:
        _port = args.port
    if args.verbose is not None:
        _verbose = args.verbose

    url_init = 'http://' + _ip + ':' + _port;
    list_restconf = []
    list_restconf.append(url_init + '/restconf/config/service-function-chain:service-function-chains')
    list_restconf.append(url_init + '/restconf/config/service-function-forwarder:service-function-forwarders')
    list_restconf.append(url_init + '/restconf/config/service-function-path:service-function-paths')
    list_restconf.append(url_init + '/restconf/config/service-function:service-functions')
    list_restconf.append(url_init + '/restconf/operational/rendered-service-path:rendered-service-paths')
    list_restconf.append(url_init + '/restconf/operational/service-function-forwarder:service-function-forwarders-state')
    list_restconf.append(url_init + '/restconf/operational/service-function-path:service-function-paths-state')
    list_restconf.append(url_init + '/restconf/operational/service-function:service-functions-state')

    try:
        #print ("ip:" + str(_ip) + " port:" + str(_port) + " verbose:" + str(_verbose))
        conn = http.client.HTTPConnection(_ip, _port, timeout=3)
        headers = {'content-type': 'application/json', 'authorization': 'Basic YWRtaW46YWRtaW4=', 'cache-control': 'no-cache'}

        for restconf in list_restconf:
            conn.request("GET", restconf, "", headers)
            r = conn.getresponse()

            data = r.read()
            str_data = str(data, "utf-8")
            if _verbose:
                print(restconf, r.status, r.reason)
                parsed = json.loads(str_data)
                print(json.dumps(parsed, indent=4, sort_keys=True))
            else:
                print(str_data)

        conn.close()

    except:
        e = sys.exc_info()[0]
        print("{}".format(e) + " '%s:%s'" % (_ip, _port))
        sys.exit(-1)

if __name__ == "__main__":
    main()
