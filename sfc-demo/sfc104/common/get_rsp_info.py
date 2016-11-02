#!/usr/bin/python
import requests,json
from requests.auth import HTTPBasicAuth
import sys
import os
import time
from string import Template

controller=Template("$ODL_CONTROLLER").substitute(os.environ)
DEFAULT_PORT='8181'
USERNAME='admin'
PASSWORD='admin'

def get(host, port, uri):
    proxies = {
        "http": None,
        "https": None
    }

    url='http://'+host+":"+port+uri
    #print url
    r = requests.get(url, proxies=proxies, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    jsondata=json.loads(r.text)
    return jsondata


def get_rsp_uri(rsp):
        return "/restconf/operational/rendered-service-path:rendered-service-paths/rendered-service-path/" + rsp

if __name__ == "__main__":

    # Some sensible defaults
    if controller == None:
        sys.exit("No controller set.")

    if len(sys.argv) != 2:
        sys.exit("Usage: %s <RSP name>" % sys.argv[0])

    resp=get(controller,DEFAULT_PORT,get_rsp_uri(sys.argv[1]))
    if len(resp['rendered-service-path']) > 0:
       path=resp['rendered-service-path'][0]
       print("%s" % path['path-id'])
