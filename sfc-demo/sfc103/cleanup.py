#!/usr/bin/python
import argparse
import requests,json
from requests.auth import HTTPBasicAuth
from subprocess import call
import time
import sys
import os

controller='192.168.1.5'
DEFAULT_PORT='8181'


USERNAME='admin'
PASSWORD='admin'

def delete(host, port, uri):
    url='http://'+host+":"+port+uri
    r = requests.delete(url, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    #r.raise_for_status()

def get_service_nodes_uri():
    return "/restconf/config/service-node:service-nodes"

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

    print "deleting service function classifiers"
    delete(controller, DEFAULT_PORT, get_service_function_classifiers_uri())
    time.sleep(1)

    print "deleting rendered service path"
    delete(controller, DEFAULT_PORT, get_rendered_service_path_uri())
    time.sleep(1)

    print "deleting service function acl"
    delete(controller, DEFAULT_PORT, get_service_function_acl_uri())
    time.sleep(1)

    print "deleting service function paths"
    delete(controller, DEFAULT_PORT, get_service_function_paths_uri())
    time.sleep(1)

    print "deleting service function metadata"
    delete(controller, DEFAULT_PORT, get_service_function_metadata_uri())
    time.sleep(1)

    print "deleting service function chains"
    delete(controller, DEFAULT_PORT, get_service_function_chains_uri())
    time.sleep(1)

    print "deleting service function forwarders"
    delete(controller, DEFAULT_PORT, get_service_function_forwarders_uri())
    time.sleep(1)

    print "deleting service functions"
    delete(controller, DEFAULT_PORT, get_service_functions_uri())
    time.sleep(1)

    print "deleting service nodes"
    delete(controller, DEFAULT_PORT, get_service_nodes_uri())
