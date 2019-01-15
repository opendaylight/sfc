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

def get(host, port, uri, debug=False):
    '''Perform a GET rest operation, using the URL provided'''

    url='http://'+host+":"+port+uri

    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    r = requests.get(url, headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))

    if debug == True:
        print "GET %s" % url
    print '\nHTTP GET %s\nresult: %s' % (url, r.status_code)
    if r.status_code >= 200 and r.status_code <= 299:
        return json.loads(r.text)
    else:
        return {}

def get_service_function_acl_uri():
    return "/restconf/config/ietf-access-control-list:access-lists/"

def get_service_function_acl_data(rsp_name, rsp_rev_name):
    return  {
  "access-lists": {
    "acl": [
      {
        "acl-name": "ACL3",
        "acl-type": "ietf-access-control-list:ipv4-acl",
        "access-list-entries": {
          "ace": [
            {
              "rule-name": "ACE1",
              "actions": {
                "service-function-acl:rendered-service-path": rsp_name
              },
              "matches": {
                "destination-ipv4-network": "192.168.2.0/24",
                "source-ipv4-network": "192.168.2.0/24",
                "protocol": "6",
                "source-port-range": {
                    "lower-port": 0
                },
                "destination-port-range": {
                    "lower-port": 80
                }
              }
            }
          ]
        }
      },
      {
        "acl-name": "ACL4",
        "acl-type": "ietf-access-control-list:ipv4-acl",
        "access-list-entries": {
          "ace": [
            {
              "rule-name": "ACE2",
              "actions": {
                "service-function-acl:rendered-service-path": rsp_rev_name
              },
              "matches": {
                "destination-ipv4-network": "192.168.2.0/24",
                "source-ipv4-network": "192.168.2.0/24",
                "protocol": "6",
                "source-port-range": {
                    "lower-port": 80
                },
                "destination-port-range": {
                    "lower-port": 0
                }
              }
            }
          ]
        }
      }
    ]
  }
}

def get_service_function_classifiers_uri():
    return "/restconf/config/service-function-classifier:service-function-classifiers/"

def get_service_function_classifiers_data():
    return  {
  "service-function-classifiers": {
    "service-function-classifier": [
      {
        "name": "Classifier1",
        "scl-service-function-forwarder": [
          {
            "name": "Classifier1",
            "interface": "veth-br"
          }
        ],
        "acl": {
            "name": "ACL3",
            "type": "ietf-access-control-list:ipv4-acl"
         }
      },
      {
        "name": "Classifier2",
        "scl-service-function-forwarder": [
          {
            "name": "Classifier2",
            "interface": "veth-br"
          }
        ],
        "acl": {
            "name": "ACL4",
            "type": "ietf-access-control-list:ipv4-acl"
         }
      }
    ]
  }
}

def get_service_function_path_state(sfp_name):
    return "/restconf/config/service-function-path:service-function-paths-state/%s", (sfp_name)

def get_rsp_name(sfp_name, is_reverse=False):
    sfp_state_dict = get(controller, DEFAULT_PORT, get_service_function_path_state(sfp_name))
    rsp_list = sfp_state_dict["sfp-rendered-service-path"]

    for rsp in rsp_list:
        rsp_name = rsp["name"]
        if rsp_name.endswith("-Reverse"):
            if is_reverse:
                return rsp_name
        else:
            return rsp_name

    return ""

if __name__ == "__main__":

    rsp_name = get_rsp_name("SFP2", False)
    rsp_rev_name = get_rsp_name("SFP2", True)

    print "updating service function acl"
    put(controller, DEFAULT_PORT, get_service_function_acl_uri(), get_service_function_acl_data(rsp_name, rsp_rev_name), True)
    print "updating service function classifiers"
    put(controller, DEFAULT_PORT, get_service_function_classifiers_uri(), get_service_function_classifiers_data(), True)
