#!/usr/bin/env python3

import http.client
import argparse
import sys
import ast

def main():
    _ip = "127.0.0.1"
    _port = "8000"

    parser = argparse.ArgumentParser(description='Python3 script to send HTTP GET request (by default to 127.0.0:8000)', prog='HttpClient.py')
    parser.add_argument('-ip', '--ipaddress',
        help='Specify the IP address to send the request')
    parser.add_argument('-p', '--port',
        help='Specify the port to send the request')
    parser.add_argument('-c', '--chain',
        help='List of SFs in the chain to be checked')
    parser.add_argument('-v', '--verbose', action='store_true', default=False,
        help='Print complete information')

    args = parser.parse_args()
    if args.ipaddress is not None:
        _ip = args.ipaddress
    if args.port is not None:
        _port = args.port
    if args.verbose is not None:
        _verbose = args.verbose
    if args.chain is None:
        print ("--chain parameter is mandatory")
        sys.exit(-1)

    _chain = None
    hhe_forward = None
    hhe_backward = None
    expected_hhe_forward = ''
    expected_hhe_backward = ''

    try:
        _chain = ast.literal_eval(args.chain)

        separator = ''
        for sf_chain in _chain:
            expected_hhe_forward += str(separator + sf_chain)
            separator = '#'
        separator = ''
        for sf_chain in _chain[::-1]:
            expected_hhe_backward += str(separator + sf_chain)
            separator = '#'
    except:
        e = sys.exc_info()[0]
        chain_example="""--chain \"[\'firewall', \'napt44\']\" """
        print("Chain format is not correct ({})".format(e) + " example: %s" % (chain_example))
        sys.exit(-1)
    print("EXPECTED FORWARD CHAIN: " + expected_hhe_forward)
    print("EXPECTED BACKWARD CHAIN: " + expected_hhe_backward)

    try:
        #print ("ip:" + str(_ip) + " port:" + str(_port) + " verbose:" + str(_verbose))
        conn = http.client.HTTPConnection(_ip, _port, timeout=3)
        headers = { "Accept": "*/*" }
        conn.request("GET", "/index.html", "", headers)
        #conn.request("GET", "", "", headers)
        r = conn.getresponse()

        if _verbose:
            print(r.status, r.reason)
            data = r.read()
            print(str(r.getheaders()))
            print(data)

        hhe_tag = "HHE"
        hhe_forward = str(r.getheader(hhe_tag + '_1'))
        hhe_backward = str(r.getheader(hhe_tag))
        hhe_backward = hhe_backward.replace('#' * len(hhe_tag + ": \r\n"), '#', 255)
        hhe_backward = hhe_backward.replace('##', '#', 255)

        print("FORWARD CHAIN: " + hhe_forward)
        print("BACKWARD CHAIN: " + hhe_backward)

        conn.close()
    except:
        e = sys.exc_info()[0]
        print("{}".format(e) + " '%s:%s'" % (_ip, _port))
        sys.exit(-1)

    if not ((hhe_forward == expected_hhe_forward) and (hhe_backward == expected_hhe_backward)):
        print ("ERROR Wrong expected result")
        sys.exit(-1)

if __name__ == "__main__":
    main()
