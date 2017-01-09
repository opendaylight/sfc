#!/usr/bin/python
import argparse
import requests,json, ast
from requests.auth import HTTPBasicAuth
from subprocess import call
import time
import sys
import os

controller='192.168.56.102'
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
    time.sleep(3)

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
    time.sleep(3)

def get_data(file_to_read):
    '''returns a python object (e.g. a dict) with the data read from file_to_read'''
    with open(file_to_read, 'r') as f:
        data = f.read()
    return ast.literal_eval(data)

def get_service_nodes_uri():
    return "/restconf/config/service-node:service-nodes"

def get_service_function_types_uri():
    return "/restconf/config/service-function-type:service-function-types"

def get_service_functions_uri():
    return "/restconf/config/service-function:service-functions"

def get_service_function_forwarders_uri():
    return "/restconf/config/service-function-forwarder:service-function-forwarders"

def get_service_function_chains_uri():
    return "/restconf/config/service-function-chain:service-function-chains/"

def get_service_function_paths_uri():
    return "/restconf/config/service-function-path:service-function-paths/"

def get_service_function_metadata_uri():
    return "/restconf/config/service-function-path-metadata:service-function-metadata/"

def get_service_function_paths_uri():
    return "/restconf/config/service-function-path:service-function-paths/"

def get_rendered_service_path_uri():
    return "/restconf/operations/rendered-service-path:create-rendered-path/"

def get_service_function_acl_uri():
    return "/restconf/config/ietf-access-control-list:access-lists/"

def get_service_function_classifiers_uri():
    return "/restconf/config/service-function-classifier:service-function-classifiers/"

if __name__ == "__main__":

    print "sending service nodes"
    put(controller, DEFAULT_PORT, get_service_nodes_uri(),
                    get_data('json/01-service_nodes.json'), True)
    print "sending service function types"
    put(controller, DEFAULT_PORT, get_service_function_types_uri(),
                    get_data('json/02a-service_function_types.json'), True)
    print "sending service functions"
    put(controller, DEFAULT_PORT, get_service_functions_uri(),
                    get_data('json/02b-service_functions.json'), True)
    print "sending service function forwarders"
    put(controller, DEFAULT_PORT, get_service_function_forwarders_uri(),
                    get_data('json/03-service_function_forwarders.json'), True)
    print "sending service function chains"
    put(controller, DEFAULT_PORT, get_service_function_chains_uri(),
                    get_data('json/04-service_function_chains.json'), True)
    print "sending service function metadata"
    put(controller, DEFAULT_PORT, get_service_function_metadata_uri(),
                    get_data('json/05-service_function_metadata.json'), True)
    print "sending service function paths"
    put(controller, DEFAULT_PORT, get_service_function_paths_uri(),
                    get_data('json/06-service_function_paths.json'), True)
    print "sending service function acl"
    put(controller, DEFAULT_PORT, get_service_function_acl_uri(),
                    get_data('json/07-service_function_acls.json'), True)
    print "sending rendered service path"
    post(controller, DEFAULT_PORT, get_rendered_service_path_uri(),
                     get_data('json/08-rendered_service_path.json'), True)
    print "sending service function classifiers"
    put(controller, DEFAULT_PORT, get_service_function_classifiers_uri(),
                    get_data('json/09-service_function_classifiers.json'), True)
