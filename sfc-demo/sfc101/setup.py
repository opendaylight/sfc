#!/usr/bin/python
import argparse
import requests,json
from requests.auth import HTTPBasicAuth
from subprocess import call
import time
import sys
import os

controller='192.168.1.4'
DEFAULT_PORT='8181'


USERNAME='admin'
PASSWORD='admin'

def get(host, port, uri):
    url='http://'+host+":"+port+uri
    r = requests.get(url, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    jsondata=json.loads(r.text)
    return jsondata

def put(host, port, uri, data, debug=False):
    '''Perform a PUT rest operation, using the URL and data provided'''

    url='http://'+host+":"+port+uri

    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "PUT %s" % url
        print json.dumps(data, indent=4, sort_keys=True)
    r = requests.put(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug == True:
        print r.text
    r.raise_for_status()
    time.sleep(5)

def post(host, port, uri, data, debug=False):
    '''Perform a POST rest operation, using the URL and data provided'''

    url='http://'+host+":"+port+uri
    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "POST %s" % url
        print json.dumps(data, indent=4, sort_keys=True)
    r = requests.post(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug == True:
        print r.text
    r.raise_for_status()
    time.sleep(5)

def get_service_functions_uri():
    return "/restconf/config/service-function:service-functions"

def get_service_functions_data():
    return {
  "service-functions": { 
    "service-function": [ 
      { 
        "name": "SF1", 
        "sf-data-plane-locator": [ 
          { 
            "name": "sf1-dpl", 
            "ip": "192.168.1.4", 
            "port": 40001, 
            "transport": "service-locator:vxlan-gpe", 
            "service-function-forwarder": "SFF1" 
          } 
        ], 
        "rest-uri": "http://192.168.1.4:5000", 
        "nsh-aware": "true", 
        "ip-mgmt-address": "192.168.1.4", 
        "type": "dpi" 
      } 
    ] 
  } 
}

def get_service_function_forwarders_uri():
    return "/restconf/config/service-function-forwarder:service-function-forwarders"

def get_service_function_forwarders_data():
    return {
  "service-function-forwarders": { 
    "service-function-forwarder": [ 
      { 
        "name": "SFF1", 
        "sff-data-plane-locator": [ 
          { 
            "name": "sff1-dpl", 
            "service-function-forwarder-ovs:ovs-bridge": {}, 
            "data-plane-locator": { 
              "port": 4789, 
              "ip": "192.168.1.4", 
              "transport": "service-locator:vxlan-gpe" 
            } 
          } 
        ], 

        "rest-uri": "http://192.168.1.4:5000", 
        "service-function-dictionary": [ 
          { 
            "name": "SF1", 
            "sff-sf-data-plane-locator": { 
              "sf-dpl-name": "sf1-dpl",
              "sff-dpl-name": "sff1-dpl"
            } 
          } 
        ], 
        "service-node": "node-103", 
        "ip-mgmt-address": "192.168.1.4" 
      } 
    ] 
  } 
}

def get_service_function_chains_uri():
    return "/restconf/config/service-function-chain:service-function-chains/"

def get_service_function_chains_data():
    return {
    "service-function-chains": {
        "service-function-chain": [
            {
                "name": "SFC1",
                "symmetric": "true",
                "sfc-service-function": [
                    {
                        "name": "dpi-abstract1",
                        "type": "dpi"
                    }
                ]
            }
        ]
    }
}

def get_service_function_paths_uri():
    return "/restconf/config/service-function-path:service-function-paths/"

def get_service_function_paths_data():
    return {
    "service-function-paths": {
        "service-function-path": [
            {
                "name": "SFP1",
                "service-chain-name": "SFC1",
                "starting-index": 255,
                "symmetric": "true",
            }
        ] 
    } 
}

def get_service_function_paths_uri():
    return "/restconf/config/service-function-path:service-function-paths/"

def get_service_function_paths_data():
    return {
  "service-function-paths": {
    "service-function-path": [
      {
        "name": "SFP1",
        "service-chain-name": "SFC1",
        "symmetric": "true"
      }
    ]
  }
}

def get_rendered_service_path_uri():
    return "/restconf/operations/rendered-service-path:create-rendered-path/"

def get_rendered_service_path_data():
    return {
    "input": {
        "name": "RSP1",
        "parent-service-function-path": "SFP1",
        "symmetric": "true"
    }
}

if __name__ == "__main__":

    print "sending service functions"
    put(controller, DEFAULT_PORT, get_service_functions_uri(), get_service_functions_data(), True)
    print "sending service function forwarders"
    put(controller, DEFAULT_PORT, get_service_function_forwarders_uri(), get_service_function_forwarders_data(), True)
    print "sending service function chains"
    put(controller, DEFAULT_PORT, get_service_function_chains_uri(), get_service_function_chains_data(), True)
    print "sending service function paths"
    put(controller, DEFAULT_PORT, get_service_function_paths_uri(), get_service_function_paths_data(), True)
    print "sending rendered service path"
    post(controller, DEFAULT_PORT, get_rendered_service_path_uri(), get_rendered_service_path_data(), True)
